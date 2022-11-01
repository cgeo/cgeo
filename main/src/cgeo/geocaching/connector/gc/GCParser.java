package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.LocParser;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.GCVoteRating;
import cgeo.geocaching.location.DistanceParser;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.LogTypeTrackable;
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
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import cgeo.geocaching.utils.TextUtils;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

    private static final String SEARCH_CONTEXT_VIEWSTATE = "sc_gc_viewstate";

    @NonNull
    private static final SynchronizedDateFormat DATE_TB_IN_1 = new SynchronizedDateFormat("EEEEE, dd MMMMM yyyy", Locale.ENGLISH); // Saturday, 28 March 2009

    @NonNull
    private static final SynchronizedDateFormat DATE_TB_IN_2 = new SynchronizedDateFormat("EEEEE, MMMMM dd, yyyy", Locale.ENGLISH); // Saturday, March 28, 2009

    @NonNull
    private static final SynchronizedDateFormat DATE_JSON = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC"), Locale.US); // 2009-03-28T18:30:31.497Z
    private static final SynchronizedDateFormat DATE_JSON_SHORT = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"), Locale.US); // 2009-03-28T18:30:31Z

    @NonNull
    private static final ImmutablePair<StatusCode, Geocache> UNKNOWN_PARSE_ERROR = ImmutablePair.of(StatusCode.UNKNOWN_ERROR, null);

    private GCParser() {
        // Utility class
    }

    @Nullable
    @WorkerThread
    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    private static SearchResult parseSearch(final IConnector con, final String url, final String pageContent, final int alreadyTaken) {
        if (StringUtils.isBlank(pageContent)) {
            Log.e("GCParser.parseSearch: No page given");
            return null;
        }

        String page = pageContent;

        final SearchResult searchResult = new SearchResult();
        searchResult.setUrl(con, url);
        searchResult.setToContext(con, b -> b.putStringArray(SEARCH_CONTEXT_VIEWSTATE, GCLogin.getViewstates(pageContent)));
        searchResult.setToContext(con, b -> b.putBoolean(GCConnector.SEARCH_CONTEXT_LEGACY_PAGING, true));

        if (!page.contains("SearchResultsTable")) {
            // there are no results. aborting here avoids a wrong error log in the next parsing step
            return searchResult;
        }

        int startPos = page.indexOf("<div id=\"ctl00_ContentBody_ResultsPanel\"");
        if (startPos == -1) {
            Log.e("GCParser.parseSearch: ID \"ctl00_ContentBody_dlResults\" not found on page");
            return null;
        }

        page = page.substring(startPos); // cut on <table

        startPos = page.indexOf('>');
        final int endPos = page.indexOf("ctl00_ContentBody_UnitTxt");
        if (startPos == -1 || endPos == -1) {
            Log.e("GCParser.parseSearch: ID \"ctl00_ContentBody_UnitTxt\" not found on page");
            return null;
        }

        page = page.substring(startPos + 1, endPos - startPos + 1); // cut between <table> and </table>

        final List<String> cids = new ArrayList<>();
        final String[] rows = StringUtils.splitByWholeSeparator(page, "<tr class=");
        final int rowsCount = rows.length;

        final List<Geocache> caches = new ArrayList<>();
        for (int z = 1; z < rowsCount; z++) {
            final Geocache cache = new Geocache();
            final String row = rows[z];

            // check for cache type presence
            if (!GCConstants.PATTERN_SEARCH_TYPE.matcher(row).find()) {
                continue;
            }

            try {
                final MatcherWrapper matcherGuidAndDisabled = new MatcherWrapper(GCConstants.PATTERN_SEARCH_GUIDANDDISABLED, row);

                while (matcherGuidAndDisabled.find()) {
                    if (matcherGuidAndDisabled.group(2) != null) {
                        cache.setName(TextUtils.stripHtml(matcherGuidAndDisabled.group(2).trim()));
                    }
                    if (matcherGuidAndDisabled.group(3) != null) {
                        cache.setLocation(TextUtils.stripHtml(matcherGuidAndDisabled.group(3).trim()));
                    }

                    final String attr = matcherGuidAndDisabled.group(1);
                    if (attr != null) {
                        cache.setDisabled(attr.contains("Strike"));

                        cache.setArchived(attr.contains("OldWarning"));
                    }
                }
            } catch (final RuntimeException e) {
                // failed to parse GUID and/or Disabled
                Log.w("GCParser.parseSearch: Failed to parse GUID and/or Disabled data", e);
            }

            cache.setGeocode(TextUtils.getMatch(row, GCConstants.PATTERN_SEARCH_GEOCODE, true, 1, cache.getGeocode(), true));

            // cache type
            cache.setType(CacheType.getByWaypointType(TextUtils.getMatch(row, GCConstants.PATTERN_SEARCH_TYPE, null)));

            // cache direction - image
            if (Settings.getLoadDirImg()) {
                final String direction = TextUtils.getMatch(row, GCConstants.PATTERN_SEARCH_DIRECTION_DISTANCE, false, null);
                if (direction != null) {
                    cache.setDirectionImg(direction);
                }
            }

            // cache distance - estimated distance for basic members
            final MatcherWrapper cacheDistanceMatcher = new MatcherWrapper(GCConstants.PATTERN_SEARCH_DIRECTION_DISTANCE, row);
            if (cacheDistanceMatcher.find()) {
                final DistanceParser.DistanceUnit unit = DistanceParser.DistanceUnit.parseUnit(cacheDistanceMatcher.group(3),
                        Settings.useImperialUnits() ? DistanceParser.DistanceUnit.FT : DistanceParser.DistanceUnit.M);
                try {
                    cache.setDistance(DistanceParser.parseDistance(cacheDistanceMatcher.group(2), unit));
                } catch (final NumberFormatException e) {
                    Log.e("GCParser.parseDistance: Failed to parse distance", e);
                }
            }

            // difficulty/terrain
            final MatcherWrapper matcherDT = new MatcherWrapper(GCConstants.PATTERN_SEARCH_DIFFICULTY_TERRAIN, row);
            if (matcherDT.find()) {
                final Float difficulty = parseStars(matcherDT.group(1));
                if (difficulty != null) {
                    cache.setDifficulty(difficulty);
                }
                final Float terrain = parseStars(matcherDT.group(3));
                if (terrain != null) {
                    cache.setTerrain(terrain);
                }
            }

            // size
            final String container = TextUtils.getMatch(row, GCConstants.PATTERN_SEARCH_CONTAINER, false, null);
            cache.setSize(CacheSize.getById(container));

            // date hidden, makes sorting event caches easier
            final String dateHidden = TextUtils.getMatch(row, GCConstants.PATTERN_SEARCH_HIDDEN_DATE, false, null);
            if (StringUtils.isNotBlank(dateHidden)) {
                try {
                    final Date date = GCLogin.parseGcCustomDate(dateHidden);
                    if (date != null) {
                        cache.setHidden(date);
                    }
                } catch (final ParseException e) {
                    Log.e("Error parsing event date from search", e);
                }
            }

            // cache inventory
            final MatcherWrapper matcherTbs = new MatcherWrapper(GCConstants.PATTERN_SEARCH_TRACKABLES, row);
            String inventoryPre = null;
            while (matcherTbs.find()) {
                try {
                    cache.setInventoryItems(Integer.parseInt(matcherTbs.group(1)));
                } catch (final NumberFormatException e) {
                    Log.e("Error parsing trackables count", e);
                }
                inventoryPre = matcherTbs.group(2);
            }

            if (StringUtils.isNotBlank(inventoryPre)) {
                final MatcherWrapper matcherTbsInside = new MatcherWrapper(GCConstants.PATTERN_SEARCH_TRACKABLESINSIDE, inventoryPre);
                while (matcherTbsInside.find()) {
                    if (matcherTbsInside.group(1) != null &&
                            !matcherTbsInside.group(1).equalsIgnoreCase("premium member only cache") &&
                            cache.getInventoryItems() <= 0) {
                        cache.setInventoryItems(1);
                    }
                }
            }

            // premium cache
            cache.setPremiumMembersOnly(row.contains("/images/icons/16/premium_only.png"));

            // found it
            cache.setFound(row.contains("/images/icons/16/found.png") || row.contains("uxUserLogDate\" class=\"Success\""));

            // infer cache id from geocode
            cache.setCacheId(String.valueOf(GCUtils.gcLikeCodeToGcLikeId(cache.getGeocode())));
            cids.add(cache.getCacheId());

            // favorite count
            try {
                final String result = getNumberString(TextUtils.getMatch(row, GCConstants.PATTERN_SEARCH_FAVORITE, false, 1, null, true));
                if (result != null) {
                    cache.setFavoritePoints(Integer.parseInt(result));
                }
            } catch (final NumberFormatException e) {
                Log.w("GCParser.parseSearch: Failed to parse favorite count", e);
            }

            caches.add(cache);
        }
        searchResult.addAndPutInCache(caches);
        searchResult.setToContext(con, b -> b.putInt(GCConnector.SEARCH_CONTEXT_TOOK_TOTAL, alreadyTaken + caches.size()));

        // total caches found
        try {
            final String result = TextUtils.getMatch(page, GCConstants.PATTERN_SEARCH_TOTALCOUNT, false, 1, null, true);
            if (result != null) {
                searchResult.setLeftToFetch(con, Integer.parseInt(result) - caches.size() - alreadyTaken);
            }
        } catch (final NumberFormatException e) {
            Log.w("GCParser.parseSearch: Failed to parse cache count", e);
        }

        if (!cids.isEmpty() && Settings.isGCPremiumMember()) {
            Log.i("Trying to get .loc for " + cids.size() + " caches");
            final Observable<Set<Geocache>> storedCaches = Observable.defer(() -> Observable.just(DataStore.loadCaches(Geocache.getGeocodes(caches), LoadFlags.LOAD_CACHE_OR_DB))).subscribeOn(Schedulers.io()).cache();
            storedCaches.subscribe();  // Force asynchronous start of database loading

            try {
                // get coordinates for parsed caches
                final Parameters params = new Parameters(
                        "__EVENTTARGET", "",
                        "__EVENTARGUMENT", "");
                GCLogin.putViewstates(params, searchResult.getFromContext(con, b -> b.getStringArray(SEARCH_CONTEXT_VIEWSTATE)));
                for (final String cid : cids) {
                    params.put("CID", cid);
                }

                params.put("Download", "Download Waypoints");

                // retrieve target url
                final String queryUrl = TextUtils.getMatch(pageContent, GCConstants.PATTERN_SEARCH_POST_ACTION, "");

                if (StringUtils.isEmpty(queryUrl)) {
                    Log.w("Loc download url not found");
                } else {

                    final String coordinates = Network.getResponseData(Network.postRequest("https://www.geocaching.com/seek/" + queryUrl, params), false);

                    if (StringUtils.contains(coordinates, GCConstants.STRING_UNAPPROVED_LICENSE)) {
                        Log.i("User has not agreed to the license agreement. Can't download .loc file.");
                        searchResult.setError(con, StatusCode.UNAPPROVED_LICENSE);
                        return searchResult;
                    }

                    LocParser.parseLoc(coordinates, storedCaches.blockingSingle());
                }

            } catch (final RuntimeException e) {
                Log.e("GCParser.parseSearch.CIDs", e);
            }
        }

        return searchResult;
    }

    @Nullable
    private static Float parseStars(final String value) {
        final float floatValue = Float.parseFloat(StringUtils.replaceChars(value, ',', '.'));
        return floatValue >= 0.5 && floatValue <= 5.0 ? floatValue : null;
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

    @NonNull
    static SearchResult parseAndSaveCacheFromText(final IConnector con, @Nullable final String page, @Nullable final DisposableHandler handler) {
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
        cache.setShortDescription(TextUtils.getMatch(page, GCConstants.PATTERN_SHORTDESC, true, ""));

        // cache description
        final String longDescription = TextUtils.getMatch(page, GCConstants.PATTERN_DESC, true, "");
        String relatedWebPage = TextUtils.getMatch(page, GCConstants.PATTERN_RELATED_WEB_PAGE, true, "");
        if (StringUtils.isNotEmpty(relatedWebPage)) {
            relatedWebPage = String.format("<br/><br/><a href=\"%s\"><b>%s</b></a>", relatedWebPage, relatedWebPage);
        }
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
        try {
            if (DisposableHandler.isDisposed(handler)) {
                return UNKNOWN_PARSE_ERROR;
            }
            DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_spoilers);

            final MatcherWrapper matcherSpoilersInside = new MatcherWrapper(GCConstants.PATTERN_SPOILER_IMAGE, page);

            while (matcherSpoilersInside.find()) {
                final String url = fullScaleImageUrl(matcherSpoilersInside.group(1));

                String title = null;
                if (matcherSpoilersInside.group(2) != null) {
                    title = matcherSpoilersInside.group(2);
                }
                String description = null;
                if (matcherSpoilersInside.group(3) != null) {
                    description = matcherSpoilersInside.group(3);
                }
                if (title != null) {
                    cache.addSpoiler(new Image.Builder().setUrl(url).setTitle(title).setDescription(description).build());
                }
            }
        } catch (final RuntimeException e) {
            // failed to parse cache spoilers
            Log.w("GCParser.parseCache: Failed to parse cache spoilers", e);
        }

        // background image, to be added only if the image is not already present in the cache listing
        final MatcherWrapper matcherBackgroundImage = new MatcherWrapper(GCConstants.PATTERN_BACKGROUND_IMAGE, page);
        if (matcherBackgroundImage.find()) {
            final String url = fullScaleImageUrl(matcherBackgroundImage.group(1));
            boolean present = false;
            for (final Image image : cache.getSpoilers()) {
                if (StringUtils.equals(image.getUrl(), url)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                cache.addSpoiler(new Image.Builder().setUrl(url).setTitle(CgeoApplication.getInstance().getString(R.string.cache_image_background)).build());
            }
        }

        // cache inventory
        try {
            final MatcherWrapper matcherInventory = new MatcherWrapper(GCConstants.PATTERN_INVENTORY, page);
            if (matcherInventory.find()) {
                final String inventoryPre = matcherInventory.group();

                final ArrayList<Trackable> inventory = new ArrayList<>();
                if (StringUtils.isNotBlank(inventoryPre)) {
                    final MatcherWrapper matcherInventoryInside = new MatcherWrapper(GCConstants.PATTERN_INVENTORYINSIDE, inventoryPre);

                    while (matcherInventoryInside.find()) {
                        final Trackable inventoryItem = new Trackable();
                        inventoryItem.forceSetBrand(TrackableBrand.TRAVELBUG);
                        inventoryItem.setGuid(matcherInventoryInside.group(1));
                        inventoryItem.setName(matcherInventoryInside.group(2));

                        inventory.add(inventoryItem);
                    }
                }
                cache.mergeInventory(inventory, EnumSet.of(TrackableBrand.TRAVELBUG));
            }
        } catch (final RuntimeException e) {
            // failed to parse cache inventory
            Log.w("GCParser.parseCache: Failed to parse cache inventory (2)", e);
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
                final Waypoint waypoint = new Waypoint(CgeoApplication.getInstance().getString(R.string.cache_coordinates_original), WaypointType.ORIGINAL, false);
                waypoint.setCoords(new Geopoint(originalCoords));
                cache.addOrChangeWaypoint(waypoint, false);
                cache.setUserModifiedCoords(true);
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

    @Nullable
    private static String getNumberString(@Nullable final String numberWithPunctuation) {
        return StringUtils.replaceChars(numberWithPunctuation, ".,", "");
    }

    @NonNull
    static String fullScaleImageUrl(@NonNull final String imageUrl) {
        // For images from geocaching.com: the original spoiler URL
        // (include .../display/... contains a low-resolution image
        // if we shorten the URL we get the original-resolution image
        return GCConstants.PATTERN_GC_HOSTED_IMAGE.matcher(imageUrl).find() ? imageUrl.replace("/display", "") : imageUrl;
    }

    @Nullable
    @WorkerThread
    public static SearchResult searchByNextPage(final IConnector con, final Bundle context) {
        if (context == null) {
            return null;
        }

        final String url = context.getString(SearchResult.CON_URL);
        if (StringUtils.isBlank(url)) {
            Log.e("GCParser.searchByNextPage: No url found");
            return new SearchResult(con, StatusCode.UNKNOWN_ERROR);
        }

        final String[] viewstates = context.getStringArray(SEARCH_CONTEXT_VIEWSTATE);
        if (GCLogin.isEmpty(viewstates)) {
            Log.e("GCParser.searchByNextPage: No viewstate given");
            return new SearchResult(con, StatusCode.NO_LOGIN_INFO_STORED);
        }

        final Parameters params = new Parameters(
                "__EVENTTARGET", "ctl00$ContentBody$pgrBottom$ctl08",
                "__EVENTARGUMENT", "");
        GCLogin.putViewstates(params, viewstates);

        final String page = GCLogin.getInstance().postRequestLogged(url, params);
        if (!GCLogin.getInstance().getLoginStatus(page)) {
            Log.e("GCParser.postLogTrackable: Can not log in geocaching");
            return new SearchResult(con, StatusCode.WRONG_LOGIN_DATA);
        }

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.searchByNextPage: No data from server");
            return new SearchResult(con, StatusCode.CONNECTION_FAILED);
        }

        final SearchResult searchResult = parseSearch(con, url, page, context.getInt(GCConnector.SEARCH_CONTEXT_TOOK_TOTAL, 0));
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.w("GCParser.searchByNextPage: No cache parsed");
            return new SearchResult(con, StatusCode.CONNECTION_FAILED);
        }

        // search results don't need to be filtered so load GCVote ratings here
        GCVote.loadRatings(new ArrayList<>(searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB)));

        return searchResult;
    }

    @Nullable
    @WorkerThread
    private static SearchResult searchByAny(final IConnector con, final Parameters params) {
        return searchByAny(con, params, null, false);
    }

    @WorkerThread
    private static SearchResult searchByAny(final IConnector con, final Parameters params, @Nullable final CacheType ct, final boolean noOwnFound) {
        final String uri = "https://www.geocaching.com/seek/nearest.aspx";

        if (ct != null) {
            //this will limit results to specific cache type
            params.put("cFilter", ct.guid);
        }
        if (noOwnFound) {
            //this will lead to skip own and found caches in result
            params.put("ex", "1");
        }

        final String page = GCLogin.getInstance().getRequestLogged(uri, params);

        if (StringUtils.isBlank(page)) {
            Log.w("GCParser.searchByAny: No data from server");
            return null;
        }

        final String fullUri = uri + "?" + params;
        final SearchResult searchResult = parseSearch(con, fullUri, page, 0);
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.w("GCParser.searchByAny: No cache parsed");
            return searchResult;
        }

        final SearchResult search = searchResult.putInCacheAndLoadRating();

        GCLogin.getInstance().getLoginStatus(page);

        return search;
    }

    public static SearchResult searchByCoords(final IConnector con, @NonNull final Geopoint coords) {
        final Parameters params = new Parameters("lat", Double.toString(coords.getLatitude()), "lng", Double.toString(coords.getLongitude()));
        return searchByAny(con, params);
    }

    static SearchResult searchByKeyword(final IConnector con, @NonNull final String keyword) {
        if (StringUtils.isBlank(keyword)) {
            Log.e("GCParser.searchByKeyword: No keyword given");
            return null;
        }

        final Parameters params = new Parameters("key", keyword);
        return searchByAny(con, params);
    }

    @WorkerThread
    public static SearchResult searchByUsername(final IConnector con, final String userName, @Nullable final CacheType ct, final boolean noOwnFound) {
        if (StringUtils.isBlank(userName)) {
            Log.e("GCParser.searchByUsername: No user name given");
            return null;
        }

        final Parameters params = new Parameters("ul", escapePlus(userName));

        final SearchResult sr = searchByAny(con, params, ct, noOwnFound);
        if (sr != null) {
            sr.getSearchContext().putString(Geocache.SEARCHCONTEXT_FINDER, userName);
        }
        return sr;
    }

    public static SearchResult searchByPocketQuery(final IConnector con, final String pocketGuid) {
        if (StringUtils.isBlank(pocketGuid)) {
            Log.e("GCParser.searchByPocket: No guid name given");
            return null;
        }

        final Parameters params = new Parameters("pq", pocketGuid);

        return searchByAny(con, params);
    }

    public static SearchResult searchByOwner(final IConnector con, final String userName) {
        if (StringUtils.isBlank(userName)) {
            Log.e("GCParser.searchByOwner: No user name given");
            return null;
        }

        final Parameters params = new Parameters("u", escapePlus(userName));
        return searchByAny(con, params);
    }

    /**
     * GC needs a double escaping of the + sign.
     */
    private static String escapePlus(@NonNull final String name) {
        return name.replace("+", "%2b");
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

                final GCList pocketQuery = new GCList(guid, name, count, true, date.getTime(), -1, true);
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
    public static String createBookmarkList(final String name) {
        final ObjectNode jo = new ObjectNode(JsonUtils.factory).put("name", name);
        jo.putObject("type").put("code", "bm");

        try {
            final String result = Network.getResponseData(Network.postJsonRequest("https://www.geocaching.com/api/proxy/web/v1/lists", jo));

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
    @WorkerThread
    public static boolean addCachesToBookmarkList(final String listGuid, final List<Geocache> geocaches) {
        final ArrayNode arrayNode = JsonUtils.createArrayNode();

        for (Geocache geocache : geocaches) {
            if (ConnectorFactory.getConnector(geocache) instanceof GCConnector) {
                arrayNode.add(new ObjectNode(JsonUtils.factory).put("referenceCode", geocache.getGeocode()));
            }
        }

        Log.d(arrayNode.toString());

        try {
            Network.completeWithSuccess(Network.putJsonRequest("https://www.geocaching.com/api/proxy/web/v1/lists/" + listGuid + "/geocaches", arrayNode));
            Log.i("GCParser.addCachesToBookmarkList - caches uploaded to GC.com bookmark list");
            return true;
        } catch (final Exception ignored) {
            Log.e("GCParser.uploadPersonalNote - cannot upload caches to GC.com bookmark list", ignored);
            return false;
        }
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
                if (!downloadablePocketQueries.containsKey(guid)) {
                    final String name = link.attr("title");
                    final GCList pocketQuery = new GCList(guid, name, -1, false, 0, -1, false);
                    list.add(pocketQuery);
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

            final GCList pocketQuery = new GCList(guid, name, count, true, lastGeneration, daysRemaining, false);
            downloadablePocketQueries.put(guid, pocketQuery);
        }

        return downloadablePocketQueries;
    }

    /**
     * Post a log to GC.com.
     *
     * @return status code of the upload and ID of the log
     */
    @NonNull
    @WorkerThread
    public static StatusCode postLogTrackable(final String tbid, final String trackingCode, final String[] viewstates,
                                              final LogTypeTrackable logType, final int year, final int month, final int day, final String log) {
        if (GCLogin.isEmpty(viewstates)) {
            Log.e("GCParser.postLogTrackable: No viewstate given");
            return StatusCode.LOG_POST_ERROR;
        }

        if (StringUtils.isBlank(log)) {
            Log.w("GCParser.postLogTrackable: No log text given");
            return StatusCode.NO_LOG_TEXT;
        }

        Log.i("Trying to post log for trackable #" + trackingCode + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + log);

        final String logInfo = log.replace("\n", "\r\n"); // windows' eol

        final Parameters params = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "__LASTFOCUS", "",
                "ctl00$ContentBody$LogBookPanel1$ddLogType", Integer.toString(logType.id),
                "ctl00$ContentBody$LogBookPanel1$tbCode", trackingCode,
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged", month + "/" + day + "/" + year,
                "ctl00$ContentBody$LogBookPanel1$uxDateVisited", GCLogin.formatGcCustomDate(year, month, day),
                "ctl00$ContentBody$LogBookPanel1$uxLogInfo", logInfo,
                "ctl00$ContentBody$LogBookPanel1$btnSubmitLog", "Submit Log Entry",
                "ctl00$ContentBody$uxVistOtherTrackableTB", "");
        GCLogin.putViewstates(params, viewstates);

        final String uri = new Uri.Builder().scheme("https").authority("www.geocaching.com").path("/track/log.aspx").encodedQuery("wid=" + tbid).build().toString();
        final String page = GCLogin.getInstance().postRequestLogged(uri, params);
        if (!GCLogin.getInstance().getLoginStatus(page)) {
            Log.e("GCParser.postLogTrackable: Cannot log in geocaching");
            return StatusCode.NOT_LOGGED_IN;
        }

        try {

            final MatcherWrapper matcherOk = new MatcherWrapper(GCConstants.PATTERN_OK2, page);
            if (matcherOk.find()) {
                Log.i("Log successfully posted to trackable #" + trackingCode);
                return StatusCode.NO_ERROR;
            }
        } catch (final Exception e) {
            Log.e("GCParser.postLogTrackable.check", e);
        }

        Log.e("GCParser.postLogTrackable: Failed to post log because of unknown error");
        return StatusCode.LOG_POST_ERROR;
    }

    /**
     * Adds the cache to the watchlist of the user.
     *
     * @param cache the cache to add
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    @WorkerThread
    static boolean addToWatchlist(@NonNull final Geocache cache) {
        return addToOrRemoveFromWatchlist(cache, true);
    }

    /**
     * internal method to handle add to / remove from watchlist
     */
    @WorkerThread
    private static boolean addToOrRemoveFromWatchlist(@NonNull final Geocache cache, final boolean doAdd) {

        final String logContext = "GCParser.addToOrRemoveFromWatchlist(cache = " + cache.getGeocode() + ", add = " + doAdd + ")";

        final ObjectNode jo = new ObjectNode(JsonUtils.factory).put("geocacheId", cache.getCacheId());
        final String uri = "https://www.geocaching.com/api/proxy/web/v1/watchlists/" + (doAdd ? "add" : "remove") + "?geocacheId=" + cache.getCacheId();

        try {
            if (doAdd) {
                Network.completeWithSuccess(Network.postJsonRequest(uri, jo));
            } else {
                Network.completeWithSuccess(Network.deleteJsonRequest(uri, jo));
            }
            Log.i(logContext + ": success");
        } catch (final Exception ex) {
            Log.e(logContext + ": error", ex);
            return false;
        }

        // Set cache properties
        cache.setOnWatchlist(doAdd);
        final String watchListPage = GCLogin.getInstance().postRequestLogged(cache.getLongUrl(), null);
        cache.setWatchlistCount(getWatchListCount(watchListPage));
        return true;
    }

    /**
     * This method extracts the amount of people watching on a geocache out of the HTMl website passed to it
     *
     * @param page Page containing the information about how many people watching on geocache
     * @return Number of people watching geocache, -1 when error
     */
    static int getWatchListCount(final String page) {
        final String sCount = TextUtils.getMatch(page, GCConstants.PATTERN_WATCHLIST_COUNT, true, 1, "notFound", false);
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
    @WorkerThread
    static boolean removeFromWatchlist(@NonNull final Geocache cache) {
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
     *
     * This must not be called from the UI thread.
     *
     * @param cache the cache to add
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    static boolean addToFavorites(@NonNull final Geocache cache) {
        return changeFavorite(cache, true);
    }

    private static boolean changeFavorite(@NonNull final Geocache cache, final boolean add) {
        final String userToken = getUserToken(cache);
        if (StringUtils.isEmpty(userToken)) {
            return false;
        }

        final String uri = "https://www.geocaching.com/datastore/favorites.svc/update?u=" + userToken + "&f=" + add;

        try {
            Network.completeWithSuccess(Network.postRequest(uri, null));
            Log.i("GCParser.changeFavorite: cache added/removed to/from favorites");
            cache.setFavorite(add);
            cache.setFavoritePoints(cache.getFavoritePoints() + (add ? 1 : -1));
            return true;
        } catch (final Exception ignored) {
            Log.e("GCParser.changeFavorite: cache not added/removed to/from favorites");
            return false;
        }
    }

    private static String getUserToken(@NonNull final Geocache cache) {
        return parseUserToken(requestHtmlPage(cache.getGeocode(), null, "n"));
    }

    private static String parseUserToken(final String page) {
        return TextUtils.getMatch(page, GCConstants.PATTERN_USERTOKEN, "");
    }

    /**
     * Removes the cache from the favorites.
     *
     * This must not be called from the UI thread.
     *
     * @param cache the cache to remove
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    static boolean removeFromFavorites(@NonNull final Geocache cache) {
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
        trackable.forceSetBrand(TrackableBrand.TRAVELBUG);

        // trackable geocode
        trackable.setGeocode(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GEOCODE, true, StringUtils.upperCase(possibleTrackingcode)));
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
            final MatcherWrapper matcherSpottedCache = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_SPOTTEDCACHE, page);
            if (matcherSpottedCache.find()) {
                trackable.setSpottedGuid(matcherSpottedCache.group(2));
                trackable.setSpottedName(matcherSpottedCache.group(1).trim());
                trackable.setSpottedType(Trackable.SPOTTED_CACHE);
            }

            final MatcherWrapper matcherSpottedUser = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_SPOTTEDUSER, page);
            if (matcherSpottedUser.find()) {
                trackable.setSpottedGuid(matcherSpottedUser.group(3));
                trackable.setSpottedName(matcherSpottedUser.group(1).trim());
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
            final DistanceParser.DistanceUnit unit = DistanceParser.DistanceUnit.parseUnit(distanceMatcher.group(2),
                    Settings.useImperialUnits() ? DistanceParser.DistanceUnit.MI : DistanceParser.DistanceUnit.KM);
            try {
                trackable.setDistance(DistanceParser.parseDistance(distanceMatcher.group(1), unit));
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
            final MatcherWrapper matcherLogs = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_LOG, page);
            /*
             * 1. Type (image)
             * 2. Date
             * 3. Author-GUID
             * 4. Author
             * 5. Cache-GUID
             * 6. <ignored> (strike-through property for ancient caches)
             * 7. Cache-name
             * 8. Log-ID
             * 9. Log text
             */
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
                    logDoneBuilder.setCacheGuid(matcherLogs.group(5));
                    logDoneBuilder.setCacheName(matcherLogs.group(7));
                }

                // Apply the pattern for images in a trackable log entry against each full log (group(0))
                final String logEntry = matcherLogs.group(0);
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

    private enum Logs {
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
        if (userToken.isEmpty()) {
            Log.e("GCParser.getLogs: unable to extract userToken");
            return Observable.empty();
        }

        return Observable.defer(() -> {
            final Parameters params = new Parameters(
                    "tkn", userToken,
                    "idx", "1",
                    "num", String.valueOf(GCConstants.NUMBER_OF_LOGS),
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
                        final String url = "https://imgcdn.geocaching.com/cache/log/large/" + image.path("FileName").asText();
                        final String title = TextUtils.removeControlCharacters(image.path("Name").asText());
                        String description = image.path("Descr").asText();
                        if (StringUtils.contains(description, "Geocaching®") && description.length() < 60) {
                            description = null;
                        }
                        final Image logImage = new Image.Builder().setUrl(url).setTitle(title).setDescription(description).build();
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
     * Javascript Object from the new Logpage: https://www.geocaching.com/play/geocache/gc.../log
     * <pre>
     *     {"Value":46,"Description":"Owner maintenance","IsRealtimeOnly":false}
     * </pre>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class AvailableLogType {
        @JsonProperty("Value")
        int value;
        @JsonProperty("Description")
        String description;
        @JsonProperty("IsRealtimeOnly")
        boolean isRealtimeOnly;
    }

    @NonNull
    static List<LogType> parseTypes(final String page) {
        if (StringUtils.isEmpty(page)) {
            return Collections.emptyList();
        }

        final List<LogType> types = new ArrayList<>();
        final MatcherWrapper typeMatcher = new MatcherWrapper(GCConstants.PATTERN_TYPE3, page);
        while (typeMatcher.find()) {
            try {
                final AvailableLogType availableLogType = MAPPER.readValue(typeMatcher.group(1), AvailableLogType.class);
                if (availableLogType.value > 0) {
                    types.add(LogType.getById(availableLogType.value));
                }
            } catch (final Exception e) {
                Log.e("Error parsing log types", e);
            }
        }

        // we don't support this log type
        types.remove(LogType.UPDATE_COORDINATES);

        return types;
    }

    @NonNull
    public static List<LogTypeTrackable> parseLogTypesTrackables(final String page) {
        if (StringUtils.isEmpty(page)) {
            return new ArrayList<>();
        }

        final List<LogTypeTrackable> types = new ArrayList<>();

        final MatcherWrapper typeBoxMatcher = new MatcherWrapper(GCConstants.PATTERN_TYPEBOX, page);
        if (typeBoxMatcher.find()) {
            final String typesText = typeBoxMatcher.group(1);
            final MatcherWrapper typeMatcher = new MatcherWrapper(GCConstants.PATTERN_TYPE2, typesText);
            while (typeMatcher.find()) {
                try {
                    final int type = Integer.parseInt(typeMatcher.group(2));
                    if (type > 0) {
                        types.add(LogTypeTrackable.getById(type));
                    }
                } catch (final NumberFormatException e) {
                    Log.e("Error parsing trackable log types", e);
                }
            }
        }
        return types;
    }

    @WorkerThread
    private static void getExtraOnlineInfo(@NonNull final Geocache cache, final String page, final DisposableHandler handler) {
        // This method starts the page parsing for logs in the background, as well as retrieve the friends and own logs
        // if requested. It merges them and stores them in the background, while the rating is retrieved if needed and
        // stored. Then we wait for the log merging and saving to be completed before returning.
        if (DisposableHandler.isDisposed(handler)) {
            return;
        }

        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_logs);
        final String userToken = parseUserToken(page);
        final Observable<LogEntry> logs = getLogs(userToken, Logs.ALL);
        final Observable<LogEntry> ownLogs = getLogs(userToken, Logs.OWN).cache();
        final Observable<LogEntry> specialLogs = Settings.isFriendLogsWanted() ?
                Observable.merge(getLogs(userToken, Logs.FRIENDS), ownLogs) : Observable.empty();
        final Single<List<LogEntry>> mergedLogs = Single.zip(logs.toList(), specialLogs.toList(),
                (logEntries, specialLogEntries) -> {
                    mergeFriendsLogs(logEntries, specialLogEntries);
                    return logEntries;
                }).cache();
        mergedLogs.subscribe(logEntries -> DataStore.saveLogs(cache.getGeocode(), logEntries, true));
        if (cache.isFound() || cache.isDNF()) {
            ownLogs.subscribe(logEntry -> {
                if (logEntry.logType.isFoundLog() || (!cache.isFound() && cache.isDNF() && logEntry.logType == LogType.DIDNT_FIND_IT)) {
                    cache.setVisitedDate(logEntry.date);
                }
            });
        }

        if (Settings.isRatingWanted() && !DisposableHandler.isDisposed(handler)) {
            DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_gcvote);
            final GCVoteRating rating = GCVote.getRating(cache.getGuid(), cache.getGeocode());
            if (rating != null) {
                cache.setRating(rating.getRating());
                cache.setVotes(rating.getVotes());
                cache.setMyVote(rating.getMyVote());
            }
        }

        // Wait for completion of logs parsing, retrieving and merging
        mergedLogs.ignoreElement().blockingAwait();
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
            if (mergedLogs.contains(log)) {
                final LogEntry friendLog = mergedLogs.get(mergedLogs.indexOf(log));
                final LogEntry updatedFriendLog = friendLog.buildUpon().setFriend(true).build();
                mergedLogs.set(mergedLogs.indexOf(log), updatedFriendLog);
            } else {
                mergedLogs.add(log);
            }
        }
    }

    static boolean uploadModifiedCoordinates(@NonNull final Geocache cache, final Geopoint wpt) {
        return editModifiedCoordinates(cache, wpt);
    }

    static boolean deleteModifiedCoordinates(@NonNull final Geocache cache) {
        return editModifiedCoordinates(cache, null);
    }

    static boolean editModifiedCoordinates(@NonNull final Geocache cache, final Geopoint wpt) {
        final String userToken = getUserToken(cache);
        if (StringUtils.isEmpty(userToken)) {
            return false;
        }

        final ObjectNode jo = new ObjectNode(JsonUtils.factory);
        final ObjectNode dto = jo.putObject("dto").put("ut", userToken);
        if (wpt != null) {
            dto.putObject("data").put("lat", wpt.getLatitudeE6() / 1E6).put("lng", wpt.getLongitudeE6() / 1E6);
        }

        final String uriSuffix = wpt != null ? "SetUserCoordinate" : "ResetUserCoordinate";

        final String uriPrefix = "https://www.geocaching.com/seek/cache_details.aspx/";

        try {
            Network.completeWithSuccess(Network.postJsonRequest(uriPrefix + uriSuffix, jo));
            Log.i("GCParser.editModifiedCoordinates - edited on GC.com");
            return true;
        } catch (final Exception ignored) {
            Log.e("GCParser.deleteModifiedCoordinates - cannot delete modified coords");
            return false;
        }
    }

    static boolean uploadPersonalNote(@NonNull final Geocache cache) {
        final String userToken = getUserToken(cache);
        if (StringUtils.isEmpty(userToken)) {
            return false;
        }

        final ObjectNode jo = new ObjectNode(JsonUtils.factory);
        jo.putObject("dto").put("et", StringUtils.defaultString(cache.getPersonalNote())).put("ut", userToken);

        final String uriSuffix = "SetUserCacheNote";

        final String uriPrefix = "https://www.geocaching.com/seek/cache_details.aspx/";

        try {
            Network.completeWithSuccess(Network.postJsonRequest(uriPrefix + uriSuffix, jo));
            Log.i("GCParser.uploadPersonalNote - uploaded to GC.com");
            return true;
        } catch (final Exception ignored) {
            Log.e("GCParser.uploadPersonalNote - cannot upload personal note");
            return false;
        }
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
    public static String getUsername(@Nullable final String page) {
        final String username = TextUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME, null);
        if (StringUtils.isNotBlank(username)) {
            return username;
        }

        // Old style webpage fallback // @todo: no longer existing?
        final Document document = Jsoup.parse(page);
        final String usernameOld = TextUtils.stripHtml(document.select("span.li-user-info > span:first-child").text());

        return StringUtils.isNotEmpty(usernameOld) ? usernameOld : null;
    }

    public static int getCachesCount(final String page) {
        int cachesCount = -1;
        try {
            final String intStringToParse = removeDotAndComma(TextUtils.getMatch(page, GCConstants.PATTERN_CACHES_FOUND, true, ""));
            if (!StringUtils.isBlank(intStringToParse)) {
                cachesCount = Integer.parseInt(intStringToParse);
            }
        } catch (final NumberFormatException e) {
            Log.e("getCachesCount: bad cache count", e);
        }

        return cachesCount;
    }

    @Nullable
    private static String removeDotAndComma(@Nullable final String str) {
        return StringUtils.replaceChars(str, ".,", null);
    }
}
