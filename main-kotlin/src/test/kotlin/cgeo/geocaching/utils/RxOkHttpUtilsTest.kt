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

package cgeo.geocaching.utils

import java.net.ConnectException
import java.net.ServerSocket

import io.reactivex.rxjava3.core.Single
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.Java6Assertions.assertThat

class RxOkHttpUtilsTest {

    private static val CLIENT: OkHttpClient = OkHttpClient.Builder().followSslRedirects(true).build()
    private Int port

    @Before
    public Unit init() throws Exception {
        // Get a port with nobod listening
        val serverSocket: ServerSocket = ServerSocket(0)
        port = serverSocket.getLocalPort()
        serverSocket.close()
    }

    private static Single<Response> get(final String url) {
        return RxOkHttpUtils.request(CLIENT, Request.Builder().url(url).build())
    }

    @Test
    public Unit testSuccessfulRequest() throws Exception {
        assertThat(get("https://google.com").blockingGet().code()).isEqualTo(200)
    }

    @Test
    public Unit testUnsuccessfulRequest() throws Exception {
        assertThat(get("https://google.com/non-existent").blockingGet().code()).isEqualTo(404)
    }

    @Test
    public Unit testErrorConnect() throws Exception {
        try {
            get("https://127.0.0.1:" + port + "/").blockingGet()
            fail("Request should end in error")
        } catch (final RuntimeException e) {
            assertThat(e.getCause()).isInstanceOf(ConnectException.class)
        }
    }

}
