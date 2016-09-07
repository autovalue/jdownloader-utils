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

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPConnectionImpl;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.ProxyAuthException;
import org.appwork.utils.net.httpconnection.ProxyConnectException;
import org.appwork.utils.net.httpconnection.ProxyEndpointConnectException;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;

/**
 * @author daniel
 *
 */
public class DirectSocketConnection extends SocketConnection {
    public DirectSocketConnection(HTTPProxy proxy) {
        super(proxy);
        if (proxy == null || !proxy.isLocal()) {
            throw new IllegalArgumentException("proxy must be of type none/direct");
        }
    }

    public DirectSocketConnection() {
        this(HTTPProxy.NONE);
    }

    @Override
    public void connect(final SocketAddress endPoint, final int connectTimeout, final StringBuffer logger) throws IOException {
        try {
            try {
                if (connectTimeout == 0) {
                    /** no workaround for infinite connect timeouts **/
                    final SocketStreamInterface connectSocket = this.createConnectSocket(connectTimeout);
                    connect(connectSocket, endPoint, connectTimeout);
                } else {
                    /**
                     * workaround for too early connect timeouts
                     */
                    int connectTimeoutWorkaround = connectTimeout;
                    while (true) {
                        final long beforeConnect = System.currentTimeMillis();
                        try {
                            final SocketStreamInterface connectSocket = this.createConnectSocket(connectTimeout);
                            connect(connectSocket, endPoint, connectTimeout);
                            break;
                        } catch (final ConnectException cE) {
                            closeConnectSocket();
                            if (StringUtils.containsIgnoreCase(cE.getMessage(), "timed out")) {
                                int timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                if (timeout < 1000) {
                                    System.out.println("Too Fast ConnectTimeout(Normal): " + timeout + "->Wait " + (2000 - timeout));
                                    try {
                                        Thread.sleep(2000 - timeout);
                                    } catch (final InterruptedException ie) {
                                        throw cE;
                                    }
                                    timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                }
                                final int lastConnectTimeout = connectTimeoutWorkaround;
                                connectTimeoutWorkaround = Math.max(0, connectTimeoutWorkaround - timeout);
                                if (connectTimeoutWorkaround == 0 || Thread.currentThread().isInterrupted()) {
                                    throw cE;
                                }
                                System.out.println("Workaround for ConnectTimeout(Normal): " + lastConnectTimeout + ">" + timeout);
                            } else {
                                throw cE;
                            }
                        } catch (final SocketTimeoutException sTE) {
                            closeConnectSocket();
                            if (StringUtils.containsIgnoreCase(sTE.getMessage(), "timed out")) {
                                int timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                if (timeout < 1000) {
                                    System.out.println("Too Fast ConnectTimeout(Interrupted): " + timeout + "->Wait " + (2000 - timeout));
                                    try {
                                        Thread.sleep(2000 - timeout);
                                    } catch (final InterruptedException ie) {
                                        throw sTE;
                                    }
                                    timeout = (int) (System.currentTimeMillis() - beforeConnect);
                                }
                                final int lastConnectTimeout = connectTimeoutWorkaround;
                                connectTimeoutWorkaround = Math.max(0, connectTimeoutWorkaround - timeout);
                                if (connectTimeoutWorkaround == 0 || Thread.currentThread().isInterrupted()) {
                                    throw sTE;
                                }
                                System.out.println("Workaround for ConnectTimeout(Interrupted): " + lastConnectTimeout + ">" + timeout);
                            } else {
                                throw sTE;
                            }
                        }
                    }
                }
            } catch (final IOException e) {
                throw new ProxyEndpointConnectException(e, this.getProxy(), endPoint);
            }
            final SocketStreamInterface connectedSocket = this.connectProxySocket(this.getConnectSocket(), endPoint, logger);
            if (connectedSocket != null) {
                this.proxySocket = connectedSocket;
                return;
            }
            throw new ProxyEndpointConnectException(this.getProxy(), endPoint);
        } catch (final ProxyAuthException e) {
            throw e;
        } catch (final ProxyConnectException e) {
            throw e;
        } catch (final IOException e) {
            throw new ProxyConnectException(e, this.getProxy());
        } finally {
            if (this.proxySocket == null) {
                this.closeConnectSocket();
            }
        }
    }

    @Override
    protected SocketStreamInterface createConnectSocket(int connectTimeout) throws IOException {
        final SocketStreamInterface socket = super.createConnectSocket(connectTimeout);
        if (this.getProxy().isDirect()) {
            try {
                if (socket.getSocket() != null) {
                    socket.getSocket().bind(new InetSocketAddress(HTTPConnectionImpl.getDirectInetAddress(getProxy()), 0));
                }
            } catch (IOException e) {
                socket.close();
                throw e;
            }
        }
        return socket;
    }

    @Override
    protected SocketStreamInterface connectProxySocket(SocketStreamInterface proxySocket, SocketAddress endpoint, StringBuffer logger) throws IOException {
        return proxySocket;
    }
}
