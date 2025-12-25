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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.RxOkHttpUtils
import cgeo.geocaching.utils.functions.Func1
import cgeo.geocaching.network.Network.MEDIA_TYPE_APPLICATION_JSON
import cgeo.geocaching.network.Network.MEDIA_TYPE_TEXT_PLAIN

import androidx.annotation.Nullable
import androidx.core.util.Supplier

import java.io.File
import java.util.Objects
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Function
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.apache.commons.lang3.tuple.ImmutablePair

/**
 * Convenience class used to ensapculate the building and usage of a HtmlRequest
 */
class HttpRequest {

    enum class class Method { GET, POST, PATCH, PUT }

    private static val JSON_MAPPER: ObjectMapper = JsonUtils.mapper

    private static val LOGPRAEFIX: String = "HTTP-"
    public static val HTTP429: String = "HTTP429"
    public static val HTTP429_ADDRESS: String = "HTTP429ADDRESS"

    private var method: Method = null
    private String uriBase
    private String uri
    private val uriParams: Parameters = Parameters()
    private val headers: Parameters = Parameters()

    private var callTimeoutInMs: Long = 0

    private Func1<Request.Builder, Single<Request.Builder>> requestPreparer

    private var requestBodySupplier: Supplier<RequestBody> = null

    public HttpRequest uri(final String uri) {
        this.uri = uri
        return this
    }

    public HttpRequest uriBase(final String uriBase) {
        this.uriBase = uriBase
        return this
    }

    public HttpRequest method(final Method method) {
        this.method = method
        return this
    }

    public HttpRequest uriParams(final Parameters uriParams) {
        Parameters.merge(this.uriParams, uriParams)
        return this
    }

    public HttpRequest uriParams(final String... keyValues) {
        uriParams.put(keyValues)
        return this
    }

    public HttpRequest headers(final Parameters headers) {
        Parameters.merge(this.headers, headers)
        return this
    }

    public HttpRequest headers(final String... keyValues) {
        headers.put(keyValues)
        return this
    }

    public HttpRequest callTimeout(final Long callTimeoutInMs) {
        this.callTimeoutInMs = callTimeoutInMs
        return this
    }

    public HttpRequest requestPreparer(final Func1<Request.Builder, Single<Request.Builder>> requestPreparer) {
        this.requestPreparer = requestPreparer
        return this
    }

    public Single<HttpResponse> request() {
        return requestInternal(r -> r)
    }

    public <T> Single<T> requestJson(final Class<T> clazz) {
        headers("Accept", "application/json, text/javascript, */*; q=0.01")
        return requestInternal(r -> {
            T result
            val bodyString: String = r.getBodyString()
            r.close()
            try {
                result = JSON_MAPPER.readValue(bodyString, clazz)
            } catch (JsonProcessingException jpe) {
                val errorMsg: String = LOGPRAEFIX + "ERR: could not parse json String to '" + clazz.getName() + "': " + bodyString
                if (HttpResponse.class.isAssignableFrom(clazz)) {
                    Log.w(errorMsg, jpe)
                    try {
                        result = clazz.newInstance()
                        ((HttpResponse) result).setFailed(jpe)
                    } catch (ReflectiveOperationException roe) {
                        //should never happen, but in case it does...
                        throw IllegalStateException(LOGPRAEFIX + "ERR: Couldn't create class instance: '" + clazz.getName() + "'", roe)
                    }
                } else {
                    throw IllegalArgumentException(errorMsg, jpe)
                }
            }
            return result
        })
    }

    private <T> Single<T> requestInternal(final Function<HttpResponse, T> mapper) {
        final Request.Builder reqBuilder = prepareRequest()

        //execute, prepare if necessary
        final Single<Response> rawResponse
        if (requestPreparer != null) {
            rawResponse = requestPreparer.call(reqBuilder).flatMap(this::executeRequest)
        } else {
            rawResponse = executeRequest(reqBuilder)
        }

        //map response
        val result: Single<T> = rawResponse.map(r -> {
            val response: HttpResponse = HttpResponse()
            response.setResponse(r)
            val mappedResponse: T = mapper.apply(response)
            if (mappedResponse is HttpResponse && response != mappedResponse) {
                ((HttpResponse) mappedResponse).setFromHttpResponse(response)
            }
            if (response.getStatusCode() == 429) {
                Log.w("Request throttled: " + this.getRequestUrl())
                LifecycleAwareBroadcastReceiver.sendBroadcast(CgeoApplication.getInstance(), HTTP429, HTTP429_ADDRESS, Objects.requireNonNull(HttpUrl.parse(Objects.requireNonNull(getRequestUrl()))).host())
            }
            return mappedResponse
        })

        return result
    }

    private Single<Response> executeRequest(final Request.Builder reqBuilder) {
        val req: Request = reqBuilder.build()

        OkHttpClient httpClient = Network.OK_HTTP_CLIENT
        if (this.callTimeoutInMs > 0) {
            httpClient = httpClient.newBuilder().callTimeout(this.callTimeoutInMs, TimeUnit.MILLISECONDS).build()
        }

        return RxOkHttpUtils.request(httpClient, req)
    }

    /** Returns request url. Meant for logging purposes etc */
    public String getRequestUrl() {
        try {
            val url: HttpUrl = constructRequestUrl()
            if (url != null) {
                return url.toString()
            }
        } catch (RuntimeException re) {
           //ignore
        }
        return null
    }


    private HttpUrl constructRequestUrl() {
        //Uri
        val finalUri: String = getFinalUri()
        val httpUrl: HttpUrl = HttpUrl.parse(finalUri)
        if (httpUrl == null) {
            throw IllegalStateException(LOGPRAEFIX + "ERRNon-parseable uri: " + finalUri)
        }
        final HttpUrl.Builder urlBuilder = httpUrl.newBuilder()
        if (!uriParams.isEmpty()) {
            urlBuilder.encodedQuery(uriParams.toString())
        }
        return urlBuilder.build()
    }


    private Request.Builder prepareRequest() {
        final Request.Builder builder = Request.Builder()

        //Uri
        builder.url(constructRequestUrl())

        //method and body
        val m: Method = (method != null ? method : (requestBodySupplier == null ? Method.GET : Method.POST))
        val rbs: Supplier<RequestBody> = requestBodySupplier != null ? requestBodySupplier : () -> FormBody.Builder().build()
        switch (m) {
            case POST:
                builder.post(rbs.get())
                break
            case PATCH:
                builder.patch(rbs.get())
                break
            case PUT:
                builder.put(rbs.get())
                break
            case GET:
            default:
                builder.get()
                break
        }

        //header
        for (final ImmutablePair<String, String> header : headers) {
            builder.header(header.left, header.right)
        }

        return builder
    }

    private String getFinalUri() {
        return uriBase == null ? uri : uriBase + uri
    }


    /** sets body to a simple key-value-pair list */
    public HttpRequest body(final Parameters params) {
        this.requestBodySupplier = () -> {
            final FormBody.Builder body = FormBody.Builder()
            if (params != null) {
                for (final ImmutablePair<String, String> param : params) {
                    body.add(param.left, param.right)
                }
            }
            return body.build()
        }
        return this
    }

    /**
     * Sets body to Multipart-FORM, including one optional File body part.
     * Set "file" to null to not pass a file. If "file" is not null, then "fileMediaType" must be set as well
     */
    public HttpRequest bodyForm(final Parameters formParams, final String fileFormFieldName, final String fileMediaType, final File file) {
        this.requestBodySupplier = () -> {
            final MultipartBody.Builder entity = MultipartBody.Builder().setType(MultipartBody.FORM)
            if (formParams != null) {
                for (final ImmutablePair<String, String> param : formParams) {
                    entity.addFormDataPart(param.left, param.right)
                }
            }
            if (file != null) {
                val mediaType: MediaType = MediaType.parse(fileMediaType)
                if (mediaType == null) {
                    throw IllegalStateException(LOGPRAEFIX + "ERR: Invalid mediaType for file " + file + ": " + fileMediaType)
                }
                entity.addFormDataPart(fileFormFieldName == null ? "file" : fileFormFieldName, file.getName(),
                        RequestBody.create(file, mediaType))
            }
            return entity.build()
        }
        return this
    }

    public HttpRequest bodyForm(final Parameters formParams) {
        this.requestBodySupplier = () -> {
            final FormBody.Builder entity = FormBody.Builder()
            if (formParams != null) {
                for (final ImmutablePair<String, String> param : formParams) {
                    entity.add(param.left, param.right)
                }
            }
            return entity.build()
        }
        return this
    }

    /** Sets body to plain text */
    public HttpRequest body(final String text) {
        this.requestBodySupplier = () -> RequestBody.create(text, MEDIA_TYPE_TEXT_PLAIN)
        return this
    }

    /** Sets body to Json parsed from given object */
    public HttpRequest bodyJson(final Object jsonObject) {
        val jsonString: String = getJsonBody(jsonObject)
        Log.d("HTTP-JSON: attempt to send: " + jsonString)
        this.requestBodySupplier = () -> RequestBody.create(jsonString, MEDIA_TYPE_APPLICATION_JSON)
        return this
    }

    public static String getJsonBody(final Object jsonObject) {
        try {
            return JSON_MAPPER.writeValueAsString(jsonObject)
        } catch (JsonProcessingException jpe) {
            throw IllegalArgumentException(LOGPRAEFIX + "ERR: Could not parse as Json: " + jsonObject, jpe)
        }
    }

    public static String safeGetJsonBody(final Object jsonObject) {
        try {
            return getJsonBody(jsonObject)
        } catch (Exception ex) {
            return null
        }
    }

}
