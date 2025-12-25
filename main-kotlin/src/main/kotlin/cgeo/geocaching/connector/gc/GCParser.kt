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

package cgeo.geocaching.connector.gc

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.trackable.TrackableBrand
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.DistanceUnit
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.log.LogTypeTrackable
import cgeo.geocaching.log.OfflineLogEntry
import cgeo.geocaching.models.GCList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.network.Network
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MatcherWrapper
import cgeo.geocaching.utils.SynchronizedDateFormat
import cgeo.geocaching.utils.TextUtils

import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread
import androidx.core.text.HtmlCompat

import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Date
import java.util.EnumSet
import java.util.HashMap
import java.util.Iterator
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Objects
import java.util.Set
import java.util.TimeZone
import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.stream.Collectors

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import okhttp3.Response
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.apache.commons.lang3.StringUtils.INDEX_NOT_FOUND
import org.apache.commons.lang3.StringUtils.substring

class GCParser {

    private static val MAPPER: ObjectMapper = ObjectMapper()

    private static val DATE_TB_IN_1: SynchronizedDateFormat = SynchronizedDateFormat("EEEEE, dd MMMMM yyyy", Locale.ENGLISH); // Saturday, 28 March 2009

    private static val DATE_TB_IN_2: SynchronizedDateFormat = SynchronizedDateFormat("EEEEE, MMMMM dd, yyyy", Locale.ENGLISH); // Saturday, March 28, 2009

    private static val DATE_JSON: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC"), Locale.US); // 2009-03-28T18:30:31.497Z
    private static val DATE_JSON_SHORT: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"), Locale.US); // 2009-03-28T18:30:31Z

    private static val UNKNOWN_PARSE_ERROR: ImmutablePair<StatusCode, Geocache> = ImmutablePair.of(StatusCode.UNKNOWN_ERROR, null)

    private static val HEADER_VERIFICATION_TOKEN: String = "X-Verification-Token"

    private GCParser() {
        // Utility class
    }

    private static SearchResult parseMap(final IConnector con, final String url, final String pageContent, final Int alreadyTaken) throws JsonProcessingException {
        if (StringUtils.isBlank(pageContent)) {
            Log.e("GCParser.parseSearch: No page given")
            return null
        }

        val caches: List<Geocache> = ArrayList<>()

        val json: JsonNode = JsonUtils.reader.readTree(pageContent)
        val features: JsonNode = json.get("data").get("layer").get("features")
        for (Int i = 0; i < features.size(); i++) {
            val properties: JsonNode = features.get(i).get("properties")
            if (properties != null) {
                val cache: Geocache = Geocache()
                cache.setName(properties.get("name").asText())
                cache.setGeocode(properties.get("key").asText())
                cache.setType(CacheType.getByWaypointType(properties.get("wptid").asText()))
                cache.setArchived(properties.get("archived").asBoolean())
                cache.setDisabled(!properties.get("available").asBoolean())
                cache.setCoords(Geopoint(properties.get("lat").asDouble(), properties.get("lng").asDouble()))
                val icon: String = properties.get("icon").asText()
                if ("MyHide" == (icon)) {
                    cache.setOwnerUserId(Settings.getUserName())
                } else if ("MyFind" == (icon)) {
                    cache.setFound(true)
                } else if (icon.startsWith("solved")) {
                    cache.setUserModifiedCoords(true)
                }
                caches.add(cache)
            }
        }

        val searchResult: SearchResult = SearchResult()
        searchResult.setUrl(con, url)
        searchResult.addAndPutInCache(caches)
        searchResult.setToContext(con, b -> b.putInt(GCConnector.SEARCH_CONTEXT_TOOK_TOTAL, alreadyTaken + caches.size()))

        return searchResult
    }

    @WorkerThread
    static SearchResult parseCache(final IConnector con, final String page, final DisposableHandler handler) {
        val parsed: ImmutablePair<StatusCode, Geocache> = parseCacheFromText(page, handler)
        // attention: parseCacheFromText already stores implicitly through searchResult.addCache
        if (parsed.left != StatusCode.NO_ERROR) {
            val result: SearchResult = SearchResult(con, parsed.left)

            if (parsed.left == StatusCode.PREMIUM_ONLY) {
                result.addAndPutInCache(Collections.singletonList(parsed.right))
            }

            return result
        }

        val cache: Geocache = parsed.right
        getExtraOnlineInfo(cache, page, handler)
        // too late: it is already stored through parseCacheFromText
        cache.setDetailedUpdatedNow()
        if (DisposableHandler.isDisposed(handler)) {
            return null
        }

        // save full detailed caches
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_cache)
        DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB))

        // update progress message so user knows we're still working. This is more of a place holder than
        // actual indication of what the program is doing
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_render)
        return SearchResult(cache)
    }

    //method is only used in AndroidTests
    static SearchResult testParseAndSaveCacheFromText(final IConnector con, final String page, final DisposableHandler handler) {
        val parsed: ImmutablePair<StatusCode, Geocache> = parseCacheFromText(page, handler)
        val result: SearchResult = SearchResult(con, parsed.left)
        if (parsed.left == StatusCode.NO_ERROR) {
            result.addAndPutInCache(Collections.singletonList(parsed.right))
            DataStore.saveLogs(parsed.right.getGeocode(), getLogs(parseUserToken(page), Logs.ALL).blockingIterable(), true)
        }
        return result
    }

    /**
     * Parse cache from text and return either an error code or a cache object in a pair. Note that inline logs are
     * not parsed nor saved, while the cache itself is.
     *
     * @param pageIn  the page text to parse
     * @param handler the handler to send the progress notifications to
     * @return a pair, with a {@link StatusCode} on the left, and a non-null cache object on the right
     * iff the status code is {@link StatusCode#NO_ERROR}.
     */
    private static ImmutablePair<StatusCode, Geocache> parseCacheFromText(final String pageIn, final DisposableHandler handler) {
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_details)

        if (StringUtils.isBlank(pageIn)) {
            Log.e("GCParser.parseCache: No page given")
            return UNKNOWN_PARSE_ERROR
        }

        if (StringUtils.contains(pageIn, GCConstants.STRING_404_FILE_NOT_FOUND)) {
            return ImmutablePair.of(StatusCode.CACHE_NOT_FOUND, null)
        }

        if (pageIn.contains(GCConstants.STRING_UNPUBLISHED_OTHER) || pageIn.contains(GCConstants.STRING_UNPUBLISHED_FROM_SEARCH)) {
            return ImmutablePair.of(StatusCode.UNPUBLISHED_CACHE, null)
        }

        if (pageIn.contains(GCConstants.STRING_PREMIUMONLY)) {
            val cache: Geocache = Geocache()
            cache.setPremiumMembersOnly(true)
            val matcher: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_PREMIUMONLY_CACHETYPE, pageIn)
            if (matcher.find()) {
                cache.setType(CacheType.getByWaypointType(matcher.group(1)))
                if (Objects == (matcher.group(2), "disabled")) {
                    cache.setDisabled(true)
                }
            }
            cache.setName(TextUtils.stripHtml(TextUtils.getMatch(pageIn, GCConstants.PATTERN_PREMIUMONLY_CACHENAME, true, "")))
            cache.setGeocode(TextUtils.getMatch(pageIn, GCConstants.PATTERN_PREMIUMONLY_GEOCODE, true, ""))
            cache.setDifficulty(Float.parseFloat(TextUtils.getMatch(pageIn, GCConstants.PATTERN_PREMIUMONLY_DIFFICULTY, true, "0")))
            cache.setTerrain(Float.parseFloat(TextUtils.getMatch(pageIn, GCConstants.PATTERN_PREMIUMONLY_TERRAIN, true, "0")))
            cache.setSize(CacheSize.getById(TextUtils.getMatch(pageIn, GCConstants.PATTERN_PREMIUMONLY_SIZE, true, CacheSize.NOT_CHOSEN.id)))
            return ImmutablePair.of(StatusCode.PREMIUM_ONLY, cache)
        }

        val cacheName: String = TextUtils.stripHtml(TextUtils.getMatch(pageIn, GCConstants.PATTERN_NAME, true, ""))
        if (GCConstants.STRING_UNKNOWN_ERROR.equalsIgnoreCase(cacheName)) {
            return UNKNOWN_PARSE_ERROR
        }

        // first handle the content with line breaks, then trim everything for easier matching and reduced memory consumption in parsed fields
        String personalNoteWithLineBreaks = ""
        val matcher: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_PERSONALNOTE, pageIn)
        if (matcher.find()) {
            personalNoteWithLineBreaks = matcher.group(1).trim()
        }

        //gc.com sends personal note with HTML-encoded entities. Replace those
        personalNoteWithLineBreaks = StringEscapeUtils.unescapeHtml4(personalNoteWithLineBreaks)

        val page: String = TextUtils.replaceWhitespace(pageIn)

        val cache: Geocache = Geocache()
        cache.setDisabled(page.contains(GCConstants.STRING_STATUS_DISABLED))
        cache.setArchived(page.contains(GCConstants.STRING_STATUS_ARCHIVED)
                || page.contains(GCConstants.STRING_STATUS_LOCKED))

        cache.setPremiumMembersOnly(TextUtils.matches(page, GCConstants.PATTERN_PREMIUMMEMBERS))

        cache.setFavorite(TextUtils.matches(page, GCConstants.PATTERN_IS_FAVORITE))

        // cache geocode
        cache.setGeocode(TextUtils.getMatch(page, GCConstants.PATTERN_GEOCODE, true, cache.getGeocode()))

        // cache id
        cache.setCacheId(String.valueOf(GCUtils.gcLikeCodeToGcLikeId(cache.getGeocode())))

        // cache guid
        cache.setGuid(TextUtils.getMatch(page, GCConstants.PATTERN_GUID, true, cache.getGuid()))

        // cache watchlistcount
        cache.setWatchlistCount(getWatchListCount(page))

        // name
        cache.setName(cacheName)

        // owner real name
        cache.setOwnerUserId(Network.decode(TextUtils.getMatch(page, GCConstants.PATTERN_OWNER_USERID, true, cache.getOwnerUserId())))

        cache.setUserModifiedCoords(false)

        String tableInside = page

        val pos: Int = tableInside.indexOf(GCConstants.STRING_CACHEDETAILS)
        if (pos == -1) {
            Log.e("GCParser.parseCache: ID \"cacheDetails\" not found on page")
            return UNKNOWN_PARSE_ERROR
        }

        tableInside = tableInside.substring(pos)

        if (StringUtils.isNotBlank(tableInside)) {
            // cache terrain
            String result = TextUtils.getMatch(tableInside, GCConstants.PATTERN_TERRAIN, true, null)
            if (result != null) {
                try {
                    cache.setTerrain(Float.parseFloat(StringUtils.replaceChars(result, '_', '.')))
                } catch (final NumberFormatException e) {
                    Log.e("Error parsing terrain value", e)
                }
            }

            // cache difficulty
            result = TextUtils.getMatch(tableInside, GCConstants.PATTERN_DIFFICULTY, true, null)
            if (result != null) {
                try {
                    cache.setDifficulty(Float.parseFloat(StringUtils.replaceChars(result, '_', '.')))
                } catch (final NumberFormatException e) {
                    Log.e("Error parsing difficulty value", e)
                }
            }

            // owner
            cache.setOwnerDisplayName(StringEscapeUtils.unescapeHtml4(TextUtils.getMatch(tableInside, GCConstants.PATTERN_OWNER_DISPLAYNAME, true, cache.getOwnerDisplayName())))
            cache.setOwnerGuid(TextUtils.getMatch(tableInside, GCConstants.PATTERN_OWNER_GUID, true, 2, cache.getOwnerGuid(), false))

            // hidden
            try {
                String hiddenString = TextUtils.getMatch(tableInside, GCConstants.PATTERN_HIDDEN, true, null)
                if (StringUtils.isNotBlank(hiddenString)) {
                    cache.setHidden(GCLogin.parseGcCustomDate(hiddenString))
                }
                if (cache.getHiddenDate() == null) {
                    // event date
                    hiddenString = TextUtils.getMatch(tableInside, GCConstants.PATTERN_HIDDENEVENT, true, null)
                    if (StringUtils.isNotBlank(hiddenString)) {
                        cache.setHidden(GCLogin.parseGcCustomDate(hiddenString))
                    }
                }
            } catch (final ParseException e) {
                // failed to parse cache hidden date
                Log.w("GCParser.parseCache: Failed to parse cache hidden (event) date", e)
            }

            // favorite
            try {
                cache.setFavoritePoints(Integer.parseInt(TextUtils.getMatch(tableInside, GCConstants.PATTERN_FAVORITECOUNT, true, "0")))
            } catch (final NumberFormatException e) {
                Log.e("Error parsing favorite count", e)
            }

            // cache size
            cache.setSize(CacheSize.getById(TextUtils.getMatch(tableInside, GCConstants.PATTERN_SIZE, true, CacheSize.NOT_CHOSEN.id)))
        }

        // cache found / DNF
        cache.setFound(TextUtils.matches(page, GCConstants.PATTERN_FOUND))
        cache.setDNF(TextUtils.matches(page, GCConstants.PATTERN_DNF))

        // cache type
        cache.setType(CacheType.getByWaypointType(TextUtils.getMatch(page, GCConstants.PATTERN_TYPE, true, cache.getType().id)))

        // on watchlist
        cache.setOnWatchlist(TextUtils.matches(page, GCConstants.PATTERN_WATCHLIST))

        // latitude and longitude. Can only be retrieved if user is logged in
        String latlon = TextUtils.getMatch(page, GCConstants.PATTERN_LATLON, true, "")
        if (StringUtils.isNotEmpty(latlon)) {
            try {
                cache.setCoords(Geopoint(latlon))
            } catch (final Geopoint.GeopointException e) {
                Log.w("GCParser.parseCache: Failed to parse cache coordinates", e)
            }
        }

        // cache location
        cache.setLocation(TextUtils.getMatch(page, GCConstants.PATTERN_LOCATION, true, ""))

        // cache hint
        val result: String = TextUtils.getMatch(page, GCConstants.PATTERN_HINT, false, null)
        if (result != null) {
            // replace linebreak and paragraph tags
            val hint: String = GCConstants.PATTERN_LINEBREAK.matcher(result).replaceAll("\n")
            cache.setHint(StringUtils.replace(hint, "</p>", "").trim())
        }

        cache.checkFields()

        // cache personal note
        cache.setPersonalNote(personalNoteWithLineBreaks, true)

        // cache Short description
        val sDesc: StringBuilder = StringBuilder()
        if (cache.isEventCache()) {
            try {
                // add event start / end info to beginning of listing
                val eventTimesMatcher: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_EVENTTIMES, tableInside)
                if (eventTimesMatcher.find()) {
                    sDesc.append("<b>")
                            .append(SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(cache.getHiddenDate()))
                            .append(", ")
                            .append(Formatter.formatGCEventTime(tableInside))
                            .append("</b>")
                }
            } catch (Exception e) {
                Log.w("GCParser.parseCache: Failed to parse event time", e)
            }
        } else {
            sDesc.append(TextUtils.getMatch(page, GCConstants.PATTERN_SHORTDESC, true, ""))
        }
        cache.setShortDescription(sDesc.toString())

        // cache description
        val longDescription: String = TextUtils.getMatch(page, GCConstants.PATTERN_DESC, true, "")
        String relatedWebPage = TextUtils.getMatch(page, GCConstants.PATTERN_RELATED_WEB_PAGE, true, "")
        if (StringUtils.isNotEmpty(relatedWebPage)) {
            relatedWebPage = String.format("<br/><br/><a href=\"%s\"><b>%s</b></a>", relatedWebPage, relatedWebPage)
        }
        String galleryImageLink = StringUtils.EMPTY
        val galleryImages: Int = getGalleryCount(page)
        if (galleryImages > 0) {
            galleryImageLink = String.format("<br/><br/><a href=\"%s\"><b>%s</b></a>",
                "https://www.geocaching.com/seek/gallery.aspx?guid=" + cache.getGuid(),
                CgeoApplication.getInstance().getString(R.string.link_gallery, galleryImages))
        }
        Log.d("Gallery image link: " + galleryImageLink)
        String gcChecker = StringUtils.EMPTY
        if (page.contains(GCConstants.PATTERN_GC_CHECKER)) {
            gcChecker = "<!--" + CgeoApplication.getInstance().getString(R.string.link_gc_checker) + "-->"
        }
        cache.setDescription(longDescription + relatedWebPage + gcChecker)

        // cache attributes
        try {
            val attributes: List<String> = ArrayList<>()
            val attributesPre: String = TextUtils.getMatch(page, GCConstants.PATTERN_ATTRIBUTES, true, null)
            if (attributesPre != null) {
                val matcherAttributesInside: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_ATTRIBUTESINSIDE, attributesPre)

                while (matcherAttributesInside.find()) {
                    if (!matcherAttributesInside.group(2).equalsIgnoreCase("blank")) {
                        // by default, use the tooltip of the attribute
                        String attribute = matcherAttributesInside.group(2).toLowerCase(Locale.US)

                        // if the image name can be recognized, use the image name as attribute
                        val imageName: String = matcherAttributesInside.group(1).trim()
                        if (StringUtils.isNotEmpty(imageName)) {
                            val start: Int = imageName.lastIndexOf('/')
                            val end: Int = imageName.lastIndexOf('.')
                            if (start >= 0 && end >= 0) {
                                attribute = imageName.substring(start + 1, end).replace('-', '_').toLowerCase(Locale.US)
                            }
                        }
                        attributes.add(attribute)
                    }
                }
            }
            cache.setAttributes(attributes)
        } catch (final RuntimeException e) {
            // failed to parse cache attributes
            Log.w("GCParser.parseCache: Failed to parse cache attributes", e)
        }

        // cache spoilers
        val cacheSpoilers: List<Image> = ArrayList<>()
        try {
            if (DisposableHandler.isDisposed(handler)) {
                return UNKNOWN_PARSE_ERROR
            }
            DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_spoilers)

            cacheSpoilers.addAll(parseSpoiler(page))

        } catch (final RuntimeException e) {
            // failed to parse cache spoilers
            Log.w("GCParser.parseCache: Failed to parse cache spoilers", e)
        }

        // background image, to be added only if the image is not already present in the cache listing
        val matcherBackgroundImage: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_BACKGROUND_IMAGE, page)
        if (matcherBackgroundImage.find()) {
            val url: String = matcherBackgroundImage.group(1)
            Boolean present = false
            for (final Image image : cacheSpoilers) {
                if (StringUtils == (image.getUrl(), url)) {
                    present = true
                    break
                }
            }
            if (!present) {
                cacheSpoilers.add(Image.Builder().setUrl(url)
                    .setTitle(LocalizationUtils.getString(R.string.image_listing_background))
                    .setDescription(LocalizationUtils.getString(R.string.cache_image_background)).build())
            }
        }
        cache.setSpoilers(cacheSpoilers)

        // cache inventory
        val inventory: List<Trackable> = parseInventory(page)
        if (inventory != null) {
            cache.mergeInventory(inventory, EnumSet.of(TrackableBrand.TRAVELBUG))
        }

        // cache logs counts
        try {
            val countlogs: String = TextUtils.getMatch(page, GCConstants.PATTERN_COUNTLOGS, true, null)
            if (countlogs != null) {
                val matcherLog: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_COUNTLOG, countlogs)

                while (matcherLog.find()) {
                    val typeStr: String = matcherLog.group(1)
                    val countStr: String = getNumberString(matcherLog.group(2))

                    if (StringUtils.isNotBlank(typeStr)
                            && LogType.getByIconName(typeStr) != LogType.UNKNOWN
                            && StringUtils.isNotBlank(countStr)) {
                        cache.getLogCounts().put(LogType.getByIconName(typeStr), Integer.valueOf(countStr))
                    }
                }
            }
            if (cache.getLogCounts().isEmpty()) {
                Log.w("GCParser.parseCache: Failed to parse cache log count")
            }
        } catch (final NumberFormatException e) {
            // failed to parse logs
            Log.w("GCParser.parseCache: Failed to parse cache log count", e)
        }

        // waypoints - reset collection
        cache.setWaypoints(Collections.emptyList())

        // add waypoint for original coordinates in case of user-modified listing-coordinates
        try {
            val originalCoords: String = TextUtils.getMatch(page, GCConstants.PATTERN_LATLON_ORIG, false, null)

            if (originalCoords != null) {
                cache.createOriginalWaypoint(Geopoint(originalCoords))
            }
        } catch (final Geopoint.GeopointException ignored) {
        }

        Int wpBegin = page.indexOf("id=\"ctl00_ContentBody_Waypoints\">")
        if (wpBegin != -1) { // parse waypoints
            if (DisposableHandler.isDisposed(handler)) {
                return UNKNOWN_PARSE_ERROR
            }
            DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_waypoints)

            String wpList = page.substring(wpBegin)

            Int wpEnd = wpList.indexOf("</p>")
            if (wpEnd > -1 && wpEnd <= wpList.length()) {
                wpList = wpList.substring(0, wpEnd)
            }

            if (!wpList.contains("No additional waypoints to display.")) {
                wpEnd = wpList.indexOf("</table>")
                wpList = wpList.substring(0, wpEnd)

                wpBegin = wpList.indexOf("<tbody>")
                wpEnd = wpList.indexOf("</tbody>")
                if (wpBegin >= 0 && wpEnd >= 0 && wpEnd <= wpList.length()) {
                    wpList = wpList.substring(wpBegin + 7, wpEnd)
                }

                final String[] wpItems = StringUtils.splitByWholeSeparator(wpList, "<tr")

                for (Int j = 1; j < wpItems.length; j += 2) {
                    final String[] wp = StringUtils.splitByWholeSeparator(wpItems[j], "<td")
                    assert wp != null
                    if (wp.length < 7) {
                        Log.e("GCParser.cacheParseFromText: not enough waypoint columns in table")
                        continue
                    }

                    // waypoint name
                    // res is null during the unit tests
                    val name: String = TextUtils.getMatch(wp[5], GCConstants.PATTERN_WPNAME, true, 1, CgeoApplication.getInstance().getString(R.string.waypoint), true)

                    // waypoint type
                    val resulttype: String = TextUtils.getMatch(wp[2], GCConstants.PATTERN_WPTYPE, null)

                    val waypoint: Waypoint = Waypoint(name, WaypointType.findById(resulttype), false)

                    // waypoint prefix
                    waypoint.setPrefix(TextUtils.getMatch(wp[3], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, true, 2, waypoint.getPrefix(), false))

                    // waypoint lookup
                    waypoint.setLookup(TextUtils.getMatch(wp[4], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, true, 2, waypoint.getLookup(), false))

                    // waypoint latitude and longitude
                    latlon = TextUtils.stripHtml(TextUtils.getMatch(wp[6], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, false, 2, "", false)).trim()
                    if (!StringUtils.startsWith(latlon, "???")) {
                        waypoint.setCoords(Geopoint(latlon))
                    } else {
                        waypoint.setOriginalCoordsEmpty(true)
                    }

                    if (j + 1 < wpItems.length) {
                        final String[] wpNote = StringUtils.splitByWholeSeparator(wpItems[j + 1], "<td")
                        assert wpNote != null
                        if (wpNote.length < 4) {
                            Log.d("GCParser.cacheParseFromText: not enough waypoint columns in table to extract note")
                            continue
                        }

                        // waypoint note, cleanup via Jsoup
                        val noteText: String = TextUtils.getMatch(wpNote[3], GCConstants.PATTERN_WPNOTE, waypoint.getNote())
                        if (StringUtils.isNotBlank(noteText)) {
                            val document: Document = Jsoup.parse(noteText)
                            waypoint.setNote(document.outerHtml())
                        } else {
                            waypoint.setNote(StringUtils.EMPTY)
                        }
                    }

                    cache.addOrChangeWaypoint(waypoint, false)
                }
            }
        }

        // last check for necessary cache conditions
        if (StringUtils.isBlank(cache.getGeocode())) {
            return UNKNOWN_PARSE_ERROR
        }

        cache.setDetailedUpdatedNow()
        return ImmutablePair.of(StatusCode.NO_ERROR, cache)
    }

    public static List<Image> parseSpoiler(final String html) {
        val cacheSpoilers: List<Image> = ArrayList<>()
        val matcherSpoilersInside: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_SPOILER_IMAGE, html)
        while (matcherSpoilersInside.find()) {
            val url: String = matcherSpoilersInside.group(1)

            String title = null
            if (matcherSpoilersInside.group(2) != null) {
                title = matcherSpoilersInside.group(2)
            } else {
                title = LocalizationUtils.getString(R.string.image_listing_spoiler)
            }
            String description = LocalizationUtils.getString(R.string.image_listing_spoiler)
            if (matcherSpoilersInside.group(3) != null) {
                description += ": " + matcherSpoilersInside.group(3)
            }
            cacheSpoilers.add(Image.Builder().setUrl(url).setTitle(title).setDescription(description).build())
        }
        return cacheSpoilers
    }

    public static List<Trackable> parseInventory(final String page) {
        try {
            val matcherInventory: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_INVENTORY, page)
            if (matcherInventory.find()) {
                val inventoryPre: String = matcherInventory.group()

                val inventory: ArrayList<Trackable> = ArrayList<>()
                if (StringUtils.isNotBlank(inventoryPre)) {
                    val matcherInventoryInside: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_INVENTORYINSIDE, inventoryPre)

                    while (matcherInventoryInside.find()) {
                        val isGeocode: Boolean = "TB" == (matcherInventoryInside.group(1))
                        val tbId: String = matcherInventoryInside.group(2)
                        val inventoryItem: Trackable = Trackable()
                        inventoryItem.setGeocode(isGeocode ? tbId : null)
                        inventoryItem.setGuid(isGeocode ? null : tbId)
                        inventoryItem.forceSetBrand(TrackableBrand.TRAVELBUG)
                        inventoryItem.setName(matcherInventoryInside.group(3))

                        inventory.add(inventoryItem)
                    }
                }
                return inventory
            }
        } catch (final RuntimeException e) {
            // failed to parse cache inventory
            Log.w("GCParser.parseCache: Failed to parse cache inventory (2)", e)
        }
        return null
    }

    private static String getNumberString(final String numberWithPunctuation) {
        return StringUtils.replaceChars(numberWithPunctuation, ".,", "")
    }

    private static SearchResult searchByMap(final IConnector con, final Parameters params, final String context) {
        val page: String = GCLogin.getInstance().getRequestLogged(GCConstants.URL_LIVE_MAP, params)

        if (StringUtils.isBlank(page)) {
            Log.w("GCParser.searchByMap: No data from server")
            return null
        }

        val sessionToken: String = TextUtils.getMatch(page, GCConstants.PATTERN_SESSIONTOKEN, "")
        if (StringUtils.isBlank(sessionToken)) {
            Log.w("GCParser.searchByMap: Failed to retrieve session token")
            return null
        }

        params.add("st", sessionToken)

        val pqJson: String = GCLogin.getInstance().getRequestLogged("https://tiles01.geocaching.com/map." + context, params)

        SearchResult searchResult
        try {
            searchResult = parseMap(con, "https://tiles01.geocaching.com/map." + context + "?" + params, pqJson, 0)
        } catch (JsonProcessingException e) {
            searchResult = null
        }
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.w("GCParser.searchByMap : No cache parsed")
            return searchResult
        }

        val search: SearchResult = searchResult.putInCacheAndLoadRating()

        GCLogin.getInstance().getLoginStatus(page)

        return search
    }

    public static SearchResult searchByPocketQuery(final IConnector con, final String shortGuid, final String pqHash) {
        if (StringUtils.isBlank(pqHash)) {
            Log.e("GCParser.searchByPocket: No guid name given")
            return null
        }

        val params: Parameters = Parameters("pq", shortGuid, "hash", pqHash)
        return searchByMap(con, params, "pq")
    }

    public static SearchResult searchByBookmarkList(final IConnector con, final String bmGuid, final Int alreadyTaken) {
        val params: Parameters = Parameters("skip", String.valueOf(alreadyTaken), "take", "1000")

        val url: String = "https://www.geocaching.com/api/proxy/web/v1/lists/" + bmGuid + "/geocaches"
        val page: String = GCLogin.getInstance().getRequestLogged(url, params)

        if (StringUtils.isBlank(page)) {
            Log.w("GCParser.searchByBookmarkList: No data from server")
            return null
        }

        val caches: List<Geocache> = ArrayList<>()
        try {
            val json: JsonNode = JsonUtils.reader.readTree(page)

            val totalCount: Int = json.get("total").asInt()
            val jsonData: JsonNode = json.get("data")
            for (Int i = 0; i < jsonData.size(); i++) {
                val cache: Geocache = Geocache()
                val properties: JsonNode = jsonData.get(i)
                val stateProps: JsonNode = properties.get("state")
                cache.setName(properties.get("name").asText())
                cache.setGeocode(properties.get("referenceCode").asText())
                cache.setOwnerDisplayName(properties.get("owner").asText())
                cache.setDifficulty(properties.get("difficulty").floatValue())
                cache.setTerrain(properties.get("terrain").floatValue())
                cache.setSize(CacheSize.getByGcId(properties.get("containerType").asInt()))
                cache.setType(CacheType.getByWaypointType(properties.get("geocacheType").asText()))

                cache.setArchived(stateProps.get("isArchived").asBoolean())
                cache.setDisabled(!stateProps.get("isAvailable").asBoolean())
                cache.setPremiumMembersOnly(stateProps.get("isPremiumOnly").asBoolean())

                caches.add(cache)
            }

            val currentFetched: Int = alreadyTaken + caches.size()
            val searchResult: SearchResult = SearchResult(caches)
            searchResult.setLeftToFetch(con, totalCount - currentFetched)
            searchResult.setToContext(con, b -> {
                b.putInt(GCConnector.SEARCH_CONTEXT_TOOK_TOTAL, currentFetched)
                b.putString(GCConnector.SEARCH_CONTEXT_BOOKMARK, bmGuid)
            })

            return searchResult
        } catch (final Exception e) {
            Log.e("GCParser.searchByBookmarkLists: error parsing html page", e)
            return null
        }
    }

    public static Trackable searchTrackable(final String geocode, final String guid, final String id) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid) && StringUtils.isBlank(id)) {
            Log.w("GCParser.searchTrackable: No geocode nor guid nor id given")
            return null
        }

        val params: Parameters = Parameters()
        if (StringUtils.isNotBlank(geocode)) {
            params.put("tracker", geocode)
        } else if (StringUtils.isNotBlank(guid)) {
            params.put("guid", guid)
        } else if (StringUtils.isNotBlank(id)) {
            params.put("id", id)
        }

        val page: String = GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/track/details.aspx", params)

        if (StringUtils.isBlank(page)) {
            Log.w("GCParser.searchTrackable: No data from server")
            return null
        }

        val trackable: Trackable = parseTrackable(page, geocode)
        if (trackable == null) {
            Log.w("GCParser.searchTrackable: No trackable parsed")
            return null
        }

        return trackable
    }

    /**
     * Fetches a list of bookmark lists. Shouldn't be called on main thread!
     *
     * @return A non-null list (which might be empty) on success. Null on error.
     */
    @WorkerThread
    public static List<GCList> searchBookmarkLists() {
        val params: Parameters = Parameters()
        params.add("skip", "0")
        params.add("take", "100")
        params.add("type", "bm")

        val page: String = GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/api/proxy/web/v1/lists", params)
        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.searchBookmarkLists: No data from server")
            return null
        }

        try {
            val json: JsonNode = JsonUtils.reader.readTree(page).get("data")
            val list: List<GCList> = ArrayList<>()

            for (Iterator<JsonNode> it = json.elements(); it.hasNext(); ) {
                val row: JsonNode = it.next()

                val name: String = row.get("name").asText()
                val guid: String = row.get("referenceCode").asText()
                val count: Int = row.get("count").asInt()
                Date date
                val lastUpdateUtc: String = row.get("lastUpdateUtc").asText()
                try {
                    date = DATE_JSON.parse(lastUpdateUtc)
                } catch (ParseException e) {
                    // if parsing with fractions of seconds failed, try Short form
                    date = DATE_JSON_SHORT.parse(lastUpdateUtc)
                    Log.d("parsing bookmark list: fallback needed for '" + lastUpdateUtc + "'")
                }

                val pocketQuery: GCList = GCList(guid, name, count, true, date.getTime(), -1, true, null, null)
                list.add(pocketQuery)
            }

            return list
        } catch (final Exception e) {
            Log.e("GCParser.searchBookmarkLists: error parsing html page", e)
            return null
        }
    }

    /**
     * Creates a bookmark list. Shouldn't be called on main thread!
     *
     * @return guid of the list.
     */
    @WorkerThread
    public static String createBookmarkList(final String name, final Geocache geocache) {
        val jo: ObjectNode = ObjectNode(JsonUtils.factory).put("name", name)
        jo.putObject("type").put("code", "bm")

        try {
            val headers: Parameters = Parameters(HEADER_VERIFICATION_TOKEN, getRequestVerificationToken(geocache))
            val result: String = Network.getResponseData(Network.postJsonRequest("https://www.geocaching.com/api/proxy/web/v1/lists", headers, jo))

            if (StringUtils.isBlank(result)) {
                Log.e("GCParser.createBookmarkList: No response from server")
                return null
            }

            val guid: String = JsonUtils.reader.readTree(result).get("referenceCode").asText()

            if (StringUtils.isBlank(guid)) {
                Log.e("GCParser.createBookmarkList: Malformed result")
                return null
            }

            return guid

        } catch (final Exception ignored) {
            Log.e("GCParser.createBookmarkList: Error while creating bookmark list")
            return null
        }
    }

    /**
     * Creates a bookmark list. Shouldn't be called on main thread!
     *
     * @return successful?
     */
    public static Single<Boolean> addCachesToBookmarkList(final String listGuid, final List<Geocache> geocaches) {
        val arrayNode: ArrayNode = JsonUtils.createArrayNode()

        for (Geocache geocache : geocaches) {
            if (ConnectorFactory.getConnector(geocache) is GCConnector) {
                arrayNode.add(ObjectNode(JsonUtils.factory).put("referenceCode", geocache.getGeocode()))
            }
        }

        Log.d(arrayNode.toString())
        val headers: Parameters = Parameters(HEADER_VERIFICATION_TOKEN, getRequestVerificationToken(geocaches.get(0)))

        return Network.completeWithSuccess(Network.putJsonRequest("https://www.geocaching.com/api/proxy/web/v1/lists/" + listGuid + "/geocaches", headers, arrayNode))
            .toSingle(() -> {
                Log.i("GCParser.addCachesToBookmarkList - caches uploaded to GC.com bookmark list")
                return true
            })
            .onErrorReturn((throwable) -> {
                Log.e("GCParser.addCachesToBookmarkList - cannot upload caches to GC.com bookmark list", throwable)
                return false
            })
    }

    /**
     * Fetches a list of pocket queries. Shouldn't be called on main thread!
     *
     * @return A non-null list (which might be empty) on success. Null on error.
     */
    @WorkerThread
    public static List<GCList> searchPocketQueries() {
        val page: String = GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/pocket/default.aspx", null)
        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.searchPocketQueryList: No data from server")
            return null
        }

        try {
            val document: Document = Jsoup.parse(page)
            val downloadablePocketQueries: Map<String, GCList> = getDownloadablePocketQueries(document)
            val list: List<GCList> = ArrayList<>(downloadablePocketQueries.values())

            val rows: Elements = document.select("#pqRepeater tr:has(td)")
            for (final Element row : rows) {
                if (row == rows.last()) {
                    break; // skip footer
                }
                val link: Element = row.select("td:eq(3) > a").first()
                val uri: Uri = Uri.parse(link.attr("href"))
                val guid: String = uri.getQueryParameter("guid")
                val mapUri: Uri = Uri.parse(row.select("td:eq(2) > a").get(1).attr("href"))
                val shortGuid: String = mapUri.getQueryParameter("pq")
                val pqHash: String = mapUri.getQueryParameter("hash")
                if (!downloadablePocketQueries.containsKey(guid)) {
                    val name: String = link.attr("title")
                    val pocketQuery: GCList = GCList(guid, name, -1, false, 0, -1, false, shortGuid, pqHash)
                    list.add(pocketQuery)
                } else {
                    val pq: GCList = downloadablePocketQueries.get(guid)
                    pq.setPqHash(pqHash)
                    pq.setShortGuid(shortGuid)
                }
            }
            return list
        } catch (final Exception e) {
            Log.e("GCParser.searchPocketQueryList: error parsing html page", e)
            return null
        }
    }

    /**
     * Reads the downloadable pocket queries from the uxOfflinePQTable
     *
     * @param document the page as Document
     * @return Map with downloadable PQs keyed by guid
     */
    private static Map<String, GCList> getDownloadablePocketQueries(final Document document) throws Exception {
        val downloadablePocketQueries: Map<String, GCList> = HashMap<>()

        val rows: Elements = document.select("#uxOfflinePQTable tr:has(td)")
        for (final Element row : rows) {
            if (row == rows.last()) {
                break; // skip footer
            }

            val cells: Elements = row.select("td")
            if (cells.size() < 6) {
                Log.d("GCParser.getDownloadablePocketQueries: less than 6 table cells, looks like an empty table")
                continue
            }
            val link: Element = cells.get(2).select("a").first()
            if (link == null) {
                Log.w("GCParser.getDownloadablePocketQueries: Downloadlink not found")
                continue
            }
            val name: String = link.text()
            val href: String = link.attr("href")
            val uri: Uri = Uri.parse(href)
            val guid: String = uri.getQueryParameter("g")

            val count: Int = Integer.parseInt(cells.get(4).text())

            val matcherLastGeneration: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_PQ_LAST_GEN, cells.get(5).text())
            Long lastGeneration = 0
            Int daysRemaining = 0
            if (matcherLastGeneration.find()) {
                val lastGenerationDate: Date = GCLogin.parseGcCustomDate(matcherLastGeneration.group(1))
                if (lastGenerationDate != null) {
                    lastGeneration = lastGenerationDate.getTime()
                }

                val daysRemainingString: String = matcherLastGeneration.group(3)
                if (daysRemainingString != null) {
                    daysRemaining = Integer.parseInt(daysRemainingString)
                }
            }

            val pocketQuery: GCList = GCList(guid, name, count, true, lastGeneration, daysRemaining, false, null, null)
            downloadablePocketQueries.put(guid, pocketQuery)
        }

        return downloadablePocketQueries
    }

    /**
     * Adds the cache to the watchlist of the user.
     *
     * @param cache the cache to add
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    static Single<Boolean> addToWatchlist(final Geocache cache) {
        return addToOrRemoveFromWatchlist(cache, true)
    }

    /**
     * internal method to handle add to / remove from watchlist
     */
    private static Single<Boolean> addToOrRemoveFromWatchlist(final Geocache cache, final Boolean doAdd) {

        val logContext: String = "GCParser.addToOrRemoveFromWatchlist(cache = " + cache.getGeocode() + ", add = " + doAdd + ")"

        val jo: ObjectNode = ObjectNode(JsonUtils.factory).put("geocacheId", cache.getCacheId())
        val uri: String = "https://www.geocaching.com/api/proxy/web/v1/watchlists/" + (doAdd ? "add" : "remove") + "?geocacheId=" + cache.getCacheId()

        val request: Single<Response> = doAdd
            ? Network.postJsonRequest(uri, jo)
            : Network.deleteJsonRequest(uri, jo)
        return Network.completeWithSuccess(request)
            .toSingle(() -> {
                Log.i(logContext + ": success")
                return true
            })
            .onErrorReturn((ex) -> {
                Log.e(logContext + ": error", ex)
                return false
            })
            .map((successful) -> {
                if (successful) {
                    // Set cache properties
                    cache.setOnWatchlist(doAdd)
                    val watchListPage: String = GCLogin.getInstance().postRequestLogged(cache.getLongUrl(), null)
                    cache.setWatchlistCount(getWatchListCount(watchListPage))
                }
                return successful
            })
    }

    /**
     * This method extracts the amount of people watching on a geocache out of the HTMl website passed to it
     *
     * @param page Page containing the information about how many people watching on geocache
     * @return Number of people watching geocache, -1 when error
     */
    static Int getWatchListCount(final String page) {
        return getCount(page, GCConstants.PATTERN_WATCHLIST_COUNT, 1)
    }

    static Int getGalleryCount(final String page) {
        return getCount(page, GCConstants.PATTERN_GALLERY_COUNT, 1)
    }

    private static Int getCount(final String page, final Pattern pattern, final Int group) {

        val sCount: String = TextUtils.getMatch(page, pattern, true, group, "notFound", false)
        if ("notFound" == (sCount)) {
            return -1
        }
        try {
            return Integer.parseInt(sCount)
        } catch (final NumberFormatException nfe) {
            Log.e("Could not parse", nfe)
            return -1
        }
    }



    /**
     * Removes the cache from the watch list
     *
     * @param cache the cache to remove
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    static Single<Boolean> removeFromWatchlist(final Geocache cache) {
        return addToOrRemoveFromWatchlist(cache, false)
    }

    static String requestHtmlPage(final String geocode, final String guid) {
        if (StringUtils.isNotBlank(geocode)) {
            val params: Parameters = Parameters("decrypt", "y")
            return GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/geocache/" + geocode, params, false)
        } else if (StringUtils.isNotBlank(guid)) {
            val params: Parameters = Parameters("decrypt", "y")
            params.put("guid", guid)
            return GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/seek/cache_details.aspx", params)
        }
        return null
    }

    /**
     * Adds the cache to the favorites of the user.
     * <br>
     * This must not be called from the UI thread.
     *
     * @param cache the cache to add
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    static Single<Boolean> addToFavorites(final Geocache cache) {
        return changeFavorite(cache, true)
    }

    private static Single<Boolean> changeFavorite(final Geocache cache, final Boolean add) {
        val userToken: String = getUserToken(cache)
        if (StringUtils.isEmpty(userToken)) {
            return Single.just(false)
        }

        val uri: String = "https://www.geocaching.com/datastore/favorites.svc/update?u=" + userToken + "&f=" + add

        return Network.completeWithSuccess(Network.postRequest(uri, null))
            .toSingle(() -> {
                Log.i("GCParser.changeFavorite: cache added/removed to/from favorites")
                return true
            })
            .onErrorReturn((throwable) -> {
                Log.e("GCParser.changeFavorite: cache not added/removed to/from favorites", throwable)
                return false
            })
            .map((successful) -> {
                if (successful) {
                    cache.setFavorite(add)
                    cache.setFavoritePoints(cache.getFavoritePoints() + (add ? 1 : -1))
                }
                return successful
            })
    }

    private static String getUserToken(final Geocache cache) {
        return getUserToken(cache.getGeocode())
    }

    private static String getUserToken(final String geocode) {
        return parseUserToken(requestHtmlPage(geocode, null))
    }

    private static String parseUserToken(final String page) {
        return TextUtils.getMatch(page, GCConstants.PATTERN_USERTOKEN, "")
    }

    private static String getRequestVerificationToken(final Geocache cache) {
        return parseRequestVerificationToken(requestHtmlPage(cache.getGeocode(), null))
    }

    private static String parseRequestVerificationToken(final String page) {
        return TextUtils.getMatch(page, GCConstants.PATTERN_REQUESTVERIFICATIONTOKEN, "")
    }

    /**
     * Removes the cache from the favorites.
     * <br>
     * This must not be called from the UI thread.
     *
     * @param cache the cache to remove
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    static Single<Boolean> removeFromFavorites(final Geocache cache) {
        return changeFavorite(cache, false)
    }

    /**
     * Parse a trackable HTML description into a Trackable object
     *
     * @param page the HTML page to parse, already processed through {@link TextUtils#replaceWhitespace}
     * @return the parsed trackable, or null if none could be parsed
     */
    static Trackable parseTrackable(final String page, final String possibleTrackingcode) {
        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.parseTrackable: No page given")
            return null
        }

        if (page.contains(GCConstants.ERROR_TB_DOES_NOT_EXIST) || page.contains(GCConstants.ERROR_TB_ARITHMETIC_OVERFLOW) || page.contains(GCConstants.ERROR_TB_ELEMENT_EXCEPTION)) {
            return null
        }

        val trackable: Trackable = Trackable()
        trackable.setGeocode(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GEOCODE, true, StringUtils.upperCase(possibleTrackingcode)))
        trackable.forceSetBrand(TrackableBrand.TRAVELBUG)
        if (trackable.getGeocode() == null) {
            Log.e("GCParser.parseTrackable: could not figure out trackable geocode")
            return null
        }

        // trackable id
        trackable.setGuid(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GUID, true, trackable.getGuid()))

        // trackable icon
        val iconUrl: String = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_ICON, true, trackable.getIconUrl())
        trackable.setIconUrl(iconUrl.startsWith("/") ? "https://www.geocaching.com" + iconUrl : iconUrl)

        // trackable name
        trackable.setName(TextUtils.stripHtml(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, true, "")))

        // trackable type
        if (StringUtils.isNotBlank(trackable.getName())) {
            // old TB pages include TB type as "alt" attribute
            String type = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_TYPE, true, trackable.getType())
            if (StringUtils.isNotBlank(type)) {
                type = TextUtils.stripHtml(type)
            } else {
                // try alternative way on pages formatted the newer style: <title>\n\t(TBxxxx) Type - Name\n</title>
                val title: String = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_TYPE_TITLE, true, "")
                if (StringUtils.isNotBlank(title)) {
                    val nameWithHTML: String = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, true, "")
                    val pos: Int = StringUtils.lastIndexOfIgnoreCase(title, nameWithHTML)
                    if (pos != INDEX_NOT_FOUND) {
                        type = substring(title, 0, pos - 3)
                        type = TextUtils.stripHtml(type)
                    }
                }
            }
            trackable.setType(type)
        }

        // trackable owner name
        try {
            val matcherOwner: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_TRACKABLE_OWNER, page)
            if (matcherOwner.find()) {
                trackable.setOwnerGuid(matcherOwner.group(2))
                trackable.setOwner(matcherOwner.group(3).trim())

            }
        } catch (final RuntimeException e) {
            // failed to parse trackable owner name
            Log.w("GCParser.parseTrackable: Failed to parse trackable owner name", e)
        }

        // trackable origin
        trackable.setOrigin(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_ORIGIN, true, trackable.getOrigin()))

        // trackable spotted
        try {
            val matcherSpottedCache: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_TRACKABLE_SPOTTEDCACHE_BY_GEOCODE, page)
            if (matcherSpottedCache.find()) {
                trackable.setSpottedCacheGeocode(matcherSpottedCache.group(2))
                trackable.setSpottedName(matcherSpottedCache.group(1).trim())
                trackable.setSpottedType(Trackable.SPOTTED_CACHE)
            } else {
                val matcherSpottedCacheGuid: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_TRACKABLE_SPOTTEDCACHE_BY_GUID, page)
                if (matcherSpottedCacheGuid.find()) {
                    trackable.setSpottedGuid(matcherSpottedCache.group(2))
                    trackable.setSpottedName(matcherSpottedCache.group(1).trim())
                    trackable.setSpottedType(Trackable.SPOTTED_CACHE)
                }
            }

            val matcherSpottedUser: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_TRACKABLE_SPOTTEDUSER, page)
            if (matcherSpottedUser.find()) {
                trackable.setSpottedGuid(matcherSpottedUser.group(3))
                trackable.setSpottedName(HtmlCompat.fromHtml(matcherSpottedUser.group(1), HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()); // remove HTML in parsed username, see #14442
                trackable.setSpottedType(Trackable.SPOTTED_USER)
            }

            if (TextUtils.matches(page, GCConstants.PATTERN_TRACKABLE_SPOTTEDUNKNOWN)) {
                trackable.setSpottedType(Trackable.SPOTTED_UNKNOWN)
            }

            if (TextUtils.matches(page, GCConstants.PATTERN_TRACKABLE_SPOTTEDOWNER)) {
                trackable.setSpottedType(Trackable.SPOTTED_OWNER)
            }
        } catch (final RuntimeException e) {
            // failed to parse trackable last known place
            Log.w("GCParser.parseTrackable: Failed to parse trackable last known place", e)
        }

        // released date - can be missing on the page
        val releaseString: String = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_RELEASES, false, null)
        if (releaseString != null) {
            try {
                trackable.setReleased(DATE_TB_IN_1.parse(releaseString))
            } catch (final ParseException ignored) {
                if (trackable.getReleased() == null) {
                    try {
                        trackable.setReleased(DATE_TB_IN_2.parse(releaseString))
                    } catch (final ParseException e) {
                        Log.e("Could not parse trackable release " + releaseString, e)
                    }
                }
            }
        }

        // log - entire section can be missing on the page if trackable hasn't been found by the user
        try {
            val logType: String = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_FOUND_LOG, false, null)
            if (logType != null) {
                trackable.setLogType(LogType.getByIconName(StringUtils.trim(logType)))
            }
            val retrievedMatcher: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_TRACKABLE_DISPOSITION_LOG, page)
            if (retrievedMatcher.find()) {
                trackable.setLogDate(GCLogin.parseGcCustomDate(StringUtils.trim(retrievedMatcher.group(2))))
                trackable.setLogGuid(StringUtils.trim(retrievedMatcher.group(1)))
            }
        } catch (final Exception e) {
            Log.e("GCParser.parseTrackable: Failed to parse log", e)
        }

        // trackable distance
        val distanceMatcher: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_TRACKABLE_DISTANCE, page)
        if (distanceMatcher.find()) {
            val unit: DistanceUnit = DistanceUnit.findById(distanceMatcher.group(2),
                    Settings.useImperialUnits() ? DistanceUnit.MILE : DistanceUnit.KILOMETER)
            try {
                trackable.setDistance(unit.parseToKilometers(distanceMatcher.group(1)))
            } catch (final NumberFormatException e) {
                Log.e("GCParser.parseTrackable: Failed to parse distance", e)
            }
        }

        // trackable goal
        trackable.setGoal(convertLinks(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GOAL, true, trackable.getGoal())))

        // trackable details & image
        try {
            val matcherDetailsImage: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_TRACKABLE_DETAILSIMAGE, page)
            if (matcherDetailsImage.find()) {
                val image: String = StringUtils.trim(matcherDetailsImage.group(3))
                val details: String = StringUtils.trim(matcherDetailsImage.group(4))

                if (StringUtils.isNotEmpty(image)) {
                    trackable.setImage(StringUtils.replace(image, "/display/", "/large/"))
                }
                if (StringUtils.isNotEmpty(details) && !StringUtils == (details, "No additional details available.")) {
                    trackable.setDetails(convertLinks(details))
                }
            }
        } catch (final RuntimeException e) {
            // failed to parse trackable details & image
            Log.w("GCParser.parseTrackable: Failed to parse trackable details & image", e)
        }
        if (StringUtils.isEmpty(trackable.getDetails()) && page.contains(GCConstants.ERROR_TB_NOT_ACTIVATED)) {
            trackable.setDetails(CgeoApplication.getInstance().getString(R.string.trackable_not_activated))
        }

        // trackable may be locked (see e.g. TB673CE)
        if (MatcherWrapper(GCConstants.PATTERN_TRACKABLE_IS_LOCKED, page).find()) {
            trackable.setIsLocked()
        }

        // trackable logs
        try {
            val matcherLogsOuter: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_TRACKABLE_LOG_OUTER, page)
            /*
             * 1. Type (image)
             * 2. Date
             * 3. Author-GUID
             * 4. Author
             * 5. Cache-GUID or cache-code
             * 6. <ignored> (strike-through property for ancient caches)
             * 7. Cache-name
             * 8. Log-ID
             * 9. Log text
             */
            while (matcherLogsOuter.find()) {
                // search each log block separately
                val matcherLogs: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_TRACKABLE_LOG_INNER, matcherLogsOuter.group(0))
                while (matcherLogs.find()) {
                    Long date = 0
                    try {
                        date = GCLogin.parseGcCustomDate(matcherLogs.group(2)).getTime()
                    } catch (final ParseException ignored) {
                    }

                    final LogEntry.Builder logDoneBuilder = LogEntry.Builder()
                            .setAuthor(TextUtils.stripHtml(matcherLogs.group(4)).trim())
                            .setAuthorGuid(matcherLogs.group(3))
                            .setDate(date)
                            .setLogType(LogType.getByIconName(matcherLogs.group(1)))
                            .setServiceLogId(matcherLogs.group(8))
                            .setLog(matcherLogs.group(9).trim())

                    if (matcherLogs.group(5) != null && matcherLogs.group(7) != null) {
                        logDoneBuilder.setCacheGeocode(matcherLogs.group(5))
                        logDoneBuilder.setCacheName(matcherLogs.group(7))
                    }

                    // Apply the pattern for images in a trackable log entry against each full log (group(0))
                    val logEntry: String = matcherLogsOuter.group(0)
                    val matcherLogImages: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_TRACKABLE_LOG_IMAGES, logEntry)
                    /*
                     * 1. Image URL
                     * 2. Image title
                     */
                    while (matcherLogImages.find()) {
                        val logImage: Image = Image.Builder()
                                .setUrl(matcherLogImages.group(1))
                                .setTitle(matcherLogImages.group(2))
                                .setCategory(Image.ImageCategory.LOG)
                                .build()
                        logDoneBuilder.addLogImage(logImage)
                    }

                    trackable.getLogs().add(logDoneBuilder.build())
                }
            }
        } catch (final Exception e) {
            // failed to parse logs
            Log.w("GCParser.parseCache: Failed to parse cache logs", e)
        }

        // tracking code
        if (!StringUtils.equalsIgnoreCase(trackable.getGeocode(), possibleTrackingcode)) {
            trackable.setTrackingcode(possibleTrackingcode)
        }

        if (CgeoApplication.getInstance() != null) {
            DataStore.saveTrackable(trackable)
        }

        return trackable
    }

    private static String convertLinks(final String input) {
        if (input == null) {
            return null
        }
        return StringUtils.replace(input, "../", GCConstants.GC_URL)
    }

    enum class class Logs {
        ALL(null),
        FRIENDS("sf"),
        OWN("sp"),
        OWNER("showOwnerOnly")

        final String paramName

        Logs(final String paramName) {
            this.paramName = paramName
        }

        private String getParamName() {
            return paramName
        }
    }

    /**
     * Extract special logs (friends, own) through separate request.
     *
     * @param userToken the user token extracted from the web page
     * @param logType   the logType to request
     * @return Observable<LogEntry> The logs
     */
    private static Observable<LogEntry> getLogs(final String userToken, final Logs logType) {
        return getLogs(userToken, logType, GCConstants.NUMBER_OF_LOGS)
    }

    private static Observable<LogEntry> getLogs(final String userToken, final Logs logType, final Int take) {
        if (userToken.isEmpty()) {
            Log.e("GCParser.getLogs: unable to extract userToken")
            return Observable.empty()
        }

        return Observable.defer(() -> {
            val params: Parameters = Parameters(
                    "tkn", userToken,
                    "idx", "1",
                    "num", String.valueOf(take),
                    "decrypt", "false"); // fetch encrypted logs as such
            if (logType != Logs.ALL) {
                params.add(logType.getParamName(), Boolean.toString(Boolean.TRUE))
            }
            try {
                val responseStream: InputStream =
                        Network.getResponseStream(Network.getRequest("https://www.geocaching.com/seek/geocache.logbook", params))
                if (responseStream == null) {
                    Log.w("getLogs: no logs were returned")
                    return Observable.empty()
                }
                return parseLogsAndClose(logType != Logs.ALL, responseStream)
            } catch (final Exception e) {
                Log.w("unable to read logs", e)
                return Observable.empty()
            }
        }).subscribeOn(AndroidRxUtils.networkScheduler)
    }

    private static Observable<LogEntry> parseLogsAndClose(final Boolean markAsFriendsLog, final InputStream responseStream) {
        return Observable.create(emitter -> {
            try {
                val resp: ObjectNode = (ObjectNode) JsonUtils.reader.readTree(responseStream)
                if (!resp.path("status").asText() == ("success")) {
                    Log.w("GCParser.parseLogsAndClose: status is " + resp.path("status").asText("[absent]"))
                    emitter.onComplete()
                    return
                }

                val data: ArrayNode = (ArrayNode) resp.get("data")
                for (final JsonNode entry : data) {
                    val logType: String = entry.path("LogType").asText()

                    final Long date
                    try {
                        date = GCLogin.parseGcCustomDate(entry.get("Visited").asText()).getTime()
                    } catch (ParseException | NullPointerException e) {
                        Log.e("Failed to parse log date", e)
                        continue
                    }

                    // TODO: we should update our log data structure to be able to record
                    // proper coordinates, and make them clickable. In the meantime, it is
                    // better to integrate those coordinates into the text rather than not
                    // display them at all.
                    val latLon: String = entry.path("LatLonString").asText()
                    val logText: String = (StringUtils.isEmpty(latLon) ? "" : (latLon + "<br/><br/>")) + TextUtils.removeControlCharacters(entry.path("LogText").asText())
                    val logCode: String = GCUtils.logIdToLogCode(entry.path("LogID").asLong())
                    final LogEntry.Builder logDoneBuilder = LogEntry.Builder()
                            .setServiceLogId(logCode)
                            .setAuthor(TextUtils.removeControlCharacters(entry.path("UserName").asText()))
                            .setAuthorGuid(entry.path("AccountGuid").asText())
                            .setDate(date)
                            .setLogType(LogType.getByType(logType))
                            .setLog(logText)
                            .setFound(entry.path("GeocacheFindCount").asInt())
                            .setFriend(markAsFriendsLog)

                    val images: ArrayNode = (ArrayNode) entry.get("Images")
                    for (final JsonNode image : images) {
                        val imageGuid: String = image.path("ImageGuid").asText()
                        val imageId: String = image.path("ImageID").asText()
                        val url: String = "https://imgcdn.geocaching.com/cache/log/large/" + image.path("FileName").asText()
                        val title: String = TextUtils.removeControlCharacters(image.path("Name").asText())
                        String description = image.path("Descr").asText()
                        if (StringUtils.contains(description, "Geocaching®") && description.length() < 60) {
                            description = null
                        }
                        val logImage: Image = Image.Builder()
                            .setServiceImageId(GCLogAPI.getLogImageId(imageGuid, imageId))
                            .setUrl(url).setTitle(title).setDescription(description).build()
                        logDoneBuilder.addLogImage(logImage)
                    }

                    emitter.onNext(logDoneBuilder.build())
                }
            } catch (final IOException e) {
                Log.w("Failed to parse cache logs", e)
            } finally {
                IOUtils.closeQuietly(responseStream)
            }
            emitter.onComplete()
        })
    }

    /**
     * Javascript Object from the Logpage: <a href="https://www.geocaching.com/play/geocache/gc.../log">...</a>
     * <pre>
     *     {"value":46}
     * </pre>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AvailableLogType {
        @JsonProperty("value")
        Int value
    }

    public static List<LogTypeTrackable> parseLogTypesTrackables(final String page) {
        final AvailableLogType[] availableTypes = parseLogTypes(page)
        if (availableTypes == null) {
            return Collections.emptyList()
        }
        return CollectionStream.of(availableTypes)
                .filter(a -> a.value > 0)
                .map(a -> LogTypeTrackable.getById(a.value))
                .toList()
    }

    static List<LogType> parseTypes(final String page) {
        final AvailableLogType[] availableTypes = parseLogTypes(page)
        if (availableTypes == null) {
            return Collections.emptyList()
        }
        return Arrays.asList(availableTypes).stream()
                .filter(a -> a.value > 0)
                .map(a -> LogType.getById(a.value))
                .filter(t -> t != LogType.UPDATE_COORDINATES)
                .toList()
    }

    private static AvailableLogType[] parseLogTypes(final String page) {
        //"logTypes":[{"value":2},{"value":3},{"value":4},{"value":45},{"value":7}]
        if (StringUtils.isBlank(page)) {
            return null
        }

        val match: String = TextUtils.getMatch(page, GCConstants.PATTERN_TYPE4, null)
        if (match == null) {
            return null
        }
        try {
            return MAPPER.readValue("[" + match + "]", AvailableLogType[].class)
        } catch (final Exception e) {
            Log.e("Error parsing log types from [" + match + "]", e)
            return null
        }
    }

    static Int parseTrackableCount(final String page) {
        val match: String = TextUtils.getMatch(page, GCConstants.PATTERN_TOTAL_TRACKABLES, null)
        try {
            return Integer.parseInt(match)
        } catch (NumberFormatException e) {
            return 0
        }
    }

    static List<Trackable> parseTrackables(final String page) {
        final GCWebAPI.TrackableInventoryEntry[] trackableInventoryItems = parseTrackablesJson(page)
        if (trackableInventoryItems == null) {
            return Collections.emptyList()
        }
        return Arrays.asList(trackableInventoryItems).stream().map(entry -> {
            val trackable: Trackable = Trackable()
            trackable.setGeocode(entry.referenceCode)
            trackable.setTrackingcode(entry.trackingNumber)
            trackable.setName(entry.name)
            trackable.forceSetBrand(TrackableBrand.TRAVELBUG)
            return trackable
        }).toList()
    }

    private static GCWebAPI.TrackableInventoryEntry[] parseTrackablesJson(final String page) {
        if (StringUtils.isBlank(page)) {
            return null
        }

        val match: String = TextUtils.getMatch(page, GCConstants.PATTERN_LOGPAGE_TRACKABLES, null)
        if (match == null) {
            return null
        }
        try {
            return MAPPER.readValue("[" + match + "]", GCWebAPI.TrackableInventoryEntry[].class)
        } catch (final Exception e) {
            Log.e("Error parsing log types from [" + match + "]", e)
            return null
        }
    }

    @WorkerThread
    private static Unit getExtraOnlineInfo(final Geocache cache, final String page, final DisposableHandler handler) {
        // This method starts the page parsing for logs in the background, as well as retrieve the friends and own logs
        // if requested. It merges them and stores them in the background, while the rating is retrieved if needed and
        // stored. Then we wait for the log merging and saving to be completed before returning.
        if (DisposableHandler.isDisposed(handler)) {
            return
        }

        // merge log-entries (friend-logs and log-times)
        mergeAndStoreLogEntries(cache, page, handler)

        //add gallery images if wanted
        addImagesFromGallery(cache, handler)
    }

    private static Unit mergeAndStoreLogEntries(final Geocache cache, final String page, final DisposableHandler handler) {

        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_logs)

        val userToken: String = getLogsPageUserToken(cache)
        val logs: Observable<LogEntry> = getLogs(userToken, Logs.ALL)
        val ownLogs: Observable<LogEntry> = getLogs(userToken, Logs.OWN).cache()
        val friendLogs: Observable<LogEntry> = Settings.isFriendLogsWanted() ?
                getLogs(userToken, Logs.FRIENDS).cache() : Observable.empty()
        val ownerLogs: Observable<LogEntry> = getLogs(userToken, Logs.OWNER).cache()

        val logsBlocked: List<LogEntry> = logs.toList().blockingGet()
        val ownLogEntriesBlocked: List<LogEntry> = ownLogs.toList().blockingGet()
        val friendLogsBlocked: List<LogEntry> = friendLogs.toList().blockingGet()
        val ownerLogsBlocked: List<LogEntry> = ownerLogs.toList().blockingGet()
        val offlineLog: OfflineLogEntry = DataStore.loadLogOffline(cache.getGeocode())

        List<LogEntry> ownLogsFromDb = Collections.emptyList()
        if (!ownLogEntriesBlocked.isEmpty()) {
            ownLogsFromDb = DataStore.loadLogsOfAuthor(cache.getGeocode(), GCConnector.getInstance().getUserName(), true)
            if (ownLogsFromDb.isEmpty()) {
                ownLogsFromDb = DataStore.loadLogsOfAuthor(cache.getGeocode(), GCConnector.getInstance().getUserName(), false)
            }
        }

        // merge time from offline log
        if (offlineLog != null) {
            mergeOfflineLogTime(ownLogEntriesBlocked, offlineLog)
        }

        // merge time from online-logs already stored in db (overrides possible offline log)
        if (!ownLogsFromDb.isEmpty()) {
            mergeLogTimes(ownLogEntriesBlocked, ownLogsFromDb)
        }

        if (cache.isFound() || cache.isDNF()) {
            for (final LogEntry logEntry : ownLogEntriesBlocked) {
                if (logEntry.logType.isFoundLog() || (!cache.isFound() && cache.isDNF() && logEntry.logType == LogType.DIDNT_FIND_IT)) {
                    cache.setVisitedDate(logEntry.date)
                    break
                }
            }
        }

        val specialLogEntries: List<LogEntry> = ArrayList<>()
        specialLogEntries.addAll(friendLogsBlocked)
        specialLogEntries.addAll(ownLogEntriesBlocked)
        specialLogEntries.addAll(ownerLogsBlocked)
        if (!specialLogEntries.isEmpty()) {
            setFriendsLogs(specialLogEntries)
            mergeModifiedLogs(logsBlocked, specialLogEntries)
        }

        DataStore.saveLogs(cache.getGeocode(), logsBlocked, true)
    }

    private static String getLogsPageUserToken(final Geocache cache) {
        val logsPage: String = GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/seek/geocache_logs.aspx", Parameters("code", cache.getGeocode()))
        return parseUserToken(logsPage)
    }

    private static Unit addImagesFromGallery(final Geocache cache, final DisposableHandler handler) {
        if (StringUtils.isBlank(cache.getGuid()) || !Settings.isStoreLogImages() /* see #16778 */) {
            return
        }
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_spoilers)
        //load page
        //https://www.geocaching.com/seek/gallery.aspx?guid=0e670e6a-4b38-45c7-97b7-fa5da0f367a2
        val galleryFirstPage: String = GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/seek/gallery.aspx", Parameters("guid", cache.getGuid()))
        if (galleryFirstPage == null) {
            return
        }

        //get existing Image URls
        val existingUrls: Set<String> = cache.getSpoilers().stream().map(Image::getUrl).collect(Collectors.toSet())
        //collect Images
        val newImages: List<Image> = parseGalleryImages(galleryFirstPage, url -> !existingUrls.contains(url))
        newImages.addAll(0, cache.getSpoilers())
        cache.setSpoilers(newImages)
    }

    public static List<Image> parseGalleryImages(final String html, final Predicate<String> take) {
        val images: List<Image> = ArrayList<>()
        val matcherImage: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_GALLERY_IMAGE, html)

        while (matcherImage.find()) {
            val date: String = matcherImage.group(1)
            val url: String = matcherImage.group(2)
            val title: String = matcherImage.group(3)

            String description = LocalizationUtils.getString(R.string.image_listing_gallery)
            if (!StringUtils.isBlank(date)) {
                description += ": " + date
            }

            if (!take.test(url)) {
                continue
            }

            images.add(Image.Builder().setCategory(Image.ImageCategory.LISTING)
                .setUrl(url)
                .setTitle(title)
                .setDescription(description).build())

        }
        return images
    }

    @WorkerThread
    public static List<LogEntry> loadLogs(final String geocode, final Logs logs, final Int take) {
        val userToken: String = getUserToken(geocode)
        val logObservable: Observable<LogEntry> = getLogs(userToken, logs, take)
        return logObservable.toList().blockingGet()
    }

    /**
     * Mark log entries as friends logs (personal and friends) to identify
     * them on friends/personal logs tab.
     *
     * @param friendLogs  the list to friend logs
     */
    private static Unit setFriendsLogs(final List<LogEntry> friendLogs) {
        for (Int i = 0; i < friendLogs.size(); i++) {
            val friendLog: LogEntry = friendLogs.get(i)
            if (!friendLog.friend) {
                val updatedFriendLog: LogEntry = friendLog.buildUpon().setFriend(true).build()
                friendLogs.set(i, updatedFriendLog)
            }
        }
    }

    /**
     * Merge log entries
     *
     * @param mergedLogs  the list to merge logs with
     * @param logsToMerge the list of logs to merge
     */
    private static Unit mergeModifiedLogs(final List<LogEntry> mergedLogs, final Iterable<LogEntry> logsToMerge) {
        val mergedLogsMap: Map<String, LogEntry> = HashMap<>()
        for (final LogEntry mergedLog : mergedLogs) {
            mergedLogsMap.put(mergedLog.serviceLogId, mergedLog)
        }
        for (final LogEntry logToMerge : logsToMerge) {
            val modifiedLog: LogEntry = mergedLogsMap.get(logToMerge.serviceLogId)
            if (modifiedLog == null) {
                mergedLogs.add(logToMerge)
            } else {
                val logIndex: Int = mergedLogs.indexOf(modifiedLog)
                if (logIndex >= 0) {
                    mergedLogs.set(logIndex, logToMerge)
                }
            }
        }
    }

    private static Unit mergeLogTimes(final List<LogEntry> mergedLogTimes, final Iterable<LogEntry> logTimesToMerge) {
        val logTimesToMergeMap: Map<String, LogEntry> = HashMap<>()
        for (final LogEntry logToMerge : logTimesToMerge) {
            logTimesToMergeMap.put(logToMerge.serviceLogId, logToMerge)
        }

        for (Int i = 0; i < mergedLogTimes.size(); i++) {
            val mergedLog: LogEntry = mergedLogTimes.get(i)
            val logToMerge: LogEntry = logTimesToMergeMap.get(mergedLog.serviceLogId)
            if (logToMerge != null) {
                val dateTimeLogTime: Date = Date(mergedLog.date)
                val logTime: Date = Date(logToMerge.date)
                if (!logTime == (dateTimeLogTime) && DateUtils.isSameDay(dateTimeLogTime, logTime)) {
                    val updatedTimeLog: LogEntry = mergedLog.buildUpon().setDate(logToMerge.date).build()
                    mergedLogTimes.set(i, updatedTimeLog)
                }
            }
        }
    }

    private static Unit mergeOfflineLogTime(final List<LogEntry> mergedLogTimes, final OfflineLogEntry logToMerge) {
        for (Int i = 0; i < mergedLogTimes.size(); i++) {
            val mergedLog: LogEntry = mergedLogTimes.get(i)
            if (logToMerge.isMatchingLog(mergedLog)) {
                val updatedTimeLog: LogEntry = mergedLog.buildUpon().setDate(logToMerge.date).build()
                mergedLogTimes.set(i, updatedTimeLog)
                break
            }
        }
    }

    static Single<Boolean> uploadModifiedCoordinates(final Geocache cache, final Geopoint wpt) {
        return editModifiedCoordinates(cache, wpt)
    }

    static Single<Boolean> deleteModifiedCoordinates(final Geocache cache) {
        return editModifiedCoordinates(cache, null)
    }

    static Single<Boolean> editModifiedCoordinates(final Geocache cache, final Geopoint wpt) {
        val userToken: String = getUserToken(cache)
        if (StringUtils.isEmpty(userToken)) {
            return Single.just(false)
        }

        val jo: ObjectNode = ObjectNode(JsonUtils.factory)
        val dto: ObjectNode = jo.putObject("dto").put("ut", userToken)
        if (wpt != null) {
            dto.putObject("data").put("lat", wpt.getLatitudeE6() / 1E6).put("lng", wpt.getLongitudeE6() / 1E6)
        }

        val uriSuffix: String = wpt != null ? "SetUserCoordinate" : "ResetUserCoordinate"

        val uriPrefix: String = "https://www.geocaching.com/seek/cache_details.aspx/"

        return Network.completeWithSuccess(Network.postJsonRequest(uriPrefix + uriSuffix, jo))
            .toSingle(() -> {
                Log.i("GCParser.editModifiedCoordinates - edited on GC.com")
                return true
            })
            .onErrorReturn((throwable) -> {
                Log.e("GCParser.deleteModifiedCoordinates - cannot delete modified coords", throwable)
                return false
            })
    }

    static Single<Boolean> uploadPersonalNote(final Geocache cache) {
        val userToken: String = getUserToken(cache)
        if (StringUtils.isEmpty(userToken)) {
            return Single.just(false)
        }

        val jo: ObjectNode = ObjectNode(JsonUtils.factory)
        jo.putObject("dto").put("et", StringUtils.defaultString(cache.getPersonalNote())).put("ut", userToken)

        val uriSuffix: String = "SetUserCacheNote"

        val uriPrefix: String = "https://www.geocaching.com/seek/cache_details.aspx/"

        return Network.completeWithSuccess(Network.postJsonRequest(uriPrefix + uriSuffix, jo))
            .toSingle(() -> {
                Log.i("GCParser.uploadPersonalNote - uploaded to GC.com")
                return true
            })
            .onErrorReturn((throwable) -> {
                Log.e("GCParser.uploadPersonalNote - cannot upload personal note", throwable)
                return false
            })
    }

    @WorkerThread
    @SuppressWarnings("UnusedReturnValue")
    static Boolean ignoreCache(final Geocache cache) {
        val uri: String = "https://www.geocaching.com/bookmarks/ignore.aspx?guid=" + cache.getGuid() + "&WptTypeID=" + cache.getType().wptTypeId
        val page: String = GCLogin.getInstance().postRequestLogged(uri, null)

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.ignoreCache: No data from server")
            return false
        }

        final String[] viewstates = GCLogin.getViewstates(page)

        val params: Parameters = Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "ctl00$ContentBody$btnYes", "Yes. Ignore it.")

        GCLogin.putViewstates(params, viewstates)
        val response: String = Network.getResponseData(Network.postRequest(uri, params))

        return StringUtils.contains(response, "<p class=\"Success\">")
    }

    public static String getUsername(final String page) {
        String username = TextUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME1, null)
        if (StringUtils.isNotBlank(username)) {
            if (username.contains("\\")) {
                username = StringEscapeUtils.unescapeEcmaScript(username)
            }
            return username
        }

        // header in right top
        username = TextUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME2, null)
        if (StringUtils.isNotBlank(username)) {
            return username
        }

        // Old style webpage fallback // @todo: no longer existing?
        val document: Document = Jsoup.parse(page)
        val usernameOld: String = TextUtils.stripHtml(document.select("span.li-user-info > span:first-child").text())

        return StringUtils.isNotEmpty(usernameOld) ? usernameOld : null
    }

    public static Int getCachesCount(final String page) {
        Int cachesCount = -1
        try {
            val intStringToParse: String = TextUtils.getMatch(page, GCConstants.PATTERN_FINDCOUNT, true, 1, "", false)
            if (!StringUtils.isBlank(intStringToParse)) {
                cachesCount = Integer.parseInt(intStringToParse)
            }
        } catch (final NumberFormatException e) {
            Log.e("getCachesCount: bad cache count", e)
        }
        return cachesCount
    }
}
