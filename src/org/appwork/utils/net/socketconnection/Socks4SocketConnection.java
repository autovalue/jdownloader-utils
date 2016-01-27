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
package org.appwork.utils.net.socketconnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.ProxyAuthException;
import org.appwork.utils.net.httpconnection.ProxyConnectException;
import org.appwork.utils.net.httpconnection.ProxyEndpointConnectException;
import org.appwork.utils.net.httpconnection.SocksHTTPconnection.DESTTYPE;

/**
 * @author daniel
 *
 */
public class Socks4SocketConnection extends SocketConnection {

    private final DESTTYPE destType;

    public DESTTYPE getDestType() {
        return this.destType;
    }

    public Socks4SocketConnection(HTTPProxy proxy, DESTTYPE destType) {
        super(proxy);
        if (proxy == null || !HTTPProxy.TYPE.SOCKS4.equals(proxy.getType())) {
            throw new IllegalArgumentException("proxy must be of type socks4");
        }
        this.destType = destType;
    }

    @Override
    protected Socket connectProxySocket(final Socket proxySocket, final SocketAddress endPoint, final StringBuffer logger) throws IOException {
        final HTTPProxy proxy = getProxy();
        try {
            Socks4SocketConnection.sayHello(proxySocket, logger);
            return Socks4SocketConnection.establishConnection(proxySocket, proxy.getUser(), endPoint, this.getDestType(), logger);
        } catch (final InvalidAuthException e) {
            throw new ProxyAuthException(e, proxy);
        } catch (final EndpointConnectException e) {
            throw new ProxyEndpointConnectException(e, proxy, endPoint);
        } catch (final IOException e) {
            throw new ProxyConnectException(e, proxy);
        }
    }

    public static Socket establishConnection(final Socket proxySocket, final String userID, final SocketAddress endPoint, DESTTYPE destType, final StringBuffer logger) throws IOException {
        final InetSocketAddress endPointAddress = (InetSocketAddress) endPoint;
        final OutputStream os = proxySocket.getOutputStream();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write((byte) 1);
        /* send port */
        /* network byte order */
        final int port = endPointAddress.getPort();
        bos.write(port >> 8 & 0xff);
        bos.write(port & 0xff);
        /* send ipv4/domain */
        switch (destType) {
        case IPV4:
            final InetAddress address = endPointAddress.getAddress();
            if (address != null) {
                if (logger != null) {
                    logger.append("->SEND tcp connect request by ipv4:" + address.getHostAddress() + "\r\n");
                }
                bos.write(address.getAddress());
                break;
            } else {
                if (logger != null) {
                    logger.append("->Cannot connect request by ipv4 (unresolved)\r\n");
                }
            }
        case DOMAIN:
            destType = DESTTYPE.DOMAIN;
            /* we use domain */
            bos.write((byte) 0);
            bos.write((byte) 0);
            bos.write((byte) 0);
            bos.write((byte) 100);
            if (logger != null) {
                logger.append("->SEND tcp connect request by domain:" + SocketConnection.getHostName(endPointAddress) + "\r\n");
            }
            break;
        default:
            throw new IllegalArgumentException("Unsupported destType");
        }
        /* send user ID string */
        if (userID != null && userID.length() > 0) {
            bos.write(userID.getBytes("ISO-8859-1"));
        }
        /* NULL/end */
        bos.write((byte) 0);
        if (DESTTYPE.DOMAIN.equals(destType)) {
            final byte[] domainBytes = SocketConnection.getHostName(endPointAddress).getBytes("ISO-8859-1");
            /* send domain as string,socks4a */
            bos.write(domainBytes);
            /* NULL/end */
            bos.write((byte) 0);
        }
        bos.writeTo(os);
        os.flush();
        /* read response, 8 bytes */
        final InputStream is = proxySocket.getInputStream();
        final byte[] resp = SocketConnection.ensureRead(is, 2, null);
        if (resp[0] != 0) {
            throw new IOException("Invalid response:" + resp[0]);
        }
        switch (resp[1]) {
        case 0x5a:
            break;
        case 0x5b:
            throw new EndpointConnectException("Socks4 request rejected or failed");
        case 0x5c:
            throw new EndpointConnectException("Socks4 request failed because client is not running identd (or not reachable from the server)");
        case 0x5d:
            throw new EndpointConnectException("Socks4 request failed because client's identd could not confirm the user ID string in the request");
        default:
            throw new EndpointConnectException("Socks4 could not establish connection, status=" + resp[1]);
        }
        /* port */
        final byte[] connectedPort = SocketConnection.ensureRead(is, 2, null);
        /* ip4v response */
        final byte[] connectedIP = SocketConnection.ensureRead(is, 4, null);
        if (logger != null) {
            logger.append("<-BOUND IP:" + InetAddress.getByAddress(connectedIP) + ":" + ByteBuffer.wrap(connectedPort).getShort() + "\r\n");
        }
        return proxySocket;
    }

    public static void sayHello(final Socket proxySocket, final StringBuffer logger) throws IOException {
        final OutputStream os = proxySocket.getOutputStream();
        if (logger != null) {
            logger.append("->SOCKS4 Hello\r\n");
        }
        /* socks4 */
        os.write((byte) 4);
    }

}
