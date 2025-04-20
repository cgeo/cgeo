package cgeo.geocaching.network;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxOkHttpUtils;
import cgeo.geocaching.utils.functions.Func1;
import static cgeo.geocaching.network.Network.MEDIA_TYPE_APPLICATION_JSON;
import static cgeo.geocaching.network.Network.MEDIA_TYPE_TEXT_PLAIN;

import androidx.annotation.Nullable;
import androidx.core.util.Supplier;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Convenience class used to ensapculate the building and usage of a HtmlRequest
 */
public class HttpRequest {

    public enum Method { GET, POST, PATCH, PUT }

    private static final ObjectMapper JSON_MAPPER = JsonUtils.mapper;

    private static final String LOGPRAEFIX = "HTTP-";
    public static final String HTTP429 = "HTTP429";
    public static final String HTTP429_ADDRESS = "HTTP429ADDRESS";

    private Method method = null;
    private String uriBase;
    private String uri;
    private final Parameters uriParams = new Parameters();
    private final Parameters headers = new Parameters();

    private long callTimeoutInMs = 0;

    private Func1<Request.Builder, Single<Request.Builder>> requestPreparer;

    private Supplier<RequestBody> requestBodySupplier = null;

    public HttpRequest uri(final String uri) {
        this.uri = uri;
        return this;
    }

    public HttpRequest uriBase(final String uriBase) {
        this.uriBase = uriBase;
        return this;
    }

    public HttpRequest method(final Method method) {
        this.method = method;
        return this;
    }

    public HttpRequest uriParams(final Parameters uriParams) {
        Parameters.merge(this.uriParams, uriParams);
        return this;
    }

    public HttpRequest uriParams(final String... keyValues) {
        uriParams.put(keyValues);
        return this;
    }

    public HttpRequest headers(final Parameters headers) {
        Parameters.merge(this.headers, headers);
        return this;
    }

    public HttpRequest headers(final String... keyValues) {
        headers.put(keyValues);
        return this;
    }

    public HttpRequest callTimeout(final long callTimeoutInMs) {
        this.callTimeoutInMs = callTimeoutInMs;
        return this;
    }

    public HttpRequest requestPreparer(final Func1<Request.Builder, Single<Request.Builder>> requestPreparer) {
        this.requestPreparer = requestPreparer;
        return this;
    }

    public Single<HttpResponse> request() {
        return requestInternal(r -> r);
    }

    public <T> Single<T> requestJson(final Class<T> clazz) {
        headers("Accept", "application/json, text/javascript, */*; q=0.01");
        return requestInternal(r -> {
            T result;
            final String bodyString = r.getBodyString();
            r.close();
            try {
                result = JSON_MAPPER.readValue(bodyString, clazz);
            } catch (JsonProcessingException jpe) {
                final String errorMsg = LOGPRAEFIX + "ERR: could not parse json String to '" + clazz.getName() + "': " + bodyString;
                if (HttpResponse.class.isAssignableFrom(clazz)) {
                    Log.w(errorMsg, jpe);
                    try {
                        result = clazz.newInstance();
                        ((HttpResponse) result).setFailed(jpe);
                    } catch (ReflectiveOperationException roe) {
                        //should never happen, but in case it does...
                        throw new IllegalStateException(LOGPRAEFIX + "ERR: Couldn't create class instance: '" + clazz.getName() + "'", roe);
                    }
                } else {
                    throw new IllegalArgumentException(errorMsg, jpe);
                }
            }
            return result;
        });
    }

    private <T> Single<T> requestInternal(final Function<HttpResponse, T> mapper) {
        final Request.Builder reqBuilder = prepareRequest();

        //execute, prepare if necessary
        final Single<Response> rawResponse;
        if (requestPreparer != null) {
            rawResponse = requestPreparer.call(reqBuilder).flatMap(this::executeRequest);
        } else {
            rawResponse = executeRequest(reqBuilder);
        }

        //map response
        final Single<T> result = rawResponse.map(r -> {
            final HttpResponse response = new HttpResponse();
            response.setResponse(r);
            final T mappedResponse = mapper.apply(response);
            if (mappedResponse instanceof HttpResponse && response != mappedResponse) {
                ((HttpResponse) mappedResponse).setFromHttpResponse(response);
            }
            if (response.getStatusCode() == 429) {
                Log.w("Request throttled: " + this.getRequestUrl());
                LifecycleAwareBroadcastReceiver.sendBroadcast(CgeoApplication.getInstance(), HTTP429, HTTP429_ADDRESS, Objects.requireNonNull(HttpUrl.parse(Objects.requireNonNull(getRequestUrl()))).host());
            }
            return mappedResponse;
        });

        return result;
    }

    private Single<Response> executeRequest(final Request.Builder reqBuilder) {
        final Request req = reqBuilder.build();

        OkHttpClient httpClient = Network.OK_HTTP_CLIENT;
        if (this.callTimeoutInMs > 0) {
            httpClient = httpClient.newBuilder().callTimeout(this.callTimeoutInMs, TimeUnit.MILLISECONDS).build();
        }

        return RxOkHttpUtils.request(httpClient, req);
    }

    /** Returns request url. Meant for logging purposes etc */
    @Nullable
    public String getRequestUrl() {
        try {
            final HttpUrl url = constructRequestUrl();
            if (url != null) {
                return url.toString();
            }
        } catch (RuntimeException re) {
           //ignore
        }
        return null;
    }


    private HttpUrl constructRequestUrl() {
        //Uri
        final String finalUri = getFinalUri();
        final HttpUrl httpUrl = HttpUrl.parse(finalUri);
        if (httpUrl == null) {
            throw new IllegalStateException(LOGPRAEFIX + "ERRNon-parseable uri: " + finalUri);
        }
        final HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        if (!uriParams.isEmpty()) {
            urlBuilder.encodedQuery(uriParams.toString());
        }
        return urlBuilder.build();
    }


    private Request.Builder prepareRequest() {
        final Request.Builder builder = new Request.Builder();

        //Uri
        builder.url(constructRequestUrl());

        //method and body
        final Method m = (method != null ? method : (requestBodySupplier == null ? Method.GET : Method.POST));
        final Supplier<RequestBody> rbs = requestBodySupplier != null ? requestBodySupplier : () -> new FormBody.Builder().build();
        switch (m) {
            case POST:
                builder.post(rbs.get());
                break;
            case PATCH:
                builder.patch(rbs.get());
                break;
            case PUT:
                builder.put(rbs.get());
                break;
            case GET:
            default:
                builder.get();
                break;
        }

        //header
        for (final ImmutablePair<String, String> header : headers) {
            builder.header(header.left, header.right);
        }

        return builder;
    }

    private String getFinalUri() {
        return uriBase == null ? uri : uriBase + uri;
    }


    /** sets body to a simple key-value-pair list */
    public HttpRequest body(final Parameters params) {
        this.requestBodySupplier = () -> {
            final FormBody.Builder body = new FormBody.Builder();
            if (params != null) {
                for (final ImmutablePair<String, String> param : params) {
                    body.add(param.left, param.right);
                }
            }
            return body.build();
        };
        return this;
    }

    /**
     * Sets body to Multipart-FORM, including one optional File body part.
     * Set "file" to null to not pass a file. If "file" is not null, then "fileMediaType" must be set as well
     */
    public HttpRequest bodyForm(@Nullable final Parameters formParams, @Nullable final String fileFormFieldName, @Nullable final String fileMediaType, @Nullable final File file) {
        this.requestBodySupplier = () -> {
            final MultipartBody.Builder entity = new MultipartBody.Builder().setType(MultipartBody.FORM);
            if (formParams != null) {
                for (final ImmutablePair<String, String> param : formParams) {
                    entity.addFormDataPart(param.left, param.right);
                }
            }
            if (file != null) {
                final MediaType mediaType = MediaType.parse(fileMediaType);
                if (mediaType == null) {
                    throw new IllegalStateException(LOGPRAEFIX + "ERR: Invalid mediaType for file " + file + ": " + fileMediaType);
                }
                entity.addFormDataPart(fileFormFieldName == null ? "file" : fileFormFieldName, file.getName(),
                        RequestBody.create(file, mediaType));
            }
            return entity.build();
        };
        return this;
    }

    public HttpRequest bodyForm(final Parameters formParams) {
        this.requestBodySupplier = () -> {
            final FormBody.Builder entity = new FormBody.Builder();
            if (formParams != null) {
                for (final ImmutablePair<String, String> param : formParams) {
                    entity.add(param.left, param.right);
                }
            }
            return entity.build();
        };
        return this;
    }

    /** Sets body to plain text */
    public HttpRequest body(final String text) {
        this.requestBodySupplier = () -> RequestBody.create(text, MEDIA_TYPE_TEXT_PLAIN);
        return this;
    }

    /** Sets body to Json parsed from given object */
    public HttpRequest bodyJson(final Object jsonObject) {
        final String jsonString = getJsonBody(jsonObject);
        Log.d("HTTP-JSON: attempt to send: " + jsonString);
        this.requestBodySupplier = () -> RequestBody.create(jsonString, MEDIA_TYPE_APPLICATION_JSON);
        return this;
    }

    public static String getJsonBody(final Object jsonObject) {
        try {
            return JSON_MAPPER.writeValueAsString(jsonObject);
        } catch (JsonProcessingException jpe) {
            throw new IllegalArgumentException(LOGPRAEFIX + "ERR: Could not parse as Json: " + jsonObject, jpe);
        }
    }

    public static String safeGetJsonBody(final Object jsonObject) {
        try {
            return getJsonBody(jsonObject);
        } catch (Exception ex) {
            return null;
        }
    }

}
