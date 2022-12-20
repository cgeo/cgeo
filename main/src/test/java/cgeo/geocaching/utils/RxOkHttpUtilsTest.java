package cgeo.geocaching.utils;

import java.net.ConnectException;
import java.net.ServerSocket;

import io.reactivex.rxjava3.core.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class RxOkHttpUtilsTest {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().followSslRedirects(true).build();
    private int port;

    @Before
    public void init() throws Exception {
        // Get a port with nobod listening
        final ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();
    }

    private static Single<Response> get(final String url) {
        return RxOkHttpUtils.request(CLIENT, new Request.Builder().url(url).build());
    }

    @Test
    public void testSuccessfulRequest() throws Exception {
        assertThat(get("https://google.com").blockingGet().code()).isEqualTo(200);
    }

    @Test
    public void testUnsuccessfulRequest() throws Exception {
        assertThat(get("https://google.com/non-existent").blockingGet().code()).isEqualTo(404);
    }

    @Test
    public void testErrorConnect() throws Exception {
        try {
            get("https://127.0.0.1:" + port + "/").blockingGet();
            fail("Request should end in error");
        } catch (final RuntimeException e) {
            assertThat(e.getCause()).isInstanceOf(ConnectException.class);
        }
    }

}
