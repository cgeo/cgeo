package cgeo.geocaching.network;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxOkHttpUtils;
import cgeo.geocaching.utils.functions.Func1;
import static cgeo.geocaching.network.Network.MEDIA_TYPE_APPLICATION_JSON;
import static cgeo.geocaching.network.Network.MEDIA_TYPE_TEXT_PLAIN;

import androidx.core.util.Supplier;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Convenience class used to ensapculate the building and usage of a HtmlRequest
 */
public class HttpRequest {

    public enum Method { GET, POST, PATCH }

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private Method method = null;
    private String uriBase;
    private String uri;
    private final Parameters uriParams = new Parameters();
    private final Parameters headers = new Parameters();

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

    public HttpRequest requestPreparer(final Func1<Request.Builder, Single<Request.Builder>> requestPreparer) {
        this.requestPreparer = requestPreparer;
        return this;
    }

    public Single<HttpResponse> request() {
        return requestInternal(r -> new HttpResponse());
    }

    public <T> Single<T> requestJson(final Class<T> clazz) {
        headers("Accept", "application/json, text/javascript, */*; q=0.01");
        return requestInternal(r -> {
            try {
                final String bodyString = HttpResponse.getBodyString(r);
                final T result = JSON_MAPPER.readValue(bodyString, clazz);
                if (result instanceof HttpResponse) {
                    ((HttpResponse) result).setBodyString(bodyString);
                }
                return result;
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not JSONize to " + clazz, e);
            }
        });
    }

    private <T> Single<T> requestInternal(final Function<Response, T> mapper) {
        final Request.Builder reqBuilder = prepareRequest();

        //execute, prepare if necessary
        final Single<Response> response;
        if (requestPreparer != null) {
            response = requestPreparer.call(reqBuilder).flatMap(this::executeRequest);
        } else {
            response = executeRequest(reqBuilder);
        }

        //map response
        return response.map(r -> {
            final T result = mapper.apply(r);
            if (result instanceof HttpResponse) {
                ((HttpResponse) result).setResponse(r);
            }
            return result;
        });
    }

    private Single<Response> executeRequest(final Request.Builder reqBuilder) {
        final Request req = reqBuilder.build();
        if (Log.isDebug()) {
            Log.d("HTTP-" + req.method() + ": " + req.url());
        }
        return RxOkHttpUtils.request(Network.OK_HTTP_CLIENT, req);
    }


    private Request.Builder prepareRequest() {
        final Request.Builder builder = new Request.Builder();

        //Uri
        final String finalUri = uriBase == null ? uri : uriBase + uri;
        final HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(finalUri)).newBuilder();
        if (!uriParams.isEmpty()) {
            urlBuilder.encodedQuery(uriParams.toString());
        }
        builder.url(urlBuilder.build());

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

    public HttpRequest body(final Parameters params, final String fileFieldName, final String fileContentType, final File file) {
        this.requestBodySupplier = () -> {
            final MultipartBody.Builder entity = new MultipartBody.Builder().setType(MultipartBody.FORM);
            if (params != null) {
                for (final ImmutablePair<String, String> param : params) {
                    entity.addFormDataPart(param.left, param.right);
                }
            }
            entity.addFormDataPart(fileFieldName, file.getName(),
                    RequestBody.create(file, MediaType.parse(fileContentType)));
            return entity.build();
        };
        return this;
    }

    public HttpRequest body(final String text) {
        this.requestBodySupplier = () -> RequestBody.create(text, MEDIA_TYPE_TEXT_PLAIN);
        return this;
    }

    public HttpRequest body(final Object jsonObject) {
        try {
            final String jsonString = JSON_MAPPER.writeValueAsString(jsonObject);
            this.requestBodySupplier = () -> RequestBody.create(jsonString, MEDIA_TYPE_APPLICATION_JSON);
            return this;
        } catch (JsonProcessingException jpe) {
            throw new IllegalArgumentException("Could not parse as Json: " + jsonObject, jpe);
        }
    }


}
