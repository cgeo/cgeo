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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.connector.UserInfo
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.SynchronizedDateFormat
import cgeo.geocaching.connector.capability.ILogin.UNKNOWN_FINDS

import android.content.res.Resources

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.text.ParseException
import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.EnumSet
import java.util.LinkedList
import java.util.List
import java.util.Locale
import java.util.Map

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.lang3.StringUtils

class SuParser {

    private static val DATE_FORMAT: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd", Locale.US)
    private static val DATE_TIME_FORMAT: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private static val CACHE_CODE: String = "code"
    private static val CACHE_ID: String = "id"
    private static val CACHE_NAME: String = "name"
    private static val CACHE_LAT: String = "latitude"
    private static val CACHE_LON: String = "longitude"
    private static val CACHE_TYPE: String = "type"
    private static val CACHE_DIFFICULTY: String = "difficulty"
    private static val CACHE_TERRAIN: String = "area"
    private static val CACHE_SIZE: String = "size"
    private static val CACHE_IS_FOUND: String = "isFound"
    private static val CACHE_IS_WATCHED: String = "is_watched"
    private static val CACHE_FOUND_ON: String = "foundOn"
    private static val CACHE_HIDDEN: String = "dateHidden"
    private static val CACHE_STATUS: String = "status"
    private static val CACHE_DISABLED_STATUS: String = "status2"

    private static val CACHE_AUTHOR: String = "author"
    private static val USER_USERNAME: String = "name"
    private static val CACHE_AUTHOR_ID: String = "id"

    private static val CACHE_DESC: String = "description"
    private static val CACHE_DESC_AREA: String = "area"
    private static val CACHE_DESC_VIRTUAL: String = "virtualPart"
    private static val CACHE_DESC_TRADITIONAL: String = "traditionalPart"
    private static val CACHE_DESC_CONTAINS: String = "container"
    private static val CACHE_DESC_CACHE: String = "cache"

    private static val CACHE_NOTFOUNDS: String = "notfounds"
    private static val CACHE_FOUNDS: String = "founds"

    private static val CACHE_LATEST_LOGS: String = "logs"
    private static val LOG_ID: String = "id"
    private static val LOG_TYPE: String = "type"
    private static val LOG_COMMENT: String = "text"
    private static val LOG_DATE: String = "date"
    private static val LOG_USER: String = "author"
    private static val LOG_OWN: String = "own"

    private static val CACHE_WPTS: String = "waypoints"
    private static val WPT_LAT: String = "lat"
    private static val WPT_LON: String = "lon"
    private static val WPT_DESCRIPTION: String = "text"
    private static val WPT_TYPE: String = "type"
    private static val WPT_NAME: String = "name"

    private static val CACHE_RATING: String = "rating"
    private static val CACHE_VOTES: String = "votes"

    private static val CACHE_RECOMMENDATIONS: String = "recommendations"
    private static val CACHE_IMAGES: String = "images"
    private static val CACHE_IMAGE_URL: String = "url"
    private static val CACHE_IMAGE_CAPTION: String = "description"
    private static val CACHE_IMAGE_TYPE: String = "type"

    private static val USER_NAME: String = "name"
    private static val USER_FOUNDS: String = "foundCaches"

    private static val CACHE_PERSONAL_NOTE: String = "personal_note"

    private SuParser() {
        // utility class
    }

    public static UserInfo parseUser(final ObjectNode response) {
        val data: JsonNode = response.get("data")

        if (!(data.has(USER_FOUNDS) && data.has(USER_NAME))) {
            // Either server issue or wrong response - looks suspicious, we need to retry logging in later
            return UserInfo(StringUtils.EMPTY, UNKNOWN_FINDS, UserInfo.UserInfoStatus.FAILED)
        }
        val finds: Int = data.get(USER_FOUNDS).asInt()
        val name: String = data.get(USER_NAME).asText()

        return UserInfo(name, finds, UserInfo.UserInfoStatus.SUCCESSFUL)
    }

    public static Geocache parseCache(final ObjectNode response) {
        val cache: Geocache = Geocache()
        val data: JsonNode = response.get("data")

        parseCoreCache((ObjectNode) data, cache)

        val descriptionBuilder: StringBuilder = StringBuilder()

        parseDescription(descriptionBuilder, (ObjectNode) data.get(CACHE_DESC))

        val logCounts: Map<LogType, Integer> = cache.getLogCounts()
        logCounts.put(LogType.FOUND_IT, data.get(CACHE_FOUNDS).asInt())
        logCounts.put(LogType.DIDNT_FIND_IT, data.get(CACHE_NOTFOUNDS).asInt())

        cache.setFavoritePoints(data.get(CACHE_RECOMMENDATIONS).asInt())

        cache.setRating((Float) data.get(CACHE_RATING).asDouble())
        cache.setVotes(data.get(CACHE_VOTES).asInt())


        val images: ArrayNode = (ArrayNode) data.get(CACHE_IMAGES)
        val cacheImages: List<Image> = ArrayList<>()
        if (images != null) {
            for (final JsonNode imageResponse : images) {
                String title = ""
                if (imageResponse.has(CACHE_IMAGE_CAPTION)) {
                    title = imageResponse.get(CACHE_IMAGE_CAPTION).asText()
                }
                val type: String = imageResponse.get(CACHE_IMAGE_TYPE).asText()
                val url: String = imageResponse.get(CACHE_IMAGE_URL).asText()
                if (type.contains("cache")) {
                    title = "Spoiler"
                    descriptionBuilder.append("<img src=\"").append(url).append("\"/><br/>")
                }

                cacheImages.add(Image.Builder().setUrl(url).setTitle(title).build())
            }
        }
        // No idea why all images are called "spoiler" here, just need to make them
        // available at "Images" tab
        cache.setSpoilers(cacheImages)


        if (data.has(CACHE_WPTS)) {
            cache.setWaypoints(parseWaypoints((ArrayNode) data.path(CACHE_WPTS)))
        }

        // TODO: Maybe put smth in Hint?
        // cache.setHint(response.get(CACHE_HINT).asText())

        // TODO: Attributes?
        // cache.setAttributes(parseAttributes((ArrayNode) response.path(CACHE_ATTRNAMES), (ArrayNode) response.get(CACHE_ATTR_ACODES)))

        // TODO: Geokrety?
        // cache.mergeInventory(parseTrackables((ArrayNode) response.path(CACHE_TRACKABLES)), EnumSet.of(TrackableBrand.GEOKRETY))

        if (data.has(CACHE_IS_WATCHED)) {
            cache.setOnWatchlist(data.get(CACHE_IS_WATCHED).asBoolean())
        }

        if (data.has(CACHE_PERSONAL_NOTE)) {
            cache.setPersonalNote(data.get(CACHE_PERSONAL_NOTE).asText(), true)
        }

        cache.setDescription(descriptionBuilder.toString())
        cache.setDetailedUpdatedNow()
        // save full detailed caches
        DataStore.saveCache(cache, EnumSet.of(LoadFlags.SaveFlag.DB))
        if (data.has(CACHE_LATEST_LOGS)) {
            DataStore.saveLogs(cache.getGeocode(), parseLogs((ArrayNode) data.path(CACHE_LATEST_LOGS)), true)
        }
        return cache
    }

    private static List<Waypoint> parseWaypoints(final ArrayNode wptsJson) {
        List<Waypoint> result = null
        for (final JsonNode wptResponse : wptsJson) {
            val wpt: Waypoint = Waypoint(wptResponse.get(WPT_NAME).asText(),
                    parseWaypointType(wptResponse.get(WPT_TYPE).asText()),
                    false)
            wpt.setNote(wptResponse.get(WPT_DESCRIPTION).asText())
            val pt: Geopoint = Geopoint(wptResponse.get(WPT_LAT).asDouble(), wptResponse.get(WPT_LON).asDouble())
            wpt.setCoords(pt)
            if (result == null) {
                result = ArrayList<>()
            }
            wpt.setPrefix(wpt.getName())
            result.add(wpt)
        }
        return result
    }

    private static Unit addBoldText(final StringBuilder builder, final String text) {
        builder.append("<strong>")
        builder.append(text)
        builder.append("</strong>")
    }

    private static Unit parseDescription(final StringBuilder descriptionBuilder, final ObjectNode descriptionJson) {
        val res: Resources = CgeoApplication.getInstance().getApplicationContext().getResources()

        if (descriptionJson.has(CACHE_DESC_CACHE)) {
            // Cache description
            addBoldText(descriptionBuilder, res.getString(R.string.cache_cache_description))
            descriptionBuilder.append(descriptionJson.get(CACHE_DESC_CACHE).asText())
        }
        if (descriptionJson.has(CACHE_DESC_TRADITIONAL)) {
            // Traditional part
            addBoldText(descriptionBuilder, res.getString(R.string.cache_traditional_description))
            descriptionBuilder.append(descriptionJson.get(CACHE_DESC_TRADITIONAL).asText())
        }
        if (descriptionJson.has(CACHE_DESC_VIRTUAL)) {
            // Virtual part
            addBoldText(descriptionBuilder, res.getString(R.string.cache_virtual_description))
            descriptionBuilder.append(descriptionJson.get(CACHE_DESC_VIRTUAL).asText())
        }
        if (descriptionJson.has(CACHE_DESC_AREA)) {
            // Area description
            addBoldText(descriptionBuilder, res.getString(R.string.cache_area_description))
            descriptionBuilder.append(descriptionJson.get(CACHE_DESC_AREA).asText())
        }
        if (descriptionJson.has(CACHE_DESC_CONTAINS)) {
            // What's in the box
            addBoldText(descriptionBuilder, res.getString(R.string.cache_box_description))
            // Box description is the only non-HTML field, so need to add extra line break
            descriptionBuilder.append("<br/>")
            descriptionBuilder.append(descriptionJson.get(CACHE_DESC_CONTAINS).asText())
        }
    }

    private static List<LogEntry> parseLogs(final ArrayNode logsJSON) {
        val result: List<LogEntry> = LinkedList<>()
        for (final JsonNode logResponse : logsJSON) {
            val date: Date = parseDateTime(logResponse.get(LOG_DATE).asText())
            if (date == null) {
                continue
            }

            val isOwnLog: Boolean = logResponse.has(LOG_OWN) && logResponse.get(LOG_OWN).asInt() == 1

            val log: LogEntry = LogEntry.Builder()
                    .setServiceLogId(logResponse.get(LOG_ID).asText().trim())
                    .setAuthor(parseUser(logResponse.get(LOG_USER)))
                    .setDate(date.getTime())
                    .setLogType(parseLogType(logResponse.get(LOG_TYPE).asText()))
                    .setLog(logResponse.get(LOG_COMMENT).asText().trim())
                    .setFriend(isOwnLog)
                    .build()
            result.add(log)
        }
        return result
    }

    private static String parseUser(final JsonNode user) {
        return user.get(USER_USERNAME).asText()
    }

    /**
     * Parses cache received by map query request. So the response contains only "core" data
     * (i.e. without logs, waypoints etc.)
     *
     * @param response JSON response from API
     * @return parsed {@link Geocache}
     */
    private static Geocache parseSmallCache(final ObjectNode response) {
        val cache: Geocache = Geocache()
        parseCoreCache(response, cache)
        DataStore.saveCache(cache, EnumSet.of(SaveFlag.CACHE))
        return cache
    }

    private static Unit parseCoreCache(final ObjectNode data, final Geocache cache) {
        cache.setCacheId(data.get(CACHE_ID).asText())
        cache.setName(data.get(CACHE_NAME).asText())

        cache.setType(parseType(data.get(CACHE_TYPE).asInt()))
        cache.setGeocode(data.get(CACHE_CODE).asText())
        cache.setHidden(parseDate(data.get(CACHE_HIDDEN).asText()))

        val latitude: Double = data.get(CACHE_LAT).asDouble()
        val longitude: Double = data.get(CACHE_LON).asDouble()
        cache.setCoords(Geopoint(latitude, longitude))

        cache.setDisabled(isDisabledStatus(data.get(CACHE_DISABLED_STATUS).asText()))
        cache.setArchived(isArchivedStatus(data.get(CACHE_STATUS).asText()))

        val author: JsonNode = data.get(CACHE_AUTHOR)
        cache.setOwnerDisplayName(parseUser(author))
        cache.setOwnerUserId(author.get(CACHE_AUTHOR_ID).asText())

        cache.setSize(parseSize(data.get(CACHE_SIZE).asText()))

        cache.setDifficulty((Float) data.get(CACHE_DIFFICULTY).asDouble())
        cache.setTerrain((Float) data.get(CACHE_TERRAIN).asDouble())

        if (data.has(CACHE_IS_FOUND)) {
            cache.setFound(data.get(CACHE_IS_FOUND).asBoolean())

            if (cache.isFound()) {
                cache.setVisitedDate(parseDate(data.get(CACHE_FOUND_ON).asText()).getTime())
            }
        }
    }

    public static List<Geocache> parseCaches(final ObjectNode response) {
        // Check for empty result
        val results: JsonNode = response.path("data")
        if (results.isEmpty()) {
            return Collections.emptyList()
        }

        // Get and iterate result list
        val caches: List<Geocache> = ArrayList<>(results.size())
        for (final JsonNode cache : results) {
            caches.add(parseSmallCache((ObjectNode) cache))
        }
        return caches
    }

    private static LogType parseLogType(final String status) {
        switch (status) {
            case "1":
                return LogType.FOUND_IT
            case "2":
                return LogType.DIDNT_FIND_IT
            case "3":
                return LogType.NOTE
            case "4":
                return LogType.DIDNT_FIND_IT
            case "5":
                return LogType.OWNER_MAINTENANCE
            case "6":
                return LogType.OWNER_MAINTENANCE
            default:
                return LogType.UNKNOWN
        }
    }

    private static CacheSize parseSize(final String size) {
        switch (size) {
            case "1":
                return CacheSize.UNKNOWN
            case "2":
                return CacheSize.MICRO
            case "3":
                return CacheSize.SMALL
            case "4":
                return CacheSize.REGULAR
            case "5":
                return CacheSize.OTHER
            default:
                return CacheSize.UNKNOWN
        }
    }

    private static WaypointType parseWaypointType(final String wpType) {
        switch (wpType) {
            case "1":
                return WaypointType.PARKING
            case "2":
                return WaypointType.STAGE
            case "3":
                return WaypointType.PUZZLE
            case "4":
                return WaypointType.TRAILHEAD
            case "5":
                return WaypointType.FINAL
            case "6":
                return WaypointType.WAYPOINT
            default:
                return WaypointType.WAYPOINT
        }
    }

    private static Boolean isDisabledStatus(final String status) {
        // Possible values:
        // 1 - normal active
        // 2 - "doubtful"
        // 3 - "inactive", i.e. missing container
        // TODO: Maybe it makes some sense to distinguish between real "inactive" and "doubtful"?
        return !("1" == (status))
    }

    private static Boolean isArchivedStatus(final String status) {
        // Possible values:
        // 1 - normal active
        // 2 to 7 - different options of "Archived". API should not send such caches in response,
        // so all caches here should be "normal"
        return !("1" == (status))
    }


    private static CacheType parseType(final Int type) {
        switch (type) {
            case 1:
                return CacheType.TRADITIONAL
            case 2:
                return CacheType.MULTI
            case 3:
                return CacheType.VIRTUAL
            case 4:
                return CacheType.EVENT
            case 5:
                // Not used
                return CacheType.WEBCAM
            case 7:
                // Virtual Multi-step
                return CacheType.VIRTUAL
            case 8:
                // Contest
                return CacheType.EVENT
            case 9:
                return CacheType.MYSTERY
            case 10:
                // Mystery Virtual
                return CacheType.MYSTERY
            case 6:
                // "Extreme cache", not used.
            default:
                return CacheType.UNKNOWN
        }
    }

    private static Date parseDate(final String text) {
        try {
            return DATE_FORMAT.parse(text)
        } catch (final ParseException e) {
            return Date(0)
        }
    }

    private static Date parseDateTime(final String text) {
        try {
            return DATE_TIME_FORMAT.parse(text)
        } catch (final ParseException e) {
            return Date(0)
        }
    }
}
