package cgeo.geocaching.network;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.NameValuePair;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.client.params.ClientPNames;
import ch.boye.httpclientandroidlib.entity.StringEntity;
import ch.boye.httpclientandroidlib.entity.mime.MultipartEntity;
import ch.boye.httpclientandroidlib.entity.mime.content.FileBody;
import ch.boye.httpclientandroidlib.entity.mime.content.StringBody;
import ch.boye.httpclientandroidlib.impl.client.DecompressingHttpClient;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.client.LaxRedirectStrategy;
import ch.boye.httpclientandroidlib.params.BasicHttpParams;
import ch.boye.httpclientandroidlib.params.CoreConnectionPNames;
import ch.boye.httpclientandroidlib.params.CoreProtocolPNames;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.util.EntityUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public abstract class Network {

    /** User agent id */
    private final static String PC_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1";
    /** Native user agent, taken from a Android 2.2 Nexus **/
    private final static String NATIVE_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

    private static final Pattern PATTERN_PASSWORD = Pattern.compile("(?<=[\\?&])[Pp]ass(w(or)?d)?=[^&#$]+");

    private final static HttpParams CLIENT_PARAMS = new BasicHttpParams();

    static {
        CLIENT_PARAMS.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, CharEncoding.UTF_8);
        CLIENT_PARAMS.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
        CLIENT_PARAMS.setParameter(CoreConnectionPNames.SO_TIMEOUT, 90000);
        CLIENT_PARAMS.setParameter(ClientPNames.HANDLE_REDIRECTS, true);
    }

    private static String hidePassword(final String message) {
        return PATTERN_PASSWORD.matcher(message).replaceAll("password=***");
    }

    private static HttpClient getHttpClient() {
        final DefaultHttpClient client = new DefaultHttpClient();
        client.setCookieStore(Cookies.cookieStore);
        client.setParams(CLIENT_PARAMS);
        client.setRedirectStrategy(new LaxRedirectStrategy());
        return new DecompressingHttpClient(client);
    }

    /**
     * POST HTTP request
     *
     * @param uri the URI to request
     * @param params the parameters to add to the POST request
     * @return the HTTP response, or null in case of an encoding error params
     */
    @Nullable
    public static HttpResponse postRequest(final String uri, final Parameters params) {
        return request("POST", uri, params, null, null);
    }

    /**
     * POST HTTP request
     *
     * @param uri the URI to request
     * @param params the parameters to add to the POST request
     * @param headers the headers to add to the request
     * @return the HTTP response, or null in case of an encoding error params
     */
    @Nullable
    public static HttpResponse postRequest(final String uri, final Parameters params, final Parameters headers) {
        return request("POST", uri, params, headers, null);
    }

    /**
     *  POST HTTP request with Json POST DATA
     *
     * @param uri the URI to request
     * @param json the json object to add to the POST request
     * @return the HTTP response, or null in case of an encoding error params
     */
    @Nullable
    public static HttpResponse postJsonRequest(final String uri, final ObjectNode json) {
        final HttpPost request = new HttpPost(uri);
        request.addHeader("Content-Type", "application/json; charset=utf-8");
        if (json != null) {
            try {
                request.setEntity(new StringEntity(json.toString(), CharEncoding.UTF_8));
            } catch (final UnsupportedEncodingException e) {
                Log.e("postJsonRequest:JSON Entity: UnsupportedEncodingException", e);
                return null;
            }
        }
        return doLogRequest(request);
    }

    /**
     * Multipart POST HTTP request
     *
     * @param uri the URI to request
     * @param params the parameters to add to the POST request
     * @param fileFieldName the name of the file field name
     * @param fileContentType the content-type of the file
     * @param file the file to include in the request
     * @return the HTTP response, or null in case of an encoding error param
     */
    @Nullable
    public static HttpResponse postRequest(final String uri, final Parameters params,
            final String fileFieldName, final String fileContentType, final File file) {
        final MultipartEntity entity = new MultipartEntity();
        for (final NameValuePair param : params) {
            try {
                entity.addPart(param.getName(), new StringBody(param.getValue(), TextUtils.CHARSET_UTF8));
            } catch (final UnsupportedEncodingException e) {
                Log.e("Network.postRequest: unsupported encoding for parameter " + param.getName(), e);
                return null;
            }
        }
        entity.addPart(fileFieldName, new FileBody(file, fileContentType));

        final HttpPost request = new HttpPost(uri);
        request.setEntity(entity);

        addHeaders(request, null, null);
        return doLogRequest(request);
    }

    /**
     * Make an HTTP request
     *
     * @param method
     *            the HTTP method to use ("GET" or "POST")
     * @param uri
     *            the URI to request
     * @param params
     *            the parameters to add to the URI
     * @param headers
     *            the headers to add to the request
     * @param cacheFile
     *            the cache file used to cache this query
     * @return the HTTP response, or null in case of an encoding error in a POST request arguments
     */
    @Nullable
    private static HttpResponse request(final String method, final String uri,
                                        @Nullable final Parameters params, @Nullable final Parameters headers, @Nullable final File cacheFile) {
        final HttpRequestBase request;
        if (method.equals("GET")) {
            final String fullUri = params == null ? uri : Uri.parse(uri).buildUpon().encodedQuery(params.toString()).build().toString();
            request = new HttpGet(fullUri);
        } else {
            request = new HttpPost(uri);
            if (params != null) {
                try {
                    ((HttpPost) request).setEntity(new UrlEncodedFormEntity(params, CharEncoding.UTF_8));
                } catch (final UnsupportedEncodingException e) {
                    Log.e("request", e);
                    return null;
                }
            }
        }

        addHeaders(request, headers, cacheFile);

        return doLogRequest(request);
    }

    /**
     * Add headers to HTTP request.
     * @param request
     *            the request to add headers to
     * @param headers
     *            the headers to add (in addition to the standard headers), can be null
     * @param cacheFile
     *            if non-null, the file to take ETag and If-Modified-Since information from
     */
    private static void addHeaders(final HttpRequestBase request, @Nullable final Parameters headers, @Nullable final File cacheFile) {
        for (final NameValuePair header : Parameters.extend(Parameters.merge(headers, cacheHeaders(cacheFile)),
                "Accept-Charset", "utf-8,iso-8859-1;q=0.8,utf-16;q=0.8,*;q=0.7",
                "Accept-Language", "en-US,*;q=0.9",
                "X-Requested-With", "XMLHttpRequest")) {
            request.setHeader(header.getName(), header.getValue());
        }
        request.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
                Settings.getUseNativeUa() ? NATIVE_USER_AGENT : PC_USER_AGENT);
    }

    /**
     * Perform an HTTP request and log it.
     *
     * @param request
     *            the request to try
     * @return
     *            the response, or null if there has been a failure
     */
    @Nullable
    private static HttpResponse doLogRequest(final HttpRequestBase request) {
        if (!isNetworkConnected()) {
            return null;
        }

        final String reqLogStr = request.getMethod() + " " + hidePassword(request.getURI().toString());
        Log.d(reqLogStr);

        final HttpClient client = getHttpClient();
        final long before = System.currentTimeMillis();
        try {
            final HttpResponse response = client.execute(request);
            final int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                Log.d(status + formatTimeSpan(before) + reqLogStr);
            } else {
                Log.w(status + " [" + response.getStatusLine().getReasonPhrase() + "]" + formatTimeSpan(before) + reqLogStr);
            }
            return response;
        } catch (final Exception e) {
            final String timeSpan = formatTimeSpan(before);
            Log.w("Failure" + timeSpan + reqLogStr + " (" + e.toString() + ")");
        }

        return null;
    }

    @Nullable
    private static Parameters cacheHeaders(@Nullable final File cacheFile) {
        if (cacheFile == null || !cacheFile.exists()) {
            return null;
        }

        final String etag = LocalStorage.getSavedHeader(cacheFile, LocalStorage.HEADER_ETAG);
        if (etag != null) {
            // The ETag is a more robust check than a timestamp. If we have an ETag, it is enough
            // to identify the right version of the resource.
            return new Parameters("If-None-Match", etag);
        }

        final String lastModified = LocalStorage.getSavedHeader(cacheFile, LocalStorage.HEADER_LAST_MODIFIED);
        if (lastModified != null) {
            return new Parameters("If-Modified-Since", lastModified);
        }

        return null;
    }

    /**
     * GET HTTP request
     *
     * @param uri
     *            the URI to request
     * @param params
     *            the parameters to add the the GET request
     * @param cacheFile
     *            the name of the file storing the cached resource, or null not to use one
     * @return the HTTP response
     */
    @Nullable
    public static HttpResponse getRequest(final String uri, @Nullable final Parameters params, @Nullable final File cacheFile) {
        return request("GET", uri, params, null, cacheFile);
    }


    /**
     * GET HTTP request
     *
     * @param uri
     *            the URI to request
     * @param params
     *            the parameters to add the the GET request
     * @return the HTTP response
     */
    @Nullable
    public static HttpResponse getRequest(final String uri, @Nullable final Parameters params) {
        return request("GET", uri, params, null, null);
    }

    /**
     * GET HTTP request
     *
     * @param uri
     *            the URI to request
     * @param params
     *            the parameters to add the the GET request
     * @param headers
     *            the headers to add to the GET request
     * @return the HTTP response
     */
    @Nullable
    public static HttpResponse getRequest(final String uri, @Nullable final Parameters params, @Nullable final Parameters headers) {
        return request("GET", uri, params, headers, null);
    }

    /**
     * GET HTTP request
     *
     * @param uri
     *            the URI to request
     * @return the HTTP response
     */
    @Nullable
    public static HttpResponse getRequest(final String uri) {
        return request("GET", uri, null, null, null);
    }

    private static String formatTimeSpan(final long before) {
        // don't use String.format in a pure logging routine, it has very bad performance
        return " (" + (System.currentTimeMillis() - before) + " ms) ";
    }

    static public boolean isSuccess(@Nullable final HttpResponse response) {
        return response != null && response.getStatusLine().getStatusCode() == 200;
    }

    static public boolean isPageNotFound(@Nullable final HttpResponse response) {
        return response != null && response.getStatusLine().getStatusCode() == 404;
    }

    /**
     * Get the result of a GET HTTP request returning a JSON body.
     *
     * @param uri the base URI of the GET HTTP request
     * @param params the query parameters, or <code>null</code> if there are none
     * @return a JSON object if the request was successful and the body could be decoded, <code>null</code> otherwise
     */
    @Nullable
    public static ObjectNode requestJSON(final String uri, @Nullable final Parameters params) {
        final HttpResponse response = request("GET", uri, params, new Parameters("Accept", "application/json, text/javascript, */*; q=0.01"), null);
        final String responseData = getResponseData(response, false);
        if (responseData != null) {
            try {
                return (ObjectNode) JsonUtils.reader.readTree(responseData);
            } catch (final IOException e) {
                Log.w("requestJSON", e);
            }
        }

        return null;
    }

    /**
     * Get the input stream corresponding to a HTTP response if it exists.
     *
     * @param response a HTTP response, which can be null
     * @return the input stream if the HTTP request is successful, <code>null</code> otherwise
     */
    @Nullable
    public static InputStream getResponseStream(@Nullable final HttpResponse response) {
        if (!isSuccess(response)) {
            return null;
        }
        assert response != null;
        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        try {
            return entity.getContent();
        } catch (final IOException e) {
            Log.e("Network.getResponseStream", e);
            return null;
        }
    }

    @Nullable
    private static String getResponseDataNoError(final HttpResponse response, final boolean replaceWhitespace) {
        try {
            final String data = EntityUtils.toString(response.getEntity(), CharEncoding.UTF_8);
            return replaceWhitespace ? TextUtils.replaceWhitespace(data) : data;
        } catch (final Exception e) {
            Log.e("getResponseData", e);
            return null;
        }
    }

    /**
     * Get the body of a HTTP response.
     *
     * {@link TextUtils#replaceWhitespace(String)} will be called on the result
     *
     * @param response a HTTP response, which can be null
     * @return the body if the response comes from a successful HTTP request, <code>null</code> otherwise
     */
    @Nullable
    public static String getResponseData(@Nullable final HttpResponse response) {
        return getResponseData(response, true);
    }

    @Nullable
    public static String getResponseDataAlways(@Nullable final HttpResponse response) {
        return response != null ? getResponseDataNoError(response, false) : null;
    }

    /**
     * Get the body of a HTTP response.
     *
     * @param response a HTTP response, which can be null
     * @param replaceWhitespace <code>true</code> if {@link TextUtils#replaceWhitespace(String)}
     *                          should be called on the body
     * @return the body if the response comes from a successful HTTP request, <code>null</code> otherwise
     */
    @Nullable
    public static String getResponseData(@Nullable final HttpResponse response, final boolean replaceWhitespace) {
        if (!isSuccess(response)) {
            return null;
        }
        assert response != null; // Caught above
        return getResponseDataNoError(response, replaceWhitespace);
    }

    @Nullable
    public static String rfc3986URLEncode(final String text) {
        final String encoded = encode(text);
        return encoded != null ? StringUtils.replace(encoded.replace("+", "%20"), "%7E", "~") : null;
    }

    @Nullable
    public static String decode(final String text) {
        try {
            return URLDecoder.decode(text, CharEncoding.UTF_8);
        } catch (final UnsupportedEncodingException e) {
            Log.e("Network.decode", e);
        }
        return null;
    }

    @Nullable
    public static String encode(final String text) {
        try {
            return URLEncoder.encode(text, CharEncoding.UTF_8);
        } catch (final UnsupportedEncodingException e) {
            Log.e("Network.encode", e);
        }
        return null;
    }

    private static ConnectivityManager connectivityManager = null;

    /**
     * Checks if the device has network connection.
     *
     * @return <code>true</code> if the device is connected to the network.
     */
    public static boolean isNetworkConnected() {
        if (connectivityManager == null) {
            // Concurrent assignment would not hurt
            connectivityManager = (ConnectivityManager) CgeoApplication.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
