package cgeo.geocaching.network;

import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;

public abstract class Network {

    static class GzipDecompressingEntity extends HttpEntityWrapper {
        public GzipDecompressingEntity(final HttpEntity entity) {
            super(entity);
        }

        @Override
        public InputStream getContent() throws IOException, IllegalStateException {
            // the wrapped entity's getContent() decides about repeatability
            return new GZIPInputStream(wrappedEntity.getContent());
        }

        @Override
        public long getContentLength() {
            // length of gunzipped content is not known
            return -1;
        }
    }

    private static final int NB_DOWNLOAD_RETRIES = 4;
    /** User agent id */
    private final static String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1";
    private static final String PATTERN_PASSWORD = "(?<=[\\?&])[Pp]ass(w(or)?d)?=[^&#$]+";

    private final static HttpParams clientParams = new BasicHttpParams();

    static {
        Network.clientParams.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
        Network.clientParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
        Network.clientParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);
    }

    private static String hidePassword(final String message) {
        return message.replaceAll(Network.PATTERN_PASSWORD, "password=***");
    }

    private static HttpClient getHttpClient() {
        final DefaultHttpClient client = new DefaultHttpClient();
        client.setCookieStore(Cookies.cookieStore);
        client.setParams(clientParams);

        client.addRequestInterceptor(new HttpRequestInterceptor() {

            @Override
            public void process(
                    final HttpRequest request,
                    final HttpContext context) throws HttpException, IOException {
                if (!request.containsHeader("Accept-Encoding")) {
                    request.addHeader("Accept-Encoding", "gzip");
                }
            }
        });
        client.addResponseInterceptor(new HttpResponseInterceptor() {

            public void process(
                    final HttpResponse response,
                    final HttpContext context) throws HttpException, IOException {
                final HttpEntity entity = response.getEntity();
                if (entity != null) {
                    final Header contentEncoding = entity.getContentEncoding();
                    if (contentEncoding != null) {
                        for (final HeaderElement codec : contentEncoding.getElements()) {
                            if (codec.getName().equalsIgnoreCase("gzip")) {
                                Log.d("Decompressing response");
                                response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                                return;
                            }
                        }
                    }
                }
            }

        });

        return client;
    }

    /**
     * POST HTTP request
     *
     * @param uri the URI to request
     * @param params the parameters to add to the POST request
     * @return the HTTP response, or null in case of an encoding error params
     */
    public static HttpResponse postRequest(final String uri, final Parameters params) {
        return request("POST", uri, params, null, null);
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
    public static HttpResponse postRequest(final String uri, final Parameters params,
                                           final String fileFieldName, final String fileContentType, final File file) {
        final MultipartEntity entity = new MultipartEntity();
        for (final NameValuePair param : params) {
            try {
                entity.addPart(param.getName(), new StringBody(param.getValue()));
            } catch (final UnsupportedEncodingException e) {
                Log.e("Network.postRequest: unsupported encoding for parameter " + param.getName(), e);
                return null;
            }
        }
        entity.addPart(fileFieldName, new FileBody(file, fileContentType));

        final HttpPost request = new HttpPost(uri);
        request.setEntity(entity);

        addHeaders(request, null, null);
        return doRepeatedRequests(request);
    }

    /**
     * Make an HTTP request
     *
     * @param method
     *            the HTTP method to use ("GET" or "POST")
     * @param uri
     *            the URI to request
     * @param params
     *            the parameters to add the the GET request
     * @param headers
     *            the headers to add to the GET request
     * @param cacheFile
     *            the cache file used to cache this query
     * @return the HTTP response, or null in case of an encoding error in a POST request arguments
     */
    private static HttpResponse request(final String method, final String uri, final Parameters params, final Parameters headers, final File cacheFile) {
        HttpRequestBase request;
        if (method.equals("GET")) {
            final String fullUri = params == null ? uri : Uri.parse(uri).buildUpon().encodedQuery(params.toString()).build().toString();
            request = new HttpGet(fullUri);
        } else {
            request = new HttpPost(uri);
            if (params != null) {
                try {
                    ((HttpPost) request).setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
                } catch (final UnsupportedEncodingException e) {
                    Log.e("request", e);
                    return null;
                }
            }
        }

        addHeaders(request, headers, cacheFile);

        return doRepeatedRequests(request);
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
    private static void addHeaders(final HttpRequestBase request, final Parameters headers, final File cacheFile) {
        for (final NameValuePair header : Parameters.extend(Parameters.merge(headers, cacheHeaders(cacheFile)),
                "Accept-Charset", "utf-8,iso-8859-1;q=0.8,utf-16;q=0.8,*;q=0.7",
                "Accept-Language", "en-US,*;q=0.9",
                "X-Requested-With", "XMLHttpRequest")) {
            request.setHeader(header.getName(), header.getValue());
        }
        request.getParams().setParameter(CoreProtocolPNames.USER_AGENT, Network.USER_AGENT);
    }

    /**
     * Retry a request for a few times.
     *
     * @param request
     *            the request to try
     * @return
     *            the response, or null if there has been a failure
     */
    private static HttpResponse doRepeatedRequests(final HttpRequestBase request) {
        final String reqLogStr = request.getMethod() + " " + Network.hidePassword(request.getURI().toString());
        Log.d(reqLogStr);

        final HttpClient client = Network.getHttpClient();
        for (int i = 0; i <= Network.NB_DOWNLOAD_RETRIES; i++) {
            final long before = System.currentTimeMillis();
            try {
                final HttpResponse response = client.execute(request);
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    Log.d(status + Network.formatTimeSpan(before) + reqLogStr);
                } else {
                    Log.w(status + " [" + response.getStatusLine().getReasonPhrase() + "]" + Network.formatTimeSpan(before) + reqLogStr);
                }
                return response;
            } catch (IOException e) {
                final String timeSpan = Network.formatTimeSpan(before);
                final String tries = (i + 1) + "/" + (Network.NB_DOWNLOAD_RETRIES + 1);
                if (i == Network.NB_DOWNLOAD_RETRIES) {
                    Log.e("Failure " + tries + timeSpan + reqLogStr, e);
                } else {
                    Log.e("Failure " + tries + " (" + e.toString() + ")" + timeSpan + "- retrying " + reqLogStr);
                }
            }
        }

        return null;
    }

    private static Parameters cacheHeaders(final File cacheFile) {
        if (cacheFile == null || !cacheFile.exists()) {
            return null;
        }

        final String etag = LocalStorage.getSavedHeader(cacheFile, "etag");
        if (etag != null) {
            return new Parameters("If-None-Match", etag);
        }

        final String lastModified = LocalStorage.getSavedHeader(cacheFile, "last-modified");
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
    public static HttpResponse getRequest(final String uri, final Parameters params, final File cacheFile) {
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
    public static HttpResponse getRequest(final String uri, final Parameters params) {
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
    public static HttpResponse getRequest(final String uri, final Parameters params, final Parameters headers) {
        return request("GET", uri, params, headers, null);
    }

    /**
     * GET HTTP request
     *
     * @param uri
     *            the URI to request
     * @return the HTTP response
     */
    public static HttpResponse getRequest(final String uri) {
        return request("GET", uri, null, null, null);
    }

    private static String formatTimeSpan(final long before) {
        // don't use String.format in a pure logging routine, it has very bad performance
        return " (" + (System.currentTimeMillis() - before) + " ms) ";
    }

    static public boolean isSuccess(final HttpResponse response) {
        return response != null && response.getStatusLine().getStatusCode() == 200;
    }

    public static JSONObject requestJSON(final String uri, final Parameters params) {
        final HttpResponse response = request("GET", uri, params, new Parameters("Accept", "application/json, text/javascript, */*; q=0.01"), null);
        if (isSuccess(response)) {
            try {
                return new JSONObject(Network.getResponseData(response));
            } catch (final JSONException e) {
                Log.e("Network.requestJSON", e);
            }
        }

        return null;
    }

    private static String getResponseDataNoError(final HttpResponse response, boolean replaceWhitespace) {
        try {
            String data = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
            return replaceWhitespace ? BaseUtils.replaceWhitespace(data) : data;
        } catch (Exception e) {
            Log.e("getResponseData", e);
            return null;
        }
    }

    public static String getResponseData(final HttpResponse response) {
        return Network.getResponseData(response, true);
    }

    public static String getResponseData(final HttpResponse response, boolean replaceWhitespace) {
        if (!isSuccess(response)) {
            return null;
        }
        return getResponseDataNoError(response, replaceWhitespace);
    }

    public static String rfc3986URLEncode(String text) {
        return StringUtils.replace(URLEncoder.encode(text).replace("+", "%20"), "%7E", "~");
    }

}
