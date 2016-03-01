/**
 *
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.net.httpconnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;

import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;

public class HTTPConnectionUtils {

    public final static byte R = (byte) 13;
    public final static byte N = (byte) 10;

    public static String getFileNameFromDispositionHeader(final String contentdisposition) {
        // http://greenbytes.de/tech/tc2231/
        if (!StringUtils.isEmpty(contentdisposition)) {
            if (contentdisposition.matches("(?i).*(;| |^)filename\\*.+")) {
                /* RFC2231 */
                final String encoding = new Regex(contentdisposition, "(?:;| |^)filename\\*\\s*=\\s*(.+?)''").getMatch(0);
                if (encoding == null) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Missing encoding: " + contentdisposition);
                    return null;
                }
                final String filename = new Regex(contentdisposition, "(?:;| |^)filename\\*\\s*=\\s*.+?''(.*?)($|;\\s*|;$)").getMatch(0);
                if (filename == null) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Broken/Unsupported: " + contentdisposition);
                    return null;
                } else {
                    try {
                        String ret = URLDecoder.decode(filename.trim(), encoding.trim()).trim();
                        ret = ret.replaceFirst("^" + Matcher.quoteReplacement("\\") + "+", Matcher.quoteReplacement("_"));
                        if (StringUtils.isNotEmpty(ret)) {
                            return ret;
                        } else {
                            return null;
                        }
                    } catch (final Exception e) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Decoding Error: " + filename + "|" + encoding + "|" + contentdisposition);
                        return null;
                    }
                }
            } else if (contentdisposition.matches("(?i).*(;| |^)(filename|file_name|name).+")) {
                final String special[] = new Regex(contentdisposition, "(?:;| |^)(?:filename|file_name|name)\\s*==\\?(.*?)\\?B\\?([a-z0-9+/=]+)\\?=").getRow(0);
                if (special != null) {
                    try {
                        final String base64 = special[1] != null ? special[1].trim() : null;
                        final String encoding = special[0] != null ? special[0].trim() : null;
                        String ret = URLDecoder.decode(new String(Base64.decode(base64), encoding), encoding).trim();
                        ret = ret.replaceFirst("^" + Matcher.quoteReplacement("\\") + "+", Matcher.quoteReplacement("_"));
                        if (StringUtils.isNotEmpty(ret)) {
                            return ret;
                        } else {
                            return null;
                        }
                    } catch (final Exception e) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Decoding(Base64) Error: " + contentdisposition);
                        return null;
                    }
                }
                final String filename = new Regex(contentdisposition, "(?:;| |^)(filename|file_name|name)\\s*=\\s*(\"|'|)(.*?)(\\2$|\\2;$|\\2;.)").getMatch(2);
                if (filename == null) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Broken/Unsupported: " + contentdisposition);
                } else {
                    String ret = filename.trim();
                    ret = ret.replaceFirst("^" + Matcher.quoteReplacement("\\") + "+", Matcher.quoteReplacement("_"));
                    if (StringUtils.isNotEmpty(ret)) {
                        return ret;
                    } else {
                        return null;
                    }
                }
            }
            if (contentdisposition.matches("(?i).*xfilename.*")) {
                return null;
            }
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Broken/Unsupported: " + contentdisposition);
        }
        return null;
    }

    public static ByteBuffer readheader(final InputStream in, final boolean readSingleLine) throws IOException {
        ByteBuffer bigbuffer = ByteBuffer.wrap(new byte[4096]);
        final byte[] minibuffer = new byte[1];
        int position;

        while (in.read(minibuffer) >= 0) {
            if (bigbuffer.remaining() < 1) {
                final ByteBuffer newbuffer = ByteBuffer.wrap(new byte[bigbuffer.capacity() * 2]);
                bigbuffer.flip();
                newbuffer.put(bigbuffer);
                bigbuffer = newbuffer;
            }
            bigbuffer.put(minibuffer);
            if (readSingleLine) {
                if (bigbuffer.position() >= 1) {
                    /*
                     * \n only line termination, for fucking buggy non rfc servers
                     */
                    position = bigbuffer.position();
                    if (bigbuffer.get(position - 1) == HTTPConnectionUtils.N) {
                        break;
                    }
                    if (bigbuffer.position() >= 2) {
                        /* \r\n, correct line termination */
                        if (bigbuffer.get(position - 2) == HTTPConnectionUtils.R && bigbuffer.get(position - 1) == HTTPConnectionUtils.N) {
                            break;
                        }
                    }
                }
            } else {
                if (bigbuffer.position() >= 2) {
                    position = bigbuffer.position();
                    if (bigbuffer.get(position - 2) == HTTPConnectionUtils.N && bigbuffer.get(position - 1) == HTTPConnectionUtils.N) {
                        /*
                         * \n\n for header<->content divider, or fucking buggy non rfc servers
                         */
                        break;
                    }
                    if (bigbuffer.position() >= 4) {
                        /* \r\n\r\n for header<->content divider */
                        if (bigbuffer.get(position - 4) == HTTPConnectionUtils.R && bigbuffer.get(position - 3) == HTTPConnectionUtils.N && bigbuffer.get(position - 2) == HTTPConnectionUtils.R && bigbuffer.get(position - 1) == HTTPConnectionUtils.N) {
                            break;
                        }
                    }
                }
            }
        }

        bigbuffer.flip();
        return bigbuffer;
    }

    public static InetAddress[] resolvHostIP(String host) throws IOException {
        if (StringUtils.isEmpty(host)) {
            throw new UnknownHostException("Could not resolve: -empty host-");
        }
        final String resolvHost;
        if (!host.matches("^[a-zA-Z0-9\\-\\.]+$") && Application.getJavaVersion() >= Application.JAVA16) {
            resolvHost = java.net.IDN.toASCII(host.trim());
        } else {
            /* remove spaces....so literal IP's work without resolving */
            resolvHost = host.trim();
        }
        InetAddress hosts[] = null;
        for (int resolvTry = 0; resolvTry < 2; resolvTry++) {
            try {
                /* resolv all possible ip's */
                hosts = InetAddress.getAllByName(resolvHost);
                return hosts;
            } catch (final UnknownHostException e) {
                try {
                    Thread.sleep(500);
                } catch (final InterruptedException e1) {
                    break;
                }
            }
        }
        throw new UnknownHostException("Could not resolve: -" + host + "<->" + resolvHost + "-");
    }
}
