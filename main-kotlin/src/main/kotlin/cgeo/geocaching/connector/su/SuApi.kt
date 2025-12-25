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

package cgeo.geocaching.connector.su

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.ImageResult
import cgeo.geocaching.connector.LogResult
import cgeo.geocaching.connector.UserInfo
import cgeo.geocaching.connector.UserInfo.UserInfoStatus
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.filters.core.BaseGeocacheFilter
import cgeo.geocaching.filters.core.DistanceGeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.NameGeocacheFilter
import cgeo.geocaching.filters.core.OriginGeocacheFilter
import cgeo.geocaching.filters.core.OwnerGeocacheFilter
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.network.Network
import cgeo.geocaching.network.OAuth
import cgeo.geocaching.network.OAuthTokens
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.SynchronizedDateFormat
import cgeo.geocaching.connector.capability.ILogin.UNKNOWN_FINDS

import android.util.Base64
import android.util.Base64.DEFAULT

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import java.io.File
import java.io.FileInputStream
import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.List
import java.util.Locale

import com.fasterxml.jackson.databind.node.ObjectNode
import okhttp3.Response
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils

class SuApi {

    private static val LOG_DATE_FORMAT: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd", Locale.US)

    private SuApi() {
        // utility class
    }


    @WorkerThread
    public static UserInfo getUserInfo(final SuConnector connector) throws SuApiException {
        val params: Parameters = Parameters()

        val result: JSONResult = getRequest(connector, SuApiEndpoint.USER, params)

        if (!result.isSuccess) {
            return UserInfo(StringUtils.EMPTY, UNKNOWN_FINDS, UserInfoStatus.FAILED)
        }

        return SuParser.parseUser(result.data)
    }

    @WorkerThread
    public static Geocache searchByGeocode(final String geocode) throws SuApiException {
        val connector: IConnector = ConnectorFactory.getConnector(geocode)
        if (!(connector is SuConnector)) {
            return null
        }
        val gcsuConn: SuConnector = (SuConnector) connector

        val id: String = StringUtils.substring(geocode, 2)

        val params: Parameters = Parameters("id", id)
        val result: JSONResult = SuApi.getRequest(gcsuConn, SuApiEndpoint.CACHE, params)

        if (result.data.get("status").get("code").toString().contains("ERROR") && result.data.get("status").get("description").toString().contains("not found")) {
            throw CacheNotFoundException()
        }

        return SuParser.parseCache(result.data)
    }

    @WorkerThread
    public static List<Geocache> searchByBBox(final Viewport viewport, final SuConnector connector) throws SuApiException {
        if (viewport.getLatitudeSpan() == 0 || viewport.getLongitudeSpan() == 0) {
            return Collections.emptyList()
        }

        val params: Parameters = Parameters(
                "minlat", String.valueOf(viewport.getLatitudeMin()),
                "maxlat", String.valueOf(viewport.getLatitudeMax()),
                "minlng", String.valueOf(viewport.getLongitudeMin()),
                "maxlng", String.valueOf(viewport.getLongitudeMax()))

        val result: JSONResult = SuApi.getRequest(connector, SuApiEndpoint.CACHE_LIST, params)
        return SuParser.parseCaches(result.data)
    }

    @WorkerThread
    public static List<Geocache> searchByKeyword(final String keyword, final SuConnector connector) throws SuApiException {
        if (keyword.isEmpty()) {
            return Collections.emptyList()
        }

        val params: Parameters = Parameters(
                "keyword", keyword)

        val result: JSONResult = SuApi.getRequest(connector, SuApiEndpoint.CACHE_LIST_KEYWORD, params)
        return SuParser.parseCaches(result.data)
    }

    @WorkerThread
    public static List<Geocache> searchByOwner(final String owner, final SuConnector connector) throws SuApiException {
        if (owner.isEmpty()) {
            return Collections.emptyList()
        }

        val params: Parameters = Parameters(
                "owner", owner)

        val result: JSONResult = SuApi.getRequest(connector, SuApiEndpoint.CACHE_LIST_OWNER, params)
        return SuParser.parseCaches(result.data)
    }

    /**
     * Returns list of caches located around {@code center}
     *
     * @param center    coordinates of the central point
     * @param radius    radius in km
     * @param connector Geocaching connector
     * @return list of caches located around {@code center}
     */
    @WorkerThread
    public static List<Geocache> searchByCenter(final Geopoint center, final Float radius, final SuConnector connector) throws SuApiException {
        val params: Parameters = Parameters(
                "lat", String.valueOf(center.getLatitude()),
                "lng", String.valueOf(center.getLongitude()),
                "radius", Float.toString(radius))

        val result: JSONResult = SuApi.getRequest(connector, SuApiEndpoint.CACHE_LIST_CENTER, params)
        return SuParser.parseCaches(result.data)
    }

    @WorkerThread
    public static List<Geocache> searchByFilter(final GeocacheFilter filter, final SuConnector connector) throws SuApiException {

        //for now we have to assume that SUConnector supports only SINGLE criteria search

        val filters: List<BaseGeocacheFilter> = filter.getAndChainIfPossible()
        val of: OriginGeocacheFilter = GeocacheFilter.findInChain(filters, OriginGeocacheFilter.class)
        if (of != null && !of.allowsCachesOf(connector)) {
            return ArrayList<>()
        }
        val df: DistanceGeocacheFilter = GeocacheFilter.findInChain(filters, DistanceGeocacheFilter.class)
        if (df != null) {
            return searchByCenter(df.getEffectiveCoordinate(), df.getMaxRangeValue() == null ? 20f : df.getMaxRangeValue(), connector)
        }
        val nf: NameGeocacheFilter = GeocacheFilter.findInChain(filters, NameGeocacheFilter.class)
        if (nf != null && !StringUtils.isEmpty(nf.getStringFilter().getTextValue())) {
            return searchByKeyword(nf.getStringFilter().getTextValue(), connector)
        }
        val ownf: OwnerGeocacheFilter = GeocacheFilter.findInChain(filters, OwnerGeocacheFilter.class)
        if (ownf != null && !StringUtils.isEmpty(ownf.getStringFilter().getTextValue())) {
            return searchByOwner(ownf.getStringFilter().getTextValue(), connector)
        }

        //by default, search around current position
        return searchByCenter(LocationDataProvider.getInstance().currentGeo().getCoords(), 20f, connector)
    }

    private static String getSuLogType(final LogType logType) {
        switch (logType) {
            case FOUND_IT:
                return "found"
            case DIDNT_FIND_IT:
                return "notFound"
            case OWNER_MAINTENANCE:
                return "authorCheck"
            case NOTE:
            default:
                return "comment"
        }
    }

    @WorkerThread
    public static LogResult postLog(final Geocache cache, final LogType logType, final Date date, final String log, final Boolean addRecommendation) throws SuApiException {
        val connector: IConnector = ConnectorFactory.getConnector(cache.getGeocode())
        if (!(connector is SuConnector)) {
            return LogResult.error(StatusCode.LOG_POST_ERROR)
        }
        val gcsuConnector: SuConnector = (SuConnector) connector

        val params: Parameters = Parameters()
        params.add("cacheID", cache.getCacheId())
        params.add("type", getSuLogType(logType))
        params.add("text", log)
        params.add("add_recommendation", addRecommendation ? "true" : "false")
        params.add("find_date", LOG_DATE_FORMAT.format(date))

        val data: ObjectNode = postRequest(gcsuConnector, SuApiEndpoint.NOTE, params).data

        if (data == null) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, "no data", null)
        }
        if (data.get("status").get("code").toString().contains("ERROR")) {
            return LogResult.error(StatusCode.LOG_POST_ERROR, data.get("status").get("code").toString(), null)
        }

        return LogResult.ok(data.get("data").get("noteID").asText())
    }

    private static String createImageCaption(final Image image) {
        val caption: StringBuilder = StringBuilder(StringUtils.trimToEmpty(image.getTitle()))
        if (StringUtils.isNotEmpty(caption) && StringUtils.isNotBlank(image.getDescription())) {
            caption.append(": ")
        }
        caption.append(StringUtils.trimToEmpty(image.getDescription()))
        return caption.toString()
    }

    public static ImageResult postImage(final Geocache cache, final Image image) {
        val connector: IConnector = ConnectorFactory.getConnector(cache.getGeocode())
        if (!(connector is SuConnector)) {
            return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR)
        }
        val gcsuConnector: SuConnector = (SuConnector) connector

        val file: File = image.getFile()
        if (file == null) {
            return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR)
        }
        val params: Parameters = Parameters("cacheID", cache.getCacheId())
        FileInputStream fileStream = null
        try {
            fileStream = FileInputStream(file)
            params.add("image", Base64.encodeToString(IOUtils.readFully(fileStream, (Int) file.length()), DEFAULT))
            params.add("caption", createImageCaption(image))

            val data: ObjectNode = postRequest(gcsuConnector, SuApiEndpoint.POST_IMAGE, params).data

            if (data != null && data.get("data").get("success").asBoolean()) {
                return ImageResult.ok(data.get("data").get("image_url").asText())
            }

        } catch (final Exception e) {
            Log.e("SuApi.postLogImage", e)
        } finally {
            IOUtils.closeQuietly(fileStream)
        }
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR)
    }

    @WorkerThread
    public static Boolean setWatchState(final Geocache cache, final Boolean watched) {
        val params: Parameters = Parameters("cacheID", cache.getCacheId())
        params.add("watched", watched ? "true" : "false")

        try {
            postRequest(SuConnector.getInstance(), SuApiEndpoint.MARK, params)
        } catch (final SuApiException e) {
            return false
        }

        cache.setOnWatchlist(watched)
        DataStore.saveChangedCache(cache)

        return true
    }

    @WorkerThread
    public static Boolean setIgnoreState(final Geocache cache, final Boolean ignored) {
        val params: Parameters = Parameters("cacheID", cache.getCacheId())
        params.add("ignored", ignored ? "true" : "false")

        try {
            postRequest(SuConnector.getInstance(), SuApiEndpoint.IGNORE, params)
        } catch (final SuApiException e) {
            return false
        }

        //cache.setOnWatchlist(ignored)
        //DataStore.saveChangedCache(cache)

        return true
    }

    @WorkerThread
    public static Boolean setRecommendation(final Geocache cache, final Boolean status) {
        val params: Parameters = Parameters("cacheID", cache.getCacheId())
        params.add("recommend", status ? "true" : "false")

        final ObjectNode data
        try {
            data = postRequest(SuConnector.getInstance(), SuApiEndpoint.RECOMMENDATION, params).data
        } catch (final SuApiException e) {
            return false
        }
        if (data == null || data.get("status").get("code").toString().contains("ERROR")) {
            return false
        }

        cache.setFavorite(status)
        cache.setFavoritePoints(cache.getFavoritePoints() + (status ? 1 : -1))

        DataStore.saveChangedCache(cache)

        return true
    }

    @WorkerThread
    public static Boolean postVote(final Geocache cache, final Float rating) {
        val params: Parameters = Parameters("cacheID", cache.getCacheId())
        params.add("value", String.valueOf(rating))

        try {
            postRequest(SuConnector.getInstance(), SuApiEndpoint.VALUE, params)
        } catch (final SuApiException e) {
            return false
        }

        // TODO: get updated rating?
        //cache.setRating(rating.getRating())

        cache.setVotes(cache.getVotes() + 1)
        cache.setMyVote(rating)
        DataStore.saveChangedCache(cache)

        return true
    }

    @WorkerThread
    public static Int getAvailableRecommendations() {
        // Nothing here as we want to get info about current user
        val params: Parameters = Parameters()

        final ObjectNode data
        try {
            data = postRequest(SuConnector.getInstance(), SuApiEndpoint.USER, params).data
        } catch (final SuApiException e) {
            return 0
        }
        if (data == null || data.get("status").get("code").toString().contains("ERROR")) {
            return 0
        }

        return data.get("data").get("recommendationsLeft").asInt()
    }

    @WorkerThread
    public static Boolean uploadPersonalNote(final Geocache cache) {
        val currentNote: String = StringUtils.defaultString(cache.getPersonalNote())
        val params: Parameters = Parameters("cacheID", cache.getCacheId())
        params.add("note_text", currentNote)
        try {
            postRequest(SuConnector.getInstance(), SuApiEndpoint.PERSONAL_NOTE, params)
        } catch (final SuApiException e) {
            return false
        }
        return true
    }

    @WorkerThread
    private static JSONResult getRequest(final SuConnector connector, final SuApiEndpoint endpoint, final Parameters params) throws SuApiException {
        return request(connector, endpoint, "GET", params)
    }

    @WorkerThread
    private static JSONResult postRequest(final SuConnector connector, final SuApiEndpoint endpoint, final Parameters params) throws SuApiException {
        return request(connector, endpoint, "POST", params)
    }

    @WorkerThread
    private static JSONResult request(final SuConnector connector, final SuApiEndpoint endpoint, final String method, final Parameters params) throws SuApiException {
        val host: String = connector.getHost()
        if (StringUtils.isBlank(host)) {
            return JSONResult("unknown connector host")
        }

        val tokens: OAuthTokens = OAuthTokens(connector)
        if (!tokens.isValid()) {
            throw NotAuthorizedException()
        }
        OAuth.signOAuth(host, endpoint.methodName, method, connector.isHttps(), params, tokens, connector.getConsumerKey(), connector.getConsumerSecret())

        val uri: String = connector.getHostUrl() + endpoint.methodName
        final JSONResult result
        try {
            if ("GET" == (method)) {
                result = JSONResult(Network.getRequest(uri, params).blockingGet())
            } else {
                result = JSONResult(Network.postRequest(uri, params).blockingGet())
            }
        } catch (final RuntimeException e) {
            if (e.getCause() is InterruptedException) {
                Log.w("SuApi.JSONResult Interrupted")
                return JSONResult("interrupted")
            }
            Log.e("SuApi.JSONResult unknown error", e)
            throw ConnectionErrorException()
        } catch (final Exception e) {
            Log.e("SuApi.JSONResult unknown error", e)
            throw ConnectionErrorException()
        }

        if (!result.isSuccess) {
            Log.w("JSONResult exception, failed request")
            throw ConnectionErrorException()
        }
        if (result.data.get("status").get("code").toString().contains("ERROR") && result.data.get("status").get("type").toString().contains("AuthorisationRequired")) {
            throw NotAuthorizedException()
        }
        return result
    }

    static class SuApiException : Exception() {
    }

    static class CacheNotFoundException : SuApiException() {
    }

    static class ConnectionErrorException : SuApiException() {
    }

    static class NotAuthorizedException : SuApiException() {
    }

    private static class JSONResult {
        public final Boolean isSuccess
        public final ObjectNode data

        JSONResult(final Response response) {
            ObjectNode tempData = null
            try {
                tempData = (ObjectNode) JsonUtils.reader.readTree(response.body().byteStream())
            } catch (final Exception e) {
                // ignore
            } finally {
                response.close()
            }
            data = tempData
            isSuccess = response.isSuccessful() && tempData != null
        }

        JSONResult(final String errorMessage) {
            isSuccess = false
            data = ObjectNode(JsonUtils.factory)
            data.putObject("error").put("developer_message", errorMessage)
        }
    }
}
