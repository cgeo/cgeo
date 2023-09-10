package cgeo.geocaching.network;

import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Convenience class used to encapsulate the building and usage of a HtmlRequest
 * Can be used directly or as base class for own (JSON) response classes
 */
public class HttpResponse {

    @JsonIgnore
    private Response response;

    @JsonIgnore
    private boolean bodyConsumed = false;
    @JsonIgnore
    private String bodyString;
    @JsonIgnore
    private boolean failed;
    @JsonIgnore
    private Exception failedException;


    /**
     * to be used by HtmlRequest only
     */
    protected HttpResponse() {
        //empty on purpose
    }

    @JsonIgnore
    public boolean isSuccessful() {
        return response.isSuccessful();
    }

    /**
     * to be used by HtmlRequest only
     */
    protected void setResponse(final Response response) {
        this.response = response;
    }

    protected void setFromHttpResponse(final HttpResponse other) {
        this.response = other.response;
        this.bodyString = other.bodyString;
        this.bodyConsumed = other.bodyConsumed;
        this.failed = failed;
        this.failedException = failedException;
    }

    protected void setFailed(final Exception exception) {
        this.failed = true;
        this.failedException = exception;
    }

    public <T> T parseJson(final Class<T> clazz, final T defaultValue) {
        try {
            return JsonUtils.mapper.readValue(getBodyString(), clazz);
        } catch (JsonProcessingException jpe) {
            return defaultValue;
        }
    }


    public Response getResponse() {
        return response;
    }

    public String getBodyString() {
        if (!bodyConsumed) {
            bodyString = getBodyString(this.response);
            bodyConsumed = true;
        }
        return bodyString;
    }

    /**
     * to be used by HtmlRequest only
     */
    private static String getBodyString(final Response response) {
        final ResponseBody body = response.body();
        if (body != null) {
            try {
                return body.string();
            } catch (IOException ie) {
                Log.e("Could not consume body string of " + response, ie);
            }
        }
        return null;
    }

}
