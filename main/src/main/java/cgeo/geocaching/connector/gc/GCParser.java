package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.GCVoteRating;
import cgeo.geocaching.location.DistanceUnit;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import cgeo.geocaching.utils.TextUtils;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.text.HtmlCompat;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import static org.apache.commons.lang3.StringUtils.INDEX_NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.substring;

public final class GCParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @NonNull
    private static final SynchronizedDateFormat DATE_TB_IN_1 = new SynchronizedDateFormat("EEEEE, dd MMMMM yyyy", Locale.ENGLISH); // Saturday, 28 March 2009

    @NonNull
    private static final SynchronizedDateFormat DATE_TB_IN_2 = new SynchronizedDateFormat("EEEEE, MMMMM dd, yyyy", Locale.ENGLISH); // Saturday, March 28, 2009

    @NonNull
    private static final SynchronizedDateFormat DATE_JSON = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC"), Locale.US); // 2009-03-28T18:30:31.497Z
    private static final SynchronizedDateFormat DATE_JSON_SHORT = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"), Locale.US); // 2009-03-28T18:30:31Z

    @NonNull
    private static final ImmutablePair<StatusCode, Geocache> UNKNOWN_PARSE_ERROR = ImmutablePair.of(StatusCode.UNKNOWN_ERROR, null);

    private static final String HEADER_VERIFICATION_TOKEN = "X-Verification-Token";

    private GCParser() {
        // Utility class
    }

    private static SearchResult parseMap(final IConnector con, final String url, final String pageContent, final int alreadyTaken) throws JsonProcessingException {
        if (StringUtils.isBlank(pageContent)) {
            Log.e("GCParser.parseSearch: No page given");
            return null;
        }

        final List<Geocache> caches = new ArrayList<>();

        final JsonNode json = JsonUtils.reader.readTree(pageContent);
        final JsonNode features = json.get("data").get("layer").get("features");
        for (int i = 0; i < features.size(); i++) {
            final JsonNode properties = features.get(i).get("properties");
            final Geocache cache = new Geocache();
            cache.setName(properties.get("name").asText());
            cache.setGeocode(properties.get("key").asText());
            cache.setType(CacheType.getByWaypointType(properties.get("wptid").asText()));
            cache.setArchived(properties.get("archived").asBoolean());
            cache.setDisabled(!properties.get("available").asBoolean());
            cache.setCoords(new Geopoint(properties.get("lat").asDouble(), properties.get("lng").asDouble()));
            final String icon = properties.get("icon").asText();
            if ("MyHide".equals(icon)) {
                cache.setOwnerUserId(Settings.getUserName());
            } else if ("MyFind".equals(icon)) {
                cache.setFound(true);
            } else if (icon.startsWith("solved")) {
                cache.setUserModifiedCoords(true);
            }
            caches.add(cache);
        }

        final SearchResult searchResult = new SearchResult();
        searchResult.setUrl(con, url);
        searchResult.addAndPutInCache(caches);
        searchResult.setToContext(con, b -> b.putInt(GCConnector.SEARCH_CONTEXT_TOOK_TOTAL, alreadyTaken + caches.size()));

        return searchResult;
    }

    @Nullable
    @WorkerThread
    static SearchResult parseCache(final IConnector con, final String page, final DisposableHandler handler) {
        final ImmutablePair<StatusCode, Geocache> parsed = parseCacheFromText(page, handler);
        // attention: parseCacheFromText already stores implicitly through searchResult.addCache
        if (parsed.left != StatusCode.NO_ERROR) {
            final SearchResult result = new SearchResult(con, parsed.left);

            if (parsed.left == StatusCode.PREMIUM_ONLY) {
                result.addAndPutInCache(Collections.singletonList(parsed.right));
            }

            return result;
        }

        final Geocache cache = parsed.right;
        getExtraOnlineInfo(cache, page, handler);
        // too late: it is already stored through parseCacheFromText
        cache.setDetailedUpdatedNow();
        if (DisposableHandler.isDisposed(handler)) {
            return null;
        }

        // save full detailed caches
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_cache);
        DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));

        // update progress message so user knows we're still working. This is more of a place holder than
        // actual indication of what the program is doing
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_render);
        return new SearchResult(cache);
    }

    //method is only used in AndroidTests
    @NonNull
    static SearchResult testParseAndSaveCacheFromText(final IConnector con, @Nullable final String page, @Nullable final DisposableHandler handler) {
        final ImmutablePair<StatusCode, Geocache> parsed = parseCacheFromText(page, handler);
        final SearchResult result = new SearchResult(con, parsed.left);
        if (parsed.left == StatusCode.NO_ERROR) {
            result.addAndPutInCache(Collections.singletonList(parsed.right));
            DataStore.saveLogs(parsed.right.getGeocode(), getLogs(parseUserToken(page), Logs.ALL).blockingIterable(), true);
        }
        return result;
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
    @NonNull
    private static ImmutablePair<StatusCode, Geocache> parseCacheFromText(@Nullable final String pageIn, @Nullable final DisposableHandler handler) {
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_details);

        if (StringUtils.isBlank(pageIn)) {
            Log.e("GCParser.parseCache: No page given");
            return UNKNOWN_PARSE_ERROR;
        }

        if (StringUtils.contains(pageIn, GCConstants.STRING_404_FILE_NOT_FOUND)) {
            return ImmutablePair.of(StatusCode.CACHE_NOT_FOUND, null);
        }

        if (pageIn.contains(GCConstants.STRING_UNPUBLISHED_OTHER) || pageIn.contains(GCConstants.STRING_UNPUBLISHED_FROM_SEARCH)) {
            return ImmutablePair.of(StatusCode.UNPUBLISHED_CACHE, null);
        }

        if (pageIn.contains(GCConstants.STRING_PREMIUMONLY)) {
            final Geocache cache = new Geocache();
            cache.setPremiumMembersOnly(true);
            final MatcherWrapper matcher = new MatcherWrapper(GCConstants.PATTERN_PREMIUMONLY_CACHETYPE, pageIn);
            if (matcher.find()) {
                cache.setType(CacheType.getByWaypointType(matcher.group(1)));
                if (Objects.equals(matcher.group(2), "disabled")) {
                    cache.setDisabled(true);
                }
            }
            cache.setName(TextUtils.stripHtml(TextUtils.getMatch(pageIn, GCConstants.PATTERN_PREMIUMONLY_CACHENAME, true, "")));
            cache.setGeocode(TextUtils.getMatch(pageIn, GCConstants.PATTERN_PREMIUMONLY_GEOCODE, true, ""));
            cache.setDifficulty(Float.parseFloat(TextUtils.getMatch(pageIn, GCConstants.PATTERN_PREMIUMONLY_DIFFICULTY, true, "0")));
            cache.setTerrain(Float.parseFloat(TextUtils.getMatch(pageIn, GCConstants.PATTERN_PREMIUMONLY_TERRAIN, true, "0")));
            cache.setSize(CacheSize.getById(TextUtils.getMatch(pageIn, GCConstants.PATTERN_PREMIUMONLY_SIZE, true, CacheSize.NOT_CHOSEN.id)));
            return ImmutablePair.of(StatusCode.PREMIUM_ONLY, cache);
        }

        final String cacheName = TextUtils.stripHtml(TextUtils.getMatch(pageIn, GCConstants.PATTERN_NAME, true, ""));
        if (GCConstants.STRING_UNKNOWN_ERROR.equalsIgnoreCase(cacheName)) {
            return UNKNOWN_PARSE_ERROR;
        }

        // first handle the content with line breaks, then trim everything for easier matching and reduced memory consumption in parsed fields
        String personalNoteWithLineBreaks = "";
        final MatcherWrapper matcher = new MatcherWrapper(GCConstants.PATTERN_PERSONALNOTE, pageIn);
        if (matcher.find()) {
            personalNoteWithLineBreaks = matcher.group(1).trim();
        }

        //gc.com sends personal note with HTML-encoded entities. Replace those
        personalNoteWithLineBreaks = StringEscapeUtils.unescapeHtml4(personalNoteWithLineBreaks);

        final String page = TextUtils.replaceWhitespace(pageIn);

        final Geocache cache = new Geocache();
        cache.setDisabled(page.contains(GCConstants.STRING_STATUS_DISABLED));
        cache.setArchived(page.contains(GCConstants.STRING_STATUS_ARCHIVED)
                || page.contains(GCConstants.STRING_STATUS_LOCKED));

        cache.setPremiumMembersOnly(TextUtils.matches(page, GCConstants.PATTERN_PREMIUMMEMBERS));

        cache.setFavorite(TextUtils.matches(page, GCConstants.PATTERN_IS_FAVORITE));

        // cache geocode
        cache.setGeocode(TextUtils.getMatch(page, GCConstants.PATTERN_GEOCODE, true, cache.getGeocode()));

        // cache id
        cache.setCacheId(String.valueOf(GCUtils.gcLikeCodeToGcLikeId(cache.getGeocode())));

        // cache guid
        cache.setGuid(TextUtils.getMatch(page, GCConstants.PATTERN_GUID, true, cache.getGuid()));

        // cache watchlistcount
        cache.setWatchlistCount(getWatchListCount(page));

        // name
        cache.setName(cacheName);

        // owner real name
        cache.setOwnerUserId(Network.decode(TextUtils.getMatch(page, GCConstants.PATTERN_OWNER_USERID, true, cache.getOwnerUserId())));

        cache.setUserModifiedCoords(false);

        String tableInside = page;

        final int pos = tableInside.indexOf(GCConstants.STRING_CACHEDETAILS);
        if (pos == -1) {
            Log.e("GCParser.parseCache: ID \"cacheDetails\" not found on page");
            return UNKNOWN_PARSE_ERROR;
        }

        tableInside = tableInside.substring(pos);

        if (StringUtils.isNotBlank(tableInside)) {
            // cache terrain
            String result = TextUtils.getMatch(tableInside, GCConstants.PATTERN_TERRAIN, true, null);
            if (result != null) {
                try {
                    cache.setTerrain(Float.parseFloat(StringUtils.replaceChars(result, '_', '.')));
                } catch (final NumberFormatException e) {
                    Log.e("Error parsing terrain value", e);
                }
            }

            // cache difficulty
            result = TextUtils.getMatch(tableInside, GCConstants.PATTERN_DIFFICULTY, true, null);
            if (result != null) {
                try {
                    cache.setDifficulty(Float.parseFloat(StringUtils.replaceChars(result, '_', '.')));
                } catch (final NumberFormatException e) {
                    Log.e("Error parsing difficulty value", e);
                }
            }

            // owner
            cache.setOwnerDisplayName(StringEscapeUtils.unescapeHtml4(TextUtils.getMatch(tableInside, GCConstants.PATTERN_OWNER_DISPLAYNAME, true, cache.getOwnerDisplayName())));
            cache.setOwnerGuid(TextUtils.getMatch(tableInside, GCConstants.PATTERN_OWNER_GUID, true, 2, cache.getOwnerGuid(), false));

            // hidden
            try {
                String hiddenString = TextUtils.getMatch(tableInside, GCConstants.PATTERN_HIDDEN, true, null);
                if (StringUtils.isNotBlank(hiddenString)) {
                    cache.setHidden(GCLogin.parseGcCustomDate(hiddenString));
                }
                if (cache.getHiddenDate() == null) {
                    // event date
                    hiddenString = TextUtils.getMatch(tableInside, GCConstants.PATTERN_HIDDENEVENT, true, null);
                    if (StringUtils.isNotBlank(hiddenString)) {
                        cache.setHidden(GCLogin.parseGcCustomDate(hiddenString));
                    }
                }
            } catch (final ParseException e) {
                // failed to parse cache hidden date
                Log.w("GCParser.parseCache: Failed to parse cache hidden (event) date", e);
            }

            // favorite
            try {
                cache.setFavoritePoints(Integer.parseInt(TextUtils.getMatch(tableInside, GCConstants.PATTERN_FAVORITECOUNT, true, "0")));
            } catch (final NumberFormatException e) {
                Log.e("Error parsing favorite count", e);
            }

            // cache size
            cache.setSize(CacheSize.getById(TextUtils.getMatch(tableInside, GCConstants.PATTERN_SIZE, true, CacheSize.NOT_CHOSEN.id)));
        }

        // cache found / DNF
        cache.setFound(TextUtils.matches(page, GCConstants.PATTERN_FOUND));
        cache.setDNF(TextUtils.matches(page, GCConstants.PATTERN_DNF));

        // cache type
        cache.setType(CacheType.getByWaypointType(TextUtils.getMatch(page, GCConstants.PATTERN_TYPE, true, cache.getType().id)));

        // on watchlist
        cache.setOnWatchlist(TextUtils.matches(page, GCConstants.PATTERN_WATCHLIST));

        // latitude and longitude. Can only be retrieved if user is logged in
        String latlon = TextUtils.getMatch(page, GCConstants.PATTERN_LATLON, true, "");
        if (StringUtils.isNotEmpty(latlon)) {
            try {
                cache.setCoords(new Geopoint(latlon));
            } catch (final Geopoint.GeopointException e) {
                Log.w("GCParser.parseCache: Failed to parse cache coordinates", e);
            }
        }

        // cache location
        cache.setLocation(TextUtils.getMatch(page, GCConstants.PATTERN_LOCATION, true, ""));

        // cache hint
        final String result = TextUtils.getMatch(page, GCConstants.PATTERN_HINT, false, null);
        if (result != null) {
            // replace linebreak and paragraph tags
            final String hint = GCConstants.PATTERN_LINEBREAK.matcher(result).replaceAll("\n");
            cache.setHint(StringUtils.replace(hint, "</p>", "").trim());
        }

        cache.checkFields();

        // cache personal note
        cache.setPersonalNote(personalNoteWithLineBreaks, true);

        // cache short description
        final StringBuilder sDesc = new StringBuilder();
        if (cache.isEventCache()) {
            try {
                // add event start / end info to beginning of listing
                final MatcherWrapper eventTimesMatcher = new MatcherWrapper(GCConstants.PATTERN_EVENTTIMES, tableInside);
                if (eventTimesMatcher.find()) {
                    sDesc.append("<b>")
                            .append(new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(cache.getHiddenDate()))
                            .append(", ")
                            .append(Formatter.formatGCEventTime(tableInside))
                            .append("</b>");
                }
            } catch (Exception e) {
                Log.w("GCParser.parseCache: Failed to parse event time", e);
            }
        } else {
            sDesc.append(TextUtils.getMatch(page, GCConstants.PATTERN_SHORTDESC, true, ""));
        }
        cache.setShortDescription(sDesc.toString());

        // cache description
        final String longDescription = TextUtils.getMatch(page, GCConstants.PATTERN_DESC, true, "");
        String relatedWebPage = TextUtils.getMatch(page, GCConstants.PATTERN_RELATED_WEB_PAGE, true, "");
        if (StringUtils.isNotEmpty(relatedWebPage)) {
            relatedWebPage = String.format("<br/><br/><a href=\"%s\"><b>%s</b></a>", relatedWebPage, relatedWebPage);
        }
        String galleryImageLink = StringUtils.EMPTY;
        final int galleryImages = getGalleryCount(page);
        if (galleryImages > 0) {
            galleryImageLink = String.format("<br/><br/><a href=\"%s\"><b>%s</b></a>",
                "https://www.geocaching.com/seek/gallery.aspx?guid=" + cache.getGuid(),
                CgeoApplication.getInstance().getString(R.string.link_gallery, galleryImages));
        }
        Log.d("Gallery image link: " + galleryImageLink);
        String gcChecker = StringUtils.EMPTY;
        if (page.contains(GCConstants.PATTERN_GC_CHECKER)) {
            gcChecker = "<!--" + CgeoApplication.getInstance().getString(R.string.link_gc_checker) + "-->";
        }
        cache.setDescription(longDescription + relatedWebPage + gcChecker);

        // cache attributes
        try {
            final List<String> attributes = new ArrayList<>();
            final String attributesPre = TextUtils.getMatch(page, GCConstants.PATTERN_ATTRIBUTES, true, null);
            if (attributesPre != null) {
                final MatcherWrapper matcherAttributesInside = new MatcherWrapper(GCConstants.PATTERN_ATTRIBUTESINSIDE, attributesPre);

                while (matcherAttributesInside.find()) {
                    if (!matcherAttributesInside.group(2).equalsIgnoreCase("blank")) {
                        // by default, use the tooltip of the attribute
                        String attribute = matcherAttributesInside.group(2).toLowerCase(Locale.US);

                        // if the image name can be recognized, use the image name as attribute
                        final String imageName = matcherAttributesInside.group(1).trim();
                        if (StringUtils.isNotEmpty(imageName)) {
                            final int start = imageName.lastIndexOf('/');
                            final int end = imageName.lastIndexOf('.');
                            if (start >= 0 && end >= 0) {
                                attribute = imageName.substring(start + 1, end).replace('-', '_').toLowerCase(Locale.US);
                            }
                        }
                        attributes.add(attribute);
                    }
                }
            }
            cache.setAttributes(attributes);
        } catch (final RuntimeException e) {
            // failed to parse cache attributes
            Log.w("GCParser.parseCache: Failed to parse cache attributes", e);
        }

        // cache spoilers
        final List<Image> cacheSpoilers = new ArrayList<>();
        try {
            if (DisposableHandler.isDisposed(handler)) {
                return UNKNOWN_PARSE_ERROR;
            }
            DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_spoilers);

            cacheSpoilers.addAll(parseSpoiler(page));

        } catch (final RuntimeException e) {
            // failed to parse cache spoilers
            Log.w("GCParser.parseCache: Failed to parse cache spoilers", e);
        }

        // background image, to be added only if the image is not already present in the cache listing
        final MatcherWrapper matcherBackgroundImage = new MatcherWrapper(GCConstants.PATTERN_BACKGROUND_IMAGE, page);
        if (matcherBackgroundImage.find()) {
            final String url = matcherBackgroundImage.group(1);
            boolean present = false;
            for (final Image image : cacheSpoilers) {
                if (StringUtils.equals(image.getUrl(), url)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                cacheSpoilers.add(new Image.Builder().setUrl(url)
                    .setTitle(LocalizationUtils.getString(R.string.image_listing_background))
                    .setDescription(LocalizationUtils.getString(R.string.cache_image_background)).build());
            }
        }
        cache.setSpoilers(cacheSpoilers);

        // cache inventory
        final List<Trackable> inventory = parseInventory(page);
        if (inventory != null) {
            cache.mergeInventory(inventory, EnumSet.of(TrackableBrand.TRAVELBUG));
        }

        // cache logs counts
        try {
            final String countlogs = TextUtils.getMatch(page, GCConstants.PATTERN_COUNTLOGS, true, null);
            if (countlogs != null) {
                final MatcherWrapper matcherLog = new MatcherWrapper(GCConstants.PATTERN_COUNTLOG, countlogs);

                while (matcherLog.find()) {
                    final String typeStr = matcherLog.group(1);
                    final String countStr = getNumberString(matcherLog.group(2));

                    if (StringUtils.isNotBlank(typeStr)
                            && LogType.getByIconName(typeStr) != LogType.UNKNOWN
                            && StringUtils.isNotBlank(countStr)) {
                        cache.getLogCounts().put(LogType.getByIconName(typeStr), Integer.valueOf(countStr));
                    }
                }
            }
            if (cache.getLogCounts().isEmpty()) {
                Log.w("GCParser.parseCache: Failed to parse cache log count");
            }
        } catch (final NumberFormatException e) {
            // failed to parse logs
            Log.w("GCParser.parseCache: Failed to parse cache log count", e);
        }

        // waypoints - reset collection
        cache.setWaypoints(Collections.emptyList(), false);

        // add waypoint for original coordinates in case of user-modified listing-coordinates
        try {
            final String originalCoords = TextUtils.getMatch(page, GCConstants.PATTERN_LATLON_ORIG, false, null);

            if (originalCoords != null) {
                cache.createOriginalWaypoint(new Geopoint(originalCoords));
            }
        } catch (final Geopoint.GeopointException ignored) {
        }

        int wpBegin = page.indexOf("id=\"ctl00_ContentBody_Waypoints\">");
        if (wpBegin != -1) { // parse waypoints
            if (DisposableHandler.isDisposed(handler)) {
                return UNKNOWN_PARSE_ERROR;
            }
            DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_waypoints);

            String wpList = page.substring(wpBegin);

            int wpEnd = wpList.indexOf("</p>");
            if (wpEnd > -1 && wpEnd <= wpList.length()) {
                wpList = wpList.substring(0, wpEnd);
            }

            if (!wpList.contains("No additional waypoints to display.")) {
                wpEnd = wpList.indexOf("</table>");
                wpList = wpList.substring(0, wpEnd);

                wpBegin = wpList.indexOf("<tbody>");
                wpEnd = wpList.indexOf("</tbody>");
                if (wpBegin >= 0 && wpEnd >= 0 && wpEnd <= wpList.length()) {
                    wpList = wpList.substring(wpBegin + 7, wpEnd);
                }

                final String[] wpItems = StringUtils.splitByWholeSeparator(wpList, "<tr");

                for (int j = 1; j < wpItems.length; j += 2) {
                    final String[] wp = StringUtils.splitByWholeSeparator(wpItems[j], "<td");
                    assert wp != null;
                    if (wp.length < 7) {
                        Log.e("GCParser.cacheParseFromText: not enough waypoint columns in table");
                        continue;
                    }

                    // waypoint name
                    // res is null during the unit tests
                    final String name = TextUtils.getMatch(wp[5], GCConstants.PATTERN_WPNAME, true, 1, CgeoApplication.getInstance().getString(R.string.waypoint), true);

                    // waypoint type
                    final String resulttype = TextUtils.getMatch(wp[2], GCConstants.PATTERN_WPTYPE, null);

                    final Waypoint waypoint = new Waypoint(name, WaypointType.findById(resulttype), false);

                    // waypoint prefix
                    waypoint.setPrefix(TextUtils.getMatch(wp[3], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, true, 2, waypoint.getPrefix(), false));

                    // waypoint lookup
                    waypoint.setLookup(TextUtils.getMatch(wp[4], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, true, 2, waypoint.getLookup(), false));

                    // waypoint latitude and longitude
                    latlon = TextUtils.stripHtml(TextUtils.getMatch(wp[6], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, false, 2, "", false)).trim();
                    if (!StringUtils.startsWith(latlon, "???")) {
                        waypoint.setCoords(new Geopoint(latlon));
                    } else {
                        waypoint.setOriginalCoordsEmpty(true);
                    }

                    if (j + 1 < wpItems.length) {
                        final String[] wpNote = StringUtils.splitByWholeSeparator(wpItems[j + 1], "<td");
                        assert wpNote != null;
                        if (wpNote.length < 4) {
                            Log.d("GCParser.cacheParseFromText: not enough waypoint columns in table to extract note");
                            continue;
                        }

                        // waypoint note, cleanup via Jsoup
                        final String noteText = TextUtils.getMatch(wpNote[3], GCConstants.PATTERN_WPNOTE, waypoint.getNote());
                        if (StringUtils.isNotBlank(noteText)) {
                            final Document document = Jsoup.parse(noteText);
                            waypoint.setNote(document.outerHtml());
                        } else {
                            waypoint.setNote(StringUtils.EMPTY);
                        }
                    }

                    cache.addOrChangeWaypoint(waypoint, false);
                }
            }
        }

        // last check for necessary cache conditions
        if (StringUtils.isBlank(cache.getGeocode())) {
            return UNKNOWN_PARSE_ERROR;
        }

        cache.setDetailedUpdatedNow();
        return ImmutablePair.of(StatusCode.NO_ERROR, cache);
    }

    public static List<Image> parseSpoiler(final String html) {
        final List<Image> cacheSpoilers = new ArrayList<>();
        final MatcherWrapper matcherSpoilersInside = new MatcherWrapper(GCConstants.PATTERN_SPOILER_IMAGE, html);
        while (matcherSpoilersInside.find()) {
            final String url = matcherSpoilersInside.group(1);

            String title = null;
            if (matcherSpoilersInside.group(2) != null) {
                title = matcherSpoilersInside.group(2);
            } else {
                title = LocalizationUtils.getString(R.string.image_listing_spoiler);
            }
            String description = LocalizationUtils.getString(R.string.image_listing_spoiler);
            if (matcherSpoilersInside.group(3) != null) {
                description += ": " + matcherSpoilersInside.group(3);
            }
            cacheSpoilers.add(new Image.Builder().setUrl(url).setTitle(title).setDescription(description).build());
        }
        return cacheSpoilers;
    }

    @Nullable
    public static List<Trackable> parseInventory(final String page) {
        try {
            final MatcherWrapper matcherInventory = new MatcherWrapper(GCConstants.PATTERN_INVENTORY, page);
            if (matcherInventory.find()) {
                final String inventoryPre = matcherInventory.group();

                final ArrayList<Trackable> inventory = new ArrayList<>();
                if (StringUtils.isNotBlank(inventoryPre)) {
                    final MatcherWrapper matcherInventoryInside = new MatcherWrapper(GCConstants.PATTERN_INVENTORYINSIDE, inventoryPre);

                    while (matcherInventoryInside.find()) {
                        final boolean isGeocode = "TB".equals(matcherInventoryInside.group(1));
                        final String tbId = matcherInventoryInside.group(2);
                        final Trackable inventoryItem = new Trackable();
                        inventoryItem.setGeocode(isGeocode ? tbId : null);
                        inventoryItem.setGuid(isGeocode ? null : tbId);
                        inventoryItem.forceSetBrand(TrackableBrand.TRAVELBUG);
                        inventoryItem.setName(matcherInventoryInside.group(3));

                        inventory.add(inventoryItem);
                    }
                }
                return inventory;
            }
        } catch (final RuntimeException e) {
            // failed to parse cache inventory
            Log.w("GCParser.parseCache: Failed to parse cache inventory (2)", e);
        }
        return null;
    }

    @Nullable
    private static String getNumberString(@Nullable final String numberWithPunctuation) {
        return StringUtils.replaceChars(numberWithPunctuation, ".,", "");
    }

    private static SearchResult searchByMap(final IConnector con, final Parameters params) {
        final String page = GCLogin.getInstance().getRequestLogged(GCConstants.URL_LIVE_MAP, params);

        if (StringUtils.isBlank(page)) {
            Log.w("GCParser.searchByMap: No data from server");
            return null;
        }

        final String sessionToken = TextUtils.getMatch(page, GCConstants.PATTERN_SESSIONTOKEN, "");
        if (StringUtils.isBlank(sessionToken)) {
            Log.w("GCParser.searchByMap: Failed to retrieve session token");
            return null;
        }

        params.add("st", sessionToken);

        final String pqJson = GCLogin.getInstance().getRequestLogged("https://tiles01.geocaching.com/map.pq", params);

        SearchResult searchResult;
        try {
            searchResult = parseMap(con, "https://tiles01.geocaching.com/map.pq" + "?" + params, pqJson, 0);
        } catch (JsonProcessingException e) {
            searchResult = null;
        }
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.w("GCParser.searchByAny: No cache parsed");
            return searchResult;
        }

        final SearchResult search = searchResult.putInCacheAndLoadRating();

        GCLogin.getInstance().getLoginStatus(page);

        return search;
    }

    public static SearchResult searchByPocketQuery(final IConnector con, final String shortGuid, final String pqHash) {
        if (StringUtils.isBlank(pqHash)) {
            Log.e("GCParser.searchByPocket: No guid name given");
            return null;
        }

        final Parameters params = new Parameters("pq", shortGuid, "hash", pqHash);
        return searchByMap(con, params);
    }

    @Nullable
    public static Trackable searchTrackable(@Nullable final String geocode, @Nullable final String guid, @Nullable final String id) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid) && StringUtils.isBlank(id)) {
            Log.w("GCParser.searchTrackable: No geocode nor guid nor id given");
            return null;
        }

        final Parameters params = new Parameters();
        if (StringUtils.isNotBlank(geocode)) {
            params.put("tracker", geocode);
        } else if (StringUtils.isNotBlank(guid)) {
            params.put("guid", guid);
        } else if (StringUtils.isNotBlank(id)) {
            params.put("id", id);
        }

        final String page = GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/track/details.aspx", params);

        if (StringUtils.isBlank(page)) {
            Log.w("GCParser.searchTrackable: No data from server");
            return null;
        }

        final Trackable trackable = parseTrackable(page, geocode);
        if (trackable == null) {
            Log.w("GCParser.searchTrackable: No trackable parsed");
            return null;
        }

        return trackable;
    }

    /**
     * Fetches a list of bookmark lists. Shouldn't be called on main thread!
     *
     * @return A non-null list (which might be empty) on success. Null on error.
     */
    @Nullable
    @WorkerThread
    public static List<GCList> searchBookmarkLists() {
        final Parameters params = new Parameters();
        params.add("skip", "0");
        params.add("take", "100");
        params.add("type", "bm");

        final String page = GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/api/proxy/web/v1/lists", params);
        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.searchBookmarkLists: No data from server");
            return null;
        }

        try {
            final JsonNode json = JsonUtils.reader.readTree(page).get("data");
            final List<GCList> list = new ArrayList<>();

            for (Iterator<JsonNode> it = json.elements(); it.hasNext(); ) {
                final JsonNode row = it.next();

                final String name = row.get("name").asText();
                final String guid = row.get("referenceCode").asText();
                final int count = row.get("count").asInt();
                Date date;
                final String lastUpdateUtc = row.get("lastUpdateUtc").asText();
                try {
                    date = DATE_JSON.parse(lastUpdateUtc);
                } catch (ParseException e) {
                    // if parsing with fractions of seconds failed, try short form
                    date = DATE_JSON_SHORT.parse(lastUpdateUtc);
                    Log.d("parsing bookmark list: fallback needed for '" + lastUpdateUtc + "'");
                }

                final GCList pocketQuery = new GCList(guid, name, count, true, date.getTime(), -1, true, null, null);
                list.add(pocketQuery);
            }

            return list;
        } catch (final Exception e) {
            Log.e("GCParser.searchBookmarkLists: error parsing html page", e);
            return null;
        }
    }

    /**
     * Creates a new bookmark list. Shouldn't be called on main thread!
     *
     * @return guid of the new list.
     */
    @Nullable
    @WorkerThread
    public static String createBookmarkList(final String name, final Geocache geocache) {
        final ObjectNode jo = new ObjectNode(JsonUtils.factory).put("name", name);
        jo.putObject("type").put("code", "bm");

        try {
            final Parameters headers = new Parameters(HEADER_VERIFICATION_TOKEN, getRequestVerificationToken(geocache));
            final String result = Network.getResponseData(Network.postJsonRequest("https://www.geocaching.com/api/proxy/web/v1/lists", headers, jo));

            if (StringUtils.isBlank(result)) {
                Log.e("GCParser.createBookmarkList: No response from server");
                return null;
            }

            final String guid = JsonUtils.reader.readTree(result).get("referenceCode").asText();

            if (StringUtils.isBlank(guid)) {
                Log.e("GCParser.createBookmarkList: Malformed result");
                return null;
            }

            return guid;

        } catch (final Exception ignored) {
            Log.e("GCParser.createBookmarkList: Error while creating new bookmark list");
            return null;
        }
    }

    /**
     * Creates a new bookmark list. Shouldn't be called on main thread!
     *
     * @return successful?
     */
    @NonNull
    public static Single<Boolean> addCachesToBookmarkList(final String listGuid, final List<Geocache> geocaches) {
        final ArrayNode arrayNode = JsonUtils.createArrayNode();

        for (Geocache geocache : geocaches) {
            if (ConnectorFactory.getConnector(geocache) instanceof GCConnector) {
                arrayNode.add(new ObjectNode(JsonUtils.factory).put("referenceCode", geocache.getGeocode()));
            }
        }

        Log.d(arrayNode.toString());
        final Parameters headers = new Parameters(HEADER_VERIFICATION_TOKEN, getRequestVerificationToken(geocaches.get(0)));

        return Network.completeWithSuccess(Network.putJsonRequest("https://www.geocaching.com/api/proxy/web/v1/lists/" + listGuid + "/geocaches", headers, arrayNode))
            .toSingle(() -> {
                Log.i("GCParser.addCachesToBookmarkList - caches uploaded to GC.com bookmark list");
                return true;
            })
            .onErrorReturn((throwable) -> {
                Log.e("GCParser.uploadPersonalNote - cannot upload caches to GC.com bookmark list", throwable);
                return false;
            });
    }

    /**
     * Fetches a list of pocket queries. Shouldn't be called on main thread!
     *
     * @return A non-null list (which might be empty) on success. Null on error.
     */
    @Nullable
    @WorkerThread
    public static List<GCList> searchPocketQueries() {
        final String page = GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/pocket/default.aspx", null);
        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.searchPocketQueryList: No data from server");
            return null;
        }

        try {
            final Document document = Jsoup.parse(page);
            final Map<String, GCList> downloadablePocketQueries = getDownloadablePocketQueries(document);
            final List<GCList> list = new ArrayList<>(downloadablePocketQueries.values());

            final Elements rows = document.select("#pqRepeater tr:has(td)");
            for (final Element row : rows) {
                if (row == rows.last()) {
                    break; // skip footer
                }
                final Element link = row.select("td:eq(3) > a").first();
                final Uri uri = Uri.parse(link.attr("href"));
                final String guid = uri.getQueryParameter("guid");
                final Uri mapUri = Uri.parse(row.select("td:eq(2) > a").get(1).attr("href"));
                final String shortGuid = mapUri.getQueryParameter("pq");
                final String pqHash = mapUri.getQueryParameter("hash");
                if (!downloadablePocketQueries.containsKey(guid)) {
                    final String name = link.attr("title");
                    final GCList pocketQuery = new GCList(guid, name, -1, false, 0, -1, false, shortGuid, pqHash);
                    list.add(pocketQuery);
                } else {
                    final GCList pq = downloadablePocketQueries.get(guid);
                    pq.setPqHash(pqHash);
                    pq.setShortGuid(shortGuid);
                }
            }
            return list;
        } catch (final Exception e) {
            Log.e("GCParser.searchPocketQueryList: error parsing html page", e);
            return null;
        }
    }

    /**
     * Reads the downloadable pocket queries from the uxOfflinePQTable
     *
     * @param document the page as Document
     * @return Map with downloadable PQs keyed by guid
     */
    @NonNull
    private static Map<String, GCList> getDownloadablePocketQueries(final Document document) throws Exception {
        final Map<String, GCList> downloadablePocketQueries = new HashMap<>();

        final Elements rows = document.select("#uxOfflinePQTable tr:has(td)");
        for (final Element row : rows) {
            if (row == rows.last()) {
                break; // skip footer
            }

            final Elements cells = row.select("td");
            if (cells.size() < 6) {
                Log.d("GCParser.getDownloadablePocketQueries: less than 6 table cells, looks like an empty table");
                continue;
            }
            final Element link = cells.get(2).select("a").first();
            if (link == null) {
                Log.w("GCParser.getDownloadablePocketQueries: Downloadlink not found");
                continue;
            }
            final String name = link.text();
            final String href = link.attr("href");
            final Uri uri = Uri.parse(href);
            final String guid = uri.getQueryParameter("g");

            final int count = Integer.parseInt(cells.get(4).text());

            final MatcherWrapper matcherLastGeneration = new MatcherWrapper(GCConstants.PATTERN_PQ_LAST_GEN, cells.get(5).text());
            long lastGeneration = 0;
            int daysRemaining = 0;
            if (matcherLastGeneration.find()) {
                final Date lastGenerationDate = GCLogin.parseGcCustomDate(matcherLastGeneration.group(1));
                if (lastGenerationDate != null) {
                    lastGeneration = lastGenerationDate.getTime();
                }

                final String daysRemainingString = matcherLastGeneration.group(3);
                if (daysRemainingString != null) {
                    daysRemaining = Integer.parseInt(daysRemainingString);
                }
            }

            final GCList pocketQuery = new GCList(guid, name, count, true, lastGeneration, daysRemaining, false, null, null);
            downloadablePocketQueries.put(guid, pocketQuery);
        }

        return downloadablePocketQueries;
    }

    /**
     * Adds the cache to the watchlist of the user.
     *
     * @param cache the cache to add
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    @NonNull
    static Single<Boolean> addToWatchlist(@NonNull final Geocache cache) {
        return addToOrRemoveFromWatchlist(cache, true);
    }

    /**
     * internal method to handle add to / remove from watchlist
     */
    @NonNull
    private static Single<Boolean> addToOrRemoveFromWatchlist(@NonNull final Geocache cache, final boolean doAdd) {

        final String logContext = "GCParser.addToOrRemoveFromWatchlist(cache = " + cache.getGeocode() + ", add = " + doAdd + ")";

        final ObjectNode jo = new ObjectNode(JsonUtils.factory).put("geocacheId", cache.getCacheId());
        final String uri = "https://www.geocaching.com/api/proxy/web/v1/watchlists/" + (doAdd ? "add" : "remove") + "?geocacheId=" + cache.getCacheId();

        final Single<Response> request = doAdd
            ? Network.postJsonRequest(uri, jo)
            : Network.deleteJsonRequest(uri, jo);
        return Network.completeWithSuccess(request)
            .toSingle(() -> {
                Log.i(logContext + ": success");
                return true;
            })
            .onErrorReturn((ex) -> {
                Log.e(logContext + ": error", ex);
                return false;
            })
            .map((successful) -> {
                if (successful) {
                    // Set cache properties
                    cache.setOnWatchlist(doAdd);
                    final String watchListPage = GCLogin.getInstance().postRequestLogged(cache.getLongUrl(), null);
                    cache.setWatchlistCount(getWatchListCount(watchListPage));
                }
                return successful;
            });
    }

    /**
     * This method extracts the amount of people watching on a geocache out of the HTMl website passed to it
     *
     * @param page Page containing the information about how many people watching on geocache
     * @return Number of people watching geocache, -1 when error
     */
    static int getWatchListCount(final String page) {
        return getCount(page, GCConstants.PATTERN_WATCHLIST_COUNT, 1);
    }

    static int getGalleryCount(final String page) {
        return getCount(page, GCConstants.PATTERN_GALLERY_COUNT, 1);
    }

    private static int getCount(final String page, final Pattern pattern, final int group) {

        final String sCount = TextUtils.getMatch(page, pattern, true, group, "notFound", false);
        if ("notFound".equals(sCount)) {
            return -1;
        }
        try {
            return Integer.parseInt(sCount);
        } catch (final NumberFormatException nfe) {
            Log.e("Could not parse", nfe);
            return -1;
        }
    }



    /**
     * Removes the cache from the watch list
     *
     * @param cache the cache to remove
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    @NonNull
    static Single<Boolean> removeFromWatchlist(@NonNull final Geocache cache) {
        return addToOrRemoveFromWatchlist(cache, false);
    }

    @Nullable
    static String requestHtmlPage(@Nullable final String geocode, @Nullable final String guid, final String log) {
        final Parameters params = new Parameters("decrypt", "y");
        if (StringUtils.isNotBlank(geocode)) {
            params.put("wp", geocode);
        } else if (StringUtils.isNotBlank(guid)) {
            params.put("guid", guid);
        }
        params.put("log", log);
        params.put("numlogs", "0");

        return GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/seek/cache_details.aspx", params);
    }

    /**
     * Adds the cache to the favorites of the user.
     * <br>
     * This must not be called from the UI thread.
     *
     * @param cache the cache to add
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    @NonNull
    static Single<Boolean> addToFavorites(@NonNull final Geocache cache) {
        return changeFavorite(cache, true);
    }

    @NonNull
    private static Single<Boolean> changeFavorite(@NonNull final Geocache cache, final boolean add) {
        final String userToken = getUserToken(cache);
        if (StringUtils.isEmpty(userToken)) {
            return Single.just(false);
        }

        final String uri = "https://www.geocaching.com/datastore/favorites.svc/update?u=" + userToken + "&f=" + add;

        return Network.completeWithSuccess(Network.postRequest(uri, null))
            .toSingle(() -> {
                Log.i("GCParser.changeFavorite: cache added/removed to/from favorites");
                return true;
            })
            .onErrorReturn((throwable) -> {
                Log.e("GCParser.changeFavorite: cache not added/removed to/from favorites", throwable);
                return false;
            })
            .map((successful) -> {
                if (successful) {
                    cache.setFavorite(add);
                    cache.setFavoritePoints(cache.getFavoritePoints() + (add ? 1 : -1));
                }
                return successful;
            });
    }

    private static String getUserToken(@NonNull final Geocache cache) {
        return getUserToken(cache.getGeocode());
    }

    private static String getUserToken(@NonNull final String geocode) {
        return parseUserToken(requestHtmlPage(geocode, null, "n"));
    }

    private static String parseUserToken(final String page) {
        return TextUtils.getMatch(page, GCConstants.PATTERN_USERTOKEN, "");
    }

    private static String getRequestVerificationToken(@NonNull final Geocache cache) {
        return parseRequestVerificationToken(requestHtmlPage(cache.getGeocode(), null, "n"));
    }

    private static String parseRequestVerificationToken(final String page) {
        return TextUtils.getMatch(page, GCConstants.PATTERN_REQUESTVERIFICATIONTOKEN, "");
    }

    /**
     * Removes the cache from the favorites.
     * <br>
     * This must not be called from the UI thread.
     *
     * @param cache the cache to remove
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    @NonNull
    static Single<Boolean> removeFromFavorites(@NonNull final Geocache cache) {
        return changeFavorite(cache, false);
    }

    /**
     * Parse a trackable HTML description into a Trackable object
     *
     * @param page the HTML page to parse, already processed through {@link TextUtils#replaceWhitespace}
     * @return the parsed trackable, or null if none could be parsed
     */
    static Trackable parseTrackable(final String page, final String possibleTrackingcode) {
        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.parseTrackable: No page given");
            return null;
        }

        if (page.contains(GCConstants.ERROR_TB_DOES_NOT_EXIST) || page.contains(GCConstants.ERROR_TB_ARITHMETIC_OVERFLOW) || page.contains(GCConstants.ERROR_TB_ELEMENT_EXCEPTION)) {
            return null;
        }

        final Trackable trackable = new Trackable();
        trackable.setGeocode(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GEOCODE, true, StringUtils.upperCase(possibleTrackingcode)));
        trackable.forceSetBrand(TrackableBrand.TRAVELBUG);
        if (trackable.getGeocode() == null) {
            Log.e("GCParser.parseTrackable: could not figure out trackable geocode");
            return null;
        }

        // trackable id
        trackable.setGuid(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GUID, true, trackable.getGuid()));

        // trackable icon
        final String iconUrl = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_ICON, true, trackable.getIconUrl());
        trackable.setIconUrl(iconUrl.startsWith("/") ? "https://www.geocaching.com" + iconUrl : iconUrl);

        // trackable name
        trackable.setName(TextUtils.stripHtml(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, true, "")));

        // trackable type
        if (StringUtils.isNotBlank(trackable.getName())) {
            // old TB pages include TB type as "alt" attribute
            String type = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_TYPE, true, trackable.getType());
            if (StringUtils.isNotBlank(type)) {
                type = TextUtils.stripHtml(type);
            } else {
                // try alternative way on pages formatted the newer style: <title>\n\t(TBxxxx) Type - Name\n</title>
                final String title = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_TYPE_TITLE, true, "");
                if (StringUtils.isNotBlank(title)) {
                    final String nameWithHTML = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, true, "");
                    final int pos = StringUtils.lastIndexOfIgnoreCase(title, nameWithHTML);
                    if (pos != INDEX_NOT_FOUND) {
                        type = substring(title, 0, pos - 3);
                        type = TextUtils.stripHtml(type);
                    }
                }
            }
            trackable.setType(type);
        }

        // trackable owner name
        try {
            final MatcherWrapper matcherOwner = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_OWNER, page);
            if (matcherOwner.find()) {
                trackable.setOwnerGuid(matcherOwner.group(2));
                trackable.setOwner(matcherOwner.group(3).trim());

            }
        } catch (final RuntimeException e) {
            // failed to parse trackable owner name
            Log.w("GCParser.parseTrackable: Failed to parse trackable owner name", e);
        }

        // trackable origin
        trackable.setOrigin(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_ORIGIN, true, trackable.getOrigin()));

        // trackable spotted
        try {
            final MatcherWrapper matcherSpottedCache = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_SPOTTEDCACHE_BY_GEOCODE, page);
            if (matcherSpottedCache.find()) {
                trackable.setSpottedCacheGeocode(matcherSpottedCache.group(2));
                trackable.setSpottedName(matcherSpottedCache.group(1).trim());
                trackable.setSpottedType(Trackable.SPOTTED_CACHE);
            } else {
                final MatcherWrapper matcherSpottedCacheGuid = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_SPOTTEDCACHE_BY_GUID, page);
                if (matcherSpottedCacheGuid.find()) {
                    trackable.setSpottedGuid(matcherSpottedCache.group(2));
                    trackable.setSpottedName(matcherSpottedCache.group(1).trim());
                    trackable.setSpottedType(Trackable.SPOTTED_CACHE);
                }
            }

            final MatcherWrapper matcherSpottedUser = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_SPOTTEDUSER, page);
            if (matcherSpottedUser.find()) {
                trackable.setSpottedGuid(matcherSpottedUser.group(3));
                trackable.setSpottedName(HtmlCompat.fromHtml(matcherSpottedUser.group(1), HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()); // remove HTML in parsed username, see #14442
                trackable.setSpottedType(Trackable.SPOTTED_USER);
            }

            if (TextUtils.matches(page, GCConstants.PATTERN_TRACKABLE_SPOTTEDUNKNOWN)) {
                trackable.setSpottedType(Trackable.SPOTTED_UNKNOWN);
            }

            if (TextUtils.matches(page, GCConstants.PATTERN_TRACKABLE_SPOTTEDOWNER)) {
                trackable.setSpottedType(Trackable.SPOTTED_OWNER);
            }
        } catch (final RuntimeException e) {
            // failed to parse trackable last known place
            Log.w("GCParser.parseTrackable: Failed to parse trackable last known place", e);
        }

        // released date - can be missing on the page
        final String releaseString = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_RELEASES, false, null);
        if (releaseString != null) {
            try {
                trackable.setReleased(DATE_TB_IN_1.parse(releaseString));
            } catch (final ParseException ignored) {
                if (trackable.getReleased() == null) {
                    try {
                        trackable.setReleased(DATE_TB_IN_2.parse(releaseString));
                    } catch (final ParseException e) {
                        Log.e("Could not parse trackable release " + releaseString, e);
                    }
                }
            }
        }

        // log - entire section can be missing on the page if trackable hasn't been found by the user
        try {
            final String logType = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_FOUND_LOG, false, null);
            if (logType != null) {
                trackable.setLogType(LogType.getByIconName(StringUtils.trim(logType)));
            }
            final MatcherWrapper retrievedMatcher = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_DISPOSITION_LOG, page);
            if (retrievedMatcher.find()) {
                trackable.setLogDate(GCLogin.parseGcCustomDate(StringUtils.trim(retrievedMatcher.group(2))));
                trackable.setLogGuid(StringUtils.trim(retrievedMatcher.group(1)));
            }
        } catch (final Exception e) {
            Log.e("GCParser.parseTrackable: Failed to parse log", e);
        }

        // trackable distance
        final MatcherWrapper distanceMatcher = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_DISTANCE, page);
        if (distanceMatcher.find()) {
            final DistanceUnit unit = DistanceUnit.findById(distanceMatcher.group(2),
                    Settings.useImperialUnits() ? DistanceUnit.MILE : DistanceUnit.KILOMETER);
            try {
                trackable.setDistance(unit.parseToKilometers(distanceMatcher.group(1)));
            } catch (final NumberFormatException e) {
                Log.e("GCParser.parseTrackable: Failed to parse distance", e);
            }
        }

        // trackable goal
        trackable.setGoal(convertLinks(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GOAL, true, trackable.getGoal())));

        // trackable details & image
        try {
            final MatcherWrapper matcherDetailsImage = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_DETAILSIMAGE, page);
            if (matcherDetailsImage.find()) {
                final String image = StringUtils.trim(matcherDetailsImage.group(3));
                final String details = StringUtils.trim(matcherDetailsImage.group(4));

                if (StringUtils.isNotEmpty(image)) {
                    trackable.setImage(StringUtils.replace(image, "/display/", "/large/"));
                }
                if (StringUtils.isNotEmpty(details) && !StringUtils.equals(details, "No additional details available.")) {
                    trackable.setDetails(convertLinks(details));
                }
            }
        } catch (final RuntimeException e) {
            // failed to parse trackable details & image
            Log.w("GCParser.parseTrackable: Failed to parse trackable details & image", e);
        }
        if (StringUtils.isEmpty(trackable.getDetails()) && page.contains(GCConstants.ERROR_TB_NOT_ACTIVATED)) {
            trackable.setDetails(CgeoApplication.getInstance().getString(R.string.trackable_not_activated));
        }

        // trackable may be locked (see e.g. TB673CE)
        if (new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_IS_LOCKED, page).find()) {
            trackable.setIsLocked();
        }

        // trackable logs
        try {
            final MatcherWrapper matcherLogsOuter = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_LOG_OUTER, page);
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
                final MatcherWrapper matcherLogs = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_LOG_INNER, matcherLogsOuter.group(0));
                while (matcherLogs.find()) {
                    long date = 0;
                    try {
                        date = GCLogin.parseGcCustomDate(matcherLogs.group(2)).getTime();
                    } catch (final ParseException ignored) {
                    }

                    final LogEntry.Builder logDoneBuilder = new LogEntry.Builder()
                            .setAuthor(TextUtils.stripHtml(matcherLogs.group(4)).trim())
                            .setAuthorGuid(matcherLogs.group(3))
                            .setDate(date)
                            .setLogType(LogType.getByIconName(matcherLogs.group(1)))
                            .setServiceLogId(matcherLogs.group(8))
                            .setLog(matcherLogs.group(9).trim());

                    if (matcherLogs.group(5) != null && matcherLogs.group(7) != null) {
                        logDoneBuilder.setCacheGeocode(matcherLogs.group(5));
                        logDoneBuilder.setCacheName(matcherLogs.group(7));
                    }

                    // Apply the pattern for images in a trackable log entry against each full log (group(0))
                    final String logEntry = matcherLogsOuter.group(0);
                    final MatcherWrapper matcherLogImages = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_LOG_IMAGES, logEntry);
                    /*
                     * 1. Image URL
                     * 2. Image title
                     */
                    while (matcherLogImages.find()) {
                        final Image logImage = new Image.Builder()
                                .setUrl(matcherLogImages.group(1))
                                .setTitle(matcherLogImages.group(2))
                                .setCategory(Image.ImageCategory.LOG)
                                .build();
                        logDoneBuilder.addLogImage(logImage);
                    }

                    trackable.getLogs().add(logDoneBuilder.build());
                }
            }
        } catch (final Exception e) {
            // failed to parse logs
            Log.w("GCParser.parseCache: Failed to parse cache logs", e);
        }

        // tracking code
        if (!StringUtils.equalsIgnoreCase(trackable.getGeocode(), possibleTrackingcode)) {
            trackable.setTrackingcode(possibleTrackingcode);
        }

        if (CgeoApplication.getInstance() != null) {
            DataStore.saveTrackable(trackable);
        }

        return trackable;
    }

    private static String convertLinks(final String input) {
        if (input == null) {
            return null;
        }
        return StringUtils.replace(input, "../", GCConstants.GC_URL);
    }

    public enum Logs {
        ALL(null),
        FRIENDS("sf"),
        OWN("sp");

        final String paramName;

        Logs(final String paramName) {
            this.paramName = paramName;
        }

        private String getParamName() {
            return paramName;
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
        return getLogs(userToken, logType, GCConstants.NUMBER_OF_LOGS);
    }

    private static Observable<LogEntry> getLogs(final String userToken, final Logs logType, final int take) {
        if (userToken.isEmpty()) {
            Log.e("GCParser.getLogs: unable to extract userToken");
            return Observable.empty();
        }

        return Observable.defer(() -> {
            final Parameters params = new Parameters(
                    "tkn", userToken,
                    "idx", "1",
                    "num", String.valueOf(take),
                    "decrypt", "false"); // fetch encrypted logs as such
            if (logType != Logs.ALL) {
                params.add(logType.getParamName(), Boolean.toString(Boolean.TRUE));
            }
            try {
                final InputStream responseStream =
                        Network.getResponseStream(Network.getRequest("https://www.geocaching.com/seek/geocache.logbook", params));
                if (responseStream == null) {
                    Log.w("getLogs: no logs were returned");
                    return Observable.empty();
                }
                return parseLogsAndClose(logType != Logs.ALL, responseStream);
            } catch (final Exception e) {
                Log.w("unable to read logs", e);
                return Observable.empty();
            }
        }).subscribeOn(AndroidRxUtils.networkScheduler);
    }

    private static Observable<LogEntry> parseLogsAndClose(final boolean markAsFriendsLog, @NonNull final InputStream responseStream) {
        return Observable.create(emitter -> {
            try {
                final ObjectNode resp = (ObjectNode) JsonUtils.reader.readTree(responseStream);
                if (!resp.path("status").asText().equals("success")) {
                    Log.w("GCParser.parseLogsAndClose: status is " + resp.path("status").asText("[absent]"));
                    emitter.onComplete();
                    return;
                }

                final ArrayNode data = (ArrayNode) resp.get("data");
                for (final JsonNode entry : data) {
                    final String logType = entry.path("LogType").asText();

                    final long date;
                    try {
                        date = GCLogin.parseGcCustomDate(entry.get("Visited").asText()).getTime();
                    } catch (ParseException | NullPointerException e) {
                        Log.e("Failed to parse log date", e);
                        continue;
                    }

                    // TODO: we should update our log data structure to be able to record
                    // proper coordinates, and make them clickable. In the meantime, it is
                    // better to integrate those coordinates into the text rather than not
                    // display them at all.
                    final String latLon = entry.path("LatLonString").asText();
                    final String logText = (StringUtils.isEmpty(latLon) ? "" : (latLon + "<br/><br/>")) + TextUtils.removeControlCharacters(entry.path("LogText").asText());
                    final String logCode = GCUtils.logIdToLogCode(entry.path("LogID").asLong());
                    final LogEntry.Builder logDoneBuilder = new LogEntry.Builder()
                            .setServiceLogId(logCode)
                            .setAuthor(TextUtils.removeControlCharacters(entry.path("UserName").asText()))
                            .setAuthorGuid(entry.path("AccountGuid").asText())
                            .setDate(date)
                            .setLogType(LogType.getByType(logType))
                            .setLog(logText)
                            .setFound(entry.path("GeocacheFindCount").asInt())
                            .setFriend(markAsFriendsLog);

                    final ArrayNode images = (ArrayNode) entry.get("Images");
                    for (final JsonNode image : images) {
                        final String imageGuid = image.path("ImageGuid").asText();
                        final String imageId = image.path("ImageID").asText();
                        final String url = "https://imgcdn.geocaching.com/cache/log/large/" + image.path("FileName").asText();
                        final String title = TextUtils.removeControlCharacters(image.path("Name").asText());
                        String description = image.path("Descr").asText();
                        if (StringUtils.contains(description, "Geocaching®") && description.length() < 60) {
                            description = null;
                        }
                        final Image logImage = new Image.Builder()
                            .setServiceImageId(GCLogAPI.getLogImageId(imageGuid, imageId))
                            .setUrl(url).setTitle(title).setDescription(description).build();
                        logDoneBuilder.addLogImage(logImage);
                    }

                    emitter.onNext(logDoneBuilder.build());
                }
            } catch (final IOException e) {
                Log.w("Failed to parse cache logs", e);
            } finally {
                IOUtils.closeQuietly(responseStream);
            }
            emitter.onComplete();
        });
    }

    /**
     * Javascript Object from the new Logpage: <a href="https://www.geocaching.com/play/geocache/gc.../log">...</a>
     * <pre>
     *     {"value":46}
     * </pre>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class AvailableLogType {
        @JsonProperty("value")
        int value;
    }

    @NonNull
    public static List<LogTypeTrackable> parseLogTypesTrackables(final String page) {
        final AvailableLogType[] availableTypes = parseLogTypes(page);
        if (availableTypes == null) {
            return Collections.emptyList();
        }
        return CollectionStream.of(availableTypes)
                .filter(a -> a.value > 0)
                .map(a -> LogTypeTrackable.getById(a.value))
                .toList();
    }

    @NonNull
    static List<LogType> parseTypes(final String page) {
            final AvailableLogType[] availableTypes = parseLogTypes(page);
            if (availableTypes == null) {
                return Collections.emptyList();
            }
            return CollectionStream.of(availableTypes)
                    .filter(a -> a.value > 0)
                    .map(a -> LogType.getById(a.value))
                    .filter(t -> t != LogType.UPDATE_COORDINATES)
                    .toList();
        }

    private static AvailableLogType[] parseLogTypes(final String page) {
        //"logTypes":[{"value":2},{"value":3},{"value":4},{"value":45},{"value":7}]
        if (StringUtils.isBlank(page)) {
            return new AvailableLogType[0];
        }

        final String match = TextUtils.getMatch(page, GCConstants.PATTERN_TYPE4, null);
        if (match == null) {
            return null;
        }
        try {
            return MAPPER.readValue("[" + match + "]", AvailableLogType[].class);
        } catch (final Exception e) {
            Log.e("Error parsing log types from [" + match + "]", e);
            return new AvailableLogType[0];
        }
    }

    @WorkerThread
    private static void getExtraOnlineInfo(@NonNull final Geocache cache, final String page, final DisposableHandler handler) {
        // This method starts the page parsing for logs in the background, as well as retrieve the friends and own logs
        // if requested. It merges them and stores them in the background, while the rating is retrieved if needed and
        // stored. Then we wait for the log merging and saving to be completed before returning.
        if (DisposableHandler.isDisposed(handler)) {
            return;
        }

        // merge log-entries (friend-logs and log-times)
        mergeAndStoreLogEntries(cache, page, handler);

        //add gallery images if wanted
        addImagesFromGallery(cache, handler);

        if (Settings.isRatingWanted() && !DisposableHandler.isDisposed(handler)) {
            DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_gcvote);
            final GCVoteRating rating = GCVote.getRating(cache.getGuid(), cache.getGeocode());
            if (rating != null) {
                cache.setRating(rating.getRating());
                cache.setVotes(rating.getVotes());
                cache.setMyVote(rating.getMyVote());
            }
        }
    }

    private static void mergeAndStoreLogEntries(@NonNull final Geocache cache, final String page, final DisposableHandler handler) {

        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_logs);

        final String userToken = parseUserToken(page);
        final Observable<LogEntry> logs = getLogs(userToken, Logs.ALL);
        final Observable<LogEntry> ownLogs = getLogs(userToken, Logs.OWN).cache();
        final Observable<LogEntry> friendLogs = Settings.isFriendLogsWanted() ?
                getLogs(userToken, Logs.FRIENDS).cache() : Observable.empty();

        final List<LogEntry> logsBlocked = logs.toList().blockingGet();
        final List<LogEntry> ownLogEntriesBlocked = ownLogs.toList().blockingGet();
        final List<LogEntry> friendLLogsBlocked = friendLogs.toList().blockingGet();
        final OfflineLogEntry offlineLog = DataStore.loadLogOffline(cache.getGeocode());

        List<LogEntry> ownLogsFromDb = Collections.emptyList();
        if (!ownLogEntriesBlocked.isEmpty()) {
            ownLogsFromDb = DataStore.loadLogsOfAuthor(cache.getGeocode(), GCConnector.getInstance().getUserName(), true);
            if (ownLogsFromDb.isEmpty()) {
                ownLogsFromDb = DataStore.loadLogsOfAuthor(cache.getGeocode(), GCConnector.getInstance().getUserName(), false);
            }
        }

        // merge time from offline log
        mergeOfflineLogTime(ownLogEntriesBlocked, offlineLog);

        // merge time from online-logs already stored in db (overrides possible offline log)
        mergeLogTimes(ownLogEntriesBlocked, ownLogsFromDb);

        if (cache.isFound() || cache.isDNF()) {
            for (final LogEntry logEntry : ownLogEntriesBlocked) {
                if (logEntry.logType.isFoundLog() || (!cache.isFound() && cache.isDNF() && logEntry.logType == LogType.DIDNT_FIND_IT)) {
                    cache.setVisitedDate(logEntry.date);
                    break;
                }
            }
        }

        final List<LogEntry> specialLogEntries = ListUtils.union(friendLLogsBlocked, ownLogEntriesBlocked);
        mergeFriendsLogs(logsBlocked, specialLogEntries);

        DataStore.saveLogs(cache.getGeocode(), logsBlocked, true);
    }

    private static void addImagesFromGallery(@NonNull final Geocache cache, final DisposableHandler handler) {
        if (StringUtils.isBlank(cache.getGuid()) || !Settings.isStoreLogImages() /* see #16778 */) {
            return;
        }
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_spoilers);
        //load page
        //https://www.geocaching.com/seek/gallery.aspx?guid=0e670e6a-4b38-45c7-97b7-fa5da0f367a2
        final String galleryFirstPage = GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/seek/gallery.aspx", new Parameters("guid", cache.getGuid()));
        if (galleryFirstPage == null) {
            return;
        }

        //get existing Image URls
        final Set<String> existingUrls = cache.getSpoilers().stream().map(Image::getUrl).collect(Collectors.toSet());
        //collect new Images
        final List<Image> newImages = parseGalleryImages(galleryFirstPage, url -> !existingUrls.contains(url));
        newImages.addAll(0, cache.getSpoilers());
        cache.setSpoilers(newImages);
    }

    public static List<Image> parseGalleryImages(final String html, final Predicate<String> take) {
        final List<Image> images = new ArrayList<>();
        final MatcherWrapper matcherImage = new MatcherWrapper(GCConstants.PATTERN_GALLERY_IMAGE, html);

        while (matcherImage.find()) {
            final String date = matcherImage.group(1);
            final String url = matcherImage.group(2);
            final String title = matcherImage.group(3);

            String description = LocalizationUtils.getString(R.string.image_listing_gallery);
            if (!StringUtils.isBlank(date)) {
                description += ": " + date;
            }

            if (!take.test(url)) {
                continue;
            }

            images.add(new Image.Builder().setCategory(Image.ImageCategory.LISTING)
                .setUrl(url)
                .setTitle(title)
                .setDescription(description).build());

        }
        return images;
    }

    @WorkerThread
    public static List<LogEntry> loadLogs(final String geocode, final Logs logs, final int take) {
        final String userToken = getUserToken(geocode);
        final Observable<LogEntry> logObservable = getLogs(userToken, logs, take);
        return logObservable.toList().blockingGet();
    }

    /**
     * Merge log entries and mark them as friends logs (personal and friends) to identify
     * them on friends/personal logs tab.
     *
     * @param mergedLogs  the list to merge logs with
     * @param logsToMerge the list of logs to merge
     */
    private static void mergeFriendsLogs(final List<LogEntry> mergedLogs, final Iterable<LogEntry> logsToMerge) {
        for (final LogEntry log : logsToMerge) {
            final int logIndex = mergedLogs.indexOf(log);
            if (logIndex < 0) {
                mergedLogs.add(log);
            } else {
                final LogEntry friendLog = mergedLogs.get(logIndex);
                final LogEntry updatedFriendLog = friendLog.buildUpon().setFriend(true).build();
                mergedLogs.set(logIndex, updatedFriendLog);
            }
        }
    }

    private static void mergeLogTimes(final List<LogEntry> mergedLogTimes, final Iterable<LogEntry> logTimesToMerge) {
        final Map<String, LogEntry> logTimesToMergeMap = new HashMap<>();
        for (final LogEntry logToMerge : logTimesToMerge) {
            logTimesToMergeMap.put(logToMerge.serviceLogId, logToMerge);
        }

        for (int i = 0; i < mergedLogTimes.size(); i++) {
            final LogEntry mergedLog = mergedLogTimes.get(i);
            final LogEntry logToMerge = logTimesToMergeMap.get(mergedLog.serviceLogId);
            if (logToMerge != null) {
                final Date dateTimeLogTime = new Date(mergedLog.date);
                final Date logTime = new Date(logToMerge.date);
                if (!logTime.equals(dateTimeLogTime) && DateUtils.isSameDay(dateTimeLogTime, logTime)) {
                    final LogEntry updatedTimeLog = mergedLog.buildUpon().setDate(logToMerge.date).build();
                    mergedLogTimes.set(i, updatedTimeLog);
                }
            }
        }
    }

    private static void mergeOfflineLogTime(final List<LogEntry> mergedLogTimes, final @Nullable OfflineLogEntry logToMerge) {
        if (logToMerge == null) {
            return;
        }

        for (int i = 0; i < mergedLogTimes.size(); i++) {
            final LogEntry mergedLog = mergedLogTimes.get(i);
            if (logToMerge.isMatchingLog(mergedLog)) {
                final LogEntry updatedTimeLog = mergedLog.buildUpon().setDate(logToMerge.date).build();
                mergedLogTimes.set(i, updatedTimeLog);

                break;
            }
        }
    }

    @NonNull
    static Single<Boolean> uploadModifiedCoordinates(@NonNull final Geocache cache, final Geopoint wpt) {
        return editModifiedCoordinates(cache, wpt);
    }

    @NonNull
    static Single<Boolean> deleteModifiedCoordinates(@NonNull final Geocache cache) {
        return editModifiedCoordinates(cache, null);
    }

    @NonNull
    static Single<Boolean> editModifiedCoordinates(@NonNull final Geocache cache, final Geopoint wpt) {
        final String userToken = getUserToken(cache);
        if (StringUtils.isEmpty(userToken)) {
            return Single.just(false);
        }

        final ObjectNode jo = new ObjectNode(JsonUtils.factory);
        final ObjectNode dto = jo.putObject("dto").put("ut", userToken);
        if (wpt != null) {
            dto.putObject("data").put("lat", wpt.getLatitudeE6() / 1E6).put("lng", wpt.getLongitudeE6() / 1E6);
        }

        final String uriSuffix = wpt != null ? "SetUserCoordinate" : "ResetUserCoordinate";

        final String uriPrefix = "https://www.geocaching.com/seek/cache_details.aspx/";

        return Network.completeWithSuccess(Network.postJsonRequest(uriPrefix + uriSuffix, jo))
            .toSingle(() -> {
                Log.i("GCParser.editModifiedCoordinates - edited on GC.com");
                return true;
            })
            .onErrorReturn((throwable) -> {
                Log.e("GCParser.deleteModifiedCoordinates - cannot delete modified coords", throwable);
                return false;
            });
    }

    @NonNull
    static Single<Boolean> uploadPersonalNote(@NonNull final Geocache cache) {
        final String userToken = getUserToken(cache);
        if (StringUtils.isEmpty(userToken)) {
            return Single.just(false);
        }

        final ObjectNode jo = new ObjectNode(JsonUtils.factory);
        jo.putObject("dto").put("et", StringUtils.defaultString(cache.getPersonalNote())).put("ut", userToken);

        final String uriSuffix = "SetUserCacheNote";

        final String uriPrefix = "https://www.geocaching.com/seek/cache_details.aspx/";

        return Network.completeWithSuccess(Network.postJsonRequest(uriPrefix + uriSuffix, jo))
            .toSingle(() -> {
                Log.i("GCParser.uploadPersonalNote - uploaded to GC.com");
                return true;
            })
            .onErrorReturn((throwable) -> {
                Log.e("GCParser.uploadPersonalNote - cannot upload personal note", throwable);
                return false;
            });
    }

    @WorkerThread
    @SuppressWarnings("UnusedReturnValue")
    static boolean ignoreCache(@NonNull final Geocache cache) {
        final String uri = "https://www.geocaching.com/bookmarks/ignore.aspx?guid=" + cache.getGuid() + "&WptTypeID=" + cache.getType().wptTypeId;
        final String page = GCLogin.getInstance().postRequestLogged(uri, null);

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.ignoreCache: No data from server");
            return false;
        }

        final String[] viewstates = GCLogin.getViewstates(page);

        final Parameters params = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "ctl00$ContentBody$btnYes", "Yes. Ignore it.");

        GCLogin.putViewstates(params, viewstates);
        final String response = Network.getResponseData(Network.postRequest(uri, params));

        return StringUtils.contains(response, "<p class=\"Success\">");
    }

    @Nullable
    public static String getUsername(@NonNull final String page) {
        String username = TextUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME1, null);
        if (StringUtils.isNotBlank(username)) {
            if (username.contains("\\")) {
                username = StringEscapeUtils.unescapeEcmaScript(username);
            }
            return username;
        }

        //second try
        username = TextUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME2, null);
        if (StringUtils.isNotBlank(username)) {
            return username;
        }

        // Old style webpage fallback // @todo: no longer existing?
        final Document document = Jsoup.parse(page);
        final String usernameOld = TextUtils.stripHtml(document.select("span.li-user-info > span:first-child").text());

        return StringUtils.isNotEmpty(usernameOld) ? usernameOld : null;
    }

    public static int getCachesCount(@Nullable final String page) {
        int cachesCount = -1;
        try {
            final String intStringToParse = TextUtils.getMatch(page, GCConstants.PATTERN_FINDCOUNT, true, 1, "", false);
            if (!StringUtils.isBlank(intStringToParse)) {
                cachesCount = Integer.parseInt(intStringToParse);
            }
        } catch (final NumberFormatException e) {
            Log.e("getCachesCount: bad cache count", e);
        }
        return cachesCount;
    }
}
