package cgeo.geocaching.network;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
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
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
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
import java.net.URLEncoder;
import java.util.List;
import java.util.zip.GZIPInputStream;


public abstract class Network {

    static class GzipDecompressingEntity extends HttpEntityWrapper {
        public GzipDecompressingEntity(final HttpEntity entity) {
            super(entity);
        }

        @Override
        public InputStream getContent() throws IOException, IllegalStateException {
            // the wrapped entity's getContent() decides about repeatability
            InputStream wrappedin = wrappedEntity.getContent();
            return new GZIPInputStream(wrappedin);
        }

        @Override
        public long getContentLength() {
            // length of ungzipped content is not known
            return -1;
        }
    }

    private static final int NB_DOWNLOAD_RETRIES = 4;
    /** User agent id */
    private final static String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1";
    private static final String PATTERN_PASSWORD = "(?<=[\\?&])[Pp]ass(w(or)?d)?=[^&#$]+";

    private final static HttpParams clientParams = new BasicHttpParams();
    private static boolean cookieStoreRestored = false;
    private final static CookieStore cookieStore = new BasicCookieStore();

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
        client.setCookieStore(cookieStore);
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
                HttpEntity entity = response.getEntity();
                if (null != entity) {
                    Header ceheader = entity.getContentEncoding();
                    if (ceheader != null) {
                        HeaderElement[] codecs = ceheader.getElements();
                        for (int i = 0; i < codecs.length; i++) {
                            if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                                Log.d(Settings.tag, "Decompressing response");
                                response.setEntity(
                                        new Network.GzipDecompressingEntity(response.getEntity()));
                                return;
                            }
                        }
                    }
                }
            }

        });

        return client;
    }

    public static void restoreCookieStore(final String oldCookies) {
        if (!cookieStoreRestored) {
            Network.clearCookies();
            if (oldCookies != null) {
                for (final String cookie : StringUtils.split(oldCookies, ';')) {
                    final String[] split = StringUtils.split(cookie, "=", 3);
                    if (split.length == 3) {
                        final BasicClientCookie newCookie = new BasicClientCookie(split[0], split[1]);
                        newCookie.setDomain(split[2]);
                        cookieStore.addCookie(newCookie);
                    }
                }
            }
            cookieStoreRestored = true;
        }
    }

    public static String dumpCookieStore() {
        StringBuilder cookies = new StringBuilder();
        for (final Cookie cookie : cookieStore.getCookies()) {
            cookies.append(cookie.getName());
            cookies.append('=');
            cookies.append(cookie.getValue());
            cookies.append('=');
            cookies.append(cookie.getDomain());
            cookies.append(';');
        }
        return cookies.toString();
    }

    public static void clearCookies() {
        cookieStore.clear();
    }

    /**
     * POST HTTP request
     *
     * @param uri
     * @param params
     * @return
     */
    public static HttpResponse postRequest(final String uri, final List<? extends NameValuePair> params) {
        try {
            HttpPost request = new HttpPost(uri);
            if (params != null) {
                request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            }
            request.setHeader("X-Requested-With", "XMLHttpRequest");
            return Network.request(request);
        } catch (Exception e) {
            // Can be UnsupportedEncodingException, ClientProtocolException or IOException
            Log.e(Settings.tag, "postRequest", e);
            return null;
        }
    }

    /**
     * GET HTTP request
     *
     * @param uri
     * @param params
     * @param xContentType
     * @param my
     * @param addF
     * @return
     */
    public static HttpResponse request(final String uri, final Parameters params, boolean xContentType, boolean my, boolean addF) {
        return Network.request(uri, cgBase.addFToParams(params, my, addF), xContentType);
    }

    /**
     * GET HTTP request
     *
     * @param uri
     * @param params
     * @param xContentType
     * @param cacheFile
     *            the name of the file storing the cached resource, or null not to use one
     * @return
     */
    public static HttpResponse request(final String uri, final Parameters params, final boolean xContentType, final File cacheFile) {
        final String fullUri = params == null ? uri : Uri.parse(uri).buildUpon().encodedQuery(params.toString()).build().toString();
        final HttpRequestBase request = new HttpGet(fullUri);

        request.setHeader("X-Requested-With", "XMLHttpRequest");

        if (xContentType) {
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        }

        if (cacheFile != null) {
            final String etag = LocalStorage.getSavedHeader(cacheFile, "etag");
            if (etag != null) {
                request.setHeader("If-None-Match", etag);
            } else {
                final String lastModified = LocalStorage.getSavedHeader(cacheFile, "last-modified");
                if (lastModified != null) {
                    request.setHeader("If-Modified-Since", lastModified);
                }
            }
        }

        return Network.request(request);
    }

    /**
     * GET HTTP request
     *
     * @param uri
     * @param params
     * @param xContentType
     * @return
     */
    public static HttpResponse request(final String uri, final Parameters params, final boolean xContentType) {
        return request(uri, params, xContentType, null);
    }

    public static HttpResponse request(final HttpRequestBase request) {
        request.setHeader("Accept-Charset", "utf-8,iso-8859-1;q=0.8,utf-16;q=0.8,*;q=0.7");
        request.setHeader("Accept-Language", "en-US,*;q=0.9");
        request.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
        return Network.doRequest(request);
    }

    private static HttpResponse doRequest(final HttpRequestBase request) {
        final String reqLogStr = request.getMethod() + " " + hidePassword(request.getURI().toString());
        Log.d(Settings.tag, reqLogStr);

        final HttpClient client = getHttpClient();
        for (int i = 0; i <= NB_DOWNLOAD_RETRIES; i++) {
            final long before = System.currentTimeMillis();
            try {
                final HttpResponse response = client.execute(request);
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    Log.d(Settings.tag, status + Network.formatTimeSpan(before) + reqLogStr);
                } else {
                    Log.w(Settings.tag, status + " [" + response.getStatusLine().getReasonPhrase() + "]" + Network.formatTimeSpan(before) + reqLogStr);
                }
                return response;
            } catch (IOException e) {
                final String timeSpan = Network.formatTimeSpan(before);
                final String tries = (i + 1) + "/" + (NB_DOWNLOAD_RETRIES + 1);
                if (i == NB_DOWNLOAD_RETRIES) {
                    Log.e(Settings.tag, "Failure " + tries + timeSpan + reqLogStr, e);
                } else {
                    Log.e(Settings.tag, "Failure " + tries + " (" + e.toString() + ")" + timeSpan + "- retrying " + reqLogStr);
                }
            }
        }

        return null;
    }

    private static String formatTimeSpan(final long before) {
        // don't use String.format in a pure logging routine, it has very bad performance
        return " (" + (System.currentTimeMillis() - before) + " ms) ";
    }

    static public boolean isSuccess(final HttpResponse response) {
        return response != null && response.getStatusLine().getStatusCode() == 200;
    }

    public static JSONObject requestJSON(final String uri, final Parameters params) {
        final HttpGet request = new HttpGet(Network.prepareParameters(uri, params));
        request.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        request.setHeader("Content-Type", "application/json; charset=UTF-8");
        request.setHeader("X-Requested-With", "XMLHttpRequest");

        final HttpResponse response = doRequest(request);
        if (response != null && response.getStatusLine().getStatusCode() == 200) {
            try {
                return new JSONObject(Network.getResponseData(response));
            } catch (JSONException e) {
                Log.e(Settings.tag, "Network.requestJSON", e);
            }
        }

        return null;
    }

    private static String prepareParameters(final String baseUri, final Parameters params) {
        return CollectionUtils.isNotEmpty(params) ? baseUri + "?" + params.toString() : baseUri;
    }

    private static String getResponseDataNoError(final HttpResponse response, boolean replaceWhitespace) {
        try {
            String data = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
            return replaceWhitespace ? BaseUtils.replaceWhitespace(data) : data;
        } catch (Exception e) {
            Log.e(Settings.tag, "getResponseData", e);
            return null;
        }
    }

    public static String getResponseData(final HttpResponse response) {
        return Network.getResponseData(response, true);
    }

    static public String getResponseData(final HttpResponse response, boolean replaceWhitespace) {
        if (!isSuccess(response)) {
            return null;
        }
        return getResponseDataNoError(response, replaceWhitespace);
    }

    /**
     * POST HTTP request. Do the request a second time if the user is not logged in
     *
     * @param uri
     * @return
     */
    public static String postRequestLogged(final String uri) {
        HttpResponse response = postRequest(uri, null);
        String data = getResponseData(response);

        if (!Login.getLoginStatus(data)) {
            if (Login.login() == StatusCode.NO_ERROR) {
                response = postRequest(uri, null);
                data = getResponseData(response);
            } else {
                Log.i(Settings.tag, "Working as guest.");
            }
        }
        return data;
    }

    /**
     * GET HTTP request. Do the request a second time if the user is not logged in
     *
     * @param uri
     * @param params
     * @param xContentType
     * @param my
     * @param addF
     * @return
     */
    public static String requestLogged(final String uri, final Parameters params, boolean xContentType, boolean my, boolean addF) {
        HttpResponse response = request(uri, params, xContentType, my, addF);
        String data = getResponseData(response);

        if (!Login.getLoginStatus(data)) {
            if (Login.login() == StatusCode.NO_ERROR) {
                response = request(uri, params, xContentType, my, addF);
                data = getResponseData(response);
            } else {
                Log.i(Settings.tag, "Working as guest.");
            }
        }
        return data;
    }

    public static String urlencode_rfc3986(String text) {
        return StringUtils.replace(URLEncoder.encode(text).replace("+", "%20"), "%7E", "~");
    }

}
