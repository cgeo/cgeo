package cgeo.geocaching.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import io.reactivex.rxjava3.core.Single;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NetworkResponseCloseTest {

    private static class TrackingBody extends ResponseBody {
        private boolean closed = false;
        private final BufferedSource source = Okio.buffer(new ForwardingSource(new Buffer().writeUtf8("<html></html>")) {
            @Override
            public void close() throws IOException {
                closed = true;
                super.close();
            }
        });

        @Nullable
        @Override
        public MediaType contentType() {
            return MediaType.parse("text/html");
        }

        @Override
        public long contentLength() {
            return -1;
        }

        @NonNull
        @Override
        public BufferedSource source() {
            return source;
        }
    }

    private static Response response(final int code, final TrackingBody body) {
        return new Response.Builder()
                .request(new Request.Builder().url("https://example.com/").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("msg")
                .header("content-type", "text/html")
                .body(body)
                .build();
    }

    @Test
    public void withSuccessClosesUnsuccessfulResponse() {
        final TrackingBody body = new TrackingBody();
        assertThatThrownBy(() -> Single.just(response(404, body)).flatMap(Network.withSuccess).blockingGet()).hasCauseInstanceOf(IOException.class);
        assertThat(body.closed).isTrue();
    }

    @Test
    public void withSuccessKeepsSuccessfulResponseOpen() {
        final TrackingBody body = new TrackingBody();
        Single.just(response(200, body)).flatMap(Network.withSuccess).blockingGet();
        assertThat(body.closed).isFalse();
    }

    @Test
    public void completeWithSuccessClosesSuccessfulResponse() {
        final TrackingBody body = new TrackingBody();
        Network.completeWithSuccess(Single.just(response(200, body))).blockingAwait();
        assertThat(body.closed).isTrue();
    }

    @Test
    public void completeWithSuccessClosesUnsuccessfulResponse() {
        final TrackingBody body = new TrackingBody();
        assertThatThrownBy(() -> Network.completeWithSuccess(Single.just(response(404, body))).blockingAwait()).hasCauseInstanceOf(IOException.class);
        assertThat(body.closed).isTrue();
    }

    @Test
    public void getResponseStreamClosesUnsuccessfulResponse() {
        final TrackingBody body = new TrackingBody();
        assertThat(Network.getResponseStream(Single.just(response(404, body)))).isNull();
        assertThat(body.closed).isTrue();
    }

    @Test
    public void getResponseDataClosesUnsuccessfulResponse() {
        final TrackingBody body = new TrackingBody();
        assertThat(Network.getResponseData(response(404, body), false)).isNull();
        assertThat(body.closed).isTrue();
    }

    @Test
    public void getResponseDataSingleClosesUnsuccessfulResponse() {
        final TrackingBody body = new TrackingBody();
        assertThat(Network.getResponseData(Single.just(response(404, body)), false)).isNull();
        assertThat(body.closed).isTrue();
    }

    @Test
    public void getResponseDocumentClosesUnsuccessfulResponse() {
        final TrackingBody body = new TrackingBody();
        assertThatThrownBy(() -> Network.getResponseDocument(Single.just(response(404, body))).blockingGet()).hasCauseInstanceOf(IOException.class);
        assertThat(body.closed).isTrue();
    }
}
