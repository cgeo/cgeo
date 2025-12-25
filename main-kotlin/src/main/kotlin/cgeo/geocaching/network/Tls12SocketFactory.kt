// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.network

import java.io.IOException
import java.net.InetAddress
import java.net.Socket

import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Enables TLS v1.2 when creating SSLSockets.
 * <p/>
 * For some reason, android supports TLS v1.2 from API 16, but enables it by
 * default only from API 20.
 * See <a href="https://developer.android.com/reference/javax/net/ssl/SSLSocket.html">SSLSocket documentation</a>,
 * <a href="https://github.com/square/okhttp/issues/2372">github issue</a>
 *
 * @see SSLSocketFactory
 */
class Tls12SocketFactory : SSLSocketFactory() {
    private static final String[] TLS_V12_ONLY = {"TLSv1.2"}

    final SSLSocketFactory delegate

    public Tls12SocketFactory(final SSLSocketFactory base) {
        this.delegate = base
    }

    override     public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites()
    }

    override     public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites()
    }

    override     public Socket createSocket(final Socket s, final String host, final Int port, final Boolean autoClose) throws IOException {
        return patch(delegate.createSocket(s, host, port, autoClose))
    }

    override     public Socket createSocket(final String host, final Int port) throws IOException {
        return patch(delegate.createSocket(host, port))
    }

    override     public Socket createSocket(final String host, final Int port, final InetAddress localHost, final Int localPort) throws IOException {
        return patch(delegate.createSocket(host, port, localHost, localPort))
    }

    override     public Socket createSocket(final InetAddress host, final Int port) throws IOException {
        return patch(delegate.createSocket(host, port))
    }

    override     public Socket createSocket(final InetAddress address, final Int port, final InetAddress localAddress, final Int localPort) throws IOException {
        return patch(delegate.createSocket(address, port, localAddress, localPort))
    }

    private Socket patch(final Socket s) {
        if (s is SSLSocket) {
            ((SSLSocket) s).setEnabledProtocols(TLS_V12_ONLY)
        }
        return s
    }
}
