package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.LogTypeTrackable;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.LocParser;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.gcvote.GCVoteRating;
import cgeo.geocaching.location.DistanceParser;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.LogEntry;
import cgeo.geocaching.models.PocketQuery;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.TrackableLog;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import cgeo.geocaching.utils.TextUtils;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public final class GCParser {
    @NonNull
    private static final SynchronizedDateFormat DATE_TB_IN_1 = new SynchronizedDateFormat("EEEEE, dd MMMMM yyyy", Locale.ENGLISH); // Saturday, 28 March 2009

    @NonNull
    private static final SynchronizedDateFormat DATE_TB_IN_2 = new SynchronizedDateFormat("EEEEE, MMMMM dd, yyyy", Locale.ENGLISH); // Saturday, March 28, 2009

    @NonNull
    private static final ImmutablePair<StatusCode, Geocache> UNKNOWN_PARSE_ERROR = ImmutablePair.of(StatusCode.UNKNOWN_ERROR, null);

    private GCParser() {
        // Utility class
    }

    @Nullable
    private static SearchResult parseSearch(final String url, final String pageContent) {
        if (StringUtils.isBlank(pageContent)) {
            Log.e("GCParser.parseSearch: No page given");
            return null;
        }

        String page = pageContent;

        final SearchResult searchResult = new SearchResult();
        searchResult.setUrl(url);
        searchResult.setViewstates(GCLogin.getViewstates(page));

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

        int excludedCaches = 0;
        final List<Geocache> caches = new ArrayList<>();
        for (int z = 1; z < rowsCount; z++) {
            final Geocache cache = new Geocache();
            final String row = rows[z];

            // check for cache type presence
            if (!row.contains("images/wpttypes")) {
                continue;
            }

            try {
                final MatcherWrapper matcherGuidAndDisabled = new MatcherWrapper(GCConstants.PATTERN_SEARCH_GUIDANDDISABLED, row);

                while (matcherGuidAndDisabled.find()) {
                    if (matcherGuidAndDisabled.groupCount() > 0) {
                        if (matcherGuidAndDisabled.group(2) != null) {
                            cache.setName(Html.fromHtml(matcherGuidAndDisabled.group(2).trim()).toString());
                        }
                        if (matcherGuidAndDisabled.group(3) != null) {
                            cache.setLocation(Html.fromHtml(matcherGuidAndDisabled.group(3).trim()).toString());
                        }

                        final String attr = matcherGuidAndDisabled.group(1);
                        if (attr != null) {
                            cache.setDisabled(attr.contains("Strike"));

                            cache.setArchived(attr.contains("OldWarning"));
                        }
                    }
                }
            } catch (final RuntimeException e) {
                // failed to parse GUID and/or Disabled
                Log.w("GCParser.parseSearch: Failed to parse GUID and/or Disabled data", e);
            }

            if (Settings.isExcludeDisabledCaches() && (cache.isDisabled() || cache.isArchived())) {
                // skip disabled and archived caches
                excludedCaches++;
                continue;
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
            final String distance = TextUtils.getMatch(row, GCConstants.PATTERN_SEARCH_DIRECTION_DISTANCE, false, 2, null, false);
            if (distance != null) {
                cache.setDistance(DistanceParser.parseDistance(distance,
                        !Settings.useImperialUnits()));
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
                if (matcherTbs.groupCount() > 0) {
                    try {
                        cache.setInventoryItems(Integer.parseInt(matcherTbs.group(1)));
                    } catch (final NumberFormatException e) {
                        Log.e("Error parsing trackables count", e);
                    }
                    inventoryPre = matcherTbs.group(2);
                }
            }

            if (StringUtils.isNotBlank(inventoryPre)) {
                assert inventoryPre != null;
                final MatcherWrapper matcherTbsInside = new MatcherWrapper(GCConstants.PATTERN_SEARCH_TRACKABLESINSIDE, inventoryPre);
                while (matcherTbsInside.find()) {
                    if (matcherTbsInside.groupCount() == 2 &&
                            matcherTbsInside.group(2) != null &&
                            !matcherTbsInside.group(2).equalsIgnoreCase("premium member only cache") &&
                            cache.getInventoryItems() <= 0) {
                        cache.setInventoryItems(1);
                    }
                }
            }

            // premium cache
            cache.setPremiumMembersOnly(row.contains("/images/icons/16/premium_only.png"));

            // found it
            cache.setFound(row.contains("/images/icons/16/found.png") || row.contains("uxUserLogDate\" class=\"Success\""));

            // id
            String result = TextUtils.getMatch(row, GCConstants.PATTERN_SEARCH_ID, null);
            if (result != null) {
                cache.setCacheId(result);
                cids.add(cache.getCacheId());
            }

            // favorite count
            try {
                result = getNumberString(TextUtils.getMatch(row, GCConstants.PATTERN_SEARCH_FAVORITE, false, 1, null, true));
                if (result != null) {
                    cache.setFavoritePoints(Integer.parseInt(result));
                }
            } catch (final NumberFormatException e) {
                Log.w("GCParser.parseSearch: Failed to parse favorite count", e);
            }

            caches.add(cache);
        }
        searchResult.addAndPutInCache(caches);

        // total caches found
        try {
            final String result = TextUtils.getMatch(page, GCConstants.PATTERN_SEARCH_TOTALCOUNT, false, 1, null, true);
            if (result != null) {
                searchResult.setTotalCountGC(Integer.parseInt(result) - excludedCaches);
            }
        } catch (final NumberFormatException e) {
            Log.w("GCParser.parseSearch: Failed to parse cache count", e);
        }

        if (!cids.isEmpty() && Settings.isGCPremiumMember()) {
            Log.i("Trying to get .loc for " + cids.size() + " caches");
            final Observable<Set<Geocache>> storedCaches = Observable.defer(new Func0<Observable<Set<Geocache>>>() {
                @Override
                public Observable<Set<Geocache>> call() {
                    return Observable.just(DataStore.loadCaches(Geocache.getGeocodes(caches), LoadFlags.LOAD_CACHE_OR_DB));
                }
            }).subscribeOn(Schedulers.io()).cache();
            storedCaches.subscribe();  // Force asynchronous start of database loading

            try {
                // get coordinates for parsed caches
                final Parameters params = new Parameters(
                        "__EVENTTARGET", "",
                        "__EVENTARGUMENT", "");
                GCLogin.putViewstates(params, searchResult.getViewstates());
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

                    if (StringUtils.contains(coordinates, "You have not agreed to the license agreement. The license agreement is required before you can start downloading GPX or LOC files from Geocaching.com")) {
                        Log.i("User has not agreed to the license agreement. Can\'t download .loc file.");
                        searchResult.setError(StatusCode.UNAPPROVED_LICENSE);
                        return searchResult;
                    }

                    LocParser.parseLoc(coordinates, storedCaches.toBlocking().single());
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
    static SearchResult parseCache(final String page, final CancellableHandler handler) {
        final ImmutablePair<StatusCode, Geocache> parsed = parseCacheFromText(page, handler);
        // attention: parseCacheFromText already stores implicitly through searchResult.addCache
        if (parsed.left != StatusCode.NO_ERROR) {
            return new SearchResult(parsed.left);
        }

        final Geocache cache = parsed.right;
        getExtraOnlineInfo(cache, page, handler);
        // too late: it is already stored through parseCacheFromText
        cache.setDetailedUpdatedNow();
        if (CancellableHandler.isCancelled(handler)) {
            return null;
        }

        // save full detailed caches
        CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_cache);
        DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));

        // update progress message so user knows we're still working. This is more of a place holder than
        // actual indication of what the program is doing
        CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_render);
        return new SearchResult(cache);
    }

    @NonNull
    static SearchResult parseAndSaveCacheFromText(final String page, @Nullable final CancellableHandler handler) {
        final ImmutablePair<StatusCode, Geocache> parsed = parseCacheFromText(page, handler);
        final SearchResult result = new SearchResult(parsed.left);
        if (parsed.left == StatusCode.NO_ERROR) {
            result.addAndPutInCache(Collections.singletonList(parsed.right));
            DataStore.saveLogs(parsed.right.getGeocode(), getLogs(parseUserToken(page), Logs.ALL).toBlocking().toIterable());
        }
        return result;
    }

    /**
     * Parse cache from text and return either an error code or a cache object in a pair. Note that inline logs are
     * not parsed nor saved, while the cache itself is.
     *
     * @param pageIn
     *            the page text to parse
     * @param handler
     *            the handler to send the progress notifications to
     * @return a pair, with a {@link StatusCode} on the left, and a non-null cache object on the right
     *         iff the status code is {@link StatusCode#NO_ERROR}.
     */
    @NonNull
    private static ImmutablePair<StatusCode, Geocache> parseCacheFromText(final String pageIn, @Nullable final CancellableHandler handler) {
        CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_details);

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

        if (pageIn.contains(GCConstants.STRING_PREMIUMONLY_1) || pageIn.contains(GCConstants.STRING_PREMIUMONLY_2)) {
            return ImmutablePair.of(StatusCode.PREMIUM_ONLY, null);
        }

        final String cacheName = Html.fromHtml(TextUtils.getMatch(pageIn, GCConstants.PATTERN_NAME, true, "")).toString();
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
        final String status = TextUtils.getMatch(page, GCConstants.PATTERN_STATUS, "");
        cache.setDisabled(containsStatus(status, GCConstants.STATUS_DISABLED));

        cache.setArchived(containsStatus(status, GCConstants.STATUS_ARCHIVED));

        cache.setPremiumMembersOnly(TextUtils.matches(page, GCConstants.PATTERN_PREMIUMMEMBERS));

        cache.setFavorite(TextUtils.matches(page, GCConstants.PATTERN_IS_FAVORITE));

        // cache geocode
        cache.setGeocode(TextUtils.getMatch(page, GCConstants.PATTERN_GEOCODE, true, cache.getGeocode()));

        // cache id
        cache.setCacheId(TextUtils.getMatch(page, GCConstants.PATTERN_CACHEID, true, cache.getCacheId()));

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

        // cache found
        cache.setFound(TextUtils.matches(page, GCConstants.PATTERN_FOUND));

        // cache type
        cache.setType(CacheType.getByGuid(TextUtils.getMatch(page, GCConstants.PATTERN_TYPE, true, cache.getType().id)));

        // on watchlist
        cache.setOnWatchlist(TextUtils.matches(page, GCConstants.PATTERN_WATCHLIST));

        // latitude and longitude. Can only be retrieved if user is logged in
        String latlon = TextUtils.getMatch(page, GCConstants.PATTERN_LATLON, true, "");
        if (StringUtils.isNotEmpty(latlon)) {
            try {
                cache.setCoords(new Geopoint(latlon));
                cache.setReliableLatLon(true);
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
        cache.setPersonalNote(personalNoteWithLineBreaks);

        // cache short description
        cache.setShortDescription(TextUtils.getMatch(page, GCConstants.PATTERN_SHORTDESC, true, ""));

        // cache description
        final String longDescription = TextUtils.getMatch(page, GCConstants.PATTERN_DESC, true, "");
        String relatedWebPage = TextUtils.getMatch(page, GCConstants.PATTERN_RELATED_WEB_PAGE, true, "");
        if (StringUtils.isNotEmpty(relatedWebPage)) {
            relatedWebPage = String.format("<br/><br/><a href=\"%s\"><b>%s</b></a>", relatedWebPage, relatedWebPage);
        }
        cache.setDescription(longDescription + relatedWebPage);

        // cache attributes
        try {
            final List<String> attributes = new ArrayList<>();
            final String attributesPre = TextUtils.getMatch(page, GCConstants.PATTERN_ATTRIBUTES, true, null);
            if (attributesPre != null) {
                final MatcherWrapper matcherAttributesInside = new MatcherWrapper(GCConstants.PATTERN_ATTRIBUTESINSIDE, attributesPre);

                while (matcherAttributesInside.find()) {
                    if (matcherAttributesInside.groupCount() > 1 && !matcherAttributesInside.group(2).equalsIgnoreCase("blank")) {
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
            if (CancellableHandler.isCancelled(handler)) {
                return UNKNOWN_PARSE_ERROR;
            }
            CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_spoilers);

            final MatcherWrapper matcherSpoilersInside = new MatcherWrapper(GCConstants.PATTERN_SPOILER_IMAGE, page);

            while (matcherSpoilersInside.find()) {
                // the original spoiler URL (include .../display/... contains a low-resolution image
                // if we shorten the URL we get the original-resolution image
                final String url = matcherSpoilersInside.group(1).replace("/display", "");

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

        // cache inventory
        try {
            final MatcherWrapper matcherInventory = new MatcherWrapper(GCConstants.PATTERN_INVENTORY, page);
            if (matcherInventory.find()) {
                final String inventoryPre = matcherInventory.group();

                final ArrayList<Trackable> inventory = new ArrayList<>();
                if (StringUtils.isNotBlank(inventoryPre)) {
                    final MatcherWrapper matcherInventoryInside = new MatcherWrapper(GCConstants.PATTERN_INVENTORYINSIDE, inventoryPre);

                    while (matcherInventoryInside.find()) {
                        if (matcherInventoryInside.groupCount() > 0) {
                            final Trackable inventoryItem = new Trackable();
                            inventoryItem.forceSetBrand(TrackableBrand.TRAVELBUG);
                            inventoryItem.setGuid(matcherInventoryInside.group(1));
                            inventoryItem.setName(matcherInventoryInside.group(2));

                            inventory.add(inventoryItem);
                        }
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
        cache.setWaypoints(Collections.<Waypoint> emptyList(), false);

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

        int wpBegin = page.indexOf("<table class=\"Table\" id=\"ctl00_ContentBody_Waypoints\">");
        if (wpBegin != -1) { // parse waypoints
            if (CancellableHandler.isCancelled(handler)) {
                return UNKNOWN_PARSE_ERROR;
            }
            CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_waypoints);

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
                    if (wp.length < 8) {
                        Log.e("GCParser.cacheParseFromText: not enough waypoint columns in table");
                        continue;
                    }

                    // waypoint name
                    // res is null during the unit tests
                    final String name = TextUtils.getMatch(wp[6], GCConstants.PATTERN_WPNAME, true, 1, CgeoApplication.getInstance().getString(R.string.waypoint), true);

                    // waypoint type
                    final String resulttype = TextUtils.getMatch(wp[3], GCConstants.PATTERN_WPTYPE, null);

                    final Waypoint waypoint = new Waypoint(name, WaypointType.findById(resulttype), false);

                    // waypoint prefix
                    waypoint.setPrefix(TextUtils.getMatch(wp[4], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, true, 2, waypoint.getPrefix(), false));

                    // waypoint lookup
                    waypoint.setLookup(TextUtils.getMatch(wp[5], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, true, 2, waypoint.getLookup(), false));

                    // waypoint latitude and longitude
                    latlon = Html.fromHtml(TextUtils.getMatch(wp[7], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, false, 2, "", false)).toString().trim();
                    if (!StringUtils.startsWith(latlon, "???")) {
                        waypoint.setCoords(new Geopoint(latlon));
                    }

                    if (wpItems.length >= j) {
                        final String[] wpNote = StringUtils.splitByWholeSeparator(wpItems[j + 1], "<td");
                        assert wpNote != null;
                        if (wpNote.length < 4) {
                            Log.d("GCParser.cacheParseFromText: not enough waypoint columns in table to extract note");
                            continue;
                        }

                        // waypoint note
                        waypoint.setNote(TextUtils.getMatch(wpNote[3], GCConstants.PATTERN_WPNOTE, waypoint.getNote()));
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

    private static boolean containsStatus(final String status, @NonNull final List<String> patterns) {
        for (final String pattern : patterns) {
            if (StringUtils.contains(status, pattern)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static String getNumberString(final String numberWithPunctuation) {
        return StringUtils.replaceChars(numberWithPunctuation, ".,", "");
    }

    @Nullable
    public static SearchResult searchByNextPage(final SearchResult search) {
        if (search == null) {
            return null;
        }

        final String url = search.getUrl();
        if (StringUtils.isBlank(url)) {
            Log.e("GCParser.searchByNextPage: No url found");
            return search;
        }

        final String[] viewstates = search.getViewstates();
        if (GCLogin.isEmpty(viewstates)) {
            Log.e("GCParser.searchByNextPage: No viewstate given");
            return search;
        }

        final Parameters params = new Parameters(
                "__EVENTTARGET", "ctl00$ContentBody$pgrBottom$ctl08",
                "__EVENTARGUMENT", "");
        GCLogin.putViewstates(params, viewstates);

        final String page = GCLogin.getInstance().postRequestLogged(url, params);
        if (!GCLogin.getInstance().getLoginStatus(page)) {
            Log.e("GCParser.postLogTrackable: Can not log in geocaching");
            return search;
        }

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.searchByNextPage: No data from server");
            return search;
        }

        final SearchResult searchResult = parseSearch(url, page);
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.w("GCParser.searchByNextPage: No cache parsed");
            return search;
        }

        // search results don't need to be filtered so load GCVote ratings here
        GCVote.loadRatings(new ArrayList<>(searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB)));

        // save to application
        search.setError(searchResult.getError());
        search.setViewstates(searchResult.getViewstates());
        for (final String geocode : searchResult.getGeocodes()) {
            search.addGeocode(geocode);
        }
        return search;
    }

    /**
     * Possibly hide caches found or hidden by user. This mutates its params argument when possible.
     *
     * @param params the parameters to mutate, or null to create a new Parameters if needed
     * @param my {@code true} if the user's caches must be forcibly included regardless of their settings
     * @return the original params if not null, maybe augmented with f=1, or a new Parameters with f=1 or null otherwise
     */
    private static Parameters addFToParams(final Parameters params, final boolean my) {
        if (!my && Settings.isExcludeMyCaches()) {
            if (params == null) {
                return new Parameters("f", "1");
            }
            params.put("f", "1");
            Log.i("Skipping caches found or hidden by user.");
        }

        return params;
    }

    @Nullable
    private static SearchResult searchByAny(@NonNull final CacheType cacheType, final boolean my, final Parameters params) {
        insertCacheType(params, cacheType);

        final String uri = "https://www.geocaching.com/seek/nearest.aspx";
        final Parameters paramsWithF = addFToParams(params, my);
        final String page = GCLogin.getInstance().getRequestLogged(uri, paramsWithF);

        if (StringUtils.isBlank(page)) {
            Log.w("GCParser.searchByAny: No data from server");
            return null;
        }
        assert page != null;

        final String fullUri = uri + "?" + paramsWithF;
        final SearchResult searchResult = parseSearch(fullUri, page);
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.w("GCParser.searchByAny: No cache parsed");
            return searchResult;
        }

        final SearchResult search = searchResult.filterSearchResults(Settings.isExcludeDisabledCaches(), cacheType);

        GCLogin.getInstance().getLoginStatus(page);

        return search;
    }

    public static SearchResult searchByCoords(@NonNull final Geopoint coords, @NonNull final CacheType cacheType) {
        final Parameters params = new Parameters("lat", Double.toString(coords.getLatitude()), "lng", Double.toString(coords.getLongitude()));
        return searchByAny(cacheType, false, params);
    }

    static SearchResult searchByKeyword(@NonNull final String keyword, @NonNull final CacheType cacheType) {
        if (StringUtils.isBlank(keyword)) {
            Log.e("GCParser.searchByKeyword: No keyword given");
            return null;
        }

        final Parameters params = new Parameters("key", keyword);
        return searchByAny(cacheType, false, params);
    }

    private static boolean isSearchForMyCaches(final String userName) {
        if (userName.equalsIgnoreCase(Settings.getGcCredentials().getUserName())) {
            Log.i("Overriding users choice because of self search, downloading all caches.");
            return true;
        }
        return false;
    }

    public static SearchResult searchByUsername(final String userName, @NonNull final CacheType cacheType) {
        if (StringUtils.isBlank(userName)) {
            Log.e("GCParser.searchByUsername: No user name given");
            return null;
        }

        final Parameters params = new Parameters("ul", userName);

        return searchByAny(cacheType, isSearchForMyCaches(userName), params);
    }

    public static SearchResult searchByPocketQuery(final String pocketGuid, @NonNull final CacheType cacheType) {
        if (StringUtils.isBlank(pocketGuid)) {
            Log.e("GCParser.searchByPocket: No guid name given");
            return null;
        }

        final Parameters params = new Parameters("pq", pocketGuid);

        return searchByAny(cacheType, false, params);
    }

    public static SearchResult searchByOwner(final String userName, @NonNull final CacheType cacheType) {
        if (StringUtils.isBlank(userName)) {
            Log.e("GCParser.searchByOwner: No user name given");
            return null;
        }

        final Parameters params = new Parameters("u", userName);
        return searchByAny(cacheType, isSearchForMyCaches(userName), params);
    }

    @Nullable
    public static Trackable searchTrackable(final String geocode, final String guid, final String id) {
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
        assert page != null;

        final Trackable trackable = parseTrackable(page, geocode);
        if (trackable == null) {
            Log.w("GCParser.searchTrackable: No trackable parsed");
            return null;
        }

        return trackable;
    }

    /**
     * Observable that fetches a list of pocket queries. Returns a single element (which may be an empty list).
     * Executes on the network scheduler.
     */
    public static final Observable<List<PocketQuery>> searchPocketQueryListObservable = Observable.defer(new Func0<Observable<List<PocketQuery>>>() {
        @Override
        public Observable<List<PocketQuery>> call() {
            final Parameters params = new Parameters();

            final String page = GCLogin.getInstance().getRequestLogged("https://www.geocaching.com/pocket/default.aspx", params);
            if (StringUtils.isBlank(page)) {
                Log.e("GCParser.searchPocketQueryList: No data from server");
                return Observable.just(Collections.<PocketQuery>emptyList());
            }

            try {
                final Document document = Jsoup.parse(page);
                final List<PocketQuery> list = new ArrayList<>();
                final Map<String, PocketQuery> downloadablePocketQueries = getDownloadablePocketQueries(document);
                list.addAll(downloadablePocketQueries.values());

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
                        final PocketQuery pocketQuery = new PocketQuery(guid, name, -1, false, 0, -1);
                        list.add(pocketQuery);
                    }
                }

                Collections.sort(list, new Comparator<PocketQuery>() {

                    @Override
                    public int compare(final PocketQuery left, final PocketQuery right) {
                        return TextUtils.COLLATOR.compare(left.getName(), right.getName());
                    }
                });

                return Observable.just(list);
            } catch (final Exception e) {
                Log.e("GCParser.searchPocketQueryList: error parsing parsing html page", e);
                return Observable.error(e);
            }
        }
    }).subscribeOn(AndroidRxUtils.networkScheduler);

    /**
     * Reads the downloadable pocket queries from the uxOfflinePQTable
     *
     * @param document
     *            the page as Document
     *
     * @return Map with downloadable PQs keyed by guid
     */
    @NonNull
    private static Map<String, PocketQuery> getDownloadablePocketQueries(final Document document) throws Exception {
        final Map<String, PocketQuery> downloadablePocketQueries = new HashMap<>();

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

            final PocketQuery pocketQuery = new PocketQuery(guid, name, count, true, lastGeneration, daysRemaining);
            downloadablePocketQueries.put(guid, pocketQuery);
        }

        return downloadablePocketQueries;
    }

    @NonNull
    static ImmutablePair<StatusCode, String> postLog(final String geocode, final String cacheid, final String[] viewstates,
                                                     final LogType logType, final int year, final int month, final int day,
                                                     final String log, final List<TrackableLog> trackables,
                                                     final boolean addToFavorites) {
        if (GCLogin.isEmpty(viewstates)) {
            Log.e("GCParser.postLog: No viewstate given");
            return new ImmutablePair<>(StatusCode.LOG_POST_ERROR, "");
        }

        if (StringUtils.isBlank(log)) {
            Log.e("GCParser.postLog: No log text given");
            return new ImmutablePair<>(StatusCode.NO_LOG_TEXT, "");
        }

        final String logInfo = log.replace("\n", "\r\n").trim(); // windows' eol and remove leading and trailing whitespaces

        Log.i("Trying to post log for cache #" + cacheid + " - action: " + logType
                + "; date: " + year + "." + month + "." + day + ", log: " + logInfo
                + "; trackables: " + (trackables != null ? trackables.size() : "0"));

        final Parameters params = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "__LASTFOCUS", "",
                "ctl00$ContentBody$LogBookPanel1$ddLogType", Integer.toString(logType.id),
                "ctl00$ContentBody$LogBookPanel1$uxDateVisited", GCLogin.formatGcCustomDate(year, month, day),
                "ctl00$ContentBody$LogBookPanel1$uxDateVisited$Month", Integer.toString(month),
                "ctl00$ContentBody$LogBookPanel1$uxDateVisited$Day", Integer.toString(day),
                "ctl00$ContentBody$LogBookPanel1$uxDateVisited$Year", Integer.toString(year),
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged", String.format(Locale.ENGLISH, "%02d", month) + '/' + String.format(Locale.ENGLISH, "%02d", day) + '/' + String.format(Locale.ENGLISH, "%04d", year),
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Month", Integer.toString(month),
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Day", Integer.toString(day),
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Year", Integer.toString(year),
                "ctl00$ContentBody$LogBookPanel1$LogButton", "Submit Log Entry",
                "ctl00$ContentBody$LogBookPanel1$uxLogInfo", logInfo,
                "ctl00$ContentBody$LogBookPanel1$btnSubmitLog", "Submit Log Entry",
                "ctl00$ContentBody$LogBookPanel1$uxLogCreationSource", "Old",
                "ctl00$ContentBody$uxVistOtherListingGC", "");

        if (addToFavorites) {
            params.put("ctl00$ContentBody$LogBookPanel1$chkAddToFavorites", "on");
        }

        GCLogin.putViewstates(params, viewstates);
        if (trackables != null && !trackables.isEmpty()) { //  we have some trackables to proceed
            final StringBuilder hdnSelected = new StringBuilder();

            for (final TrackableLog tb : trackables) {
                if (tb.action != LogTypeTrackable.DO_NOTHING && tb.brand == TrackableBrand.TRAVELBUG) {
                    hdnSelected.append(Integer.toString(tb.id));
                    hdnSelected.append(tb.action.action);
                    hdnSelected.append(',');
                }
            }

            params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnSelectedActions", hdnSelected.toString(), // selected trackables
                    "ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnCurrentFilter", "");
        }

        final String uri = new Uri.Builder().scheme("https").authority("www.geocaching.com").path("/seek/log.aspx").encodedQuery("ID=" + cacheid).build().toString();
        final GCLogin gcLogin = GCLogin.getInstance();
        String page = gcLogin.postRequestLogged(uri, params);
        if (!gcLogin.getLoginStatus(page)) {
            Log.e("GCParser.postLog: Cannot log in geocaching");
            return new ImmutablePair<>(StatusCode.NOT_LOGGED_IN, "");
        }

        // maintenance, archived needs to be confirmed

        final MatcherWrapper matcher = new MatcherWrapper(GCConstants.PATTERN_MAINTENANCE, page);

        try {
            if (matcher.find() && matcher.groupCount() > 0) {
                final String[] viewstatesConfirm = GCLogin.getViewstates(page);

                if (GCLogin.isEmpty(viewstatesConfirm)) {
                    Log.e("GCParser.postLog: No viewstate for confirm log");
                    return new ImmutablePair<>(StatusCode.LOG_POST_ERROR, "");
                }

                params.clear();
                GCLogin.putViewstates(params, viewstatesConfirm);
                params.put("__EVENTTARGET", "");
                params.put("__EVENTARGUMENT", "");
                params.put("__LASTFOCUS", "");
                params.put("ctl00$ContentBody$LogBookPanel1$btnConfirm", "Yes");
                params.put("ctl00$ContentBody$LogBookPanel1$uxLogInfo", logInfo);
                params.put("ctl00$ContentBody$uxVistOtherListingGC", "");
                if (trackables != null && !trackables.isEmpty()) { //  we have some trackables to proceed
                    final StringBuilder hdnSelected = new StringBuilder();

                    for (final TrackableLog tb : trackables) {
                        final String action = Integer.toString(tb.id) + tb.action.action;
                        final StringBuilder paramText = new StringBuilder("ctl00$ContentBody$LogBookPanel1$uxTrackables$repTravelBugs$ctl");

                        if (tb.ctl < 10) {
                            paramText.append('0');
                        }
                        paramText.append(tb.ctl).append("$ddlAction");
                        params.put(paramText.toString(), action);
                        if (tb.action != LogTypeTrackable.DO_NOTHING) {
                            hdnSelected.append(action);
                            hdnSelected.append(',');
                        }
                    }

                    params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnSelectedActions", hdnSelected.toString()); // selected trackables
                    params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnCurrentFilter", "");
                }

                page = Network.getResponseData(Network.postRequest(uri, params));
            }
        } catch (final RuntimeException e) {
            Log.e("GCParser.postLog.confirm", e);
        }

        if (page == null) {
            Log.e("GCParser.postLog: didn't get response");
            return new ImmutablePair<>(StatusCode.LOG_POST_ERROR, "");
        }

        try {

            final MatcherWrapper matcherOk = new MatcherWrapper(GCConstants.PATTERN_OK1, page);
            if (matcherOk.find()) {
                Log.i("Log successfully posted to cache #" + cacheid);

                if (geocode != null) {
                    DataStore.saveVisitDate(geocode);
                }

                gcLogin.getLoginStatus(page);
                // the log-successful-page contains still the old value
                if (gcLogin.getActualCachesFound() >= 0) {
                    gcLogin.setActualCachesFound(gcLogin.getActualCachesFound() + (logType.isFoundLog() ? 1 : 0));
                }

                final String logID = TextUtils.getMatch(page, GCConstants.PATTERN_LOG_IMAGE_UPLOAD, "");

                return new ImmutablePair<>(StatusCode.NO_ERROR, logID);
            }
        } catch (final Exception e) {
            Log.e("GCParser.postLog.check", e);
        }

        Log.e("GCParser.postLog: Failed to post log because of unknown error");
        return new ImmutablePair<>(StatusCode.LOG_POST_ERROR, "");
    }

    /**
     * Upload an image to a log that has already been posted
     *
     * @param logId
     *            the ID of the log to upload the image to. Found on page returned when log is uploaded
     * @param image
     *            The Image Object
     * @return status code to indicate success or failure
     */
    @NonNull
    static ImmutablePair<StatusCode, String> uploadLogImage(final String logId, @NonNull final Image image) {
        final String uri = new Uri.Builder().scheme("https").authority("www.geocaching.com").path("/seek/upload.aspx").encodedQuery("LID=" + logId).build().toString();

        final String page = GCLogin.getInstance().getRequestLogged(uri, null);
        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.uploadLogImage: No data from server");
            return new ImmutablePair<>(StatusCode.UNKNOWN_ERROR, null);
        }
        assert page != null;

        final String[] viewstates = GCLogin.getViewstates(page);

        final Parameters uploadParams = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "ctl00$ContentBody$ImageUploadControl1$uxFileCaption", image.getTitle(),
                "ctl00$ContentBody$ImageUploadControl1$uxFileDesc", image.getDescription(),
                "ctl00$ContentBody$ImageUploadControl1$uxUpload", "Upload");
        GCLogin.putViewstates(uploadParams, viewstates);

        final String response = Network.getResponseData(Network.postRequest(uri, uploadParams, "ctl00$ContentBody$ImageUploadControl1$uxFileUpload", "image/jpeg", image.getFile()));

        if (response == null) {
            Log.e("GCParser.uploadLogImage: didn't get response for image upload");
            return ImmutablePair.of(StatusCode.LOGIMAGE_POST_ERROR, null);
        }

        final MatcherWrapper matcherUrl = new MatcherWrapper(GCConstants.PATTERN_IMAGE_UPLOAD_URL, response);

        if (matcherUrl.find()) {
            Log.i("Logimage successfully uploaded.");
            final String uploadedImageUrl = matcherUrl.group(1);
            return ImmutablePair.of(StatusCode.NO_ERROR, uploadedImageUrl);
        }
        Log.e("GCParser.uploadLogImage: Failed to upload image because of unknown error");

        return ImmutablePair.of(StatusCode.LOGIMAGE_POST_ERROR, null);
    }

    /**
     * Post a log to GC.com.
     *
     * @return status code of the upload and ID of the log
     */
    @NonNull
    public static StatusCode postLogTrackable(final String tbid, final String trackingCode, final String[] viewstates,
            final LogTypeTrackable logType, final int year, final int month, final int day, final String log) {
        if (GCLogin.isEmpty(viewstates)) {
            Log.e("GCParser.postLogTrackable: No viewstate given");
            return StatusCode.LOG_POST_ERROR;
        }

        if (StringUtils.isBlank(log)) {
            Log.e("GCParser.postLogTrackable: No log text given");
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
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged", Integer.toString(month) + "/" + Integer.toString(day) + "/" + Integer.toString(year),
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
     * @param cache
     *            the cache to add
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    static boolean addToWatchlist(@NonNull final Geocache cache) {
        final String uri = "https://www.geocaching.com/my/watchlist.aspx?w=" + cache.getCacheId();
        final String page = GCLogin.getInstance().postRequestLogged(uri, null);

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.addToWatchlist: No data from server");
            return false; // error
        }

        final boolean guidOnPage = isGuidContainedInPage(cache, page);
        if (guidOnPage) {
            Log.i("GCParser.addToWatchlist: cache is on watchlist");
            cache.setOnWatchlist(true);
        } else {
            Log.e("GCParser.addToWatchlist: cache is not on watchlist");
        }
        // WatchListCount
        final String watchListPage = GCLogin.getInstance().postRequestLogged(cache.getLongUrl(), null);
        cache.setWatchlistCount(getWatchListCount(watchListPage));
        return guidOnPage; // on watchlist (=added) / else: error
    }

    /**
     * This method extracts the amount of people watching on a geocache out of the HTMl website passed to it
     * @param page Page containing the information about howm many people watching on geocache
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
     * @param cache
     *            the cache to remove
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    static boolean removeFromWatchlist(@NonNull final Geocache cache) {
        final String uri = "https://www.geocaching.com/my/watchlist.aspx?ds=1&action=rem&id=" + cache.getCacheId();
        String page = GCLogin.getInstance().postRequestLogged(uri, null);

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.removeFromWatchlist: No data from server");
            return false; // error
        }

        // removing cache from list needs approval by hitting "Yes" button
        final Parameters params = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "ctl00$ContentBody$btnYes", "Yes");
        GCLogin.transferViewstates(page, params);

        page = Network.getResponseData(Network.postRequest(uri, params));
        final boolean guidOnPage = isGuidContainedInPage(cache, page);
        if (!guidOnPage) {
            Log.i("GCParser.removeFromWatchlist: cache removed from watchlist");
            cache.setOnWatchlist(false);
        } else {
            Log.e("GCParser.removeFromWatchlist: cache not removed from watchlist");
        }

        // WatchListCount
        final String watchListPage = GCLogin.getInstance().postRequestLogged(cache.getLongUrl(), null);
        cache.setWatchlistCount(getWatchListCount(watchListPage));
        return !guidOnPage; // on watch list (=error) / not on watch list
    }

    /**
     * Checks if a page contains the guid of a cache
     *
     * @param cache the geocache
     * @param page
     *            the page to search in, may be null
     * @return true if the page contains the guid of the cache, false otherwise
     */
    private static boolean isGuidContainedInPage(@NonNull final Geocache cache, final String page) {
        if (StringUtils.isBlank(page) || StringUtils.isBlank(cache.getGuid())) {
            return false;
        }
        return Pattern.compile(cache.getGuid(), Pattern.CASE_INSENSITIVE).matcher(page).find();
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
     * @param cache
     *            the cache to add
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

        final String uri = "https://www.geocaching.com/datastore/favorites.svc/update?u=" + userToken + "&f=" + Boolean.toString(add);

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
     * @param cache
     *            the cache to remove
     * @return {@code false} if an error occurred, {@code true} otherwise
     */
    static boolean removeFromFavorites(@NonNull final Geocache cache) {
        return changeFavorite(cache, false);
    }

    /**
     * Parse a trackable HTML description into a Trackable object
     *
     * @param page
     *            the HTML page to parse, already processed through {@link TextUtils#replaceWhitespace}
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
        trackable.setName(Html.fromHtml(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, true, "")).toString());

        // trackable type
        if (StringUtils.isNotBlank(trackable.getName())) {
            trackable.setType(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_TYPE, true, trackable.getType()));
        }

        // trackable owner name
        try {
            final MatcherWrapper matcherOwner = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_OWNER, page);
            if (matcherOwner.find() && matcherOwner.groupCount() > 0) {
                trackable.setOwnerGuid(matcherOwner.group(1));
                trackable.setOwner(matcherOwner.group(2).trim());
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
            if (matcherSpottedCache.find() && matcherSpottedCache.groupCount() > 0) {
                trackable.setSpottedGuid(matcherSpottedCache.group(1));
                trackable.setSpottedName(matcherSpottedCache.group(2).trim());
                trackable.setSpottedType(Trackable.SPOTTED_CACHE);
            }

            final MatcherWrapper matcherSpottedUser = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_SPOTTEDUSER, page);
            if (matcherSpottedUser.find() && matcherSpottedUser.groupCount() > 0) {
                trackable.setSpottedGuid(matcherSpottedUser.group(1));
                trackable.setSpottedName(matcherSpottedUser.group(2).trim());
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

        // trackable distance
        final String distance = TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_DISTANCE, false, null);
        if (distance != null) {
            try {
                trackable.setDistance(DistanceParser.parseDistance(distance,
                        !Settings.useImperialUnits()));
            } catch (final NumberFormatException e) {
                Log.e("GCParser.parseTrackable: Failed to parse distance", e);
            }
        }

        // trackable goal
        trackable.setGoal(convertLinks(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GOAL, true, trackable.getGoal())));

        // trackable details & image
        try {
            final MatcherWrapper matcherDetailsImage = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_DETAILSIMAGE, page);
            if (matcherDetailsImage.find() && matcherDetailsImage.groupCount() > 0) {
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

        // trackable may be locked
        if (page.contains(GCConstants.TRACKABLE_IS_LOCKED)) {
            trackable.setIsLocked();
        }

        // trackable logs
        try {
            final MatcherWrapper matcherLogs = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE_LOG, page);
            /*
             * 1. Type (image)
             * 2. Date
             * 3. Author
             * 4. Cache-GUID
             * 5. <ignored> (strike-through property for ancient caches)
             * 6. Cache-name
             * 7. Log text
             */
            while (matcherLogs.find()) {
                long date = 0;
                try {
                    date = GCLogin.parseGcCustomDate(matcherLogs.group(2)).getTime();
                } catch (final ParseException ignored) {
                }

                final LogEntry.Builder logDoneBuilder = new LogEntry.Builder()
                        .setAuthor(Html.fromHtml(matcherLogs.group(3)).toString().trim())
                        .setDate(date)
                        .setLogType(LogType.getByIconName(matcherLogs.group(1)))
                        .setLog(matcherLogs.group(7).trim());

                if (matcherLogs.group(4) != null && matcherLogs.group(6) != null) {
                    logDoneBuilder.setCacheGuid(matcherLogs.group(4));
                    logDoneBuilder.setCacheName(matcherLogs.group(6));
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
     * @param userToken
     *            the user token extracted from the web page
     * @param logType
     *            the logType to request
     * @return Observable<LogEntry> The logs
     */
    private static Observable<LogEntry> getLogs(final String userToken, final Logs logType) {
        if (userToken.isEmpty()) {
            Log.e("GCParser.loadLogsFromDetails: unable to extract userToken");
            return Observable.empty();
        }

        return Observable.defer(new Func0<Observable<LogEntry>>() {
            @Override
            public Observable<LogEntry> call() {
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
                    return parseLogs(logType != Logs.ALL, responseStream);
                } catch (final Exception e) {
                    Log.e("unable to read logs", e);
                    return Observable.empty();
                }
            }
        }).subscribeOn(AndroidRxUtils.networkScheduler);
    }

    private static Observable<LogEntry> parseLogs(final boolean markAsFriendsLog, final InputStream responseStream) {
        return Observable.create(new OnSubscribe<LogEntry>() {
            @Override
            public void call(final Subscriber<? super LogEntry> subscriber) {
                try {
                    final ObjectNode resp = (ObjectNode) JsonUtils.reader.readTree(responseStream);
                    if (!resp.path("status").asText().equals("success")) {
                        Log.w("GCParser.loadLogsFromDetails: status is " + resp.path("status").asText("[absent]"));
                        subscriber.onCompleted();
                        return;
                    }

                    final ArrayNode data = (ArrayNode) resp.get("data");
                    for (final JsonNode entry: data) {
                        final String logType = entry.path("LogType").asText();

                        final long date;
                        try {
                            date = GCLogin.parseGcCustomDate(entry.get("Visited").asText()).getTime();
                        } catch (ParseException | NullPointerException e) {
                            Log.e("GCParser.loadLogsFromDetails: failed to parse log date", e);
                            continue;
                        }

                        // TODO: we should update our log data structure to be able to record
                        // proper coordinates, and make them clickable. In the meantime, it is
                        // better to integrate those coordinates into the text rather than not
                        // display them at all.
                        final String latLon = entry.path("LatLonString").asText();
                        final String logText = (StringUtils.isEmpty(latLon) ? "" : (latLon + "<br/><br/>")) + TextUtils.removeControlCharacters(entry.path("LogText").asText());
                        final LogEntry.Builder logDoneBuilder = new LogEntry.Builder()
                                .setAuthor(TextUtils.removeControlCharacters(entry.path("UserName").asText()))
                                .setDate(date)
                                .setLogType(LogType.getByType(logType))
                                .setLog(logText)
                                .setFound(entry.path("GeocacheFindCount").asInt())
                                .setFriend(markAsFriendsLog);

                        final ArrayNode images = (ArrayNode) entry.get("Images");
                        for (final JsonNode image: images) {
                            final String url = "https://imgcdn.geocaching.com/cache/log/large/" + image.path("FileName").asText();
                            final String title = TextUtils.removeControlCharacters(image.path("Name").asText());
                            final String description = image.path("Descr").asText();
                            final Image logImage = new Image.Builder().setUrl(url).setTitle(title).setDescription(description).build();
                            logDoneBuilder.addLogImage(logImage);
                        }

                        subscriber.onNext(logDoneBuilder.build());
                    }
                } catch (final IOException e) {
                    Log.w("GCParser.loadLogsFromDetails: Failed to parse cache logs", e);
                }
                subscriber.onCompleted();
            }
        });
    }

    @NonNull
    static List<LogType> parseTypes(final String page) {
        if (StringUtils.isEmpty(page)) {
            return Collections.emptyList();
        }

        final List<LogType> types = new ArrayList<>();

        final MatcherWrapper typeBoxMatcher = new MatcherWrapper(GCConstants.PATTERN_TYPEBOX, page);
        if (typeBoxMatcher.find() && typeBoxMatcher.groupCount() > 0) {
            final String typesText = typeBoxMatcher.group(1);
            final MatcherWrapper typeMatcher = new MatcherWrapper(GCConstants.PATTERN_TYPE2, typesText);
            while (typeMatcher.find()) {
                if (typeMatcher.groupCount() > 1) {
                    try {
                        final int type = Integer.parseInt(typeMatcher.group(2));
                        if (type > 0) {
                            types.add(LogType.getById(type));
                        }
                    } catch (final NumberFormatException e) {
                        Log.e("Error parsing log types", e);
                    }
                }
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
        if (typeBoxMatcher.find() && typeBoxMatcher.groupCount() > 0) {
            final String typesText = typeBoxMatcher.group(1);
            final MatcherWrapper typeMatcher = new MatcherWrapper(GCConstants.PATTERN_TYPE2, typesText);
            while (typeMatcher.find()) {
                if (typeMatcher.groupCount() > 1) {
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
        }
        return types;
    }

    static List<TrackableLog> parseTrackableLog(final String page) {
        if (StringUtils.isEmpty(page)) {
            return Collections.emptyList();
        }

        String table = StringUtils.substringBetween(page, "<table id=\"tblTravelBugs\"", "</table>");

        // if no trackables are currently in the account, the table is not available, so return an empty list instead of null
        if (StringUtils.isBlank(table)) {
            return Collections.emptyList();
        }

        table = StringUtils.substringBetween(table, "<tbody>", "</tbody>");
        if (StringUtils.isBlank(table)) {
            Log.e("GCParser.parseTrackableLog: tbody not found on page");
            return Collections.emptyList();
        }

        final List<TrackableLog> trackableLogs = new ArrayList<>();

        final MatcherWrapper trackableMatcher = new MatcherWrapper(GCConstants.PATTERN_TRACKABLE, page);
        while (trackableMatcher.find()) {
            if (trackableMatcher.groupCount() > 0) {

                final String trackCode = trackableMatcher.group(1);
                final String name = Html.fromHtml(trackableMatcher.group(2)).toString();
                try {
                    final Integer ctl = Integer.valueOf(trackableMatcher.group(3));
                    final Integer id = Integer.valueOf(trackableMatcher.group(5));
                    if (trackCode != null && ctl != null && id != null) {
                        final TrackableLog entry = new TrackableLog("", trackCode, name, id, ctl, TrackableBrand.TRAVELBUG);

                        Log.i("Trackable in inventory (#" + entry.ctl + "/" + entry.id + "): " + entry.trackCode + " - " + entry.name);
                        trackableLogs.add(entry);
                    }
                } catch (final NumberFormatException e) {
                    Log.e("GCParser.parseTrackableLog", e);
                }
            }
        }

        return trackableLogs;
    }

    /**
     * Insert the right cache type restriction in parameters
     *
     * @param params
     *            the parameters to insert the restriction into
     * @param cacheType
     *            the type of cache, or null to include everything
     */
    private static void insertCacheType(final Parameters params, final CacheType cacheType) {
        params.put("tx", cacheType.guid);
    }

    private static void getExtraOnlineInfo(@NonNull final Geocache cache, final String page, final CancellableHandler handler) {
        // This method starts the page parsing for logs in the background, as well as retrieve the friends and own logs
        // if requested. It merges them and stores them in the background, while the rating is retrieved if needed and
        // stored. Then we wait for the log merging and saving to be completed before returning.
        if (CancellableHandler.isCancelled(handler)) {
            return;
        }

        CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_logs);
        final String userToken = parseUserToken(page);
        final Observable<LogEntry> logs = getLogs(userToken, Logs.ALL);
        final Observable<LogEntry> ownLogs = getLogs(userToken, Logs.OWN).cache();
        final Observable<LogEntry> specialLogs = Settings.isFriendLogsWanted() ?
                Observable.merge(getLogs(userToken, Logs.FRIENDS), ownLogs) : Observable.<LogEntry>empty();
        final Observable<List<LogEntry>> mergedLogs = Observable.zip(logs.toList(), specialLogs.toList(),
                new Func2<List<LogEntry>, List<LogEntry>, List<LogEntry>>() {
                    @Override
                    public List<LogEntry> call(final List<LogEntry> logEntries, final List<LogEntry> specialLogEntries) {
                        mergeFriendsLogs(logEntries, specialLogEntries);
                        return logEntries;
                    }
                }).cache();
        mergedLogs.subscribe(new Action1<List<LogEntry>>() {
                                 @Override
                                 public void call(final List<LogEntry> logEntries) {
                                     DataStore.saveLogs(cache.getGeocode(), logEntries);
                                 }
                             });
        if (cache.isFound() && cache.getVisitedDate() == 0) {
            ownLogs.subscribe(new Action1<LogEntry>() {
                @Override
                public void call(final LogEntry logEntry) {
                    if (logEntry.getType().isFoundLog()) {
                        cache.setVisitedDate(logEntry.date);
                    }
                }
            });
        }

        if (Settings.isRatingWanted() && !CancellableHandler.isCancelled(handler)) {
            CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_gcvote);
            final GCVoteRating rating = GCVote.getRating(cache.getGuid(), cache.getGeocode());
            if (rating != null) {
                cache.setRating(rating.getRating());
                cache.setVotes(rating.getVotes());
                cache.setMyVote(rating.getMyVote());
            }
        }

        // Wait for completion of logs parsing, retrieving and merging
        mergedLogs.toCompletable().await();
    }

    /**
     * Merge log entries and mark them as friends logs (personal and friends) to identify
     * them on friends/personal logs tab.
     *
     * @param mergedLogs
     *            the list to merge logs with
     * @param logsToMerge
     *            the list of logs to merge
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

    public static int getFavoritePoints(final String page) {
        if (page != null) {
            final String favCount = TextUtils.getMatch(page, GCConstants.PATTERN_LOG_FAVORITE_POINTS, false, 1, null, true);
            if (favCount != null) {
                return Integer.parseInt(favCount);
            }
        }
        return 0;
    }
}
