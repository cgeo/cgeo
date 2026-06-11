package cgeo.geocaching.network;

import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class HttpRequestTest {

    private MockWebServer mockServer;
    private String mockServerBaseUrl;

    public static class JsonHolder extends HttpResponse {
        public String name;
        public int id;
    }

    @Before
    public void before() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
        mockServerBaseUrl = mockServer.url("").url().toString();
    }

    @After
    public void after() throws IOException {
        mockServer.close();
    }

    @Test
    public void simple() {
        mockServerEnqueue(new MockResponse.Builder().code(200).body("test").build());
        final HttpResponse resp = new HttpRequest().uri(mockServerBaseUrl + "/cgeo").request().blockingGet();
        assertThat(resp.getBodyString()).isEqualTo("test");
    }

    @Test
    public void simpleRequestResponse() {

        mockServerEnqueue(new MockResponse.Builder().code(200).body("testResp").build());
        final HttpResponse resp = new HttpRequest().uri(mockServerBaseUrl + "/cgeo").body("testReq").request().blockingGet();
        final RecordedRequest req = mockServerTakeRequest();
        assertThat(req.getMethod()).isEqualTo("POST");

        assertThat(req.getBody() != null ? req.getBody().utf8() : "").isEqualTo("testReq");
        assertThat(resp.getBodyString()).isEqualTo("testResp");
    }

    @Test
    public void getRequest() {
        mockServerEnqueue(new MockResponse.Builder().code(200).body("testResp").build());
        final HttpResponse resp = new HttpRequest().uri(mockServerBaseUrl + "/cgeo").request().blockingGet();
        final RecordedRequest req = mockServerTakeRequest();
        assertThat(req.getMethod()).isEqualTo("GET");

        assertThat(req.getBody() != null ? req.getBody().utf8() : "").isEqualTo("");
        assertThat(resp.getBodyString()).isEqualTo("testResp");
    }

    @Test
    public void simpleJsonRequestResponse() {
        final JsonHolder jsonReq = new JsonHolder();
        jsonReq.id = 5;
        jsonReq.name = "Huber";
        final String jsonResp = "{ \"id\": 13, \"name\": \"Schmidt\" }";

        mockServerEnqueue(new MockResponse.Builder().code(200).body(jsonResp).build());
        final JsonHolder resp = new HttpRequest().uri(mockServerBaseUrl + "/cgeo").bodyJson(jsonReq).requestJson(JsonHolder.class).blockingGet();
        final RecordedRequest req = mockServerTakeRequest();

        assertThat(req.getMethod()).isEqualTo("POST");

        final String respJson = TextUtils.replaceWhitespace(req.getBody() != null ? req.getBody().utf8() : "");
        assertThat(respJson).contains("\"id\":5");
        assertThat(respJson).contains("\"name\":\"Huber\"");
        assertThat(resp.id).isEqualTo(13);
        assertThat(resp.name).isEqualTo("Schmidt");
        assertThat(TextUtils.replaceWhitespace(resp.getBodyString())).isEqualTo(TextUtils.replaceWhitespace(jsonResp));
    }

    @Test
    public void headers() {
        mockServerEnqueue(new MockResponse.Builder().code(200).body("testResp").addHeader("h1", "v1").build());
        final HttpResponse resp = new HttpRequest().uri(mockServerBaseUrl + "/cgeo").headers("reqh1", "reqv1").request().blockingGet();
        final RecordedRequest req = mockServerTakeRequest();
        assertThat(req.getMethod()).isEqualTo("GET");

        assertThat(req.getHeaders().get("reqh1")).isEqualTo("reqv1");
        assertThat(resp.getResponse().headers("h1").get(0)).isEqualTo("v1");
    }

    @Test
    public void requestPrepare() {
        mockServerEnqueue(new MockResponse.Builder().code(200).build());
        final HttpResponse resp = new HttpRequest().uri(mockServerBaseUrl + "/cgeo")
                .headers("h1", "v1")
                .requestPreparer(r -> {
                    r.addHeader("h2", "v2");
                    return Single.just(r);
                }).request().blockingGet();
        assertThat(resp).isNotNull();
        final RecordedRequest req = mockServerTakeRequest();
        assertThat(req.getHeaders().get("h1")).isEqualTo("v1");
        assertThat(req.getHeaders().get("h2")).isEqualTo("v2");
    }

    private void mockServerEnqueue(final MockResponse response) {
        mockServer.enqueue(response);
    }

    @NonNull
    private RecordedRequest mockServerTakeRequest() {
        try {
            final RecordedRequest req = mockServer.takeRequest(1000, TimeUnit.MILLISECONDS);
            assertThat(req).as("No Request present on mock server").isNotNull();
            return req;
        } catch (InterruptedException e) {
            fail("Timeout waiting for request on mock server");
            throw new IllegalArgumentException("This exception throw will never be reached");
        }
    }
}
