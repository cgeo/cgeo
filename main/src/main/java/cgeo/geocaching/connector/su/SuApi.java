package cgeo.geocaching.connector.su;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.UserInfo;
import cgeo.geocaching.connector.UserInfo.UserInfoStatus;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.OAuthTokens;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import static cgeo.geocaching.connector.capability.ILogin.UNKNOWN_FINDS;

import android.util.Base64;
import static android.util.Base64.DEFAULT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class SuApi {

    private static final SynchronizedDateFormat LOG_DATE_FORMAT = new SynchronizedDateFormat("yyyy-MM-dd", Locale.US);

    private SuApi() {
        // utility class
    }


    @NonNull
    @WorkerThread
    public static UserInfo getUserInfo(@NonNull final SuConnector connector) throws SuApiException {
        final Parameters params = new Parameters();

        final JSONResult result = getRequest(connector, SuApiEndpoint.USER, params);

        if (!result.isSuccess) {
            return new UserInfo(StringUtils.EMPTY, UNKNOWN_FINDS, UserInfoStatus.FAILED);
        }

        return SuParser.parseUser(result.data);
    }

    @Nullable
    @WorkerThread
    public static Geocache searchByGeocode(final String geocode) throws SuApiException {
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
    @WorkerThread
    public static List<Geocache> searchByBBox(final Viewport viewport, @NonNull final SuConnector connector) throws SuApiException {
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

    @NonNull
    @WorkerThread
    public static List<Geocache> searchByKeyword(final String keyword, @NonNull final SuConnector connector) throws SuApiException {
        if (keyword.isEmpty()) {
            return Collections.emptyList();
        }

        final Parameters params = new Parameters(
                "keyword", keyword);

        final JSONResult result = SuApi.getRequest(connector, SuApiEndpoint.CACHE_LIST_KEYWORD, params);
        return SuParser.parseCaches(result.data);
    }

    @NonNull
    @WorkerThread
    public static List<Geocache> searchByOwner(final String owner, @NonNull final SuConnector connector) throws SuApiException {
        if (owner.isEmpty()) {
            return Collections.emptyList();
        }

        final Parameters params = new Parameters(
                "owner", owner);

        final JSONResult result = SuApi.getRequest(connector, SuApiEndpoint.CACHE_LIST_OWNER, params);
        return SuParser.parseCaches(result.data);
    }

    /**
     * Returns list of caches located around {@code center}
     *
     * @param center    coordinates of the central point
     * @param radius    radius in km
     * @param connector Geocaching connector
     * @return list of caches located around {@code center}
     */
    @NonNull
    @WorkerThread
    public static List<Geocache> searchByCenter(final Geopoint center, final float radius, @NonNull final SuConnector connector) throws SuApiException {
        final Parameters params = new Parameters(
                "lat", String.valueOf(center.getLatitude()),
                "lng", String.valueOf(center.getLongitude()),
                "radius", Float.toString(radius));

        final JSONResult result = SuApi.getRequest(connector, SuApiEndpoint.CACHE_LIST_CENTER, params);
        return SuParser.parseCaches(result.data);
    }

    @NonNull
    @WorkerThread
    public static List<Geocache> searchByFilter(@NonNull final GeocacheFilter filter, @NonNull final SuConnector connector) throws SuApiException {

        //for now we have to assume that SUConnector supports only SINGLE criteria search

        final List<BaseGeocacheFilter> filters = filter.getAndChainIfPossible();
        final OriginGeocacheFilter of = GeocacheFilter.findInChain(filters, OriginGeocacheFilter.class);
        if (of != null && !of.allowsCachesOf(connector)) {
            return new ArrayList<>();
        }
        final DistanceGeocacheFilter df = GeocacheFilter.findInChain(filters, DistanceGeocacheFilter.class);
        if (df != null) {
            return searchByCenter(df.getEffectiveCoordinate(), df.getMaxRangeValue() == null ? 20f : df.getMaxRangeValue(), connector);
        }
        final NameGeocacheFilter nf = GeocacheFilter.findInChain(filters, NameGeocacheFilter.class);
        if (nf != null && !StringUtils.isEmpty(nf.getStringFilter().getTextValue())) {
            return searchByKeyword(nf.getStringFilter().getTextValue(), connector);
        }
        final OwnerGeocacheFilter ownf = GeocacheFilter.findInChain(filters, OwnerGeocacheFilter.class);
        if (ownf != null && !StringUtils.isEmpty(ownf.getStringFilter().getTextValue())) {
            return searchByOwner(ownf.getStringFilter().getTextValue(), connector);
        }

        //by default, search around current position
        return searchByCenter(LocationDataProvider.getInstance().currentGeo().getCoords(), 20f, connector);
    }

    private static String getSuLogType(final LogType logType) {
        switch (logType) {
            case FOUND_IT:
                return "found";
            case DIDNT_FIND_IT:
                return "notFound";
            case OWNER_MAINTENANCE:
                return "authorCheck";
            case NOTE:
            default:
                return "comment";
        }
    }

    @NonNull
    @WorkerThread
    public static LogResult postLog(@NonNull final Geocache cache, @NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, final boolean addRecommendation) throws SuApiException {
        final IConnector connector = ConnectorFactory.getConnector(cache.getGeocode());
        if (!(connector instanceof SuConnector)) {
            return new LogResult(StatusCode.LOG_POST_ERROR, "");
        }
        final SuConnector gcsuConnector = (SuConnector) connector;

        final Parameters params = new Parameters();
        params.add("cacheID", cache.getCacheId());
        params.add("type", getSuLogType(logType));
        params.add("text", log);
        params.add("add_recommendation", addRecommendation ? "true" : "false");
        params.add("find_date", LOG_DATE_FORMAT.format(date.getTime()));

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
    private static String createImageCaption(final Image image) {
        final StringBuilder caption = new StringBuilder(StringUtils.trimToEmpty(image.getTitle()));
        if (StringUtils.isNotEmpty(caption) && StringUtils.isNotBlank(image.getDescription())) {
            caption.append(": ");
        }
        caption.append(StringUtils.trimToEmpty(image.getDescription()));
        return caption.toString();
    }

    @NonNull
    public static ImageResult postImage(final Geocache cache, final Image image) {
        final IConnector connector = ConnectorFactory.getConnector(cache.getGeocode());
        if (!(connector instanceof SuConnector)) {
            return new ImageResult(StatusCode.LOGIMAGE_POST_ERROR, "");
        }
        final SuConnector gcsuConnector = (SuConnector) connector;

        final File file = image.getFile();
        if (file == null) {
            return new ImageResult(StatusCode.LOGIMAGE_POST_ERROR, "");
        }
        final Parameters params = new Parameters("cacheID", cache.getCacheId());
        FileInputStream fileStream = null;
        try {
            fileStream = new FileInputStream(file);
            params.add("image", Base64.encodeToString(IOUtils.readFully(fileStream, (int) file.length()), DEFAULT));
            params.add("caption", createImageCaption(image));

            final ObjectNode data = postRequest(gcsuConnector, SuApiEndpoint.POST_IMAGE, params).data;

            if (data != null && data.get("data").get("success").asBoolean()) {
                return new ImageResult(StatusCode.NO_ERROR, data.get("data").get("image_url").asText());
            }

        } catch (final Exception e) {
            Log.e("SuApi.postLogImage", e);
        } finally {
            IOUtils.closeQuietly(fileStream);
        }
        return new ImageResult(StatusCode.LOGIMAGE_POST_ERROR, "");
    }

    @WorkerThread
    public static boolean setWatchState(@NonNull final Geocache cache, final boolean watched) {
        final Parameters params = new Parameters("cacheID", cache.getCacheId());
        params.add("watched", watched ? "true" : "false");

        try {
            postRequest(SuConnector.getInstance(), SuApiEndpoint.MARK, params);
        } catch (final SuApiException e) {
            return false;
        }

        cache.setOnWatchlist(watched);
        DataStore.saveChangedCache(cache);

        return true;
    }

    @WorkerThread
    public static boolean setIgnoreState(@NonNull final Geocache cache, final boolean ignored) {
        final Parameters params = new Parameters("cacheID", cache.getCacheId());
        params.add("ignored", ignored ? "true" : "false");

        try {
            postRequest(SuConnector.getInstance(), SuApiEndpoint.IGNORE, params);
        } catch (final SuApiException e) {
            return false;
        }

        //cache.setOnWatchlist(ignored);
        //DataStore.saveChangedCache(cache);

        return true;
    }

    @WorkerThread
    public static boolean setRecommendation(@NonNull final Geocache cache, final boolean status) {
        final Parameters params = new Parameters("cacheID", cache.getCacheId());
        params.add("recommend", status ? "true" : "false");

        final ObjectNode data;
        try {
            data = postRequest(SuConnector.getInstance(), SuApiEndpoint.RECOMMENDATION, params).data;
        } catch (final SuApiException e) {
            return false;
        }
        if (data == null || data.get("status").get("code").toString().contains("ERROR")) {
            return false;
        }

        cache.setFavorite(status);
        cache.setFavoritePoints(cache.getFavoritePoints() + (status ? 1 : -1));

        DataStore.saveChangedCache(cache);

        return true;
    }

    @WorkerThread
    public static boolean postVote(@NonNull final Geocache cache, final float rating) {
        final Parameters params = new Parameters("cacheID", cache.getCacheId());
        params.add("value", String.valueOf(rating));

        try {
            postRequest(SuConnector.getInstance(), SuApiEndpoint.VALUE, params);
        } catch (final SuApiException e) {
            return false;
        }

        // TODO: get updated rating?
        //cache.setRating(rating.getRating());

        cache.setVotes(cache.getVotes() + 1);
        cache.setMyVote(rating);
        DataStore.saveChangedCache(cache);

        return true;
    }

    @WorkerThread
    public static int getAvailableRecommendations() {
        // Nothing here as we want to get info about current user
        final Parameters params = new Parameters();

        final ObjectNode data;
        try {
            data = postRequest(SuConnector.getInstance(), SuApiEndpoint.USER, params).data;
        } catch (final SuApiException e) {
            return 0;
        }
        if (data == null || data.get("status").get("code").toString().contains("ERROR")) {
            return 0;
        }

        return data.get("data").get("recommendationsLeft").asInt();
    }

    @WorkerThread
    public static boolean uploadPersonalNote(final Geocache cache) {
        final String currentNote = StringUtils.defaultString(cache.getPersonalNote());
        final Parameters params = new Parameters("cacheID", cache.getCacheId());
        params.add("note_text", currentNote);
        try {
            postRequest(SuConnector.getInstance(), SuApiEndpoint.PERSONAL_NOTE, params);
        } catch (final SuApiException e) {
            return false;
        }
        return true;
    }

    @NonNull
    @WorkerThread
    private static JSONResult getRequest(@NonNull final SuConnector connector, @NonNull final SuApiEndpoint endpoint, @NonNull final Parameters params) throws SuApiException {
        return request(connector, endpoint, "GET", params);
    }

    @NonNull
    @WorkerThread
    private static JSONResult postRequest(@NonNull final SuConnector connector, @NonNull final SuApiEndpoint endpoint, @NonNull final Parameters params) throws SuApiException {
        return request(connector, endpoint, "POST", params);
    }

    @NonNull
    @WorkerThread
    private static JSONResult request(@NonNull final SuConnector connector, @NonNull final SuApiEndpoint endpoint, @NonNull final String method, @NonNull final Parameters params) throws SuApiException {
        final String host = connector.getHost();
        if (StringUtils.isBlank(host)) {
            return new JSONResult("unknown connector host");
        }

        final OAuthTokens tokens = new OAuthTokens(connector);
        if (!tokens.isValid()) {
            throw new NotAuthorizedException();
        }
        OAuth.signOAuth(host, endpoint.methodName, method, connector.isHttps(), params, tokens, connector.getConsumerKey(), connector.getConsumerSecret());

        final String uri = connector.getHostUrl() + endpoint.methodName;
        final JSONResult result;
        try {
            if ("GET".equals(method)) {
                result = new JSONResult(Network.getRequest(uri, params).blockingGet());
            } else {
                result = new JSONResult(Network.postRequest(uri, params).blockingGet());
            }
        } catch (final RuntimeException e) {
            if (e.getCause() instanceof InterruptedException) {
                Log.w("SuApi.JSONResult Interrupted");
                return new JSONResult("interrupted");
            }
            Log.e("SuApi.JSONResult unknown error", e);
            throw new ConnectionErrorException();
        } catch (final Exception e) {
            Log.e("SuApi.JSONResult unknown error", e);
            throw new ConnectionErrorException();
        }

        if (!result.isSuccess) {
            Log.w("JSONResult exception, failed request");
            throw new ConnectionErrorException();
        }
        if (result.data.get("status").get("code").toString().contains("ERROR") && result.data.get("status").get("type").toString().contains("AuthorisationRequired")) {
            throw new NotAuthorizedException();
        }
        return result;
    }

    static class SuApiException extends Exception {
    }

    static class CacheNotFoundException extends SuApiException {
    }

    static class ConnectionErrorException extends SuApiException {
    }

    static class NotAuthorizedException extends SuApiException {
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
