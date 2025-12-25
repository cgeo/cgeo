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

import cgeo.geocaching.utils.TextUtils

import androidx.annotation.NonNull

import java.io.IOException
import java.util.concurrent.TimeUnit

import io.reactivex.rxjava3.core.Single
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.fail

class HttpRequestTest {

    private MockWebServer mockServer
    private String mockServerBaseUrl

    public static class JsonHolder : HttpResponse() {
        public String name
        public Int id
    }

    @Before
    public Unit before() throws IOException {
        mockServer = MockWebServer()
        mockServer.start()
        mockServerBaseUrl = mockServer.url("").url().toString()
    }

    @After
    public Unit after() throws IOException {
        mockServer.shutdown()
    }


    @Test
    public Unit simple() {
        mockServerEnqueue(MockResponse().setResponseCode(200).setBody("test"))
        val resp: HttpResponse = HttpRequest().uri(mockServerBaseUrl + "/cgeo").request().blockingGet()
        assertThat(resp.getBodyString()).isEqualTo("test")
    }

    @Test
    public Unit simpleRequestResponse() {

        mockServerEnqueue(MockResponse().setResponseCode(200).setBody("testResp"))
        val resp: HttpResponse = HttpRequest().uri(mockServerBaseUrl + "/cgeo").body("testReq").request().blockingGet()
        val req: RecordedRequest = mockServerTakeRequest()
        assertThat(req.getMethod()).isEqualTo("POST")

        assertThat(req.getBody().readUtf8()).isEqualTo("testReq")
        assertThat(resp.getBodyString()).isEqualTo("testResp")
    }

    @Test
    public Unit getRequest() {
        mockServerEnqueue(MockResponse().setResponseCode(200).setBody("testResp"))
        val resp: HttpResponse = HttpRequest().uri(mockServerBaseUrl + "/cgeo").request().blockingGet()
        val req: RecordedRequest = mockServerTakeRequest()
        assertThat(req.getMethod()).isEqualTo("GET")

        assertThat(req.getBody().readUtf8()).isEqualTo("")
        assertThat(resp.getBodyString()).isEqualTo("testResp")
    }

    @Test
    public Unit simpleJsonRequestResponse() {
        val jsonReq: JsonHolder = JsonHolder()
        jsonReq.id = 5
        jsonReq.name = "Huber"
        val jsonResp: String = "{ \"id\": 13, \"name\": \"Schmidt\" }"

        mockServerEnqueue(MockResponse().setResponseCode(200).setBody(jsonResp))
        val resp: JsonHolder = HttpRequest().uri(mockServerBaseUrl + "/cgeo").bodyJson(jsonReq).requestJson(JsonHolder.class).blockingGet()
        val req: RecordedRequest = mockServerTakeRequest()

        assertThat(req.getMethod()).isEqualTo("POST")

        val respJson: String = TextUtils.replaceWhitespace(req.getBody().readUtf8())
        assertThat(respJson).contains("\"id\":5")
        assertThat(respJson).contains("\"name\":\"Huber\"")
        assertThat(resp.id).isEqualTo(13)
        assertThat(resp.name).isEqualTo("Schmidt")
        assertThat(TextUtils.replaceWhitespace(resp.getBodyString())).isEqualTo(TextUtils.replaceWhitespace(jsonResp))
    }

    @Test
    public Unit headers() {
        mockServerEnqueue(MockResponse().setResponseCode(200).setBody("testResp").addHeader("h1", "v1"))
        val resp: HttpResponse = HttpRequest().uri(mockServerBaseUrl + "/cgeo").headers("reqh1", "reqv1").request().blockingGet()
        val req: RecordedRequest = mockServerTakeRequest()
        assertThat(req.getMethod()).isEqualTo("GET")

        assertThat(req.getHeader("reqh1")).isEqualTo("reqv1")
        assertThat(resp.getResponse().headers("h1").get(0)).isEqualTo("v1")
    }

    @Test
    public Unit requestPrepare() {
        mockServerEnqueue(MockResponse().setResponseCode(200))
        val resp: HttpResponse = HttpRequest().uri(mockServerBaseUrl + "/cgeo")
                .headers("h1", "v1")
                .requestPreparer(r -> {
                    r.addHeader("h2", "v2")
                    return Single.just(r)
                }).request().blockingGet()
        assertThat(resp).isNotNull()
        val req: RecordedRequest = mockServerTakeRequest()
        assertThat(req.getHeader("h1")).isEqualTo("v1")
        assertThat(req.getHeader("h2")).isEqualTo("v2")
    }

    private Unit mockServerEnqueue(final MockResponse response) {
        mockServer.enqueue(response)
    }

    private RecordedRequest mockServerTakeRequest() {
        try {
            val req: RecordedRequest = mockServer.takeRequest(1000, TimeUnit.MILLISECONDS)
            assertThat(req).as("No Request present on mock server").isNotNull()
            return req
        } catch (InterruptedException e) {
            fail("Timeout waiting for request on mock server")
            throw IllegalArgumentException("This exception throw will never be reached")
        }
    }

}
