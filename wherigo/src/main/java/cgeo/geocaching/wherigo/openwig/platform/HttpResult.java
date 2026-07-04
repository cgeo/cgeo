package cgeo.geocaching.wherigo.openwig.platform;

import java.util.Map;

/** Result of a synchronous HTTP request made via {@link HttpClient}. */
public final class HttpResult {

    public final int statusCode;
    public final String body;
    public final Map<String, String> headers;

    public HttpResult(final int statusCode, final String body, final Map<String, String> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }
}
