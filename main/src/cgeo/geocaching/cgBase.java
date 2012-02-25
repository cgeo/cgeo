// $codepro.audit.disable logExceptions
package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.GCConnector;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
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
import cgeo.geocaching.geopoint.GeopointFormatter.Format;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.ui.DirectionImage;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class cgBase {

    private static final String passMatch = "(?<=[\\?&])[Pp]ass(w(or)?d)?=[^&#$]+";

    private final static Map<String, SimpleDateFormat> gcCustomDateFormats;
    static {
        final String[] formats = new String[] {
                "MM/dd/yyyy",
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd/MMM/yyyy",
                "MMM/dd/yyyy",
                "dd MMM yy",
                "dd/MM/yyyy"
        };

        Map<String, SimpleDateFormat> map = new HashMap<String, SimpleDateFormat>();

        for (String format : formats) {
            map.put(format, new SimpleDateFormat(format, Locale.ENGLISH));
        }

        gcCustomDateFormats = Collections.unmodifiableMap(map);
    }
    private final static SimpleDateFormat dateTbIn1 = new SimpleDateFormat("EEEEE, dd MMMMM yyyy", Locale.ENGLISH); // Saturday, 28 March 2009
    private final static SimpleDateFormat dateTbIn2 = new SimpleDateFormat("EEEEE, MMMMM dd, yyyy", Locale.ENGLISH); // Saturday, March 28, 2009
    public static String version = null;

    private static Context context;
    private static Resources res;

    private static final int NB_DOWNLOAD_RETRIES = 4;

    public static final int UPDATE_LOAD_PROGRESS_DETAIL = 42186;

    // false = not logged in
    private static boolean actualLoginStatus = false;
    private static String actualUserName = "";
    private static String actualMemberStatus = "";
    private static int actualCachesFound = -1;
    private static String actualStatus = "";

    private cgBase() {
        //initialize(app);
        throw new UnsupportedOperationException(); // static class, not to be instantiated
    }

    /**
     * Called from AbstractActivity.onCreate() and AbstractListActivity.onCreate()
     *
     * @param app
     */
    public static void initialize(final cgeoapplication app) {
        context = app.getBaseContext();
        res = app.getBaseContext().getResources();

        try {
            final PackageManager manager = app.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(app.getPackageName(), 0);
            version = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(Settings.tag, "unable to get version information", e);
            version = null;
        }
    }

    public static String hidePassword(final String message) {
        return message.replaceAll(passMatch, "password=***");
    }

    public static void sendLoadProgressDetail(final Handler handler, final int str) {
        if (null != handler) {
            handler.obtainMessage(UPDATE_LOAD_PROGRESS_DETAIL, cgeoapplication.getInstance().getString(str)).sendToTarget();
        }
    }

    /**
     * read all viewstates from page
     *
     * @return String[] with all view states
     */
    public static String[] getViewstates(String page) {
        // Get the number of viewstates.
        // If there is only one viewstate, __VIEWSTATEFIELDCOUNT is not present

        if (page == null) { // no network access
            return null;
        }

        int count = 1;
        final Matcher matcherViewstateCount = GCConstants.PATTERN_VIEWSTATEFIELDCOUNT.matcher(page);
        if (matcherViewstateCount.find()) {
            count = Integer.parseInt(matcherViewstateCount.group(1));
        }

        String[] viewstates = new String[count];

        // Get the viewstates
        int no;
        final Matcher matcherViewstates = GCConstants.PATTERN_VIEWSTATES.matcher(page);
        while (matcherViewstates.find()) {
            String sno = matcherViewstates.group(1); // number of viewstate
            if ("".equals(sno)) {
                no = 0;
            }
            else {
                no = Integer.parseInt(sno);
            }
            viewstates[no] = matcherViewstates.group(2);
        }

        if (viewstates.length != 1 || viewstates[0] != null) {
            return viewstates;
        }
        // no viewstates were present
        return null;
    }

    /**
     * put viewstates into request parameters
     */
    private static void putViewstates(final Parameters params, final String[] viewstates) {
        if (ArrayUtils.isEmpty(viewstates)) {
            return;
        }
        params.put("__VIEWSTATE", viewstates[0]);
        if (viewstates.length > 1) {
            for (int i = 1; i < viewstates.length; i++) {
                params.put("__VIEWSTATE" + i, viewstates[i]);
            }
            params.put("__VIEWSTATEFIELDCOUNT", viewstates.length + "");
        }
    }

    /**
     * transfers the viewstates variables from a page (response) to parameters
     * (next request)
     */
    public static void transferViewstates(final String page, final Parameters params) {
        putViewstates(params, getViewstates(page));
    }

    /**
     * checks if an Array of Strings is empty or not. Empty means:
     * - Array is null
     * - or all elements are null or empty strings
     */
    public static boolean isEmpty(String[] a) {
        if (a == null) {
            return true;
        }

        for (String s : a) {
            if (StringUtils.isNotEmpty(s)) {
                return false;
            }
        }
        return true;
    }

    public static StatusCode login() {
        final ImmutablePair<String, String> login = Settings.getLogin();

        if (login == null || StringUtils.isEmpty(login.left) || StringUtils.isEmpty(login.right)) {
            cgBase.setActualStatus(res.getString(R.string.err_login));
            Log.e(Settings.tag, "cgeoBase.login: No login information stored");
            return StatusCode.NO_LOGIN_INFO_STORED;
        }

        // res is null during the unit tests
        if (res != null) {
            cgBase.setActualStatus(res.getString(R.string.init_login_popup_working));
        }
        HttpResponse loginResponse = request("https://www.geocaching.com/login/default.aspx", null, false, false, false);
        String loginData = getResponseData(loginResponse);
        if (loginResponse != null && loginResponse.getStatusLine().getStatusCode() == 503 && BaseUtils.matches(loginData, GCConstants.PATTERN_MAINTENANCE)) {
            return StatusCode.MAINTENANCE;
        }

        if (StringUtils.isBlank(loginData)) {
            Log.e(Settings.tag, "cgeoBase.login: Failed to retrieve login page (1st)");
            return StatusCode.CONNECTION_FAILED; // no loginpage
        }

        if (getLoginStatus(loginData)) {
            Log.i(Settings.tag, "Already logged in Geocaching.com as " + login.left);
            switchToEnglish(loginData);
            return StatusCode.NO_ERROR; // logged in
        }

        clearCookies();
        Settings.setCookieStore(null);

        final Parameters params = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "ctl00$ContentBody$tbUsername", login.left,
                "ctl00$ContentBody$tbPassword", login.right,
                "ctl00$ContentBody$cbRememberMe", "on",
                "ctl00$ContentBody$btnSignIn", "Login");
        final String[] viewstates = getViewstates(loginData);
        if (isEmpty(viewstates)) {
            Log.e(Settings.tag, "cgeoBase.login: Failed to find viewstates");
            return StatusCode.LOGIN_PARSE_ERROR; // no viewstates
        }
        putViewstates(params, viewstates);

        loginResponse = postRequest("https://www.geocaching.com/login/default.aspx", params);
        loginData = getResponseData(loginResponse);

        if (StringUtils.isNotBlank(loginData)) {
            if (getLoginStatus(loginData)) {
                Log.i(Settings.tag, "Successfully logged in Geocaching.com as " + login.left);

                switchToEnglish(loginData);
                Settings.setCookieStore(dumpCookieStore());

                return StatusCode.NO_ERROR; // logged in
            } else {
                if (loginData.contains("Your username/password combination does not match.")) {
                    Log.i(Settings.tag, "Failed to log in Geocaching.com as " + login.left + " because of wrong username/password");
                    return StatusCode.WRONG_LOGIN_DATA; // wrong login
                } else {
                    Log.i(Settings.tag, "Failed to log in Geocaching.com as " + login.left + " for some unknown reason");
                    return StatusCode.UNKNOWN_ERROR; // can't login
                }
            }
        } else {
            Log.e(Settings.tag, "cgeoBase.login: Failed to retrieve login page (2nd)");
            // FIXME: should it be CONNECTION_FAILED to match the first attempt?
            return StatusCode.COMMUNICATION_ERROR; // no login page
        }
    }

    public static StatusCode logout() {
        HttpResponse logoutResponse = request("https://www.geocaching.com/login/default.aspx?RESET=Y&redir=http%3a%2f%2fwww.geocaching.com%2fdefault.aspx%3f", null, false, false, false);
        String logoutData = getResponseData(logoutResponse);
        if (logoutResponse != null && logoutResponse.getStatusLine().getStatusCode() == 503 && BaseUtils.matches(logoutData, GCConstants.PATTERN_MAINTENANCE)) {
            return StatusCode.MAINTENANCE;
        }

        clearCookies();
        Settings.setCookieStore(null);
        return StatusCode.NO_ERROR;
    }

    /**
     * Check if the user has been logged in when he retrieved the data.
     *
     * @param page
     * @return <code>true</code> if user is logged in, <code>false</code> otherwise
     */
    private static boolean getLoginStatus(final String page) {
        if (StringUtils.isBlank(page)) {
            Log.e(Settings.tag, "cgeoBase.checkLogin: No page given");
            return false;
        }

        // res is null during the unit tests
        if (res != null) {
            setActualStatus(res.getString(R.string.init_login_popup_ok));
        }

        // on every page except login page
        setActualLoginStatus(BaseUtils.matches(page, GCConstants.PATTERN_LOGIN_NAME));
        if (isActualLoginStatus()) {
            setActualUserName(BaseUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME, true, "???"));
            setActualMemberStatus(BaseUtils.getMatch(page, GCConstants.PATTERN_MEMBER_STATUS, true, "???"));
            setActualCachesFound(Integer.parseInt(BaseUtils.getMatch(page, GCConstants.PATTERN_CACHES_FOUND, true, "0").replaceAll("[,.]", "")));
            return true;
        }

        // login page
        setActualLoginStatus(BaseUtils.matches(page, GCConstants.PATTERN_LOGIN_NAME_LOGIN_PAGE));
        if (isActualLoginStatus()) {
            setActualUserName(Settings.getUsername());
            setActualMemberStatus(Settings.getMemberStatus());
            // number of caches found is not part of this page
            return true;
        }

        // res is null during the unit tests
        if (res != null) {
            setActualStatus(res.getString(R.string.init_login_popup_failed));
        }
        return false;
    }

    public static void switchToEnglish(String previousPage) {
        final String ENGLISH = "English&#9660;";
        if (previousPage != null && previousPage.indexOf(ENGLISH) >= 0) {
            Log.i(Settings.tag, "Geocaching.com language already set to English");
            // get find count
            getLoginStatus(getResponseData(request("http://www.geocaching.com/email/", null, false)));
        } else {
            final String page = getResponseData(request("http://www.geocaching.com/default.aspx", null, false));
            getLoginStatus(page);
            if (page == null) {
                Log.e(Settings.tag, "Failed to read viewstates to set geocaching.com language");
            }
            final Parameters params = new Parameters(
                    "__EVENTTARGET", "ctl00$uxLocaleList$uxLocaleList$ctl00$uxLocaleItem", // switch to english
                    "__EVENTARGUMENT", "");
            transferViewstates(page, params);
            final HttpResponse response = postRequest("http://www.geocaching.com/default.aspx", params);
            if (!isSuccess(response)) {
                Log.e(Settings.tag, "Failed to set geocaching.com language to English");
            }
        }
    }

    private static SearchResult parseSearch(final cgSearchThread thread, final String url, final String pageContent, final boolean showCaptcha, final int listId) {
        if (StringUtils.isBlank(pageContent)) {
            Log.e(Settings.tag, "cgeoBase.parseSearch: No page given");
            return null;
        }

        final List<String> cids = new ArrayList<String>();
        final List<String> guids = new ArrayList<String>();
        String recaptchaChallenge = null;
        String recaptchaText = null;
        String page = pageContent;

        final SearchResult searchResult = new SearchResult();
        searchResult.setUrl(url);
        searchResult.viewstates = getViewstates(page);

        // recaptcha
        if (showCaptcha) {
            String recaptchaJsParam = BaseUtils.getMatch(page, GCConstants.PATTERN_SEARCH_RECAPTCHA, false, null);

            if (recaptchaJsParam != null) {
                final Parameters params = new Parameters("k", recaptchaJsParam.trim());
                final String recaptchaJs = cgBase.getResponseData(request("http://www.google.com/recaptcha/api/challenge", params, true));

                if (StringUtils.isNotBlank(recaptchaJs)) {
                    recaptchaChallenge = BaseUtils.getMatch(recaptchaJs, GCConstants.PATTERN_SEARCH_RECAPTCHACHALLENGE, true, 1, recaptchaChallenge, true);
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
            Log.e(Settings.tag, "cgeoBase.parseSearch: ID \"ctl00_ContentBody_dlResults\" not found on page");
            return null;
        }

        page = page.substring(startPos); // cut on <table

        startPos = page.indexOf(">");
        int endPos = page.indexOf("ctl00_ContentBody_UnitTxt");
        if (startPos == -1 || endPos == -1) {
            Log.e(Settings.tag, "cgeoBase.parseSearch: ID \"ctl00_ContentBody_UnitTxt\" not found on page");
            return null;
        }

        page = page.substring(startPos + 1, endPos - startPos + 1); // cut between <table> and </table>

        final String[] rows = page.split("<tr class=");
        final int rows_count = rows.length;

        for (int z = 1; z < rows_count; z++) {
            cgCache cache = new cgCache();
            cache.setListId(listId);
            String row = rows[z];

            // check for cache type presence
            if (!row.contains("images/wpttypes")) {
                continue;
            }

            try {
                final Matcher matcherGuidAndDisabled = GCConstants.PATTERN_SEARCH_GUIDANDDISABLED.matcher(row);

                while (matcherGuidAndDisabled.find()) {
                    if (matcherGuidAndDisabled.groupCount() > 0) {
                        guids.add(matcherGuidAndDisabled.group(1));

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
                Log.w(Settings.tag, "cgeoBase.parseSearch: Failed to parse GUID and/or Disabled data");
            }

            if (Settings.isExcludeDisabledCaches() && (cache.isDisabled() || cache.isArchived())) {
                // skip disabled and archived caches
                continue;
            }

            String inventoryPre = null;

            cache.setGeocode(BaseUtils.getMatch(row, GCConstants.PATTERN_SEARCH_GEOCODE, true, 1, cache.getGeocode(), true).toUpperCase());

            // cache type
            cache.setType(CacheType.getByPattern(BaseUtils.getMatch(row, GCConstants.PATTERN_SEARCH_TYPE, true, 1, null, true)));

            // cache direction - image
            if (Settings.getLoadDirImg()) {
                cache.setDirectionImg(URLDecoder.decode(BaseUtils.getMatch(row, GCConstants.PATTERN_SEARCH_DIRECTION, true, 1, cache.getDirectionImg(), true)));
            }

            // cache inventory
            final Matcher matcherTbs = GCConstants.PATTERN_SEARCH_TRACKABLES.matcher(row);
            while (matcherTbs.find()) {
                if (matcherTbs.groupCount() > 0) {
                    cache.setInventoryItems(Integer.parseInt(matcherTbs.group(1)));
                    inventoryPre = matcherTbs.group(2);
                }
            }

            if (StringUtils.isNotBlank(inventoryPre)) {
                final Matcher matcherTbsInside = GCConstants.PATTERN_SEARCH_TRACKABLESINSIDE.matcher(inventoryPre);
                while (matcherTbsInside.find()) {
                    if (matcherTbsInside.groupCount() == 2 && matcherTbsInside.group(2) != null) {
                        final String inventoryItem = matcherTbsInside.group(2).toLowerCase();
                        if (inventoryItem.equals("premium member only cache")) {
                            continue;
                        } else {
                            if (cache.getInventoryItems() <= 0) {
                                cache.setInventoryItems(1);
                            }
                        }
                    }
                }
            }

            // premium cache
            cache.setPremiumMembersOnly(row.contains("/images/small_profile.gif"));

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

            // favourite count
            try {
                result = BaseUtils.getMatch(row, GCConstants.PATTERN_SEARCH_FAVORITE, false, 1, null, true);
                if (null != result) {
                    cache.setFavoritePoints(Integer.parseInt(result));
                }
            } catch (NumberFormatException e) {
                Log.w(Settings.tag, "cgeoBase.parseSearch: Failed to parse favourite count");
            }

            if (cache.getNameSp() == null) {
                cache.setNameSp((new Spannable.Factory()).newSpannable(cache.getName()));
                if (cache.isDisabled() || cache.isArchived()) { // strike
                    cache.getNameSp().setSpan(new StrikethroughSpan(), 0, cache.getNameSp().toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // location is reliable because the search return correct coords independant of the login status
            cache.setReliableLatLon(true);

            searchResult.addCache(cache);
        }

        // total caches found
        try {
            String result = BaseUtils.getMatch(page, GCConstants.PATTERN_SEARCH_TOTALCOUNT, false, 1, null, true);
            if (null != result) {
                searchResult.totalCnt = Integer.parseInt(result);
            }
        } catch (NumberFormatException e) {
            Log.w(Settings.tag, "cgeoBase.parseSearch: Failed to parse cache count");
        }

        if (thread != null && recaptchaChallenge != null) {
            if (thread.getText() == null) {
                thread.waitForUser();
            }

            recaptchaText = thread.getText();
        }

        if (cids.size() > 0 && (recaptchaChallenge == null || StringUtils.isNotBlank(recaptchaText))) {
            Log.i(Settings.tag, "Trying to get .loc for " + cids.size() + " caches");

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
                        params.put("__VIEWSTATEFIELDCOUNT", "" + searchResult.viewstates.length);
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

                final String coordinates = getResponseData(postRequest("http://www.geocaching.com/seek/nearest.aspx", params));

                if (StringUtils.isNotBlank(coordinates)) {
                    if (coordinates.contains("You have not agreed to the license agreement. The license agreement is required before you can start downloading GPX or LOC files from Geocaching.com")) {
                        Log.i(Settings.tag, "User has not agreed to the license agreement. Can\'t download .loc file.");

                        searchResult.error = StatusCode.UNAPPROVED_LICENSE;

                        return searchResult;
                    }
                }

                LocParser.parseLoc(searchResult, coordinates);
            } catch (Exception e) {
                Log.e(Settings.tag, "cgBase.parseSearch.CIDs: " + e.toString());
            }
        }

        // get direction images
        if (Settings.getLoadDirImg())
        {
            for (String geocode : searchResult.getGeocodes()) {
                cgCache oneCache = cgeoapplication.getInstance().loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                if (oneCache.getCoords() == null && StringUtils.isNotEmpty(oneCache.getDirectionImg())) {
                    DirectionImage.getDrawable(oneCache.getGeocode(), oneCache.getDirectionImg());
                }
            }
        }

        if (Settings.isRatingWanted()) {
            // get ratings
            if (guids.size() > 0) {
                Log.i(Settings.tag, "Trying to get ratings for " + cids.size() + " caches");

                try {
                    final Map<String, GCVoteRating> ratings = GCVote.getRating(guids, null);

                    if (MapUtils.isNotEmpty(ratings)) {
                        // save found cache coordinates
                        for (String geocode : searchResult.getGeocodes()) {
                            cgCache cache = cgeoapplication.getInstance().loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                            if (ratings.containsKey(cache.getGuid())) {
                                GCVoteRating rating = ratings.get(cache.getGuid());

                                cache.setRating(rating.getRating());
                                cache.setVotes(rating.getVotes());
                                cache.setMyVote(rating.getMyVote());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(Settings.tag, "cgBase.parseSearch.GCvote: " + e.toString());
                }
            }
        }

        return searchResult;
    }

    public static SearchResult parseCache(final String page, final int listId, final CancellableHandler handler) {
        final SearchResult searchResult = parseCacheFromText(page, listId, handler);
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
            sendLoadProgressDetail(handler, R.string.cache_dialog_offline_save_message);
            cache.setListId(StoredList.TEMPORARY_LIST_ID);
            cgeoapplication.getInstance().saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));
        }
        return searchResult;
    }

    static SearchResult parseCacheFromText(final String page, final int listId, final CancellableHandler handler) {
        sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_details);

        if (StringUtils.isBlank(page)) {
            Log.e(Settings.tag, "cgeoBase.parseCache: No page given");
            return null;
        }

        final SearchResult searchResult = new SearchResult();

        if (page.contains("Cache is Unpublished")) {
            searchResult.error = StatusCode.UNPUBLISHED_CACHE;
            return searchResult;
        }

        if (page.contains("Sorry, the owner of this listing has made it viewable to Premium Members only.")) {
            searchResult.error = StatusCode.PREMIUM_ONLY;
            return searchResult;
        }

        if (page.contains("has chosen to make this cache listing visible to Premium Members only.")) {
            searchResult.error = StatusCode.PREMIUM_ONLY;
            return searchResult;
        }

        final cgCache cache = new cgCache();
        cache.setDisabled(page.contains("<li>This cache is temporarily unavailable."));

        cache.setArchived(page.contains("<li>This cache has been archived,"));

        cache.setPremiumMembersOnly(BaseUtils.matches(page, GCConstants.PATTERN_PREMIUMMEMBERS));

        cache.setFavorite(BaseUtils.matches(page, GCConstants.PATTERN_FAVORITE));

        cache.setListId(listId);

        // cache geocode
        cache.setGeocode(BaseUtils.getMatch(page, GCConstants.PATTERN_GEOCODE, true, cache.getGeocode()));

        // cache id
        cache.setCacheId(BaseUtils.getMatch(page, GCConstants.PATTERN_CACHEID, true, cache.getCacheId()));

        // cache guid
        cache.setGuid(BaseUtils.getMatch(page, GCConstants.PATTERN_GUID, true, cache.getGuid()));

        // name
        cache.setName(Html.fromHtml(BaseUtils.getMatch(page, GCConstants.PATTERN_NAME, true, cache.getName())).toString());

        // owner real name
        cache.setOwnerReal(URLDecoder.decode(BaseUtils.getMatch(page, GCConstants.PATTERN_OWNERREAL, true, cache.getOwnerReal())));

        final String username = Settings.getUsername();
        if (cache.getOwnerReal() != null && username != null && cache.getOwnerReal().equalsIgnoreCase(username)) {
            cache.setOwn(true);
        }

        String tableInside = page;

        int pos = tableInside.indexOf("id=\"cacheDetails\"");
        if (pos == -1) {
            Log.e(Settings.tag, "cgeoBase.parseCache: ID \"cacheDetails\" not found on page");
            return null;
        }

        tableInside = tableInside.substring(pos);

        pos = tableInside.indexOf("<div class=\"CacheInformationTable\"");
        if (pos == -1) {
            Log.e(Settings.tag, "cgeoBase.parseCache: class \"CacheInformationTable\" not found on page");
            return null;
        }

        tableInside = tableInside.substring(0, pos);

        if (StringUtils.isNotBlank(tableInside)) {
            // cache terrain
            String result = BaseUtils.getMatch(tableInside, GCConstants.PATTERN_TERRAIN, true, null);
            if (result != null) {
                cache.setTerrain(Float.parseFloat(StringUtils.replaceChars(result, '_', '.')));
            }

            // cache difficulty
            result = BaseUtils.getMatch(tableInside, GCConstants.PATTERN_DIFFICULTY, true, null);
            if (result != null) {
                cache.setDifficulty(Float.parseFloat(StringUtils.replaceChars(result, '_', '.')));
            }

            // owner
            cache.setOwner(Html.fromHtml(BaseUtils.getMatch(tableInside, GCConstants.PATTERN_OWNER, true, cache.getOwner())).toString());

            // hidden
            try {
                String hiddenString = BaseUtils.getMatch(tableInside, GCConstants.PATTERN_HIDDEN, true, null);
                if (StringUtils.isNotBlank(hiddenString)) {
                    cache.setHidden(parseGcCustomDate(hiddenString));
                }
                if (cache.getHiddenDate() == null) {
                    // event date
                    hiddenString = BaseUtils.getMatch(tableInside, GCConstants.PATTERN_HIDDENEVENT, true, null);
                    if (StringUtils.isNotBlank(hiddenString)) {
                        cache.setHidden(parseGcCustomDate(hiddenString));
                    }
                }
            } catch (ParseException e) {
                // failed to parse cache hidden date
                Log.w(Settings.tag, "cgeoBase.parseCache: Failed to parse cache hidden (event) date");
            }

            // favourite
            cache.setFavoritePoints(Integer.parseInt(BaseUtils.getMatch(tableInside, GCConstants.PATTERN_FAVORITECOUNT, true, "0")));

            // cache size
            cache.setSize(CacheSize.getById(BaseUtils.getMatch(tableInside, GCConstants.PATTERN_SIZE, true, CacheSize.NOT_CHOSEN.id).toLowerCase()));
        }

        // cache found
        cache.setFound(BaseUtils.matches(page, GCConstants.PATTERN_FOUND) || BaseUtils.matches(page, GCConstants.PATTERN_FOUND_ALTERNATIVE));

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
                Log.w(Settings.tag, "cgeoBase.parseCache: Failed to parse cache coordinates: " + e.toString());
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

        checkFields(cache);

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

                while (matcherAttributesInside.find()) {
                    if (matcherAttributesInside.groupCount() > 1 && !matcherAttributesInside.group(2).equalsIgnoreCase("blank")) {
                        // by default, use the tooltip of the attribute
                        String attribute = matcherAttributesInside.group(2).toLowerCase();

                        // if the image name can be recognized, use the image name as attribute
                        String imageName = matcherAttributesInside.group(1).trim();
                        if (imageName.length() > 0) {
                            int start = imageName.lastIndexOf('/');
                            int end = imageName.lastIndexOf('.');
                            if (start >= 0 && end >= 0) {
                                attribute = imageName.substring(start + 1, end).replace('-', '_').toLowerCase();
                            }
                        }
                        cache.addAttribute(attribute);
                    }
                }
            }
        } catch (Exception e) {
            // failed to parse cache attributes
            Log.w(Settings.tag, "cgeoBase.parseCache: Failed to parse cache attributes");
        }

        // cache spoilers
        try {
            final String spoilers = BaseUtils.getMatch(page, GCConstants.PATTERN_SPOILERS, false, null);
            if (null != spoilers) {
                if (CancellableHandler.isCancelled(handler)) {
                    return null;
                }
                sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_spoilers);

                final Matcher matcherSpoilersInside = GCConstants.PATTERN_SPOILERSINSIDE.matcher(spoilers);

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
                    final cgImage spoiler = new cgImage(url, title, description);

                    if (cache.getSpoilers() == null) {
                        cache.setSpoilers(new ArrayList<cgImage>());
                    }
                    cache.getSpoilers().add(spoiler);
                }
            }
        } catch (Exception e) {
            // failed to parse cache spoilers
            Log.w(Settings.tag, "cgeoBase.parseCache: Failed to parse cache spoilers");
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
            Log.w(Settings.tag, "cgeoBase.parseCache: Failed to parse cache inventory (2)");
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
                            && LogType.LOG_UNKNOWN != LogType.getByIconName(typeStr)
                            && StringUtils.isNotBlank(countStr)) {
                        cache.getLogCounts().put(LogType.getByIconName(typeStr), Integer.parseInt(countStr));
                    }
                }
            }
        } catch (Exception e)
        {
            // failed to parse logs
            Log.w(Settings.tag, "cgeoBase.parseCache: Failed to parse cache log count");
        }

        // add waypoint for original coordinates in case of user-modified listing-coordinates
        try {
            final String originalCoords = BaseUtils.getMatch(page, GCConstants.PATTERN_LATLON_ORIG, false, null);

            if (null != originalCoords) {
                // res is null during the unit tests
                final cgWaypoint waypoint = new cgWaypoint(res != null ? res.getString(R.string.cache_coordinates_original) : "res = null", WaypointType.WAYPOINT, false);
                waypoint.setCoords(new Geopoint(originalCoords));
                cache.addWaypoint(waypoint);
                cache.setUserModifiedCoords(true);
            }
        } catch (Geopoint.GeopointException e) {
        }

        // waypoints
        int wpBegin = 0;
        int wpEnd = 0;

        wpBegin = page.indexOf("<table class=\"Table\" id=\"ctl00_ContentBody_Waypoints\">");
        if (wpBegin != -1) { // parse waypoints
            if (CancellableHandler.isCancelled(handler)) {
                return null;
            }
            sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_waypoints);


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
                    final String name = BaseUtils.getMatch(wp[6], GCConstants.PATTERN_WPNAME, true, 1, res != null ? res.getString(R.string.waypoint) : "res = null", true);

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

                    cache.addWaypoint(waypoint);
                }
            }
        }

        cache.parseWaypointsFromNote();

        // logs
        cache.setLogs(loadLogsFromDetails(page, cache, false, true));

        searchResult.addCache(cache);
        return searchResult;
    }

    private static void getExtraOnlineInfo(final cgCache cache, final String page, final CancellableHandler handler) {
        if (CancellableHandler.isCancelled(handler)) {
            return;
        }

        //cache.setLogs(loadLogsFromDetails(page, cache, false));
        if (Settings.isFriendLogsWanted()) {
            sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_logs);
            List<cgLog> allLogs = cache.getLogs();
            List<cgLog> friendLogs = loadLogsFromDetails(page, cache, true, false);
            if (friendLogs != null) {
                for (cgLog log : friendLogs) {
                    if (allLogs.contains(log)) {
                        allLogs.get(allLogs.indexOf(log)).friend = true;
                    } else {
                        allLogs.add(log);
                    }
                }
            }
        }

        if (Settings.isElevationWanted()) {
            if (CancellableHandler.isCancelled(handler)) {
                return;
            }
            sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_elevation);
            if (cache.getCoords() != null) {
                cache.setElevation(getElevation(cache.getCoords()));
            }
        }

        if (Settings.isRatingWanted()) {
            if (CancellableHandler.isCancelled(handler)) {
                return;
            }
            sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_gcvote);
            final GCVoteRating rating = GCVote.getRating(cache.getGuid(), cache.getGeocode());
            if (rating != null) {
                cache.setRating(rating.getRating());
                cache.setVotes(rating.getVotes());
                cache.setMyVote(rating.getMyVote());
            }
        }
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
    private static List<cgLog> loadLogsFromDetails(final String page, final cgCache cache, final boolean friends, final boolean getDataFromPage) {
        String rawResponse = null;

        if (!getDataFromPage) {
            final Matcher userTokenMatcher = GCConstants.PATTERN_USERTOKEN2.matcher(page);
            if (!userTokenMatcher.find()) {
                Log.e(Settings.tag, "cgBase.loadLogsFromDetails: unable to extract userToken");
                return null;
            }

            final String userToken = userTokenMatcher.group(1);
            final Parameters params = new Parameters(
                    "tkn", userToken,
                    "idx", "1",
                    "num", String.valueOf(Constants.NUMBER_OF_LOGS),
                    "decrypt", "true",
                    // "sp", Boolean.toString(personal), // personal logs
                    "sf", Boolean.toString(friends));

            final HttpResponse response = request("http://www.geocaching.com/seek/geocache.logbook", params, false, false, false);
            if (response == null) {
                Log.e(Settings.tag, "cgBase.loadLogsFromDetails: cannot log logs, response is null");
                return null;
            }
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                Log.e(Settings.tag, "cgBase.loadLogsFromDetails: error " + statusCode + " when requesting log information");
                return null;
            }
            rawResponse = cgBase.getResponseData(response);
            if (rawResponse == null) {
                Log.e(Settings.tag, "cgBase.loadLogsFromDetails: unable to read whole response");
                return null;
            }
        } else {
            // extract embedded JSON data from page
            rawResponse = BaseUtils.getMatch(page, GCConstants.PATTERN_LOGBOOK, "");
        }

        List<cgLog> logs = new ArrayList<cgLog>();

        try {
            final JSONObject resp = new JSONObject(rawResponse);
            if (!resp.getString("status").equals("success")) {
                Log.e(Settings.tag, "cgBase.loadLogsFromDetails: status is " + resp.getString("status"));
                return null;
            }

            final JSONArray data = resp.getJSONArray("data");

            for (int index = 0; index < data.length(); index++) {
                final JSONObject entry = data.getJSONObject(index);
                final cgLog logDone = new cgLog();
                logDone.friend = friends;

                // FIXME: use the "LogType" field instead of the "LogTypeImage" one.
                final String logIconNameExt = entry.optString("LogTypeImage", ".gif");
                final String logIconName = logIconNameExt.substring(0, logIconNameExt.length() - 4);
                logDone.type = LogType.getByIconName(logIconName);

                try {
                    logDone.date = parseGcCustomDate(entry.getString("Visited")).getTime();
                } catch (ParseException e) {
                    Log.e(Settings.tag, "cgBase.loadLogsFromDetails: failed to parse log date.");
                }

                logDone.author = entry.getString("UserName");
                logDone.found = entry.getInt("GeocacheFindCount");
                logDone.log = entry.getString("LogText");

                final JSONArray images = entry.getJSONArray("Images");
                for (int i = 0; i < images.length(); i++) {
                    final JSONObject image = images.getJSONObject(i);
                    String url = "http://img.geocaching.com/cache/log/" + image.getString("FileName");
                    String title = image.getString("Name");
                    final cgImage logImage = new cgImage(url, title);
                    if (logDone.logImages == null) {
                        logDone.logImages = new ArrayList<cgImage>();
                    }
                    logDone.logImages.add(logImage);
                }

                logs.add(logDone);
            }
        } catch (JSONException e) {
            // failed to parse logs
            Log.w(Settings.tag, "cgBase.loadLogsFromDetails: Failed to parse cache logs", e);
        }

        return logs;
    }

    private static void checkFields(cgCache cache) {
        if (StringUtils.isBlank(cache.getGeocode())) {
            Log.e(Settings.tag, "cgBase.loadLogsFromDetails: geo code not parsed correctly");
        }
        if (StringUtils.isBlank(cache.getName())) {
            Log.e(Settings.tag, "name not parsed correctly");
        }
        if (StringUtils.isBlank(cache.getGuid())) {
            Log.e(Settings.tag, "guid not parsed correctly");
        }
        if (cache.getTerrain() == 0.0) {
            Log.e(Settings.tag, "terrain not parsed correctly");
        }
        if (cache.getDifficulty() == 0.0) {
            Log.e(Settings.tag, "difficulty not parsed correctly");
        }
        if (StringUtils.isBlank(cache.getOwner())) {
            Log.e(Settings.tag, "owner not parsed correctly");
        }
        if (StringUtils.isBlank(cache.getOwnerReal())) {
            Log.e(Settings.tag, "owner real not parsed correctly");
        }
        if (cache.getHiddenDate() == null) {
            Log.e(Settings.tag, "hidden not parsed correctly");
        }
        if (cache.getFavoritePoints() < 0) {
            Log.e(Settings.tag, "favoriteCount not parsed correctly");
        }
        if (cache.getSize() == null) {
            Log.e(Settings.tag, "size not parsed correctly");
        }
        if (cache.getType() == null || cache.getType() == CacheType.UNKNOWN) {
            Log.e(Settings.tag, "type not parsed correctly");
        }
        if (cache.getCoords() == null) {
            Log.e(Settings.tag, "coordinates not parsed correctly");
        }
        if (StringUtils.isBlank(cache.getLocation())) {
            Log.e(Settings.tag, "location not parsed correctly");
        }
    }

    public static Date parseGcCustomDate(final String input, final String format) throws ParseException {
        if (StringUtils.isBlank(input)) {
            throw new ParseException("Input is null", 0);
        }

        final String trimmed = input.trim();

        if (gcCustomDateFormats.containsKey(format)) {
            try {
                return gcCustomDateFormats.get(format).parse(trimmed);
            } catch (ParseException e) {
            }
        }

        for (SimpleDateFormat sdf : gcCustomDateFormats.values()) {
            try {
                return sdf.parse(trimmed);
            } catch (ParseException e) {
            }
        }

        throw new ParseException("No matching pattern", 0);
    }

    public static Date parseGcCustomDate(final String input) throws ParseException {
        return parseGcCustomDate(input, Settings.getGcCustomDate());
    }

    /**
     * Detect user date settings on geocaching.com
     */
    public static void detectGcCustomDate() {

        final String result = getResponseData(request("http://www.geocaching.com/account/ManagePreferences.aspx", null, false, false, false));

        if (null == result) {
            Log.w(Settings.tag, "cgeoBase.detectGcCustomDate: result is null");
            return;
        }

        String customDate = BaseUtils.getMatch(result, GCConstants.PATTERN_CUSTOMDATE, true, null);
        if (null != customDate) {
            Settings.setGcCustomDate(customDate);
        }
    }

    public static BitmapDrawable downloadAvatarAndGetMemberStatus(final Context context) {
        try {
            final String profile = BaseUtils.replaceWhitespace(getResponseData(request("http://www.geocaching.com/my/", null, false)));

            Settings.setMemberStatus(BaseUtils.getMatch(profile, GCConstants.PATTERN_MEMBER_STATUS, true, null));

            setActualCachesFound(Integer.parseInt(BaseUtils.getMatch(profile, GCConstants.PATTERN_CACHES_FOUND, true, "-1").replaceAll("[,.]", "")));

            final String avatarURL = BaseUtils.getMatch(profile, GCConstants.PATTERN_AVATAR_IMAGE_PROFILE_PAGE, false, null);
            if (null != avatarURL) {
                final HtmlImage imgGetter = new HtmlImage(context, "", false, 0, false);
                return imgGetter.getDrawable(avatarURL);
            }
            // No match? There may be no avatar set by user.
            Log.d(Settings.tag, "No avatar set for user");
        } catch (Exception e) {
            Log.w(Settings.tag, "Error when retrieving user avatar", e);
        }
        return null;
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
    public static cgTrackable parseTrackable(final String page, final cgeoapplication app, final String possibleTrackingcode) {
        if (StringUtils.isBlank(page)) {
            Log.e(Settings.tag, "cgeoBase.parseTrackable: No page given");
            return null;
        }

        final cgTrackable trackable = new cgTrackable();

        // trackable geocode
        trackable.setGeocode(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GEOCODE, true, trackable.getGeocode()).toUpperCase());

        // trackable id
        trackable.setGuid(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GUID, true, trackable.getGuid()));

        // trackable icon
        trackable.setIconUrl(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_ICON, true, trackable.getIconUrl()));

        // trackable name
        trackable.setName(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, true, trackable.getName()));

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
            Log.w(Settings.tag, "cgeoBase.parseTrackable: Failed to parse trackable owner name");
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
            Log.w(Settings.tag, "cgeoBase.parseTrackable: Failed to parse trackable last known place");
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
        try {
            final String distance = BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_DISTANCE, false, null);
            if (null != distance) {
                trackable.setDistance(DistanceParser.parseDistance(distance, Settings.isUseMetricUnits()));
            }
        } catch (NumberFormatException e) {
            throw e;
        }

        // trackable goal
        trackable.setGoal(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_GOAL, true, trackable.getGoal()));

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
                    trackable.setDetails(details);
                }
            }
        } catch (Exception e) {
            // failed to parse trackable details & image
            Log.w(Settings.tag, "cgeoBase.parseTrackable: Failed to parse trackable details & image");
        }

        // trackable logs
        try
        {
            final Matcher matcherLogs = GCConstants.PATTERN_TRACKABLE_LOG.matcher(page);
            /*
             * 1. Type (img)
             * 2. Date
             * 3. Author
             * 4. Cache-GUID
             * 5. <ignored> (strike-through property for ancien caches)
             * 6. Cache-name
             * 7. Logtext
             */
            while (matcherLogs.find())
            {
                final cgLog logDone = new cgLog();

                logDone.type = LogType.getByIconName(matcherLogs.group(1));
                logDone.author = Html.fromHtml(matcherLogs.group(3)).toString().trim();

                try
                {
                    logDone.date = parseGcCustomDate(matcherLogs.group(2)).getTime();
                } catch (ParseException e) {
                }

                logDone.log = matcherLogs.group(7).trim();

                if (matcherLogs.group(4) != null && matcherLogs.group(6) != null)
                {
                    logDone.cacheGuid = matcherLogs.group(4);
                    logDone.cacheName = matcherLogs.group(6);
                }

                // Apply the pattern for images in a trackable log entry against each full log (group(0))
                final Matcher matcherLogImages = GCConstants.PATTERN_TRACKABLE_LOG_IMAGES.matcher(matcherLogs.group(0));
                /*
                 * 1. Image URL
                 * 2. Image title
                 */
                while (matcherLogImages.find())
                {
                    final cgImage logImage = new cgImage(matcherLogImages.group(1), matcherLogImages.group(2));
                    if (logDone.logImages == null) {
                        logDone.logImages = new ArrayList<cgImage>();
                    }
                    logDone.logImages.add(logImage);
                }

                trackable.getLogs().add(logDone);
            }
        } catch (Exception e) {
            // failed to parse logs
            Log.w(Settings.tag, "cgeoBase.parseCache: Failed to parse cache logs" + e.toString());
        }

        // trackingcode
        if (!StringUtils.equalsIgnoreCase(trackable.getGeocode(), possibleTrackingcode)) {
            trackable.setTrackingcode(possibleTrackingcode);
        }

        if (app != null) {
            app.saveTrackable(trackable);
        }

        return trackable;
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
                    final int type = Integer.parseInt(typeMatcher.group(2));

                    if (type > 0) {
                        types.add(LogType.getById(type));
                    }
                }
            }
        }

        return types;
    }

    public static List<cgTrackableLog> parseTrackableLog(final String page) {
        if (StringUtils.isEmpty(page)) {
            return null;
        }

        final List<cgTrackableLog> trackables = new ArrayList<cgTrackableLog>();

        String table = StringUtils.substringBetween(page, "<table id=\"tblTravelBugs\"", "</table>");

        // if no trackables are currently in the account, the table is not available, so return an empty list instead of null
        if (StringUtils.isBlank(table)) {
            return trackables;
        }

        table = StringUtils.substringBetween(table, "<tbody>", "</tbody>");
        if (StringUtils.isBlank(table)) {
            Log.e(Settings.tag, "cgeoBase.parseTrackableLog: tbody not found on page");
            return null;
        }

        final Matcher trackableMatcher = GCConstants.PATTERN_TRACKABLE.matcher(page);
        while (trackableMatcher.find()) {
            if (trackableMatcher.groupCount() > 0) {
                final cgTrackableLog trackableLog = new cgTrackableLog();

                if (trackableMatcher.group(1) != null) {
                    trackableLog.trackCode = trackableMatcher.group(1);
                } else {
                    continue;
                }
                if (trackableMatcher.group(2) != null) {
                    trackableLog.name = Html.fromHtml(trackableMatcher.group(2)).toString();
                } else {
                    continue;
                }
                if (trackableMatcher.group(3) != null) {
                    trackableLog.ctl = Integer.valueOf(trackableMatcher.group(3));
                } else {
                    continue;
                }
                if (trackableMatcher.group(5) != null) {
                    trackableLog.id = Integer.valueOf(trackableMatcher.group(5));
                } else {
                    continue;
                }

                Log.i(Settings.tag, "Trackable in inventory (#" + trackableLog.ctl + "/" + trackableLog.id + "): " + trackableLog.trackCode + " - " + trackableLog.name);

                trackables.add(trackableLog);
            }
        }

        return trackables;
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

    public static SearchResult searchByNextPage(cgSearchThread thread, final SearchResult search, int listId, boolean showCaptcha) {
        if (search == null) {
            return search;
        }
        final String[] viewstates = search.getViewstates();

        final String url = search.getUrl();

        if (StringUtils.isBlank(url)) {
            Log.e(Settings.tag, "cgeoBase.searchByNextPage: No url found");
            return search;
        }

        if (isEmpty(viewstates)) {
            Log.e(Settings.tag, "cgeoBase.searchByNextPage: No viewstate given");
            return search;
        }

        // As in the original code, remove the query string
        final String uri = Uri.parse(url).buildUpon().query(null).build().toString();

        final Parameters params = new Parameters(
                "__EVENTTARGET", "ctl00$ContentBody$pgrBottom$ctl08",
                "__EVENTARGUMENT", "");
        putViewstates(params, viewstates);

        String page = getResponseData(postRequest(uri, params));
        if (!getLoginStatus(page)) {
            final StatusCode loginState = login();
            if (loginState == StatusCode.NO_ERROR) {
                page = getResponseData(postRequest(uri, params));
            } else if (loginState == StatusCode.NO_LOGIN_INFO_STORED) {
                Log.i(Settings.tag, "Working as guest.");
            } else {
                search.setError(loginState);
                Log.e(Settings.tag, "cgeoBase.searchByNextPage: Can not log in geocaching");
                return search;
            }
        }

        if (StringUtils.isBlank(page)) {
            Log.e(Settings.tag, "cgeoBase.searchByNextPage: No data from server");
            return search;
        }

        final SearchResult searchResult = parseSearch(thread, url, page, showCaptcha, listId);
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.e(Settings.tag, "cgeoBase.searchByNextPage: No cache parsed");
            return search;
        }

        // save to application
        search.setError(searchResult.error);
        search.setViewstates(searchResult.viewstates);
        for (String geocode : searchResult.getGeocodes()) {
            search.addGeocode(geocode);
        }
        return search;
    }

    public static SearchResult searchByGeocode(final String geocode, final String guid, final int listId, final boolean forceReload, final CancellableHandler handler) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid)) {
            Log.e(Settings.tag, "cgeoBase.searchByGeocode: No geocode nor guid given");
            return null;
        }

        cgeoapplication app = cgeoapplication.getInstance();
        if (!forceReload && listId == StoredList.TEMPORARY_LIST_ID && (app.isOffline(geocode, guid) || app.isThere(geocode, guid, true, true))) {
            final SearchResult search = new SearchResult();
            final String realGeocode = StringUtils.isNotBlank(geocode) ? geocode : app.getGeocode(guid);
            search.addGeocode(realGeocode);
            return search;
        }

        // if we have no geocode, we can't dynamically select the handler, but must explicitly use GC
        if (geocode == null && guid != null) {
            return GCConnector.getInstance().searchByGeocode(null, guid, app, listId, handler);
        }

        return ConnectorFactory.getConnector(geocode).searchByGeocode(geocode, guid, app, listId, handler);
    }

    public static SearchResult searchByStored(final Geopoint coords, final CacheType cacheType, final int list) {
        return cgeoapplication.getInstance().getBatchOfStoredCaches(true, coords, cacheType, list);
    }

    /**
     * @param thread
     *            thread to run the captcha if needed
     * @param cacheType
     * @param listId
     * @param showCaptcha
     * @param params
     *            the parameters to add to the request URI
     * @return
     */
    private static SearchResult searchByAny(final cgSearchThread thread, final CacheType cacheType, final boolean my, final int listId, final boolean showCaptcha, final Parameters params) {
        insertCacheType(params, cacheType);

        final String uri = "http://www.geocaching.com/seek/nearest.aspx";
        final String fullUri = uri + "?" + addFToParams(params, false, true);
        String page = requestLogged(uri, params, false, my, true);

        if (StringUtils.isBlank(page)) {
            Log.e(Settings.tag, "cgeoBase.searchByAny: No data from server");
            return null;
        }

        final SearchResult searchResult = parseSearch(thread, fullUri, page, showCaptcha, listId);
        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.e(Settings.tag, "cgeoBase.searchByAny: No cache parsed");
            return searchResult;
        }

        final SearchResult search = searchResult.filterSearchResults(Settings.isExcludeDisabledCaches(), false, cacheType, listId);

        getLoginStatus(page);

        return search;
    }

    public static SearchResult searchByCoords(final cgSearchThread thread, final Geopoint coords, final CacheType cacheType, final int listId, final boolean showCaptcha) {
        final Parameters params = new Parameters("lat", Double.toString(coords.getLatitude()), "lng", Double.toString(coords.getLongitude()));
        return searchByAny(thread, cacheType, false, listId, showCaptcha, params);
    }

    public static SearchResult searchByKeyword(final cgSearchThread thread, final String keyword, final CacheType cacheType, final int listId, final boolean showCaptcha) {
        if (StringUtils.isBlank(keyword)) {
            Log.e(Settings.tag, "cgeoBase.searchByKeyword: No keyword given");
            return null;
        }

        final Parameters params = new Parameters("key", keyword);
        return searchByAny(thread, cacheType, false, listId, showCaptcha, params);
    }

    public static SearchResult searchByUsername(final cgSearchThread thread, final String userName, final CacheType cacheType, final int listId, final boolean showCaptcha) {
        if (StringUtils.isBlank(userName)) {
            Log.e(Settings.tag, "cgeoBase.searchByUsername: No user name given");
            return null;
        }

        final Parameters params = new Parameters("ul", userName);

        boolean my = false;
        if (userName.equalsIgnoreCase(Settings.getLogin().left)) {
            my = true;
            Log.i(Settings.tag, "cgBase.searchByUsername: Overriding users choice, downloading all caches.");
        }

        return searchByAny(thread, cacheType, my, listId, showCaptcha, params);
    }

    public static SearchResult searchByOwner(final cgSearchThread thread, final String userName, final CacheType cacheType, final int listId, final boolean showCaptcha) {
        if (StringUtils.isBlank(userName)) {
            Log.e(Settings.tag, "cgeoBase.searchByOwner: No user name given");
            return null;
        }

        final Parameters params = new Parameters("u", userName);
        return searchByAny(thread, cacheType, false, listId, showCaptcha, params);
    }

    /** Request .png image for a tile. Ignore the image - just load it */
    public static void requestMapTile(final String url, final String referer) {
        final HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
        request.addHeader("Accept-Encoding", "gzip, deflate");
        request.addHeader("Referer", referer);
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        request(request);
    }

    /** Request JSON informations for a tile */
    public static String requestMapInfo(final String url, final String referer) {
        final HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        // NO request.addHeader("Accept-Encoding", "gzip deflate");
        request.addHeader("Referer", referer);
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        return getResponseData(request(request), false);
    }

    public static cgTrackable searchTrackable(final String geocode, final String guid, final String id) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid) && StringUtils.isBlank(id)) {
            Log.w(Settings.tag, "cgeoBase.searchTrackable: No geocode nor guid nor id given");
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

        String page = requestLogged("http://www.geocaching.com/track/details.aspx", params, false, false, false);

        if (StringUtils.isBlank(page)) {
            Log.e(Settings.tag, "cgeoBase.searchTrackable: No data from server");
            return trackable;
        }

        trackable = parseTrackable(page, cgeoapplication.getInstance(), geocode);
        if (trackable == null) {
            Log.e(Settings.tag, "cgeoBase.searchTrackable: No trackable parsed");
            return null;
        }

        return trackable;
    }

    public static StatusCode postLog(final String geocode, final String cacheid, final String[] viewstates,
            final LogType logType, final int year, final int month, final int day,
            final String log, final List<cgTrackableLog> trackables) {
        if (isEmpty(viewstates)) {
            Log.e(Settings.tag, "cgeoBase.postLog: No viewstate given");
            return StatusCode.LOG_POST_ERROR;
        }

        if (StringUtils.isBlank(log)) {
            Log.e(Settings.tag, "cgeoBase.postLog: No log text given");
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
            Log.i(Settings.tag, "Trying to post log for cache #" + cacheid + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + logInfo + "; trackables: " + trackables.size());
        } else {
            Log.i(Settings.tag, "Trying to post log for cache #" + cacheid + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + logInfo + "; trackables: 0");
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
        putViewstates(params, viewstates);
        if (trackables != null && !trackables.isEmpty()) { //  we have some trackables to proceed
            final StringBuilder hdnSelected = new StringBuilder();

            for (final cgTrackableLog tb : trackables) {
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
        String page = getResponseData(postRequest(uri, params));
        if (!getLoginStatus(page)) {
            final StatusCode loginState = login();
            if (loginState == StatusCode.NO_ERROR) {
                page = getResponseData(postRequest(uri, params));
            } else {
                Log.e(Settings.tag, "cgeoBase.postLog: Can not log in geocaching (error: " + loginState + ")");
                return loginState;
            }
        }

        if (StringUtils.isBlank(page)) {
            Log.e(Settings.tag, "cgeoBase.postLog: No data from server");
            return StatusCode.NO_DATA_FROM_SERVER;
        }

        // maintenance, archived needs to be confirmed

        final Matcher matcher = GCConstants.PATTERN_MAINTENANCE.matcher(page);

        try {
            if (matcher.find() && matcher.groupCount() > 0) {
                final String[] viewstatesConfirm = getViewstates(page);

                if (isEmpty(viewstatesConfirm)) {
                    Log.e(Settings.tag, "cgeoBase.postLog: No viewstate for confirm log");
                    return StatusCode.LOG_POST_ERROR;
                }

                params.clear();
                putViewstates(params, viewstatesConfirm);
                params.put("__EVENTTARGET", "");
                params.put("__EVENTARGUMENT", "");
                params.put("__LASTFOCUS", "");
                params.put("ctl00$ContentBody$LogBookPanel1$btnConfirm", "Yes");
                params.put("ctl00$ContentBody$LogBookPanel1$uxLogInfo", logInfo);
                params.put("ctl00$ContentBody$uxVistOtherListingGC", "");
                if (trackables != null && !trackables.isEmpty()) { //  we have some trackables to proceed
                    final StringBuilder hdnSelected = new StringBuilder();

                    for (cgTrackableLog tb : trackables) {
                        String ctl = null;
                        final String action = Integer.toString(tb.id) + tb.action.action;

                        if (tb.ctl < 10) {
                            ctl = "0" + Integer.toString(tb.ctl);
                        } else {
                            ctl = Integer.toString(tb.ctl);
                        }

                        params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$repTravelBugs$ctl" + ctl + "$ddlAction", action);
                        if (tb.action != LogTypeTrackable.DO_NOTHING) {
                            hdnSelected.append(action);
                            hdnSelected.append(',');
                        }
                    }

                    params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnSelectedActions", hdnSelected.toString()); // selected trackables
                    params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnCurrentFilter", "");
                }

                page = getResponseData(postRequest(uri, params));
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeoBase.postLog.confim: " + e.toString());
        }

        try {

            final Matcher matcherOk = GCConstants.PATTERN_OK1.matcher(page);
            if (matcherOk.find()) {
                Log.i(Settings.tag, "Log successfully posted to cache #" + cacheid);

                if (geocode != null) {
                    cgeoapplication.getInstance().saveVisitDate(geocode);
                }

                getLoginStatus(page);
                // the log-successful-page contains still the old value
                if (getActualCachesFound() >= 0) {
                    setActualCachesFound(getActualCachesFound() + 1);
                }
                return StatusCode.NO_ERROR;
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeoBase.postLog.check: " + e.toString());
        }

        Log.e(Settings.tag, "cgeoBase.postLog: Failed to post log because of unknown error");
        return StatusCode.LOG_POST_ERROR;
    }

    public static StatusCode postLogTrackable(final String tbid, final String trackingCode, final String[] viewstates,
            final LogType logType, final int year, final int month, final int day, final String log) {
        if (isEmpty(viewstates)) {
            Log.e(Settings.tag, "cgeoBase.postLogTrackable: No viewstate given");
            return StatusCode.LOG_POST_ERROR;
        }

        if (StringUtils.isBlank(log)) {
            Log.e(Settings.tag, "cgeoBase.postLogTrackable: No log text given");
            return StatusCode.NO_LOG_TEXT;
        }

        Log.i(Settings.tag, "Trying to post log for trackable #" + trackingCode + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + log);

        final String logInfo = log.replace("\n", "\r\n"); // windows' eol

        final Calendar currentDate = Calendar.getInstance();
        final Parameters params = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "__LASTFOCUS", "",
                "ctl00$ContentBody$LogBookPanel1$ddLogType", Integer.toString(logType.id),
                "ctl00$ContentBody$LogBookPanel1$tbCode", trackingCode);
        putViewstates(params, viewstates);
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
        String page = getResponseData(postRequest(uri, params));
        if (!getLoginStatus(page)) {
            final StatusCode loginState = login();
            if (loginState == StatusCode.NO_ERROR) {
                page = getResponseData(postRequest(uri, params));
            } else {
                Log.e(Settings.tag, "cgeoBase.postLogTrackable: Can not log in geocaching (error: " + loginState + ")");
                return loginState;
            }
        }

        if (StringUtils.isBlank(page)) {
            Log.e(Settings.tag, "cgeoBase.postLogTrackable: No data from server");
            return StatusCode.NO_DATA_FROM_SERVER;
        }

        try {

            final Matcher matcherOk = GCConstants.PATTERN_OK2.matcher(page);
            if (matcherOk.find()) {
                Log.i(Settings.tag, "Log successfully posted to trackable #" + trackingCode);
                return StatusCode.NO_ERROR;
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeoBase.postLogTrackable.check: " + e.toString());
        }

        Log.e(Settings.tag, "cgeoBase.postLogTrackable: Failed to post log because of unknown error");
        return StatusCode.LOG_POST_ERROR;
    }

    /**
     * Adds the cache to the watchlist of the user.
     *
     * @param cache
     *            the cache to add
     * @return -1: error occured
     */
    public static int addToWatchlist(final cgCache cache) {
        final String uri = "http://www.geocaching.com/my/watchlist.aspx?w=" + cache.getCacheId();
        String page = postRequestLogged(uri);

        if (StringUtils.isBlank(page)) {
            Log.e(Settings.tag, "cgBase.addToWatchlist: No data from server");
            return -1; // error
        }

        boolean guidOnPage = cache.isGuidContainedInPage(page);
        if (guidOnPage) {
            Log.i(Settings.tag, "cgBase.addToWatchlist: cache is on watchlist");
            cache.setOnWatchlist(true);
        } else {
            Log.e(Settings.tag, "cgBase.addToWatchlist: cache is not on watchlist");
        }
        return guidOnPage ? 1 : -1; // on watchlist (=added) / else: error
    }

    /**
     * Removes the cache from the watchlist
     *
     * @param cache
     *            the cache to remove
     * @return -1: error occured
     */
    public static int removeFromWatchlist(final cgCache cache) {
        final String uri = "http://www.geocaching.com/my/watchlist.aspx?ds=1&action=rem&id=" + cache.getCacheId();
        String page = postRequestLogged(uri);

        if (StringUtils.isBlank(page)) {
            Log.e(Settings.tag, "cgBase.removeFromWatchlist: No data from server");
            return -1; // error
        }

        // removing cache from list needs approval by hitting "Yes" button
        final Parameters params = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "ctl00$ContentBody$btnYes", "Yes");
        transferViewstates(page, params);

        page = getResponseData(postRequest(uri, params));
        boolean guidOnPage = cache.isGuidContainedInPage(page);
        if (!guidOnPage) {
            Log.i(Settings.tag, "cgBase.removeFromWatchlist: cache removed from watchlist");
            cache.setOnWatchlist(false);
        } else {
            Log.e(Settings.tag, "cgBase.removeFromWatchlist: cache not removed from watchlist");
        }
        return guidOnPage ? -1 : 0; // on watchlist (=error) / not on watchlist
    }

    final public static HostnameVerifier doNotVerify = new HostnameVerifier() {

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public static void postTweetCache(String geocode) {
        final cgCache cache = cgeoapplication.getInstance().loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        String status;
        final String url = cache.getUrl();
        if (url.length() >= 100) {
            status = "I found " + url;
        }
        else {
            String name = cache.getName();
            status = "I found " + name + " (" + url + ")";
            if (status.length() > Twitter.MAX_TWEET_SIZE) {
                name = name.substring(0, name.length() - (status.length() - Twitter.MAX_TWEET_SIZE) - 3) + "...";
            }
            status = "I found " + name + " (" + url + ")";
            status = Twitter.appendHashTag(status, "cgeo");
            status = Twitter.appendHashTag(status, "geocaching");
        }

        Twitter.postTweet(cgeoapplication.getInstance(), status, null);
    }

    public static void postTweetTrackable(String geocode) {
        final cgTrackable trackable = cgeoapplication.getInstance().getTrackableByGeocode(geocode);
        String name = trackable.getName();
        if (name.length() > 82) {
            name = name.substring(0, 79) + "...";
        }
        StringBuilder builder = new StringBuilder("I touched ");
        builder.append(name);
        if (trackable.getUrl() != null) {
            builder.append(" (").append(trackable.getUrl()).append(')');
        }
        builder.append('!');
        String status = Twitter.appendHashTag(builder.toString(), "cgeo");
        status = Twitter.appendHashTag(status, "geocaching");
        Twitter.postTweet(cgeoapplication.getInstance(), status, null);
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // nothing
        }

        return null;
    }

    public static String urlencode_rfc3986(String text) {
        final String encoded = StringUtils.replace(URLEncoder.encode(text).replace("+", "%20"), "%7E", "~");

        return encoded;
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
    public static Parameters addFToParams(final Parameters params, final boolean my, final boolean addF) {
        if (!my && Settings.isExcludeMyCaches() && addF) {
            if (params == null) {
                return new Parameters("f", "1");
            }
            params.put("f", "1");
            Log.i(Settings.tag, "Skipping caches found or hidden by user.");
        }

        return params;
    }

    static private String prepareParameters(final String baseUri, final Parameters params) {
        return CollectionUtils.isNotEmpty(params) ? baseUri + "?" + params.toString() : baseUri;
    }

    static public String[] requestViewstates(final String uri, final Parameters params, boolean xContentType, boolean my) {
        final HttpResponse response = request(uri, params, xContentType, my, false);

        return getViewstates(getResponseData(response));
    }

    static public String getResponseDataOnError(final HttpResponse response, boolean replaceWhitespace) {
        try {
            String data = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
            return replaceWhitespace ? BaseUtils.replaceWhitespace(data) : data;
        } catch (Exception e) {
            Log.e(Settings.tag, "getResponseData", e);
            return null;
        }
    }

    static public String getResponseData(final HttpResponse response) {
        return getResponseData(response, true);
    }

    static public String getResponseData(final HttpResponse response, boolean replaceWhitespace) {
        if (!isSuccess(response)) {
            return null;
        }
        return getResponseDataOnError(response, replaceWhitespace);
    }

    /**
     * POST HTTP request. Do the request a second time if the user is not logged in
     *
     * @param uri
     * @return
     */
    public static String postRequestLogged(final String uri) {
        HttpResponse response = postRequest(uri, null);
        String data = getResponseData(response);

        if (!getLoginStatus(data)) {
            if (login() == StatusCode.NO_ERROR) {
                response = postRequest(uri, null);
                data = getResponseData(response);
            } else {
                Log.i(Settings.tag, "Working as guest.");
            }
        }
        return data;
    }

    /**
     * GET HTTP request. Do the request a second time if the user is not logged in
     *
     * @param uri
     * @param params
     * @param xContentType
     * @param my
     * @param addF
     * @return
     */
    public static String requestLogged(final String uri, final Parameters params, boolean xContentType, boolean my, boolean addF) {
        HttpResponse response = request(uri, params, xContentType, my, addF);
        String data = getResponseData(response);

        if (!getLoginStatus(data)) {
            if (login() == StatusCode.NO_ERROR) {
                response = request(uri, params, xContentType, my, addF);
                data = getResponseData(response);
            } else {
                Log.i(Settings.tag, "Working as guest.");
            }
        }
        return data;
    }

    /**
     * GET HTTP request
     *
     * @param uri
     * @param params
     * @param xContentType
     * @param my
     * @param addF
     * @return
     */
    public static HttpResponse request(final String uri, final Parameters params, boolean xContentType, boolean my, boolean addF) {
        return request(uri, addFToParams(params, my, addF), xContentType);
    }

    final private static CookieStore cookieStore = new BasicCookieStore();
    private static boolean cookieStoreRestored = false;
    final private static HttpParams clientParams = new BasicHttpParams();

    static {
        clientParams.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
        clientParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
        clientParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);
    }

    public static HttpClient getHttpClient() {
        final DefaultHttpClient client = new DefaultHttpClient();
        client.setCookieStore(cookieStore);
        client.setParams(clientParams);
        return client;
    }

    public static void restoreCookieStore(final String oldCookies) {
        if (!cookieStoreRestored) {
            clearCookies();
            if (oldCookies != null) {
                for (final String cookie : StringUtils.split(oldCookies, ';')) {
                    final String[] split = StringUtils.split(cookie, "=", 3);
                    if (split.length == 3) {
                        final BasicClientCookie newCookie = new BasicClientCookie(split[0], split[1]);
                        newCookie.setDomain(split[2]);
                        cookieStore.addCookie(newCookie);
                    }
                }
            }
            cookieStoreRestored = true;
        }
    }

    public static String dumpCookieStore() {
        StringBuilder cookies = new StringBuilder();
        for (final Cookie cookie : cookieStore.getCookies()) {
            cookies.append(cookie.getName());
            cookies.append('=');
            cookies.append(cookie.getValue());
            cookies.append('=');
            cookies.append(cookie.getDomain());
            cookies.append(';');
        }
        return cookies.toString();
    }

    public static void clearCookies() {
        cookieStore.clear();
    }

    /**
     * POST HTTP request
     *
     * @param uri
     * @param params
     * @return
     */
    public static HttpResponse postRequest(final String uri, final List<? extends NameValuePair> params) {
        try {
            HttpPost request = new HttpPost(uri);
            if (params != null) {
                request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            }
            request.setHeader("X-Requested-With", "XMLHttpRequest");
            return request(request);
        } catch (Exception e) {
            // Can be UnsupportedEncodingException, ClientProtocolException or IOException
            Log.e(Settings.tag, "postRequest", e);
            return null;
        }
    }

    /**
     * GET HTTP request
     *
     * @param uri
     * @param params
     * @param xContentType
     * @return
     */
    public static HttpResponse request(final String uri, final Parameters params, final boolean xContentType) {
        final String fullUri = params == null ? uri : Uri.parse(uri).buildUpon().encodedQuery(params.toString()).build().toString();
        final HttpRequestBase request = new HttpGet(fullUri);

        request.setHeader("X-Requested-With", "XMLHttpRequest");

        if (xContentType) {
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        }

        return request(request);
    }

    private static HttpResponse request(final HttpRequestBase request) {
        request.setHeader("Accept-Charset", "utf-8,iso-8859-1;q=0.8,utf-16;q=0.8,*;q=0.7");
        request.setHeader("Accept-Language", "en-US,*;q=0.9");
        request.getParams().setParameter(CoreProtocolPNames.USER_AGENT, Constants.USER_AGENT);
        return doRequest(request);
    }

    static private String formatTimeSpan(final long before) {
        // don't use String.format in a pure logging routine, it has very bad performance
        return " (" + (System.currentTimeMillis() - before) + " ms) ";
    }

    static public boolean isSuccess(final HttpResponse response) {
        return response != null && response.getStatusLine().getStatusCode() == 200;
    }

    static public HttpResponse doRequest(final HttpRequestBase request) {
        final String reqLogStr = request.getMethod() + " " + hidePassword(request.getURI().toString());
        Log.d(Settings.tag, reqLogStr);

        final HttpClient client = getHttpClient();
        for (int i = 0; i <= NB_DOWNLOAD_RETRIES; i++) {
            final long before = System.currentTimeMillis();
            try {
                final HttpResponse response = client.execute(request);
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    Log.d(Settings.tag, status + formatTimeSpan(before) + reqLogStr);
                } else {
                    Log.w(Settings.tag, status + " [" + response.getStatusLine().getReasonPhrase() + "]" + formatTimeSpan(before) + reqLogStr);
                }
                return response;
            } catch (IOException e) {
                final String timeSpan = formatTimeSpan(before);
                final String tries = (i + 1) + "/" + (NB_DOWNLOAD_RETRIES + 1);
                if (i == NB_DOWNLOAD_RETRIES) {
                    Log.e(Settings.tag, "Failure " + tries + timeSpan + reqLogStr, e);
                } else {
                    Log.e(Settings.tag, "Failure " + tries + " (" + e.toString() + ")" + timeSpan + "- retrying " + reqLogStr);
                }
            }
        }

        return null;
    }

    public static JSONObject requestJSON(final String uri, final Parameters params) {
        final HttpGet request = new HttpGet(prepareParameters(uri, params));
        request.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        request.setHeader("Content-Type", "application/json; charset=UTF-8");
        request.setHeader("X-Requested-With", "XMLHttpRequest");

        final HttpResponse response = doRequest(request);
        if (response != null && response.getStatusLine().getStatusCode() == 200) {
            try {
                return new JSONObject(getResponseData(response));
            } catch (JSONException e) {
                Log.e(Settings.tag, "cgeoBase.requestJSON", e);
            }
        }

        return null;
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            for (final File file : path.listFiles()) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }

        return path.delete();
    }

    public static void storeCache(Activity activity, cgCache origCache, String geocode, int listId, CancellableHandler handler) {
        try {
            cgCache cache;
            // get cache details, they may not yet be complete
            if (origCache != null) {
                // only reload the cache, if it was already stored or has not all details (by checking the description)
                if (origCache.getListId() >= StoredList.STANDARD_LIST_ID || StringUtils.isBlank(origCache.getDescription())) {
                    final SearchResult search = searchByGeocode(origCache.getGeocode(), null, listId, false, null);
                    cache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
                } else {
                    cache = origCache;
                }
            } else if (StringUtils.isNotBlank(geocode)) {
                final SearchResult search = searchByGeocode(geocode, null, listId, false, null);
                cache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
            } else {
                cache = null;
            }

            if (cache == null) {
                if (handler != null) {
                    handler.sendMessage(new Message());
                }

                return;
            }

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            final HtmlImage imgGetter = new HtmlImage(activity, cache.getGeocode(), false, listId, true);

            // store images from description
            if (StringUtils.isNotBlank(cache.getDescription())) {
                Html.fromHtml(cache.getDescription(), imgGetter, null);
            }

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            // store spoilers
            if (CollectionUtils.isNotEmpty(cache.getSpoilers())) {
                for (cgImage oneSpoiler : cache.getSpoilers()) {
                    imgGetter.getDrawable(oneSpoiler.getUrl());
                }
            }

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            // store images from logs
            if (Settings.isStoreLogImages()) {
                for (cgLog log : cache.getLogs(true)) {
                    if (CollectionUtils.isNotEmpty(log.logImages)) {
                        for (cgImage oneLogImg : log.logImages) {
                            imgGetter.getDrawable(oneLogImg.getUrl());
                        }
                    }
                }
            }

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            // store map previews
            StaticMapsProvider.downloadMaps(cache, activity);

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            cache.setListId(listId);
            cgeoapplication.getInstance().saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));

            if (handler != null) {
                handler.sendMessage(new Message());
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgBase.storeCache");
        }
    }

    public static void dropCache(final cgCache cache, final Handler handler) {
        try {
            cgeoapplication.getInstance().markDropped(cache.getGeocode());
            cgeoapplication.getInstance().removeCache(cache.getGeocode(), EnumSet.of(RemoveFlag.REMOVE_CACHE));

            handler.sendMessage(new Message());
        } catch (Exception e) {
            Log.e(Settings.tag, "cgBase.dropCache: " + e.toString());
        }
    }

    public static boolean isInViewPort(int centerLat1, int centerLon1, int centerLat2, int centerLon2, int spanLat1, int spanLon1, int spanLat2, int spanLon2) {
        try {
            // expects coordinates in E6 format
            final int left1 = centerLat1 - (spanLat1 / 2);
            final int right1 = centerLat1 + (spanLat1 / 2);
            final int top1 = centerLon1 + (spanLon1 / 2);
            final int bottom1 = centerLon1 - (spanLon1 / 2);

            final int left2 = centerLat2 - (spanLat2 / 2);
            final int right2 = centerLat2 + (spanLat2 / 2);
            final int top2 = centerLon2 + (spanLon2 / 2);
            final int bottom2 = centerLon2 - (spanLon2 / 2);

            if (left2 <= left1) {
                return false;
            }
            if (right2 >= right1) {
                return false;
            }
            if (top2 >= top1) {
                return false;
            }
            if (bottom2 <= bottom1) {
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(Settings.tag, "cgBase.isInViewPort: " + e.toString());
            return false;
        }
    }

    // viewport is defined by center, span and some (10%) reserve on every side
    /**
     * Check if coordinates are located in a viewport (defined by its center and span
     * in each direction). The viewport also includes a 10% extension on each side.
     *
     * @param centerLat
     *            the viewport center latitude
     * @param centerLon
     *            the viewport center longitude
     * @param spanLat
     *            the latitude span
     * @param spanLon
     *            the longitude span
     * @param coords
     *            the coordinates to check
     * @return true if the coordinates are in the viewport
     */
    public static boolean isCacheInViewPort(int centerLat, int centerLon, int spanLat, int spanLon, final Geopoint coords) {
        return Math.abs(coords.getLatitudeE6() - centerLat) <= Math.abs(spanLat) * 0.6 &&
                Math.abs(coords.getLongitudeE6() - centerLon) <= Math.abs(spanLon) * 0.6;
    }

    private static char[] base64map1 = new char[64];

    static {
        int i = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            base64map1[i++] = c;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            base64map1[i++] = c;
        }
        for (char c = '0'; c <= '9'; c++) {
            base64map1[i++] = c;
        }
        base64map1[i++] = '+';
        base64map1[i++] = '/';
    }
    private static byte[] base64map2 = new byte[128];

    static {
        for (int i = 0; i < base64map2.length; i++) {
            base64map2[i] = -1;
        }
        for (int i = 0; i < 64; i++) {
            base64map2[base64map1[i]] = (byte) i;
        }
    }

    public static String base64Encode(byte[] in) {
        int iLen = in.length;
        int oDataLen = (iLen * 4 + 2) / 3; // output length without padding
        int oLen = ((iLen + 2) / 3) * 4; // output length including padding
        char[] out = new char[oLen];
        int ip = 0;
        int op = 0;

        while (ip < iLen) {
            int i0 = in[ip++] & 0xff;
            int i1 = ip < iLen ? in[ip++] & 0xff : 0;
            int i2 = ip < iLen ? in[ip++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            out[op++] = base64map1[o0];
            out[op++] = base64map1[o1];
            out[op] = op < oDataLen ? base64map1[o2] : '=';
            op++;
            out[op] = op < oDataLen ? base64map1[o3] : '=';
            op++;
        }

        return new String(out);
    }

    public static byte[] base64Decode(String text) {
        char[] in = text.toCharArray();

        int iLen = in.length;
        if (iLen % 4 != 0) {
            throw new IllegalArgumentException("Length of Base64 encoded input string is not a multiple of 4.");
        }
        while (iLen > 0 && in[iLen - 1] == '=') {
            iLen--;
        }
        int oLen = (iLen * 3) / 4;
        byte[] out = new byte[oLen];
        int ip = 0;
        int op = 0;
        while (ip < iLen) {
            int i0 = in[ip++];
            int i1 = in[ip++];
            int i2 = ip < iLen ? in[ip++] : 'A';
            int i3 = ip < iLen ? in[ip++] : 'A';
            if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127) {
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            int b0 = base64map2[i0];
            int b1 = base64map2[i1];
            int b2 = base64map2[i2];
            int b3 = base64map2[i3];
            if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            int o0 = (b0 << 2) | (b1 >>> 4);
            int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
            int o2 = ((b2 & 3) << 6) | b3;
            out[op++] = (byte) o0;
            if (op < oLen) {
                out[op++] = (byte) o1;
            }
            if (op < oLen) {
                out[op++] = (byte) o2;
            }
        }
        return out;
    }

    public static boolean runNavigation(Activity activity, Resources res, Settings settings, final Geopoint coords) {
        return runNavigation(activity, res, settings, coords, null);
    }

    public static boolean runNavigation(Activity activity, Resources res, Settings settings, final Geopoint coords, final Geopoint coordsNow) {
        if (activity == null) {
            return false;
        }
        if (settings == null) {
            return false;
        }

        // Google Navigation
        if (Settings.isUseGoogleNavigation()) {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:ll=" + coords.getLatitude() + "," + coords.getLongitude())));

                return true;
            } catch (Exception e) {
                // nothing
            }
        }

        // Google Maps Directions
        try {
            if (coordsNow != null) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?f=d&saddr=" + coordsNow.getLatitude() + "," + coordsNow.getLongitude() +
                                "&daddr=" + coords.getLatitude() + "," + coords.getLongitude())));
            } else {
                activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?f=d&daddr=" + coords.getLatitude() + "," + coords.getLongitude())));
            }

            return true;
        } catch (Exception e) {
            // nothing
        }

        Log.i(Settings.tag, "cgBase.runNavigation: No navigation application available.");

        if (res != null) {
            ActivityMixin.showToast(activity, res.getString(R.string.err_navigation_no));
        }

        return false;
    }

    public static Double getElevation(final Geopoint coords) {
        try {
            final String uri = "http://maps.googleapis.com/maps/api/elevation/json";
            final Parameters params = new Parameters(
                    "sensor", "false",
                    "locations", coords.format(Format.LAT_LON_DECDEGREE_COMMA));
            final JSONObject response = requestJSON(uri, params);

            if (response == null) {
                return null;
            }

            if (!StringUtils.equalsIgnoreCase(response.getString("status"), "OK")) {
                return null;
            }

            if (response.has("results")) {
                JSONArray results = response.getJSONArray("results");
                JSONObject result = results.getJSONObject(0);
                return result.getDouble("elevation");
            }
        } catch (Exception e) {
            Log.w(Settings.tag, "cgBase.getElevation: " + e.toString());
        }

        return null;
    }

    /**
     * Generate a time string according to system-wide settings (locale, 12/24 hour)
     * such as "13:24".
     *
     * @param date
     *            milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatTime(long date) {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_TIME);
    }

    /**
     * Generate a date string according to system-wide settings (locale, date format)
     * such as "20 December" or "20 December 2010". The year will only be included when necessary.
     *
     * @param date
     *            milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatDate(long date) {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE);
    }

    /**
     * Generate a date string according to system-wide settings (locale, date format)
     * such as "20 December 2010". The year will always be included, making it suitable
     * to generate long-lived log entries.
     *
     * @param date
     *            milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatFullDate(long date) {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_YEAR);
    }

    /**
     * Generate a numeric date string according to system-wide settings (locale, date format)
     * such as "10/20/2010".
     *
     * @param date
     *            milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatShortDate(long date) {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_NUMERIC_DATE);
    }

    /**
     * Generate a numeric date and time string according to system-wide settings (locale,
     * date format) such as "7 sept. at 12:35".
     *
     * @param context
     *            a Context
     * @param date
     *            milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatShortDateTime(Context context, long date) {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL);
    }

    /**
     * insert text into the EditText at the current cursor position
     *
     * @param editText
     * @param insertText
     * @param moveCursor
     *            place the cursor after the inserted text
     */
    public static void insertAtPosition(final EditText editText, final String insertText, final boolean moveCursor) {
        int selectionStart = editText.getSelectionStart();
        int selectionEnd = editText.getSelectionEnd();
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);

        final String content = editText.getText().toString();
        String completeText;
        if (start > 0 && !Character.isWhitespace(content.charAt(start - 1))) {
            completeText = " " + insertText;
        } else {
            completeText = insertText;
        }

        editText.getText().replace(start, end, completeText);
        int newCursor = moveCursor ? start + completeText.length() : start;
        editText.setSelection(newCursor, newCursor);
    }

    public static LinearLayout createStarRating(final float value, final int count, final Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout starsContainer = new LinearLayout(context);
        starsContainer.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < count; i++) {
            ImageView star = (ImageView) inflater.inflate(R.layout.star, null);
            if (value - i >= 0.75) {
                star.setImageResource(R.drawable.star_on);
            } else if (value - i >= 0.25) {
                star.setImageResource(R.drawable.star_half);
            } else {
                star.setImageResource(R.drawable.star_off);
            }
            starsContainer.addView(star);
        }

        return starsContainer;
    }

    public static boolean isActualLoginStatus() {
        return actualLoginStatus;
    }

    private static void setActualLoginStatus(boolean actualLoginStatus) {
        cgBase.actualLoginStatus = actualLoginStatus;
    }

    public static String getActualUserName() {
        return actualUserName;
    }

    private static void setActualUserName(String actualUserName) {
        cgBase.actualUserName = actualUserName;
    }

    public static String getActualMemberStatus() {
        return actualMemberStatus;
    }

    private static void setActualMemberStatus(final String actualMemberStatus) {
        cgBase.actualMemberStatus = actualMemberStatus;
    }

    public static int getActualCachesFound() {
        return actualCachesFound;
    }

    private static void setActualCachesFound(final int actualCachesFound) {
        cgBase.actualCachesFound = actualCachesFound;
    }

    public static String getActualStatus() {
        return actualStatus;
    }

    private static void setActualStatus(final String actualStatus) {
        cgBase.actualStatus = actualStatus;
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * From: http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
     *
     * @param context
     *            The application's environment.
     * @param action
     *            The Intent action to check for availability.
     *
     * @return True if an Intent with the specified action can be sent and
     *         responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

}

