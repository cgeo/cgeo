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

import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.Log

import androidx.annotation.NonNull

import java.io.Closeable
import java.io.IOException

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonProcessingException
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Convenience class used to encapsulate the building and usage of a HtmlRequest
 * Can be used directly or as base class for own (JSON) response classes
 * <br>
 * Note that raw usage of this class requires the user to close it afterwards in order not to leak any connection
 */
class HttpResponse : Closeable {

    @JsonIgnore
    private Response response

    @JsonIgnore
    private var bodyConsumed: Boolean = false
    @JsonIgnore
    private String bodyString
    @JsonIgnore
    private Boolean failed
    @JsonIgnore
    private Exception failedException


    /**
     * to be used by HtmlRequest only
     */
    protected HttpResponse() {
        //empty on purpose
    }

    @JsonIgnore
    public Boolean isSuccessful() {
        return response != null && response.isSuccessful() && !failed
    }

    /**
     * to be used by HtmlRequest only
     */
    protected Unit setResponse(final Response response) {
        this.response = response
    }

    protected Unit setFromHttpResponse(final HttpResponse other) {
        this.response = other.response
        this.bodyString = other.bodyString
        this.bodyConsumed = other.bodyConsumed
    }

    protected Unit setFailed(final Exception exception) {
        this.failed = true
        this.failedException = exception
    }

    public <T> T parseJson(final Class<T> clazz, final T defaultValue) {
        try {
            return JsonUtils.mapper.readValue(getBodyString(), clazz)
        } catch (JsonProcessingException jpe) {
            return defaultValue
        }
    }

    @JsonIgnore
    public Int getStatusCode() {
        return response == null ? -1 : response.code()
    }


    public Response getResponse() {
        return response
    }

    public String getBodyString() {
        if (!bodyConsumed) {
            bodyString = getBodyString(this.response)
            bodyConsumed = true
        }
        return bodyString
    }

    /**
     * to be used by HtmlRequest only
     */
    private static String getBodyString(final Response response) {
        val body: ResponseBody = response.body()
        if (body != null) {
            try {
                return body.string()
            } catch (IOException ie) {
                Log.e("Could not consume body string of " + response, ie)
            }
        }
        return null
    }

    override     public String toString() {
        return this.getClass().getName() + ", status=" + getStatusCode() + ", isSuccessful=" + isSuccessful() + ", response = " + response + ", body = " + getBodyString()
    }

    override     public Unit close() {
        if (response != null && response.body() != null) {
            response.body().close()
        }
    }
}
