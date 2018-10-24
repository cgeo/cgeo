package cgeo.geocaching.connector.su;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.OAuthTokens;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.JsonUtils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class SuApi {

    private SuApi() {
        // utility class
    }

    @Nullable
    public static Geocache searchByGeocode(final String geocode) {
        final IConnector connector = ConnectorFactory.getConnector(geocode);
        if (!(connector instanceof SuConnector)) {
            return null;
        }
        final SuConnector gcsuConn = (SuConnector) connector;

        final String id = StringUtils.substring(geocode, 2);

        final Parameters params = new Parameters("id", id);
        final JSONResult result = SuApi.getRequest(gcsuConn, SuApiEndpoint.CACHE, params);

        if (result.data.get("status").get("code").toString().contains("ERROR") && result.data.get("status").get("description").toString().contains("not found")) {
            throw new CacheNotFoundException();
        }

        return SuParser.parseCache(result.data);
    }

    @NonNull
    public static List<Geocache> searchByBBox(final Viewport viewport, @NonNull final SuConnector connector) {
        if (viewport.getLatitudeSpan() == 0 || viewport.getLongitudeSpan() == 0) {
            return Collections.emptyList();
        }

        final Parameters params = new Parameters(
                "minlat", String.valueOf(viewport.getLatitudeMin()),
                "maxlat", String.valueOf(viewport.getLatitudeMax()),
                "minlng", String.valueOf(viewport.getLongitudeMin()),
                "maxlng", String.valueOf(viewport.getLongitudeMax()));

        final JSONResult result = SuApi.getRequest(connector, SuApiEndpoint.CACHE_LIST, params);
        return SuParser.parseCaches(result.data);
    }

    private static String getSuLogType(final LogType logType) {
        switch (logType) {
            case FOUND_IT:
                return "found";
            case DIDNT_FIND_IT:
                return "notFound";
            case NOTE:
            default:
                return "comment";
        }
    }

    @NonNull
    public static LogResult postLog(@NonNull final Geocache cache, @NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log) {
        final IConnector connector = ConnectorFactory.getConnector(cache.getGeocode());
        if (!(connector instanceof SuConnector)) {
            return new LogResult(StatusCode.LOG_POST_ERROR, "");
        }
        final SuConnector gcsuConnector = (SuConnector) connector;

        final Parameters params = new Parameters();
        params.add("cacheID", cache.getCacheId());
        params.add("type", getSuLogType(logType));
        params.add("text", log);

        final ObjectNode data = postRequest(gcsuConnector, SuApiEndpoint.NOTE, params).data;

        if (data == null) {
            return new LogResult(StatusCode.LOG_POST_ERROR, "");
        }
        if (data.get("status").get("code").toString().contains("ERROR")) {
            return new LogResult(StatusCode.LOG_POST_ERROR, "");
        }

        return new LogResult(StatusCode.NO_ERROR, data.get("data").get("noteID").asText());
    }

    @NonNull
    private static JSONResult getRequest(@NonNull final SuConnector connector, @NonNull final SuApiEndpoint endpoint, @NonNull final Parameters params) {
        return request(connector, endpoint, "GET", params);
    }

    @NonNull
    private static JSONResult postRequest(@NonNull final SuConnector connector, @NonNull final SuApiEndpoint endpoint, @NonNull final Parameters params) {
        return request(connector, endpoint, "POST", params);
    }

    @NonNull
    private static JSONResult request(@NonNull final SuConnector connector, @NonNull final SuApiEndpoint endpoint, @NonNull final String method, @NonNull final Parameters params) {
        final String host = connector.getHost();
        if (StringUtils.isBlank(host)) {
            return new JSONResult("unknown connector host");
        }

        final SuLogin suLogin = SuLogin.getInstance();

        //TODO: Try to re-login if needed ?
//        if (suLogin.oAtoken.isEmpty() && !suLogin.getActualUserName().isEmpty()) {
//            suLogin.login(true);
//        }

        final OAuthTokens tokens = suLogin.getOAuthTokens();
        if (!tokens.isValid()) {
            throw new NotAuthorizedException();
        }
        OAuth.signOAuth(host, endpoint.methodName, method, connector.getHttps(), params, tokens, connector.getConsumerKey(), connector.getConsumerSecret());

        final String uri = connector.getHostUrl() + endpoint.methodName;
        final JSONResult result;
        try {
            if ("GET".equals(method)) {
                result = new JSONResult(Network.getRequest(uri, params).blockingGet());
            } else {
                result = new JSONResult(Network.postRequest(uri, params).blockingGet());
            }
        } catch (final Exception e) {
            throw new ConnectionErrorException();
        }

        if (result.data.get("status").get("code").toString().contains("ERROR") && result.data.get("status").get("type").toString().contains("AuthorisationRequired")) {
            throw new NotAuthorizedException();
        }
        return result;
    }

    static class CacheNotFoundException extends RuntimeException {
    }

    static class ConnectionErrorException extends RuntimeException {
    }

    static class NotAuthorizedException extends RuntimeException {
    }

    private static class JSONResult {
        public final boolean isSuccess;
        public final ObjectNode data;

        JSONResult(final Response response) {
            ObjectNode tempData = null;
            try {
                tempData = (ObjectNode) JsonUtils.reader.readTree(response.body().byteStream());
            } catch (final Exception e) {
                // ignore
            } finally {
                response.close();
            }
            data = tempData;
            isSuccess = response.isSuccessful() && tempData != null;
        }

        JSONResult(@NonNull final String errorMessage) {
            isSuccess = false;
            data = new ObjectNode(JsonUtils.factory);
            data.putObject("error").put("developer_message", errorMessage);
        }
    }
}
