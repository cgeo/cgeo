package cgeo.geocaching.network;

import cgeo.geocaching.utils.Log;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Convenience class used to ensapculate the building and usage of a HtmlRequest
 *
 * Can be used directly or as base class for own (JSON) reqponse classes
 */
public class HttpResponse {

    @JsonIgnore
    private Response response;

    @JsonIgnore
    private boolean bodyConsumed = false;
    @JsonIgnore
    private String bodyString;

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

    /**
     * to be used by HtmlRequest only
     */
    protected void setBodyString(final String bodyString) {
        this.bodyConsumed = true;
        this.bodyString = bodyString;
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
    protected static String getBodyString(final Response response) {
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
