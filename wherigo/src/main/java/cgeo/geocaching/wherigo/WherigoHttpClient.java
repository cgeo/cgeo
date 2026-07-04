package cgeo.geocaching.wherigo;

import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.network.HttpResponse;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.wherigo.openwig.platform.HttpClient;
import cgeo.geocaching.wherigo.openwig.platform.HttpResult;

import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.Headers;

/**
 * {@link HttpClient} implementation backed by c:geo's own OkHttp-based network stack
 * (cgeo.geocaching.network.*), so cartridge-triggered requests behave the same way (proxy,
 * timeouts, any interceptors) as the rest of the app's networking. This class - unlike the
 * openwig engine it serves - is allowed to depend on the main module, matching how
 * WherigoDownloader already does.
 */
public class WherigoHttpClient implements HttpClient {

    public HttpResult get(final String url, final Map<String, String> headers) {
        final HttpRequest request = new HttpRequest().uri(url).method(HttpRequest.Method.GET);
        addHeaders(request, headers);
        return execute(request);
    }

    public HttpResult post(final String url, final String body, final Map<String, String> headers) {
        final HttpRequest request = new HttpRequest().uri(url).method(HttpRequest.Method.POST);
        if (body != null) {
            request.body(body);
        }
        addHeaders(request, headers);
        return execute(request);
    }

    private static void addHeaders(final HttpRequest request, final Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        final Parameters params = new Parameters();
        for (final Map.Entry<String, String> entry : headers.entrySet()) {
            params.add(entry.getKey(), entry.getValue());
        }
        request.headers(params);
    }

    private static HttpResult execute(final HttpRequest request) {
        try (HttpResponse response = request.request().blockingGet()) {
            final Map<String, String> responseHeaders = new LinkedHashMap<>();
            final Headers rawHeaders = response.getResponse().headers();
            for (final String name : rawHeaders.names()) {
                responseHeaders.put(name, rawHeaders.get(name));
            }
            return new HttpResult(response.getStatusCode(), response.getBodyString(), responseHeaders);
        }
    }
}
