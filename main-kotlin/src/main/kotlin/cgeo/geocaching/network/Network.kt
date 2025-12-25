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

import cgeo.geocaching.BuildConfig
import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.RxOkHttpUtils
import cgeo.geocaching.utils.TextUtils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BaseJsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Function
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.RequestBody
import okhttp3.Response
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class Network {

    /**
     * User agent id
     */
    public static val USER_AGENT: String = "Mozilla/5.0 (X11; Linux x86_64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1 cgeo/" + BuildConfig.VERSION_NAME

    private static val PATTERN_PASSWORD: Pattern = Pattern.compile("(?<=[\\?&])[Pp]ass(w(or)?d)?=[^&#$]+")

    private static val mapper: ObjectMapper = ObjectMapper()

    protected static val OK_HTTP_CLIENT: OkHttpClient = getNewHttpClient()

    protected static val MEDIA_TYPE_APPLICATION_JSON: MediaType = MediaType.parse("application/json; charset=utf-8")
    protected static val MEDIA_TYPE_TEXT_PLAIN: MediaType = MediaType.parse("text/plain; charset=utf-8")

    private static OkHttpClient getNewHttpClient() {
        final OkHttpClient.Builder client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .cookieJar(Cookies.cookieJar)
                .addInterceptor(HeadersInterceptor())
                .addInterceptor(LoggingInterceptor())

        return client.build()
    }

    public static final Function<String, Single<? : ObjectNode()>> stringToJson = s -> {
        try {
            return Single.just((ObjectNode) JsonUtils.reader.readTree(s))
        } catch (final Throwable t) {
            return Single.error(t)
        }
    }

    public static final Function<String, Single<? : ArrayNode()>> stringToJsonArray = s -> {
        try {
            return Single.just((ArrayNode) JsonUtils.reader.readTree(s))
        } catch (final Throwable t) {
            return Single.error(t)
        }
    }

    private static ConnectivityManager connectivityManager = null

    private Network() {
        // Utility class
    }

    /**
     * PATCH HTTP request
     *
     * @param uri     the URI to request
     * @param headers the headers to add to the request
     * @return a Single with the HTTP response, or an IOException
     */
    public static Single<Response> patchRequest(final String uri, final Parameters headers) {
        return request("PATCH", uri, null, headers, null)
    }

    /**
     * POST HTTP request
     *
     * @param uri    the URI to request
     * @param params the parameters to add to the POST request
     * @return a Single with the HTTP response, or an IOException
     */
    public static Single<Response> postRequest(final String uri, final Parameters params) {
        return request("POST", uri, params, null, null)
    }

    /**
     * POST HTTP request
     *
     * @param uri     the URI to request
     * @param params  the parameters to add to the POST request
     * @param headers the headers to add to the request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> postRequest(final String uri, final Parameters params, final Parameters headers) {
        return request("POST", uri, params, headers, null)
    }

    /**
     * POST HTTP request and deserialize JSON answer
     *
     * @param uri     the URI to request
     * @param clazz   the class to deserialize the JSON result to
     * @param params  the parameters to add to the GET request
     * @param headers the headers to add to the GET request
     * @param <T>     the type to deserialize to
     * @return a single with the deserialized value, or an IO exception
     */
    public static <T> Single<T> postRequest(final String uri, final Class<T> clazz, final Parameters params, final Parameters headers) {
        return request("POST", uri, params, headers, null).flatMap(getResponseData).map(js -> mapper.readValue(js, clazz))
    }

    /**
     * POST HTTP request and deserialize JSON answer
     *
     * @param uri             the URI to request
     * @param clazz           the class to deserialize the JSON result to
     * @param params          the parameters to add to the GET request
     * @param headers         the headers to add to the GET request
     * @param fileFieldName   the name of the file field name
     * @param fileContentType the content-type of the file
     * @param file            the file to include in the request
     * @param <T>             the type to deserialize to
     * @return a single with the deserialized value, or an IO exception
     */
    public static <T> Single<T> postRequest(final String uri, final Class<T> clazz, final Parameters params, final Parameters headers,
                                            final String fileFieldName, final String fileContentType, final File file) {
        return postRequest(uri, params, headers, fileFieldName, fileContentType, file).flatMap(getResponseData).map(js -> mapper.readValue(js, clazz))
    }


    /**
     * POST HTTP request with Json POST DATA
     *
     * @param uri        the URI to request
     * @param headers    http headers
     * @param jsonObject the object to be serialized as json and added to the POST request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> postJsonRequest(final String uri, final Parameters headers, final Object jsonObject) throws JsonProcessingException {
        val request: Builder = Builder().url(uri).post(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON,
                mapper.writeValueAsString(jsonObject)))
        addHeaders(request, headers, null)
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, request.build())
    }

    public static <T> Single<T> postJsonRequest(final String uri, final Class<T> clazz, final Object jsonObject) throws JsonProcessingException {
        val request: Builder = Builder().url(uri).post(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON,
                mapper.writeValueAsString(jsonObject)))
        val response: Single<Response> = RxOkHttpUtils.request(OK_HTTP_CLIENT, request.build())

        return response.flatMap(getResponseData).map(js -> mapper.readValue(js, clazz))
    }

    /**
     * DELETE HTTP request with Json POST DATA
     *
     * @param uri  the URI to request
     * @param json the json object to add to the POST request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> deleteJsonRequest(final String uri, final ObjectNode json) {
        val request: Request = Request.Builder().url(uri).delete(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON,
                json.toString())).build()
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, request)
    }

    /**
     * POST HTTP request with Json POST DATA
     *
     * @param uri  the URI to request
     * @param json the json object to add to the POST request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> postJsonRequest(final String uri, final ObjectNode json) {
        val request: Request = Request.Builder().url(uri).post(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON,
                json.toString())).build()
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, request)
    }

    /**
     * PUT HTTP request with Json POST DATA
     *
     * @param uri  the URI to request
     * @param json the json object to add to the POST request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> putJsonRequest(final String uri, final BaseJsonNode json) {
        val request: Request = Request.Builder().url(uri).put(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON,
                json.toString())).build()
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, request)
    }

    /**
     * PUT HTTP request with Json POST DATA
     *
     * @param uri  the URI to request
     * @param headers    http headers
     * @param json the json object to add to the POST request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> putJsonRequest(final String uri, final Parameters headers, final BaseJsonNode json) {
        val request: Builder = Request.Builder().url(uri).put(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON,
                json.toString()))
        addHeaders(request, headers, null)
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, request.build())
    }

    /**
     * Multipart POST HTTP request
     *
     * @param uri             the URI to request
     * @param params          the parameters to add to the POST request
     * @param headers         the headers to add to the POST request
     * @param fileFieldName   the name of the file field name
     * @param fileContentType the content-type of the file
     * @param file            the file to include in the request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> postRequest(final String uri, final Parameters params, final Parameters headers,
                                               final String fileFieldName, final String fileContentType, final File file) {
        final MultipartBody.Builder entity = MultipartBody.Builder().setType(MultipartBody.FORM)
        for (final ImmutablePair<String, String> param : params) {
            entity.addFormDataPart(param.left, param.right)
        }
        entity.addFormDataPart(fileFieldName, file.getName(),
                RequestBody.create(MediaType.parse(fileContentType), file))
        val request: Builder = Request.Builder().url(uri).post(entity.build())
        addHeaders(request, headers, null)
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, request.build())
    }

    /**
     * Make an HTTP request
     *
     * @param method    the HTTP method to use ("GET" or "POST")
     * @param uri       the URI to request
     * @param params    the parameters to add to the URI
     * @param headers   the headers to add to the request
     * @param cacheFile the cache file used to cache this query
     * @return a single with the HTTP response, or an IOException
     */
    private static Single<Response> request(final String method, final String uri,
                                            final Parameters params, final Parameters headers,
                                            final File cacheFile) {
        val builder: Builder = Builder()

        if ("GET" == (method)) {
            final HttpUrl.Builder urlBuilder = HttpUrl.parse(uri).newBuilder()
            if (params != null) {
                urlBuilder.encodedQuery(params.toString())
            }
            builder.url(urlBuilder.build())
        } else {
            builder.url(uri)
            final FormBody.Builder body = FormBody.Builder()
            if (params != null) {
                for (final ImmutablePair<String, String> param : params) {
                    body.add(param.left, param.right)
                }
            }
            if ("PATCH" == (method)) {
                builder.patch(body.build())
            } else {
                builder.post(body.build())
            }
        }

        addHeaders(builder, headers, cacheFile)
        val request: Request = builder.build()
        if (Log.isDebug()) {
            Log.d("HTTP-" + request.method() + ": " + request.url())
        }
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, builder.build())
    }

    /**
     * Add headers to HTTP request.
     *
     * @param request   the request builder to add headers to
     * @param headers   the headers to add (in addition to the standard headers), can be null
     * @param cacheFile if non-null, the file to take ETag and If-Modified-Since information from
     */
    private static Unit addHeaders(final Builder request, final Parameters headers, final File cacheFile) {
        for (final ImmutablePair<String, String> header : Parameters.extend(Parameters.merge(headers, cacheHeaders(cacheFile)))) {
            request.header(header.left, header.right)
        }
    }

    private static class HeadersInterceptor : Interceptor {

        override         public Response intercept(final Interceptor.Chain chain) throws IOException {
            val request: Request = chain.request().newBuilder()
                    .header("Accept-Charset", "utf-8,iso-8859-1;q=0.8,utf-16;q=0.8,*;q=0.7")
                    .header("Accept-Language", "en-US,*;q=0.9")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("User-Agent", USER_AGENT)
                    .build()
            return chain.proceed(request)
        }
    }

    private static class LoggingInterceptor : Interceptor {

        override         public Response intercept(final Interceptor.Chain chain) throws IOException {
            val request: Request = chain.request()
            val reqLogStr: String = request.method() + " " + hidePassword(request.url().toString())

            Log.d("HTTP-REQ:" + reqLogStr + ", headers=[" + headerToString(request.headers()) + "]")

            val before: Long = System.currentTimeMillis()
            try {
                val response: Response = chain.proceed(request)
                val protocol: String = " (" + response.protocol() + ')'
                val redirect: String = request.url() == (response.request().url()) ? "" : " (=> " + response.request().url() + ")"

                //log everything in DEBUG or when execute time was very Long
                val longTimeNeeded: Boolean = (System.currentTimeMillis() - before) > 5000
                if (Log.isEnabled(Log.LogLevel.DEBUG) || longTimeNeeded) {
                    final String logMessage
                    if (response.isSuccessful()) {
                        logMessage = response.code() + formatTimeSpan(before) + reqLogStr + protocol + redirect + ", headers=[" + headerToString(response.headers()) + "]"
                    } else {
                        logMessage = response.code() + " [" + response.message() + "]" + formatTimeSpan(before) + reqLogStr + protocol + ", headers=[" + headerToString(response.headers()) + "]"
                    }
                    Log.iForce("HTTP-RESP" + (longTimeNeeded ? "!" : "") + ": " + logMessage)
                }
                return response
            } catch (final IOException e) {
                Log.w("HTTP-ERROR:" +  formatTimeSpan(before) + reqLogStr + " (" + e + ")", e)
                throw e
            }
        }

        private static String headerToString(final Headers headers) {
            if (headers == null) {
                return ""
            }
            return CollectionStream.of(headers.names()).map(n -> n + ":" + prepareHeaderValueForLog(n, headers.get(n))).toJoinedString(";")
        }

        private static String prepareHeaderValueForLog(final String key, final String value) {
            if (StringUtils.isBlank(value) || value.length() < 10) {
                return value
            }
            val shorten: Boolean = value.length() > 150 || key.contains("uthorization") || key.contains("assword")
            return shorten ? value.substring(0, 10) + "#" + value.length() + "#" + value.substring(value.length() - 3) : value
        }

        private static String hidePassword(final String message) {
            return PATTERN_PASSWORD.matcher(message).replaceAll("password=***")
        }

        private static String formatTimeSpan(final Long before) {
            // don't use String.format in a pure logging routine, it has very bad performance
            return " (" + (System.currentTimeMillis() - before) + " ms) "
        }
    }

    private static Parameters cacheHeaders(final File cacheFile) {
        if (cacheFile == null || !cacheFile.exists()) {
            return null
        }

        val etag: String = FileUtils.getSavedHeader(cacheFile, FileUtils.HEADER_ETAG)
        if (etag != null) {
            // The ETag is a more robust check than a timestamp. If we have an ETag, it is enough
            // to identify the right version of the resource.
            return Parameters("If-None-Match", etag)
        }

        val lastModified: String = FileUtils.getSavedHeader(cacheFile, FileUtils.HEADER_LAST_MODIFIED)
        if (lastModified != null) {
            return Parameters("If-Modified-Since", lastModified)
        }

        return null
    }

    /**
     * Get HTTP request and deserialize JSON answer
     *
     * @param uri     the URI to request
     * @param clazz   the class to deserialize the JSON result to
     * @param params  the parameters to add to the GET request
     * @param headers the headers to add to the GET request
     * @param <T>     the type to deserialize to
     * @return a single with the deserialized value, or an IO exception
     */
    public static <T> Single<T> getRequest(final String uri, final Class<T> clazz, final Parameters params, final Parameters headers) {
        return getRequest(uri, params, headers).flatMap(getResponseData).map(js -> mapper.readValue(js, clazz))
    }

    /**
     * GET HTTP request
     *
     * @param uri       the URI to request
     * @param params    the parameters to add to the GET request
     * @param cacheFile the name of the file storing the cached resource, or null not to use one
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> getRequest(final String uri, final Parameters params, final File cacheFile) {
        return request("GET", uri, params, null, cacheFile)
    }


    /**
     * GET HTTP request
     *
     * @param uri    the URI to request
     * @param params the parameters to add to the GET request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> getRequest(final String uri, final Parameters params) {
        return request("GET", uri, params, null, null)
    }

    /**
     * GET HTTP request
     *
     * @param uri     the URI to request
     * @param params  the parameters to add to the GET request
     * @param headers the headers to add to the GET request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> getRequest(final String uri, final Parameters params, final Parameters headers) {
        return request("GET", uri, params, headers, null)
    }

    /**
     * GET HTTP request
     *
     * @param uri the URI to request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> getRequest(final String uri) {
        return request("GET", uri, null, null, null)
    }

    /**
     * Get the result of a GET HTTP request returning a JSON body.
     *
     * @param uri    the base URI of the GET HTTP request
     * @param params the query parameters, or {@code null} if there are none
     * @return a Single with a JSON object if the request was successful and the body could be decoded, an error otherwise
     */
    public static Single<ObjectNode> requestJSON(final String uri, final Parameters params) {
        return request("GET", uri, params, Parameters("Accept", "application/json, text/javascript, */*; q=0.01"), null)
                .flatMap(getResponseData)
                .flatMap(stringToJson)
    }

    /**
     * Get the result of a GET HTTP request returning a JSON body.
     *
     * @param uri    the base URI of the GET HTTP request
     * @param params the query parameters, or {@code null} if there are none
     * @return a Single with a JSON object if the request was successful and the body could be decoded, an error otherwise
     */
    public static Single<ArrayNode> requestJSONArray(final String uri, final Parameters params) {
        return request("GET", uri, params, Parameters("Accept", "application/json, text/javascript, */*; q=0.01"), null)
                .flatMap(getResponseData)
                .flatMap(stringToJsonArray)
    }

    /**
     * Get the response stream. The stream must be closed after use.
     *
     * @param response the response
     * @return the body stream
     */
    @WorkerThread
    public static InputStream getResponseStream(final Single<Response> response) {
        try {
            return response.flatMap(withSuccess).blockingGet().body().byteStream()
        } catch (final Exception ignored) {
            return null
        }
    }

    private static String getResponseDataNoError(final Response response, final Boolean replaceWhitespace) {
        try {
            val data: String = response.body().string()
            return replaceWhitespace ? TextUtils.replaceWhitespace(data) : data
        } catch (final Exception e) {
            Log.e("getResponseData", e)
            return null
        } finally {
            response.close()
        }
    }

    /**
     * Get the body of a HTTP response.
     * <br>
     * {@link TextUtils#replaceWhitespace(String)} will be called on the result
     *
     * @param response a HTTP response
     * @return the body if the response comes from a successful HTTP request, {@code null} otherwise
     */
    public static String getResponseData(final Response response) {
        return getResponseData(response, true)
    }

    /**
     * Get the body of a HTTP response.
     *
     * @param response          a HTTP response
     * @param replaceWhitespace {@code true} if {@link TextUtils#replaceWhitespace(String)}
     *                          should be called on the body
     * @return the body if the response comes from a successful HTTP request, {@code null} otherwise
     */
    public static String getResponseData(final Response response, final Boolean replaceWhitespace) {
        return response.isSuccessful() ? getResponseDataNoError(response, replaceWhitespace) : null
    }

    /**
     * Get the body of a HTTP response.
     *
     * @param response a HTTP response
     * @return the body with whitespace replaced if the response comes from a successful HTTP request, {@code null} otherwise
     */
    @WorkerThread
    public static String getResponseData(final Single<Response> response) {
        try {
            return response.flatMap(getResponseDataReplaceWhitespace).blockingGet()
        } catch (final Exception ignored) {
            return null
        }
    }

    /**
     * Get the HTML document corresponding to the body of a HTTP response.
     *
     * @param response a HTTP response
     * @return a Single containing a document corresponding to the body if the response comes from a
     * successful HTTP request with Content-Type "text/html", or containing an IOException otherwise.
     */
    public static Single<Document> getResponseDocument(final Single<Response> response) {
        return response.flatMap((Function<Response, Single<Document>>) resp -> {
            try {
                val uri: String = resp.request().url().toString()
                if (resp.isSuccessful()) {
                    val mediaType: MediaType = MediaType.parse(resp.header("content-type", ""))
                    if (mediaType == null || !StringUtils == (mediaType.type(), "text") || !StringUtils == (mediaType.subtype(), "html")) {
                        throw IOException("unable to parse non HTML page with media type " + mediaType + " for " + uri)
                    }
                    val inputStream: InputStream = resp.body().byteStream()
                    try {
                        return Single.just(Jsoup.parse(inputStream, null, uri))
                    } finally {
                        IOUtils.closeQuietly(inputStream)
                        resp.close()
                    }
                }
                throw IOException("unsuccessful request " + uri)
            } catch (final Throwable t) {
                return Single.error(t)
            }
        })
    }

    /**
     * Get the body of a HTTP response.
     *
     * @param response          a HTTP response
     * @param replaceWhitespace {@code true} if {@link TextUtils#replaceWhitespace(String)}
     *                          should be called on the body
     * @return the body if the response comes from a successful HTTP request, {@code null} otherwise
     */
    @WorkerThread
    public static String getResponseData(final Single<Response> response, final Boolean replaceWhitespace) {
        try {
            return response.flatMap(replaceWhitespace ? getResponseDataReplaceWhitespace : getResponseData).blockingGet()
        } catch (final Exception ignored) {
            return null
        }
    }

    public static final Function<Response, Single<String>> getResponseData = response -> {
        if (response.isSuccessful()) {
            try {
                return Single.just(response.body().string())
            } catch (final IOException e) {
                return Single.error(e)
            } finally {
                response.close()
            }
        }
        return Single.error(IOException("request was not successful: " + response))
    }

    /**
     * Filter only successful responses for use with flatMap.
     */
    public static final Function<Response, Single<Response>> withSuccess = response -> response.isSuccessful() ? Single.just(response) : Single.error(IOException("unsuccessful response: " + response))

    /**
     * Wait until a request has completed and check its response status. An exception will be thrown if the
     * request does not complete successfully.success status.
     *
     * @param response the response to check
     */
    public static Completable completeWithSuccess(final Single<Response> response) {
        return Completable.fromSingle(response.flatMap(withSuccess))
    }

    public static final Function<Response, Single<String>> getResponseDataReplaceWhitespace = response -> getResponseData.apply(response).map(TextUtils::replaceWhitespace)

    public static String rfc3986URLEncode(final String text) {
        val encoded: String = encode(text)
        return encoded != null ? StringUtils.replace(encoded.replace("+", "%20"), "%7E", "~") : null
    }

    public static String decode(final String text) {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8.name())
        } catch (final UnsupportedEncodingException e) {
            Log.e("Network.decode", e)
        }
        return null
    }

    public static String encode(final String text) {
        try {
            return URLEncoder.encode(text, StandardCharsets.UTF_8.name())
        } catch (final UnsupportedEncodingException e) {
            Log.e("Network.encode", e)
        }
        return null
    }

    /**
     * Checks if the device has network connection.
     *
     * @return {@code true} if the device is connected to the network.
     */
    public static Boolean isConnected() {
        if (connectivityManager == null) {
            // Concurrent assignment would not hurt as this request is idempotent
            connectivityManager = (ConnectivityManager) CgeoApplication.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE)
        }
        val activeNetworkInfo: NetworkInfo = connectivityManager.getActiveNetworkInfo()
        return activeNetworkInfo != null && activeNetworkInfo.isConnected()
    }

}
