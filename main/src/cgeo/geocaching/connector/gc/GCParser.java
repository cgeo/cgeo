package cgeo.geocaching.connector.gc;

import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgImage;
import cgeo.geocaching.cgTrackable;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
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
import cgeo.geocaching.geopoint.DistanceParser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.ui.DirectionImage;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.LazyInitializedList;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.text.Html;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;

public abstract class GCParser {
    private final static SimpleDateFormat dateTbIn1 = new SimpleDateFormat("EEEEE, dd MMMMM yyyy", Locale.ENGLISH); // Saturday, 28 March 2009
    private final static SimpleDateFormat dateTbIn2 = new SimpleDateFormat("EEEEE, MMMMM dd, yyyy", Locale.ENGLISH); // Saturday, March 28, 2009

    private static SearchResult parseSearch(final String url, final String pageContent, final boolean showCaptcha) {
        if (StringUtils.isBlank(pageContent)) {
            Log.e("GCParser.parseSearch: No page given");
            return null;
        }

        final List<String> cids = new ArrayList<String>();
        String recaptchaChallenge = null;
        String recaptchaText = null;
        String page = pageContent;

        final SearchResult searchResult = new SearchResult();
        searchResult.setUrl(url);
        searchResult.viewstates = Login.getViewstates(page);

        // recaptcha
        AbstractSearchThread thread = AbstractSearchThread.getCurrentInstance();
        if (showCaptcha) {
            String recaptchaJsParam = BaseUtils.getMatch(page, GCConstants.PATTERN_SEARCH_RECAPTCHA, false, null);

            if (recaptchaJsParam != null) {
                final Parameters params = new Parameters("k", recaptchaJsParam.trim());
                final String recaptchaJs = Network.getResponseData(Network.getRequest("http://www.google.com/recaptcha/api/challenge", params));

                if (StringUtils.isNotBlank(recaptchaJs)) {
                    recaptchaChallenge = BaseUtils.getMatch(recaptchaJs, GCConstants.PATTERN_SEARCH_RECAPTCHACHALLENGE, true, 1, null, true);
                }
            }
            if (thread != null && StringUtils.isNotBlank(recaptchaChallenge)) {
                thread.setChallenge(recaptchaChallenge);
                thread.notifyNeed();
            }
        }

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
        int endPos = page.indexOf("ctl00_ContentBody_UnitTxt");
        if (startPos == -1 || endPos == -1) {
            Log.e("GCParser.parseSearch: ID \"ctl00_ContentBody_UnitTxt\" not found on page");
            return null;
        }

        page = page.substring(startPos + 1, endPos - startPos + 1); // cut between <table> and </table>

        final String[] rows = page.split("<tr class=");
        final int rows_count = rows.length;

        for (int z = 1; z < rows_count; z++) {
            final cgCache cache = new cgCache();
            String row = rows[z];

            // check for cache type presence
            if (!row.contains("images/wpttypes")) {
                continue;
            }

            try {
                final Matcher matcherGuidAndDisabled = GCConstants.PATTERN_SEARCH_GUIDANDDISABLED.matcher(row);

                while (matcherGuidAndDisabled.find()) {
                    if (matcherGuidAndDisabled.groupCount() > 0) {
                        cache.setGuid(matcherGuidAndDisabled.group(1));
                        if (matcherGuidAndDisabled.group(4) != null) {
                            cache.setName(Html.fromHtml(matcherGuidAndDisabled.group(4).trim()).toString());
                        }
                        if (matcherGuidAndDisabled.group(6) != null) {
                            cache.setLocation(Html.fromHtml(matcherGuidAndDisabled.group(6).trim()).toString());
                        }

                        final String attr = matcherGuidAndDisabled.group(2);
                        if (attr != null) {
                            cache.setDisabled(attr.contains("Strike"));

                            cache.setArchived(attr.contains("OldWarning"));
                        }
                    }
                }
            } catch (Exception e) {
                // failed to parse GUID and/or Disabled
                Log.w("GCParser.parseSearch: Failed to parse GUID and/or Disabled data");
            }

            if (Settings.isExcludeDisabledCaches() && (cache.isDisabled() || cache.isArchived())) {
                // skip disabled and archived caches
                continue;
            }

            String inventoryPre = null;

            cache.setGeocode(BaseUtils.getMatch(row, GCConstants.PATTERN_SEARCH_GEOCODE, true, 1, cache.getGeocode(), true));

            // cache type
            cache.setType(CacheType.getByPattern(BaseUtils.getMatch(row, GCConstants.PATTERN_SEARCH_TYPE, true, 1, null, true)));

            // cache direction - image
            if (Settings.getLoadDirImg()) {
                cache.setDirectionImg(Network.decode(BaseUtils.getMatch(row, GCConstants.PATTERN_SEARCH_DIRECTION, true, 1, cache.getDirectionImg(), true)));
            }

            // cache inventory
            final Matcher matcherTbs = GCConstants.PATTERN_SEARCH_TRACKABLES.matcher(row);
            while (matcherTbs.find()) {
                if (matcherTbs.groupCount() > 0) {
                    try {
                        cache.setInventoryItems(Integer.parseInt(matcherTbs.group(1)));
                    } catch (NumberFormatException e) {
                        Log.e("Error parsing trackables count", e);
                    }
                    inventoryPre = matcherTbs.group(2);
                }
            }

            if (StringUtils.isNotBlank(inventoryPre)) {
                final Matcher matcherTbsInside = GCConstants.PATTERN_SEARCH_TRACKABLESINSIDE.matcher(inventoryPre);
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
            cache.setFound(row.contains("/images/icons/icon_smile"));

            // own it
            cache.setOwn(row.contains("/images/silk/star.png"));

            // id
            String result = BaseUtils.getMatch(row, GCConstants.PATTERN_SEARCH_ID, null);
            if (null != result) {
                cache.setCacheId(result);
                cids.add(cache.getCacheId());
            }

            // favorite count
            try {
                result = BaseUtils.getMatch(row, GCConstants.PATTERN_SEARCH_FAVORITE, false, 1, null, true);
                if (null != result) {
                    cache.setFavoritePoints(Integer.parseInt(result));
                }
            } catch (NumberFormatException e) {
                Log.w("GCParser.parseSearch: Failed to parse favourite count");
            }

            searchResult.addCache(cache);
        }

        // total caches found
        try {
            String result = BaseUtils.getMatch(page, GCConstants.PATTERN_SEARCH_TOTALCOUNT, false, 1, null, true);
            if (null != result) {
                searchResult.setTotal(Integer.parseInt(result));
            }
        } catch (NumberFormatException e) {
            Log.w("GCParser.parseSearch: Failed to parse cache count");
        }

        if (thread != null && recaptchaChallenge != null) {
            if (thread.getText() == null) {
                thread.waitForUser();
            }

            recaptchaText = thread.getText();
        }

        if (cids.size() > 0 && (Settings.isPremiumMember() || showCaptcha) && (recaptchaChallenge == null || StringUtils.isNotBlank(recaptchaText))) {
            Log.i("Trying to get .loc for " + cids.size() + " caches");

            try {
                // get coordinates for parsed caches
                final Parameters params = new Parameters(
                        "__EVENTTARGET", "",
                        "__EVENTARGUMENT", "");
                if (ArrayUtils.isNotEmpty(searchResult.viewstates)) {
                    params.put("__VIEWSTATE", searchResult.viewstates[0]);
                    if (searchResult.viewstates.length > 1) {
                        for (int i = 1; i < searchResult.viewstates.length; i++) {
                            params.put("__VIEWSTATE" + i, searchResult.viewstates[i]);
                        }
                        params.put("__VIEWSTATEFIELDCOUNT", String.valueOf(searchResult.viewstates.length));
                    }
                }
                for (String cid : cids) {
                    params.put("CID", cid);
                }

                if (recaptchaChallenge != null && StringUtils.isNotBlank(recaptchaText)) {
                    params.put("recaptcha_challenge_field", recaptchaChallenge);
                    params.put("recaptcha_response_field", recaptchaText);
                }
                params.put("ctl00$ContentBody$uxDownloadLoc", "Download Waypoints");

                final String coordinates = Network.getResponseData(Network.postRequest("http://www.geocaching.com/seek/nearest.aspx", params), false);

                if (StringUtils.isNotBlank(coordinates)) {
                    if (coordinates.contains("You have not agreed to the license agreement. The license agreement is required before you can start downloading GPX or LOC files from Geocaching.com")) {
                        Log.i("User has not agreed to the license agreement. Can\'t download .loc file.");

                        searchResult.setError(StatusCode.UNAPPROVED_LICENSE);

                        return searchResult;
                    }
                }

                LocParser.parseLoc(searchResult, coordinates);

            } catch (Exception e) {
                Log.e("GCParser.parseSearch.CIDs: " + e.toString());
            }
        }

        // get direction images
        if (Settings.getLoadDirImg()) {
            final Set<cgCache> caches = searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB);
            for (cgCache cache : caches) {
                if (cache.getCoords() == null && StringUtils.isNotEmpty(cache.getDirectionImg())) {
                    DirectionImage.getDrawable(cache.getGeocode(), cache.getDirectionImg());
                }
            }
        }

        return searchResult;
    }

    static SearchResult parseCache(final String page, final CancellableHandler handler) {
        final SearchResult searchResult = parseCacheFromText(page, handler);
        if (searchResult != null && !searchResult.getGeocodes().isEmpty()) {
            final cgCache cache = searchResult.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
            getExtraOnlineInfo(cache, page, handler);
            cache.setUpdated(System.currentTimeMillis());
            cache.setDetailedUpdate(cache.getUpdated());
            cache.setDetailed(true);
            if (CancellableHandler.isCancelled(handler)) {
                return null;
            }

            // save full detailed caches
            CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_cache);
            cgData.saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));

            // update progress message so user knows we're still working. This is more of a place holder than
            // actual indication of what the program is doing
            CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_render);
        }
        return searchResult;
    }

    static SearchResult parseCacheFromText(final String page, final CancellableHandler handler) {
        CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_details);

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.parseCache: No page given");
            return null;
        }

        final SearchResult searchResult = new SearchResult();

        if (page.contains(GCConstants.STRING_UNPUBLISHED_OTHER) || page.contains(GCConstants.STRING_UNPUBLISHED_OWNER)) {
            searchResult.setError(StatusCode.UNPUBLISHED_CACHE);
            return searchResult;
        }

        if (page.contains(GCConstants.STRING_PREMIUMONLY_1) || page.contains(GCConstants.STRING_PREMIUMONLY_2)) {
            searchResult.setError(StatusCode.PREMIUM_ONLY);
            return searchResult;
        }

        final String cacheName = Html.fromHtml(BaseUtils.getMatch(page, GCConstants.PATTERN_NAME, true, "")).toString();
        if (GCConstants.STRING_UNKNOWN_ERROR.equalsIgnoreCase(cacheName)) {
            searchResult.setError(StatusCode.UNKNOWN_ERROR);
            return searchResult;
        }

        final cgCache cache = new cgCache();
        cache.setDisabled(page.contains(GCConstants.STRING_DISABLED));

        cache.setArchived(page.contains(GCConstants.STRING_ARCHIVED));

        cache.setPremiumMembersOnly(BaseUtils.matches(page, GCConstants.PATTERN_PREMIUMMEMBERS));

        cache.setFavorite(BaseUtils.matches(page, GCConstants.PATTERN_FAVORITE));

        // cache geocode
        cache.setGeocode(BaseUtils.getMatch(page, GCConstants.PATTERN_GEOCODE, true, cache.getGeocode()));

        // cache id
        cache.setCacheId(BaseUtils.getMatch(page, GCConstants.PATTERN_CACHEID, true, cache.getCacheId()));

        // cache guid
        cache.setGuid(BaseUtils.getMatch(page, GCConstants.PATTERN_GUID, true, cache.getGuid()));

        // name
        cache.setName(cacheName);

        // owner real name
        cache.setOwnerUserId(Network.decode(BaseUtils.getMatch(page, GCConstants.PATTERN_OWNER_USERID, true, cache.getOwnerUserId())));

        cache.setOwn(StringUtils.equalsIgnoreCase(cache.getOwnerUserId(), Settings.getUsername()));

        String tableInside = page;

        int pos = tableInside.indexOf(GCConstants.STRING_CACHEDETAILS);
        if (pos == -1) {
            Log.e("GCParser.parseCache: ID \"cacheDetails\" not found on page");
            return null;
        }

        tableInside = tableInside.substring(pos);

        if (StringUtils.isNotBlank(tableInside)) {
            // cache terrain
            String result = BaseUtils.getMatch(tableInside, GCConstants.PATTERN_TERRAIN, true, null);
            if (result != null) {
                try {
                    cache.setTerrain(Float.parseFloat(StringUtils.replaceChars(result, '_', '.')));
                } catch (NumberFormatException e) {
                    Log.e("Error parsing terrain value", e);
                }
            }

            // cache difficulty
            result = BaseUtils.getMatch(tableInside, GCConstants.PATTERN_DIFFICULTY, true, null);
            if (result != null) {
                try {
                    cache.setDifficulty(Float.parseFloat(StringUtils.replaceChars(result, '_', '.')));
                } catch (NumberFormatException e) {
                    Log.e("Error parsing difficulty value", e);
                }
            }

            // owner
            cache.setOwnerDisplayName(StringEscapeUtils.unescapeHtml4(BaseUtils.getMatch(tableInside, GCConstants.PATTERN_OWNER_DISPLAYNAME, true, cache.getOwnerDisplayName())));

            // hidden
            try {
                String hiddenString = BaseUtils.getMatch(tableInside, GCConstants.PATTERN_HIDDEN, true, null);
                if (StringUtils.isNotBlank(hiddenString)) {
                    cache.setHidden(Login.parseGcCustomDate(hiddenString));
                }
                if (cache.getHiddenDate() == null) {
                    // event date
                    hiddenString = BaseUtils.getMatch(tableInside, GCConstants.PATTERN_HIDDENEVENT, true, null);
                    if (StringUtils.isNotBlank(hiddenString)) {
                        cache.setHidden(Login.parseGcCustomDate(hiddenString));
                    }
                }
            } catch (ParseException e) {
                // failed to parse cache hidden date
                Log.w("GCParser.parseCache: Failed to parse cache hidden (event) date");
            }

            // favourite
            try {
                cache.setFavoritePoints(Integer.parseInt(BaseUtils.getMatch(tableInside, GCConstants.PATTERN_FAVORITECOUNT, true, "0")));
            } catch (NumberFormatException e) {
                Log.e("Error parsing favourite count", e);
            }

            // cache size
            cache.setSize(CacheSize.getById(BaseUtils.getMatch(tableInside, GCConstants.PATTERN_SIZE, true, CacheSize.NOT_CHOSEN.id)));
        }

        // cache found
        cache.setFound(BaseUtils.matches(page, GCConstants.PATTERN_FOUND) || BaseUtils.matches(page, GCConstants.PATTERN_FOUND_ALTERNATIVE));

        // cache found date
        try {
            final String foundDateString = BaseUtils.getMatch(page, GCConstants.PATTERN_FOUND_DATE, true, null);
            if (StringUtils.isNotBlank(foundDateString)) {
                cache.setVisitedDate(Login.parseGcCustomDate(foundDateString).getTime());
            }
        } catch (ParseException e) {
            // failed to parse cache found date
            Log.w("GCParser.parseCache: Failed to parse cache found date");
        }

        // cache type
        cache.setType(CacheType.getByPattern(BaseUtils.getMatch(page, GCConstants.PATTERN_TYPE, true, cache.getType().id)));

        // on watchlist
        cache.setOnWatchlist(BaseUtils.matches(page, GCConstants.PATTERN_WATCHLIST));

        // latitude and longitude. Can only be retrieved if user is logged in
        cache.setLatlon(BaseUtils.getMatch(page, GCConstants.PATTERN_LATLON, true, cache.getLatlon()));
        if (StringUtils.isNotEmpty(cache.getLatlon())) {
            try {
                cache.setCoords(new Geopoint(cache.getLatlon()));
                cache.setReliableLatLon(true);
            } catch (Geopoint.GeopointException e) {
                Log.w("GCParser.parseCache: Failed to parse cache coordinates: " + e.toString());
            }
        }

        // cache location
        cache.setLocation(BaseUtils.getMatch(page, GCConstants.PATTERN_LOCATION, true, cache.getLocation()));

        // cache hint
        String result = BaseUtils.getMatch(page, GCConstants.PATTERN_HINT, false, null);
        if (result != null) {
            // replace linebreak and paragraph tags
            String hint = GCConstants.PATTERN_LINEBREAK.matcher(result).replaceAll("\n");
            if (hint != null) {
                cache.setHint(StringUtils.replace(hint, "</p>", "").trim());
            }
        }

        cache.checkFields();

        // cache personal note
        cache.setPersonalNote(BaseUtils.getMatch(page, GCConstants.PATTERN_PERSONALNOTE, true, cache.getPersonalNote()));

        // cache short description
        cache.setShortdesc(BaseUtils.getMatch(page, GCConstants.PATTERN_SHORTDESC, true, cache.getShortdesc()));

        // cache description
        cache.setDescription(BaseUtils.getMatch(page, GCConstants.PATTERN_DESC, true, ""));

        // cache attributes
        try {
            final String attributesPre = BaseUtils.getMatch(page, GCConstants.PATTERN_ATTRIBUTES, true, null);
            if (null != attributesPre) {
                final Matcher matcherAttributesInside = GCConstants.PATTERN_ATTRIBUTESINSIDE.matcher(attributesPre);

                final ArrayList<String> attributes = new ArrayList<String>();
                while (matcherAttributesInside.find()) {
                    if (matcherAttributesInside.groupCount() > 1 && !matcherAttributesInside.group(2).equalsIgnoreCase("blank")) {
                        // by default, use the tooltip of the attribute
                        String attribute = matcherAttributesInside.group(2).toLowerCase(Locale.US);

                        // if the image name can be recognized, use the image name as attribute
                        String imageName = matcherAttributesInside.group(1).trim();
                        if (imageName.length() > 0) {
                            int start = imageName.lastIndexOf('/');
                            int end = imageName.lastIndexOf('.');
                            if (start >= 0 && end >= 0) {
                                attribute = imageName.substring(start + 1, end).replace('-', '_').toLowerCase(Locale.US);
                            }
                        }
                        attributes.add(attribute);
                    }
                }
                cache.setAttributes(attributes);
            }
        } catch (Exception e) {
            // failed to parse cache attributes
            Log.w("GCParser.parseCache: Failed to parse cache attributes");
        }

        // cache spoilers
        try {
            if (CancellableHandler.isCancelled(handler)) {
                return null;
            }
            CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_spoilers);

            final Matcher matcherSpoilersInside = GCConstants.PATTERN_SPOILER_IMAGE.matcher(page);

            while (matcherSpoilersInside.find()) {
                // the original spoiler URL (include .../display/... contains a low-resolution image
                // if we shorten the URL we get the original-resolution image
                String url = matcherSpoilersInside.group(1).replace("/display", "");

                String title = null;
                if (matcherSpoilersInside.group(2) != null) {
                    title = matcherSpoilersInside.group(2);
                }
                String description = null;
                if (matcherSpoilersInside.group(3) != null) {
                    description = matcherSpoilersInside.group(3);
                }
                cache.addSpoiler(new cgImage(url, title, description));
            }
        } catch (Exception e) {
            // failed to parse cache spoilers
            Log.w("GCParser.parseCache: Failed to parse cache spoilers");
        }

        // cache inventory
        try {
            cache.setInventoryItems(0);

            final Matcher matcherInventory = GCConstants.PATTERN_INVENTORY.matcher(page);
            if (matcherInventory.find()) {
                if (cache.getInventory() == null) {
                    cache.setInventory(new ArrayList<cgTrackable>());
                }

                if (matcherInventory.groupCount() > 1) {
                    final String inventoryPre = matcherInventory.group(2);

                    if (StringUtils.isNotBlank(inventoryPre)) {
                        final Matcher matcherInventoryInside = GCConstants.PATTERN_INVENTORYINSIDE.matcher(inventoryPre);

                        while (matcherInventoryInside.find()) {
                            if (matcherInventoryInside.groupCount() > 0) {
                                final cgTrackable inventoryItem = new cgTrackable();
                                inventoryItem.setGuid(matcherInventoryInside.group(1));
                                inventoryItem.setName(matcherInventoryInside.group(2));

                                cache.getInventory().add(inventoryItem);
                                cache.setInventoryItems(cache.getInventoryItems() + 1);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // failed to parse cache inventory
            Log.w("GCParser.parseCache: Failed to parse cache inventory (2)");
        }

        // cache logs counts
        try
        {
            final String countlogs = BaseUtils.getMatch(page, GCConstants.PATTERN_COUNTLOGS, true, null);
            if (null != countlogs) {
                final Matcher matcherLog = GCConstants.PATTERN_COUNTLOG.matcher(countlogs);

                while (matcherLog.find())
                {
                    String typeStr = matcherLog.group(1);
                    String countStr = matcherLog.group(2).replaceAll("[.,]", "");

                    if (StringUtils.isNotBlank(typeStr)
                            && LogType.UNKNOWN != LogType.getByIconName(typeStr)
                            && StringUtils.isNotBlank(countStr)) {
                        cache.getLogCounts().put(LogType.getByIconName(typeStr), Integer.parseInt(countStr));
                    }
                }
            }
        } catch (Exception e)
        {
            // failed to parse logs
            Log.w("GCParser.parseCache: Failed to parse cache log count");
        }

        // waypoints - reset collection
        cache.setWaypoints(Collections.<cgWaypoint> emptyList(), false);

        // add waypoint for original coordinates in case of user-modified listing-coordinates
        try {
            final String originalCoords = BaseUtils.getMatch(page, GCConstants.PATTERN_LATLON_ORIG, false, null);

            if (null != originalCoords) {
                final cgWaypoint waypoint = new cgWaypoint(cgeoapplication.getInstance().getString(R.string.cache_coordinates_original), WaypointType.WAYPOINT, false);
                waypoint.setCoords(new Geopoint(originalCoords));
                cache.addOrChangeWaypoint(waypoint, false);
                cache.setUserModifiedCoords(true);
            }
        } catch (Geopoint.GeopointException e) {
        }

        int wpBegin;
        int wpEnd;

        wpBegin = page.indexOf("<table class=\"Table\" id=\"ctl00_ContentBody_Waypoints\">");
        if (wpBegin != -1) { // parse waypoints
            if (CancellableHandler.isCancelled(handler)) {
                return null;
            }
            CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_waypoints);

            String wpList = page.substring(wpBegin);

            wpEnd = wpList.indexOf("</p>");
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

                final String[] wpItems = wpList.split("<tr");

                String[] wp;
                for (int j = 1; j < wpItems.length; j++) {
                    wp = wpItems[j].split("<td");

                    // waypoint name
                    // res is null during the unit tests
                    final String name = BaseUtils.getMatch(wp[6], GCConstants.PATTERN_WPNAME, true, 1, cgeoapplication.getInstance().getString(R.string.waypoint), true);

                    // waypoint type
                    final String resulttype = BaseUtils.getMatch(wp[3], GCConstants.PATTERN_WPTYPE, null);

                    final cgWaypoint waypoint = new cgWaypoint(name, WaypointType.findById(resulttype), false);

                    // waypoint prefix
                    waypoint.setPrefix(BaseUtils.getMatch(wp[4], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, true, 2, waypoint.getPrefix(), false));

                    // waypoint lookup
                    waypoint.setLookup(BaseUtils.getMatch(wp[5], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, true, 2, waypoint.getLookup(), false));

                    // waypoint latitude and logitude
                    String latlon = Html.fromHtml(BaseUtils.getMatch(wp[7], GCConstants.PATTERN_WPPREFIXORLOOKUPORLATLON, false, 2, "", false)).toString().trim();
                    if (!StringUtils.startsWith(latlon, "???")) {
                        waypoint.setLatlon(latlon);
                        waypoint.setCoords(new Geopoint(latlon));
                    }

                    j++;
                    if (wpItems.length > j) {
                        wp = wpItems[j].split("<td");
                    }

                    // waypoint note
                    waypoint.setNote(BaseUtils.getMatch(wp[3], GCConstants.PATTERN_WPNOTE, waypoint.getNote()));

                    cache.addOrChangeWaypoint(waypoint, false);
                }
            }
        }

        cache.parseWaypointsFromNote();

        // logs
        cache.setLogs(loadLogsFromDetails(page, cache, false, true));

        // last check for necessary cache conditions
        if (StringUtils.isBlank(cache.getGeocode())) {
            searchResult.setError(StatusCode.UNKNOWN_ERROR);
            return searchResult;
        }

        searchResult.addCache(cache);
        return searchResult;
    }

    public static SearchResult searchByNextPage(final SearchResult search, boolean showCaptcha) {
        if (search == null) {
            return search;
        }
        final String[] viewstates = search.getViewstates();

        final String url = search.getUrl();

        if (StringUtils.isBlank(url)) {
            Log.e("GCParser.searchByNextPage: No url found");
            return search;
        }

        if (Login.isEmpty(viewstates)) {
            Log.e("GCParser.searchByNextPage: No viewstate given");
            return search;
        }

        // As in the original code, remove the query string
        final String uri = Uri.parse(url).buildUpon().query(null).build().toString();

        final Parameters params = new Parameters(
                "__EVENTTARGET", "ctl00$ContentBody$pgrBottom$ctl08",
                "__EVENTARGUMENT", "");
        Login.putViewstates(params, viewstates);

        final String page = Login.postRequestLogged(uri, params);
        if (!Login.getLoginStatus(page)) {
            Log.e("GCParser.postLogTrackable: Can not log in geocaching");
            return search;
        }

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.searchByNextPage: No data from server");
            return search;
        }

        final SearchResult searchResult = parseSearch(url, page, showCaptcha);
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.e("GCParser.searchByNextPage: No cache parsed");
            return search;
        }

        // search results don't need to be filtered so load GCVote ratings here
        GCVote.loadRatings(new ArrayList<cgCache>(searchResult.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB)));

        // save to application
        search.setError(searchResult.getError());
        search.setViewstates(searchResult.viewstates);
        for (String geocode : searchResult.getGeocodes()) {
            search.addGeocode(geocode);
        }
        return search;
    }

    /**
     * Possibly hide caches found or hidden by user. This mutates its params argument when possible.
     *
     * @param params
     *            the parameters to mutate, or null to create a new Parameters if needed
     * @param my
     * @param addF
     * @return the original params if not null, maybe augmented with f=1, or a new Parameters with f=1 or null otherwise
     */
    private static Parameters addFToParams(final Parameters params, final boolean my, final boolean addF) {
        if (!my && Settings.isExcludeMyCaches() && addF) {
            if (params == null) {
                return new Parameters("f", "1");
            }
            params.put("f", "1");
            Log.i("Skipping caches found or hidden by user.");
        }

        return params;
    }

    /**
     * @param cacheType
     * @param listId
     * @param showCaptcha
     * @param params
     *            the parameters to add to the request URI
     * @return
     */
    private static SearchResult searchByAny(final CacheType cacheType, final boolean my, final boolean showCaptcha, final Parameters params) {
        insertCacheType(params, cacheType);

        final String uri = "http://www.geocaching.com/seek/nearest.aspx";
        final String fullUri = uri + "?" + addFToParams(params, false, true);
        final String page = Login.getRequestLogged(uri, addFToParams(params, my, true));

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.searchByAny: No data from server");
            return null;
        }

        final SearchResult searchResult = parseSearch(fullUri, page, showCaptcha);
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.e("GCParser.searchByAny: No cache parsed");
            return searchResult;
        }

        final SearchResult search = searchResult.filterSearchResults(Settings.isExcludeDisabledCaches(), false, cacheType);

        Login.getLoginStatus(page);

        return search;
    }

    public static SearchResult searchByCoords(final Geopoint coords, final CacheType cacheType, final boolean showCaptcha) {
        final Parameters params = new Parameters("lat", Double.toString(coords.getLatitude()), "lng", Double.toString(coords.getLongitude()));
        return searchByAny(cacheType, false, showCaptcha, params);
    }

    public static SearchResult searchByKeyword(final String keyword, final CacheType cacheType, final boolean showCaptcha) {
        if (StringUtils.isBlank(keyword)) {
            Log.e("GCParser.searchByKeyword: No keyword given");
            return null;
        }

        final Parameters params = new Parameters("key", keyword);
        return searchByAny(cacheType, false, showCaptcha, params);
    }

    public static SearchResult searchByUsername(final String userName, final CacheType cacheType, final boolean showCaptcha) {
        if (StringUtils.isBlank(userName)) {
            Log.e("GCParser.searchByUsername: No user name given");
            return null;
        }

        final Parameters params = new Parameters("ul", userName);

        boolean my = false;
        if (userName.equalsIgnoreCase(Settings.getLogin().left)) {
            my = true;
            Log.i("GCParser.searchByUsername: Overriding users choice, downloading all caches.");
        }

        return searchByAny(cacheType, my, showCaptcha, params);
    }

    public static SearchResult searchByOwner(final String userName, final CacheType cacheType, final boolean showCaptcha) {
        if (StringUtils.isBlank(userName)) {
            Log.e("GCParser.searchByOwner: No user name given");
            return null;
        }

        final Parameters params = new Parameters("u", userName);
        return searchByAny(cacheType, false, showCaptcha, params);
    }

    public static SearchResult searchByAddress(final String address, final CacheType cacheType, final boolean showCaptcha) {
        if (StringUtils.isBlank(address)) {
            Log.e("GCParser.searchByAddress: No address given");
            return null;
        }
        try {
            JSONObject response = Network.requestJSON("http://www.geocaching.com/api/geocode", new Parameters("q", address));
            if (response == null) {
                return null;
            }
            if (!StringUtils.equalsIgnoreCase(response.getString("status"), "success")) {
                return null;
            }
            if (!response.has("data")) {
                return null;
            }
            JSONObject data = response.getJSONObject("data");
            if (data == null) {
                return null;
            }
            return searchByCoords(new Geopoint(data.getDouble("lat"), data.getDouble("lng")), cacheType, showCaptcha);
        } catch (JSONException e) {
            Log.w("GCParser.searchByAddress", e);
        }

        return null;
    }

    public static cgTrackable searchTrackable(final String geocode, final String guid, final String id) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid) && StringUtils.isBlank(id)) {
            Log.w("GCParser.searchTrackable: No geocode nor guid nor id given");
            return null;
        }

        cgTrackable trackable = new cgTrackable();

        final Parameters params = new Parameters();
        if (StringUtils.isNotBlank(geocode)) {
            params.put("tracker", geocode);
            trackable.setGeocode(geocode);
        } else if (StringUtils.isNotBlank(guid)) {
            params.put("guid", guid);
        } else if (StringUtils.isNotBlank(id)) {
            params.put("id", id);
        }

        final String page = Login.getRequestLogged("http://www.geocaching.com/track/details.aspx", params);

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.searchTrackable: No data from server");
            return trackable;
        }

        trackable = parseTrackable(page, geocode);
        if (trackable == null) {
            Log.e("GCParser.searchTrackable: No trackable parsed");
            return null;
        }

        return trackable;
    }

    public static StatusCode postLog(final String geocode, final String cacheid, final String[] viewstates,
            final LogType logType, final int year, final int month, final int day,
            final String log, final List<TrackableLog> trackables) {
        if (Login.isEmpty(viewstates)) {
            Log.e("GCParser.postLog: No viewstate given");
            return StatusCode.LOG_POST_ERROR;
        }

        if (StringUtils.isBlank(log)) {
            Log.e("GCParser.postLog: No log text given");
            return StatusCode.NO_LOG_TEXT;
        }

        // fix log (non-Latin characters converted to HTML entities)
        final int logLen = log.length();
        final StringBuilder logUpdated = new StringBuilder();

        for (int i = 0; i < logLen; i++) {
            char c = log.charAt(i);

            if (c > 300) {
                logUpdated.append("&#");
                logUpdated.append(Integer.toString(c));
                logUpdated.append(';');
            } else {
                logUpdated.append(c);
            }
        }

        final String logInfo = logUpdated.toString().replace("\n", "\r\n").trim(); // windows' eol and remove leading and trailing whitespaces

        if (trackables != null) {
            Log.i("Trying to post log for cache #" + cacheid + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + logInfo + "; trackables: " + trackables.size());
        } else {
            Log.i("Trying to post log for cache #" + cacheid + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + logInfo + "; trackables: 0");
        }

        final Parameters params = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "__LASTFOCUS", "",
                "ctl00$ContentBody$LogBookPanel1$ddLogType", Integer.toString(logType.id),
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged", String.format("%02d", month) + "/" + String.format("%02d", day) + "/" + String.format("%04d", year),
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Month", Integer.toString(month),
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Day", Integer.toString(day),
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Year", Integer.toString(year),
                "ctl00$ContentBody$LogBookPanel1$uxLogInfo", logInfo,
                "ctl00$ContentBody$LogBookPanel1$LogButton", "Submit Log Entry",
                "ctl00$ContentBody$uxVistOtherListingGC", "");
        Login.putViewstates(params, viewstates);
        if (trackables != null && !trackables.isEmpty()) { //  we have some trackables to proceed
            final StringBuilder hdnSelected = new StringBuilder();

            for (final TrackableLog tb : trackables) {
                if (tb.action != LogTypeTrackable.DO_NOTHING) {
                    hdnSelected.append(Integer.toString(tb.id));
                    hdnSelected.append(tb.action.action);
                    hdnSelected.append(',');
                }
            }

            params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnSelectedActions", hdnSelected.toString(), // selected trackables
                    "ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnCurrentFilter", "");
        }

        final String uri = new Uri.Builder().scheme("http").authority("www.geocaching.com").path("/seek/log.aspx").encodedQuery("ID=" + cacheid).build().toString();
        String page = Login.postRequestLogged(uri, params);
        if (!Login.getLoginStatus(page)) {
            Log.e("GCParser.postLogTrackable: Can not log in geocaching");
            return StatusCode.NOT_LOGGED_IN;
        }

        // maintenance, archived needs to be confirmed

        final Matcher matcher = GCConstants.PATTERN_MAINTENANCE.matcher(page);

        try {
            if (matcher.find() && matcher.groupCount() > 0) {
                final String[] viewstatesConfirm = Login.getViewstates(page);

                if (Login.isEmpty(viewstatesConfirm)) {
                    Log.e("GCParser.postLog: No viewstate for confirm log");
                    return StatusCode.LOG_POST_ERROR;
                }

                params.clear();
                Login.putViewstates(params, viewstatesConfirm);
                params.put("__EVENTTARGET", "");
                params.put("__EVENTARGUMENT", "");
                params.put("__LASTFOCUS", "");
                params.put("ctl00$ContentBody$LogBookPanel1$btnConfirm", "Yes");
                params.put("ctl00$ContentBody$LogBookPanel1$uxLogInfo", logInfo);
                params.put("ctl00$ContentBody$uxVistOtherListingGC", "");
                if (trackables != null && !trackables.isEmpty()) { //  we have some trackables to proceed
                    final StringBuilder hdnSelected = new StringBuilder();

                    for (TrackableLog tb : trackables) {
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
        } catch (Exception e) {
            Log.e("GCParser.postLog.confim: " + e.toString());
        }

        try {

            final Matcher matcherOk = GCConstants.PATTERN_OK1.matcher(page);
            if (matcherOk.find()) {
                Log.i("Log successfully posted to cache #" + cacheid);

                if (geocode != null) {
                    cgData.saveVisitDate(geocode);
                }

                Login.getLoginStatus(page);
                // the log-successful-page contains still the old value
                if (Login.getActualCachesFound() >= 0) {
                    Login.setActualCachesFound(Login.getActualCachesFound() + 1);
                }
                return StatusCode.NO_ERROR;
            }
        } catch (Exception e) {
            Log.e("GCParser.postLog.check: " + e.toString());
        }

        Log.e("GCParser.postLog: Failed to post log because of unknown error");
        return StatusCode.LOG_POST_ERROR;
    }

    public static StatusCode postLogTrackable(final String tbid, final String trackingCode, final String[] viewstates,
            final LogType logType, final int year, final int month, final int day, final String log) {
        if (Login.isEmpty(viewstates)) {
            Log.e("GCParser.postLogTrackable: No viewstate given");
            return StatusCode.LOG_POST_ERROR;
        }

        if (StringUtils.isBlank(log)) {
            Log.e("GCParser.postLogTrackable: No log text given");
            return StatusCode.NO_LOG_TEXT;
        }

        Log.i("Trying to post log for trackable #" + trackingCode + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + log);

        final String logInfo = log.replace("\n", "\r\n"); // windows' eol

        final Calendar currentDate = Calendar.getInstance();
        final Parameters params = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "__LASTFOCUS", "",
                "ctl00$ContentBody$LogBookPanel1$ddLogType", Integer.toString(logType.id),
                "ctl00$ContentBody$LogBookPanel1$tbCode", trackingCode);
        Login.putViewstates(params, viewstates);
        if (currentDate.get(Calendar.YEAR) == year && (currentDate.get(Calendar.MONTH) + 1) == month && currentDate.get(Calendar.DATE) == day) {
            params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged", "");
        } else {
            params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged", Integer.toString(month) + "/" + Integer.toString(day) + "/" + Integer.toString(year));
        }
        params.put(
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Day", Integer.toString(day),
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Month", Integer.toString(month),
                "ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Year", Integer.toString(year),
                "ctl00$ContentBody$LogBookPanel1$uxLogInfo", logInfo,
                "ctl00$ContentBody$LogBookPanel1$LogButton", "Submit Log Entry",
                "ctl00$ContentBody$uxVistOtherListingGC", "");

        final String uri = new Uri.Builder().scheme("http").authority("www.geocaching.com").path("/track/log.aspx").encodedQuery("wid=" + tbid).build().toString();
        final String page = Login.postRequestLogged(uri, params);
        if (!Login.getLoginStatus(page)) {
            Log.e("GCParser.postLogTrackable: Can not log in geocaching");
            return StatusCode.NOT_LOGGED_IN;
        }

        try {

            final Matcher matcherOk = GCConstants.PATTERN_OK2.matcher(page);
            if (matcherOk.find()) {
                Log.i("Log successfully posted to trackable #" + trackingCode);
                return StatusCode.NO_ERROR;
            }
        } catch (Exception e) {
            Log.e("GCParser.postLogTrackable.check: " + e.toString());
        }

        Log.e("GCParser.postLogTrackable: Failed to post log because of unknown error");
        return StatusCode.LOG_POST_ERROR;
    }

    /**
     * Adds the cache to the watchlist of the user.
     *
     * @param cache
     *            the cache to add
     * @return <code>false</code> if an error occurred, <code>true</code> otherwise
     */
    static boolean addToWatchlist(final cgCache cache) {
        final String uri = "http://www.geocaching.com/my/watchlist.aspx?w=" + cache.getCacheId();
        String page = Login.postRequestLogged(uri, null);

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.addToWatchlist: No data from server");
            return false; // error
        }

        boolean guidOnPage = cache.isGuidContainedInPage(page);
        if (guidOnPage) {
            Log.i("GCParser.addToWatchlist: cache is on watchlist");
            cache.setOnWatchlist(true);
        } else {
            Log.e("GCParser.addToWatchlist: cache is not on watchlist");
        }
        return guidOnPage; // on watchlist (=added) / else: error
    }

    /**
     * Removes the cache from the watch list
     *
     * @param cache
     *            the cache to remove
     * @return <code>false</code> if an error occurred, <code>true</code> otherwise
     */
    static boolean removeFromWatchlist(final cgCache cache) {
        final String uri = "http://www.geocaching.com/my/watchlist.aspx?ds=1&action=rem&id=" + cache.getCacheId();
        String page = Login.postRequestLogged(uri, null);

        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.removeFromWatchlist: No data from server");
            return false; // error
        }

        // removing cache from list needs approval by hitting "Yes" button
        final Parameters params = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "ctl00$ContentBody$btnYes", "Yes");
        Login.transferViewstates(page, params);

        page = Network.getResponseData(Network.postRequest(uri, params));
        boolean guidOnPage = cache.isGuidContainedInPage(page);
        if (!guidOnPage) {
            Log.i("GCParser.removeFromWatchlist: cache removed from watchlist");
            cache.setOnWatchlist(false);
        } else {
            Log.e("GCParser.removeFromWatchlist: cache not removed from watchlist");
        }
        return !guidOnPage; // on watch list (=error) / not on watch list
    }

    static String requestHtmlPage(final String geocode, final String guid, final String log, final String numlogs) {
        final Parameters params = new Parameters("decrypt", "y");
        if (StringUtils.isNotBlank(geocode)) {
            params.put("wp", geocode);
        } else if (StringUtils.isNotBlank(guid)) {
            params.put("guid", guid);
        }
        params.put("log", log);
        params.put("numlogs", numlogs);

        return Login.getRequestLogged("http://www.geocaching.com/seek/cache_details.aspx", params);
    }

    /**
     * Adds the cache to the favorites of the user.
     *
     * @param cache
     *            the cache to add
     * @return <code>false</code> if an error occurred, <code>true</code> otherwise
     */
    static boolean addToFavorites(final cgCache cache) {
        return changeFavorite(cache, true);
    }

    private static boolean changeFavorite(final cgCache cache, final boolean add) {
        final String page = requestHtmlPage(cache.getGeocode(), null, "n", "0");
        final String userToken = BaseUtils.getMatch(page, GCConstants.PATTERN_USERTOKEN, "");
        if (StringUtils.isEmpty(userToken)) {
            return false;
        }

        final String uri = "http://www.geocaching.com/datastore/favorites.svc/update?u=" + userToken + "&f=" + Boolean.toString(add);

        HttpResponse response = Network.postRequest(uri, null);

        if (response != null && response.getStatusLine().getStatusCode() == 200) {
            Log.i("GCParser.changeFavorite: cache added/removed to/from favorites");
            cache.setFavorite(add);
            cache.setFavoritePoints(cache.getFavoritePoints() + (add ? 1 : -1));
            return true;
        }
        Log.e("GCParser.changeFavorite: cache not added/removed to/from favorites");
        return false;
    }

    /**
     * Removes the cache from the Favorites
     *
     * @param cache
     *            the cache to remove
     * @return <code>false</code> if an error occurred, <code>true</code> otherwise
     */
    static boolean removeFromFavorites(final cgCache cache) {
        return changeFavorite(cache, false);
    }

    /**
     * Parse a trackable HTML description into a cgTrackable object
     *
     * @param page
     *            the HTML page to parse, already processed through {@link BaseUtils#replaceWhitespace}
     * @param app
     *            if not null, the application to use to save the trackable
     * @return the parsed trackable, or null if none could be parsed
     */
    static cgTrackable parseTrackable(final String page, final String possibleTrackingcode) {
        if (StringUtils.isBlank(page)) {
            Log.e("GCParser.parseTrackable: No page given");
            return null;
        }

        if (page.contains(GCConstants.ERROR_TB_DOES_NOT_EXIST) || page.contains(GCConstants.ERROR_TB_ARITHMETIC_OVERFLOW) || page.contains(GCConstants.ERROR_TB_ELEMENT_EXCEPTION)) {
            return null;
        }

        final cgTrackable trackable = new cgTrackable();

        // trackable geocode
        trackable.setGeocode(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GEOCODE, true, trackable.getGeocode()));

        // trackable id
        trackable.setGuid(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GUID, true, trackable.getGuid()));

        // trackable icon
        trackable.setIconUrl(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_ICON, true, trackable.getIconUrl()));

        // trackable name
        trackable.setName(Html.fromHtml(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, true, "")).toString());

        // trackable type
        if (StringUtils.isNotBlank(trackable.getName())) {
            trackable.setType(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_TYPE, true, trackable.getType()));
        }

        // trackable owner name
        try {
            final Matcher matcherOwner = GCConstants.PATTERN_TRACKABLE_OWNER.matcher(page);
            if (matcherOwner.find() && matcherOwner.groupCount() > 0) {
                trackable.setOwnerGuid(matcherOwner.group(1));
                trackable.setOwner(matcherOwner.group(2).trim());
            }
        } catch (Exception e) {
            // failed to parse trackable owner name
            Log.w("GCParser.parseTrackable: Failed to parse trackable owner name");
        }

        // trackable origin
        trackable.setOrigin(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_ORIGIN, true, trackable.getOrigin()));

        // trackable spotted
        try {
            final Matcher matcherSpottedCache = GCConstants.PATTERN_TRACKABLE_SPOTTEDCACHE.matcher(page);
            if (matcherSpottedCache.find() && matcherSpottedCache.groupCount() > 0) {
                trackable.setSpottedGuid(matcherSpottedCache.group(1));
                trackable.setSpottedName(matcherSpottedCache.group(2).trim());
                trackable.setSpottedType(cgTrackable.SPOTTED_CACHE);
            }

            final Matcher matcherSpottedUser = GCConstants.PATTERN_TRACKABLE_SPOTTEDUSER.matcher(page);
            if (matcherSpottedUser.find() && matcherSpottedUser.groupCount() > 0) {
                trackable.setSpottedGuid(matcherSpottedUser.group(1));
                trackable.setSpottedName(matcherSpottedUser.group(2).trim());
                trackable.setSpottedType(cgTrackable.SPOTTED_USER);
            }

            if (BaseUtils.matches(page, GCConstants.PATTERN_TRACKABLE_SPOTTEDUNKNOWN)) {
                trackable.setSpottedType(cgTrackable.SPOTTED_UNKNOWN);
            }

            if (BaseUtils.matches(page, GCConstants.PATTERN_TRACKABLE_SPOTTEDOWNER)) {
                trackable.setSpottedType(cgTrackable.SPOTTED_OWNER);
            }
        } catch (Exception e) {
            // failed to parse trackable last known place
            Log.w("GCParser.parseTrackable: Failed to parse trackable last known place");
        }

        // released date - can be missing on the page
        try {
            String releaseString = BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_RELEASES, false, null);
            if (releaseString != null) {
                trackable.setReleased(dateTbIn1.parse(releaseString));
                if (trackable.getReleased() == null) {
                    trackable.setReleased(dateTbIn2.parse(releaseString));
                }
            }
        } catch (ParseException e1) {
            trackable.setReleased(null);
        }

        // trackable distance
        final String distance = BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_DISTANCE, false, null);
        if (null != distance) {
            trackable.setDistance(DistanceParser.parseDistance(distance, Settings.isUseMetricUnits()));
        }

        // trackable goal
        trackable.setGoal(convertLinks(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GOAL, true, trackable.getGoal())));

        // trackable details & image
        try {
            final Matcher matcherDetailsImage = GCConstants.PATTERN_TRACKABLE_DETAILSIMAGE.matcher(page);
            if (matcherDetailsImage.find() && matcherDetailsImage.groupCount() > 0) {
                final String image = StringUtils.trim(matcherDetailsImage.group(3));
                final String details = StringUtils.trim(matcherDetailsImage.group(4));

                if (StringUtils.isNotEmpty(image)) {
                    trackable.setImage(image);
                }
                if (StringUtils.isNotEmpty(details) && !StringUtils.equals(details, "No additional details available.")) {
                    trackable.setDetails(convertLinks(details));
                }
            }
        } catch (Exception e) {
            // failed to parse trackable details & image
            Log.w("GCParser.parseTrackable: Failed to parse trackable details & image");
        }

        // trackable logs
        try {
            final Matcher matcherLogs = GCConstants.PATTERN_TRACKABLE_LOG.matcher(page);
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
                    date = Login.parseGcCustomDate(matcherLogs.group(2)).getTime();
                } catch (ParseException e) {
                }

                final LogEntry logDone = new LogEntry(
                        Html.fromHtml(matcherLogs.group(3)).toString().trim(),
                        date,
                        LogType.getByIconName(matcherLogs.group(1)),
                        matcherLogs.group(7).trim());

                if (matcherLogs.group(4) != null && matcherLogs.group(6) != null) {
                    logDone.cacheGuid = matcherLogs.group(4);
                    logDone.cacheName = matcherLogs.group(6);
                }

                // Apply the pattern for images in a trackable log entry against each full log (group(0))
                final Matcher matcherLogImages = GCConstants.PATTERN_TRACKABLE_LOG_IMAGES.matcher(matcherLogs.group(0));
                /*
                 * 1. Image URL
                 * 2. Image title
                 */
                while (matcherLogImages.find()) {
                    final cgImage logImage = new cgImage(matcherLogImages.group(1), matcherLogImages.group(2));
                    logDone.addLogImage(logImage);
                }

                trackable.getLogs().add(logDone);
            }
        } catch (Exception e) {
            // failed to parse logs
            Log.w("GCParser.parseCache: Failed to parse cache logs" + e.toString());
        }

        // tracking code
        if (!StringUtils.equalsIgnoreCase(trackable.getGeocode(), possibleTrackingcode)) {
            trackable.setTrackingcode(possibleTrackingcode);
        }

        if (cgeoapplication.getInstance() != null) {
            cgData.saveTrackable(trackable);
        }

        return trackable;
    }

    private static String convertLinks(String input) {
        if (input == null) {
            return null;
        }
        return StringUtils.replace(input, "../", GCConstants.GC_URL);
    }

    /**
     * Load logs from a cache details page.
     *
     * @param page
     *            the text of the details page
     * @param cache
     *            the cache object to put the logs in
     * @param friends
     *            retrieve friend logs
     */
    private static List<LogEntry> loadLogsFromDetails(final String page, final cgCache cache, final boolean friends, final boolean getDataFromPage) {
        String rawResponse;

        if (!getDataFromPage) {
            final Matcher userTokenMatcher = GCConstants.PATTERN_USERTOKEN.matcher(page);
            if (!userTokenMatcher.find()) {
                Log.e("GCParser.loadLogsFromDetails: unable to extract userToken");
                return null;
            }

            final String userToken = userTokenMatcher.group(1);
            final Parameters params = new Parameters(
                    "tkn", userToken,
                    "idx", "1",
                    "num", String.valueOf(GCConstants.NUMBER_OF_LOGS),
                    "decrypt", "true",
                    // "sp", Boolean.toString(personal), // personal logs
                    "sf", Boolean.toString(friends));

            final HttpResponse response = Network.getRequest("http://www.geocaching.com/seek/geocache.logbook", params);
            if (response == null) {
                Log.e("GCParser.loadLogsFromDetails: cannot log logs, response is null");
                return null;
            }
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                Log.e("GCParser.loadLogsFromDetails: error " + statusCode + " when requesting log information");
                return null;
            }
            rawResponse = Network.getResponseData(response);
            if (rawResponse == null) {
                Log.e("GCParser.loadLogsFromDetails: unable to read whole response");
                return null;
            }
        } else {
            // extract embedded JSON data from page
            rawResponse = BaseUtils.getMatch(page, GCConstants.PATTERN_LOGBOOK, "");
        }

        List<LogEntry> logs = new ArrayList<LogEntry>();

        try {
            final JSONObject resp = new JSONObject(rawResponse);
            if (!resp.getString("status").equals("success")) {
                Log.e("GCParser.loadLogsFromDetails: status is " + resp.getString("status"));
                return null;
            }

            final JSONArray data = resp.getJSONArray("data");

            for (int index = 0; index < data.length(); index++) {
                final JSONObject entry = data.getJSONObject(index);

                // FIXME: use the "LogType" field instead of the "LogTypeImage" one.
                final String logIconNameExt = entry.optString("LogTypeImage", ".gif");
                final String logIconName = logIconNameExt.substring(0, logIconNameExt.length() - 4);

                long date = 0;
                try {
                    date = Login.parseGcCustomDate(entry.getString("Visited")).getTime();
                } catch (ParseException e) {
                    Log.e("GCParser.loadLogsFromDetails: failed to parse log date.");
                }

                // TODO: we should update our log data structure to be able to record
                // proper coordinates, and make them clickable. In the meantime, it is
                // better to integrate those coordinates into the text rather than not
                // display them as all.
                final String latLon = entry.getString("LatLonString");
                final LogEntry logDone = new LogEntry(
                        entry.getString("UserName"),
                        date,
                        LogType.getByIconName(logIconName),
                        (StringUtils.isEmpty(latLon) ? "" : (latLon + "<br/><br/>")) + entry.getString("LogText"));
                logDone.found = entry.getInt("GeocacheFindCount");
                logDone.friend = friends;

                final JSONArray images = entry.getJSONArray("Images");
                for (int i = 0; i < images.length(); i++) {
                    final JSONObject image = images.getJSONObject(i);
                    String url = "http://img.geocaching.com/cache/log/" + image.getString("FileName");
                    String title = image.getString("Name");
                    final cgImage logImage = new cgImage(url, title);
                    logDone.addLogImage(logImage);
                }

                logs.add(logDone);
            }
        } catch (JSONException e) {
            // failed to parse logs
            Log.w("GCParser.loadLogsFromDetails: Failed to parse cache logs", e);
        }

        return logs;
    }

    public static List<LogType> parseTypes(String page) {
        if (StringUtils.isEmpty(page)) {
            return null;
        }

        final List<LogType> types = new ArrayList<LogType>();

        final Matcher typeBoxMatcher = GCConstants.PATTERN_TYPEBOX.matcher(page);
        String typesText = null;
        if (typeBoxMatcher.find()) {
            if (typeBoxMatcher.groupCount() > 0) {
                typesText = typeBoxMatcher.group(1);
            }
        }

        if (typesText != null) {

            final Matcher typeMatcher = GCConstants.PATTERN_TYPE2.matcher(typesText);
            while (typeMatcher.find()) {
                if (typeMatcher.groupCount() > 1) {
                    try {
                        int type = Integer.parseInt(typeMatcher.group(2));
                        if (type > 0) {
                            types.add(LogType.getById(type));
                        }
                    } catch (NumberFormatException e) {
                        Log.e("Error parsing log types", e);
                    }
                }
            }
        }

        return types;
    }

    public static List<TrackableLog> parseTrackableLog(final String page) {
        if (StringUtils.isEmpty(page)) {
            return null;
        }

        String table = StringUtils.substringBetween(page, "<table id=\"tblTravelBugs\"", "</table>");

        // if no trackables are currently in the account, the table is not available, so return an empty list instead of null
        if (StringUtils.isBlank(table)) {
            return Collections.emptyList();
        }

        table = StringUtils.substringBetween(table, "<tbody>", "</tbody>");
        if (StringUtils.isBlank(table)) {
            Log.e("GCParser.parseTrackableLog: tbody not found on page");
            return null;
        }

        final List<TrackableLog> trackableLogs = new ArrayList<TrackableLog>();

        final Matcher trackableMatcher = GCConstants.PATTERN_TRACKABLE.matcher(page);
        while (trackableMatcher.find()) {
            if (trackableMatcher.groupCount() > 0) {

                final String trackCode = trackableMatcher.group(1);
                final String name = Html.fromHtml(trackableMatcher.group(2)).toString();
                try {
                    final Integer ctl = Integer.valueOf(trackableMatcher.group(3));
                    final Integer id = Integer.valueOf(trackableMatcher.group(5));
                    if (trackCode != null && name != null && ctl != null && id != null) {
                        final TrackableLog entry = new TrackableLog(trackCode, name, id, ctl);

                        Log.i("Trackable in inventory (#" + entry.ctl + "/" + entry.id + "): " + entry.trackCode + " - " + entry.name);
                        trackableLogs.add(entry);
                    }
                } catch (NumberFormatException e) {
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
    static private void insertCacheType(final Parameters params, final CacheType cacheType) {
        params.put("tx", cacheType.guid);
    }

    private static void getExtraOnlineInfo(final cgCache cache, final String page, final CancellableHandler handler) {
        if (CancellableHandler.isCancelled(handler)) {
            return;
        }

        //cache.setLogs(loadLogsFromDetails(page, cache, false));
        if (Settings.isFriendLogsWanted()) {
            CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_logs);
            LazyInitializedList<LogEntry> allLogs = cache.getLogs();
            List<LogEntry> friendLogs = loadLogsFromDetails(page, cache, true, false);
            if (friendLogs != null) {
                for (LogEntry log : friendLogs) {
                    if (allLogs.contains(log)) {
                        allLogs.get(allLogs.indexOf(log)).friend = true;
                    } else {
                        cache.getLogs().add(log);
                    }
                }
            }
        }

        if (Settings.isElevationWanted()) {
            if (CancellableHandler.isCancelled(handler)) {
                return;
            }
            CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_elevation);
            if (cache.getCoords() != null) {
                cache.setElevation(cache.getCoords().getElevation());
            }
        }

        if (Settings.isRatingWanted()) {
            if (CancellableHandler.isCancelled(handler)) {
                return;
            }
            CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_gcvote);
            final GCVoteRating rating = GCVote.getRating(cache.getGuid(), cache.getGeocode());
            if (rating != null) {
                cache.setRating(rating.getRating());
                cache.setVotes(rating.getVotes());
                cache.setMyVote(rating.getMyVote());
            }
        }
    }

}
