package cgeo.geocaching.network;

import cgeo.geocaching.BuildConfig;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxOkHttpUtils;
import cgeo.geocaching.utils.TextUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import okhttp3.ConnectionSpec;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public final class Network {

    /**
     * User agent id
     */
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1 cgeo/" + BuildConfig.VERSION_NAME;

    private static final Pattern PATTERN_PASSWORD = Pattern.compile("(?<=[\\?&])[Pp]ass(w(or)?d)?=[^&#$]+");

    private static final ObjectMapper mapper = new ObjectMapper();

    protected static final OkHttpClient OK_HTTP_CLIENT = getNewHttpClient();

    protected static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");
    protected static final MediaType MEDIA_TYPE_TEXT_PLAIN = MediaType.parse("text/plain; charset=utf-8");

    private static OkHttpClient getNewHttpClient() {
        final OkHttpClient.Builder client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .cookieJar(Cookies.cookieJar)
                .addInterceptor(new HeadersInterceptor())
                .addInterceptor(new LoggingInterceptor());

        return enableTls12OnPreLollipop(client).build();
    }

    private static OkHttpClient.Builder enableTls12OnPreLollipop(final OkHttpClient.Builder builder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init((KeyStore) null);
                final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    throw new IllegalStateException("Unexpected default trust managers:"
                            + Arrays.toString(trustManagers));
                }
                final X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

                final SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);
                builder.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), trustManager);

                final ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                final List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);

                builder.connectionSpecs(specs);
            } catch (final Exception exc) {
                Log.e("Error while setting TLS 1.2", exc);
            }
        }

        return builder;
    }

    public static final Function<String, Single<? extends ObjectNode>> stringToJson = s -> {
        try {
            return Single.just((ObjectNode) JsonUtils.reader.readTree(s));
        } catch (final Throwable t) {
            return Single.error(t);
        }
    };

    private static ConnectivityManager connectivityManager = null;

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
    @NonNull
    public static Single<Response> patchRequest(final String uri, final Parameters headers) {
        return request("PATCH", uri, null, headers, null);
    }

    /**
     * POST HTTP request
     *
     * @param uri    the URI to request
     * @param params the parameters to add to the POST request
     * @return a Single with the HTTP response, or an IOException
     */
    @NonNull
    public static Single<Response> postRequest(final String uri, final Parameters params) {
        return request("POST", uri, params, null, null);
    }

    /**
     * POST HTTP request
     *
     * @param uri     the URI to request
     * @param params  the parameters to add to the POST request
     * @param headers the headers to add to the request
     * @return a single with the HTTP response, or an IOException
     */
    @NonNull
    public static Single<Response> postRequest(final String uri, final Parameters params, final Parameters headers) {
        return request("POST", uri, params, headers, null);
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
        return request("POST", uri, params, headers, null).flatMap(getResponseData).map(js -> mapper.readValue(js, clazz));
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
        return postRequest(uri, params, headers, fileFieldName, fileContentType, file).flatMap(getResponseData).map(js -> mapper.readValue(js, clazz));
    }


    /**
     * POST HTTP request with Json POST DATA
     *
     * @param uri        the URI to request
     * @param headers    http headers
     * @param jsonObject the object to be serialized as json and added to the POST request
     * @return a single with the HTTP response, or an IOException
     */
    @NonNull
    public static Single<Response> postJsonRequest(final String uri, final Parameters headers, final Object jsonObject) throws JsonProcessingException {
        final Builder request = new Builder().url(uri).post(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON,
                mapper.writeValueAsString(jsonObject)));
        addHeaders(request, headers, null);
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, request.build());
    }

    @NonNull
    public static <T> Single<T> postJsonRequest(final String uri, final Class<T> clazz, final Object jsonObject) throws JsonProcessingException {
        final Builder request = new Builder().url(uri).post(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON,
                mapper.writeValueAsString(jsonObject)));
        final Single<Response> response = RxOkHttpUtils.request(OK_HTTP_CLIENT, request.build());

        return response.flatMap(getResponseData).map(js -> mapper.readValue(js, clazz));
    }

    /**
     * DELETE HTTP request with Json POST DATA
     *
     * @param uri  the URI to request
     * @param json the json object to add to the POST request
     * @return a single with the HTTP response, or an IOException
     */
    @NonNull
    public static Single<Response> deleteJsonRequest(final String uri, final ObjectNode json) {
        final Request request = new Request.Builder().url(uri).delete(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON,
                json.toString())).build();
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, request);
    }

    /**
     * POST HTTP request with Json POST DATA
     *
     * @param uri  the URI to request
     * @param json the json object to add to the POST request
     * @return a single with the HTTP response, or an IOException
     */
    @NonNull
    public static Single<Response> postJsonRequest(final String uri, final ObjectNode json) {
        final Request request = new Request.Builder().url(uri).post(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON,
                json.toString())).build();
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, request);
    }

    /**
     * PUT HTTP request with Json POST DATA
     *
     * @param uri  the URI to request
     * @param json the json object to add to the POST request
     * @return a single with the HTTP response, or an IOException
     */
    @NonNull
    public static Single<Response> putJsonRequest(final String uri, final BaseJsonNode json) {
        final Request request = new Request.Builder().url(uri).put(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON,
                json.toString())).build();
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, request);
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
    @NonNull
    public static Single<Response> postRequest(final String uri, final Parameters params, final Parameters headers,
                                               final String fileFieldName, final String fileContentType, final File file) {
        final MultipartBody.Builder entity = new MultipartBody.Builder().setType(MultipartBody.FORM);
        for (final ImmutablePair<String, String> param : params) {
            entity.addFormDataPart(param.left, param.right);
        }
        entity.addFormDataPart(fileFieldName, file.getName(),
                RequestBody.create(MediaType.parse(fileContentType), file));
        final Builder request = new Request.Builder().url(uri).post(entity.build());
        addHeaders(request, headers, null);
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, request.build());
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
    @NonNull
    private static Single<Response> request(final String method, final String uri,
                                            @Nullable final Parameters params, @Nullable final Parameters headers,
                                            @Nullable final File cacheFile) {
        final Builder builder = new Builder();

        if ("GET".equals(method)) {
            final HttpUrl.Builder urlBuilder = HttpUrl.parse(uri).newBuilder();
            if (params != null) {
                urlBuilder.encodedQuery(params.toString());
            }
            builder.url(urlBuilder.build());
        } else {
            builder.url(uri);
            final FormBody.Builder body = new FormBody.Builder();
            if (params != null) {
                for (final ImmutablePair<String, String> param : params) {
                    body.add(param.left, param.right);
                }
            }
            if ("PATCH".equals(method)) {
                builder.patch(body.build());
            } else {
                builder.post(body.build());
            }
        }

        addHeaders(builder, headers, cacheFile);
        final Request request = builder.build();
        if (Log.isDebug()) {
            Log.d("HTTP-" + request.method() + ": " + request.url());
        }
        return RxOkHttpUtils.request(OK_HTTP_CLIENT, builder.build());
    }

    /**
     * Add headers to HTTP request.
     *
     * @param request   the request builder to add headers to
     * @param headers   the headers to add (in addition to the standard headers), can be null
     * @param cacheFile if non-null, the file to take ETag and If-Modified-Since information from
     */
    private static void addHeaders(final Builder request, @Nullable final Parameters headers, @Nullable final File cacheFile) {
        for (final ImmutablePair<String, String> header : Parameters.extend(Parameters.merge(headers, cacheHeaders(cacheFile)))) {
            request.header(header.left, header.right);
        }
    }

    private static class HeadersInterceptor implements Interceptor {

        @Override
        @NonNull
        public Response intercept(final Interceptor.Chain chain) throws IOException {
            final Request request = chain.request().newBuilder()
                    .header("Accept-Charset", "utf-8,iso-8859-1;q=0.8,utf-16;q=0.8,*;q=0.7")
                    .header("Accept-Language", "en-US,*;q=0.9")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("User-Agent", USER_AGENT)
                    .build();
            return chain.proceed(request);
        }
    }

    private static class LoggingInterceptor implements Interceptor {

        @Override
        @NonNull
        public Response intercept(final Interceptor.Chain chain) throws IOException {
            final Request request = chain.request();
            final String reqLogStr = request.method() + " " + hidePassword(request.url().toString());

            Log.d(reqLogStr);

            final long before = System.currentTimeMillis();
            try {
                final Response response = chain.proceed(request);
                final String protocol = " (" + response.protocol() + ')';
                final String redirect = request.url().equals(response.request().url()) ? "" : " (=> " + response.request().url() + ")";
                if (response.isSuccessful()) {
                    Log.d(response.code() + formatTimeSpan(before) + reqLogStr + protocol + redirect);
                } else {
                    Log.d(response.code() + " [" + response.message() + "]" + formatTimeSpan(before) + reqLogStr + protocol);
                }
                return response;
            } catch (final IOException e) {
                Log.w("Failure" + formatTimeSpan(before) + reqLogStr + " (" + e + ")");
                throw e;
            }
        }

        private static String hidePassword(final String message) {
            return PATTERN_PASSWORD.matcher(message).replaceAll("password=***");
        }

        private static String formatTimeSpan(final long before) {
            // don't use String.format in a pure logging routine, it has very bad performance
            return " (" + (System.currentTimeMillis() - before) + " ms) ";
        }
    }

    @Nullable
    private static Parameters cacheHeaders(@Nullable final File cacheFile) {
        if (cacheFile == null || !cacheFile.exists()) {
            return null;
        }

        final String etag = FileUtils.getSavedHeader(cacheFile, FileUtils.HEADER_ETAG);
        if (etag != null) {
            // The ETag is a more robust check than a timestamp. If we have an ETag, it is enough
            // to identify the right version of the resource.
            return new Parameters("If-None-Match", etag);
        }

        final String lastModified = FileUtils.getSavedHeader(cacheFile, FileUtils.HEADER_LAST_MODIFIED);
        if (lastModified != null) {
            return new Parameters("If-Modified-Since", lastModified);
        }

        return null;
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
    @NonNull
    public static <T> Single<T> getRequest(final String uri, final Class<T> clazz, @Nullable final Parameters params, @Nullable final Parameters headers) {
        return getRequest(uri, params, headers).flatMap(getResponseData).map(js -> mapper.readValue(js, clazz));
    }

    /**
     * GET HTTP request
     *
     * @param uri       the URI to request
     * @param params    the parameters to add to the GET request
     * @param cacheFile the name of the file storing the cached resource, or null not to use one
     * @return a single with the HTTP response, or an IOException
     */
    @NonNull
    public static Single<Response> getRequest(final String uri, @Nullable final Parameters params, @Nullable final File cacheFile) {
        return request("GET", uri, params, null, cacheFile);
    }


    /**
     * GET HTTP request
     *
     * @param uri    the URI to request
     * @param params the parameters to add to the GET request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> getRequest(final String uri, @Nullable final Parameters params) {
        return request("GET", uri, params, null, null);
    }

    /**
     * GET HTTP request
     *
     * @param uri     the URI to request
     * @param params  the parameters to add to the GET request
     * @param headers the headers to add to the GET request
     * @return a single with the HTTP response, or an IOException
     */
    @NonNull
    public static Single<Response> getRequest(final String uri, @Nullable final Parameters params, @Nullable final Parameters headers) {
        return request("GET", uri, params, headers, null);
    }

    /**
     * GET HTTP request
     *
     * @param uri the URI to request
     * @return a single with the HTTP response, or an IOException
     */
    public static Single<Response> getRequest(final String uri) {
        return request("GET", uri, null, null, null);
    }

    /**
     * Get the result of a GET HTTP request returning a JSON body.
     *
     * @param uri    the base URI of the GET HTTP request
     * @param params the query parameters, or {@code null} if there are none
     * @return a Single with a JSON object if the request was successful and the body could be decoded, an error otherwise
     */
    @NonNull
    public static Single<ObjectNode> requestJSON(final String uri, @Nullable final Parameters params) {
        return request("GET", uri, params, new Parameters("Accept", "application/json, text/javascript, */*; q=0.01"), null)
                .flatMap(getResponseData)
                .flatMap(stringToJson);
    }

    /**
     * Get the response stream. The stream must be closed after use.
     *
     * @param response the response
     * @return the body stream
     */
    @Nullable
    @WorkerThread
    public static InputStream getResponseStream(final Single<Response> response) {
        try {
            return response.flatMap(withSuccess).blockingGet().body().byteStream();
        } catch (final Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static String getResponseDataNoError(final Response response, final boolean replaceWhitespace) {
        try {
            final String data = response.body().string();
            return replaceWhitespace ? TextUtils.replaceWhitespace(data) : data;
        } catch (final Exception e) {
            Log.e("getResponseData", e);
            return null;
        } finally {
            response.close();
        }
    }

    /**
     * Get the body of a HTTP response.
     *
     * {@link TextUtils#replaceWhitespace(String)} will be called on the result
     *
     * @param response a HTTP response
     * @return the body if the response comes from a successful HTTP request, {@code null} otherwise
     */
    @Nullable
    public static String getResponseData(final Response response) {
        return getResponseData(response, true);
    }

    /**
     * Get the body of a HTTP response.
     *
     * @param response          a HTTP response
     * @param replaceWhitespace {@code true} if {@link TextUtils#replaceWhitespace(String)}
     *                          should be called on the body
     * @return the body if the response comes from a successful HTTP request, {@code null} otherwise
     */
    @Nullable
    public static String getResponseData(final Response response, final boolean replaceWhitespace) {
        return response.isSuccessful() ? getResponseDataNoError(response, replaceWhitespace) : null;
    }

    /**
     * Get the body of a HTTP response.
     *
     * @param response a HTTP response
     * @return the body with whitespace replaced if the response comes from a successful HTTP request, {@code null} otherwise
     */
    @Nullable
    @WorkerThread
    public static String getResponseData(final Single<Response> response) {
        try {
            return response.flatMap(getResponseDataReplaceWhitespace).blockingGet();
        } catch (final Exception ignored) {
            return null;
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
                final String uri = resp.request().url().toString();
                if (resp.isSuccessful()) {
                    final MediaType mediaType = MediaType.parse(resp.header("content-type", ""));
                    if (mediaType == null || !StringUtils.equals(mediaType.type(), "text") || !StringUtils.equals(mediaType.subtype(), "html")) {
                        throw new IOException("unable to parse non HTML page with media type " + mediaType + " for " + uri);
                    }
                    final InputStream inputStream = resp.body().byteStream();
                    try {
                        return Single.just(Jsoup.parse(inputStream, null, uri));
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                        resp.close();
                    }
                }
                throw new IOException("unsuccessful request " + uri);
            } catch (final Throwable t) {
                return Single.error(t);
            }
        });
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
    @Nullable
    public static String getResponseData(final Single<Response> response, final boolean replaceWhitespace) {
        try {
            return response.flatMap(replaceWhitespace ? getResponseDataReplaceWhitespace : getResponseData).blockingGet();
        } catch (final Exception ignored) {
            return null;
        }
    }

    public static final Function<Response, Single<String>> getResponseData = response -> {
        if (response.isSuccessful()) {
            try {
                return Single.just(response.body().string());
            } catch (final IOException e) {
                return Single.error(e);
            } finally {
                response.close();
            }
        }
        return Single.error(new IOException("request was not successful: " + response));
    };

    /**
     * Filter only successful responses for use with flatMap.
     */
    public static final Function<Response, Single<Response>> withSuccess = response -> response.isSuccessful() ? Single.just(response) : Single.error(new IOException("unsuccessful response: " + response));

    /**
     * Wait until a request has completed and check its response status. An exception will be thrown if the
     * request does not complete successfully.success status.
     *
     * @param response the response to check
     */
    public static void completeWithSuccess(final Single<Response> response) {
        Completable.fromSingle(response.flatMap(withSuccess)).blockingAwait();
    }

    public static final Function<Response, Single<String>> getResponseDataReplaceWhitespace = response -> getResponseData.apply(response).map(TextUtils::replaceWhitespace);

    @Nullable
    public static String rfc3986URLEncode(final String text) {
        final String encoded = encode(text);
        return encoded != null ? StringUtils.replace(encoded.replace("+", "%20"), "%7E", "~") : null;
    }

    @Nullable
    public static String decode(final String text) {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            Log.e("Network.decode", e);
        }
        return null;
    }

    @Nullable
    public static String encode(final String text) {
        try {
            return URLEncoder.encode(text, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            Log.e("Network.encode", e);
        }
        return null;
    }

    /**
     * Checks if the device has network connection.
     *
     * @return {@code true} if the device is connected to the network.
     */
    public static boolean isConnected() {
        if (connectivityManager == null) {
            // Concurrent assignment would not hurt as this request is idempotent
            connectivityManager = (ConnectivityManager) CgeoApplication.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
