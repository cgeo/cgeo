package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import android.graphics.drawable.BitmapDrawable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class Login {

    private final static String ENGLISH = "<a href=\"#\">English&#9660;</a>";

    // false = not logged in
    private static boolean actualLoginStatus = false;
    private static String actualUserName = "";
    private static int actualCachesFound = -1;
    private static String actualStatus = "";

    private final static Map<String, SimpleDateFormat> gcCustomDateFormats;
    public static final String LANGUAGE_CHANGE_URI = "http://www.geocaching.com/my/souvenirs.aspx";

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

    public static StatusCode login() {
        return login(true);
    }

    private static StatusCode login(boolean retry) {
        final ImmutablePair<String, String> login = Settings.getLogin();

        if (login == null || StringUtils.isEmpty(login.left) || StringUtils.isEmpty(login.right)) {
            Login.setActualStatus(cgeoapplication.getInstance().getString(R.string.err_login));
            Log.e("Login.login: No login information stored");
            return StatusCode.NO_LOGIN_INFO_STORED;
        }

        Login.setActualStatus(cgeoapplication.getInstance().getString(R.string.init_login_popup_working));
        HttpResponse loginResponse = Network.getRequest("https://www.geocaching.com/login/default.aspx");
        String loginData = Network.getResponseData(loginResponse);
        if (loginResponse != null && loginResponse.getStatusLine().getStatusCode() == 503 && BaseUtils.matches(loginData, GCConstants.PATTERN_MAINTENANCE)) {
            return StatusCode.MAINTENANCE;
        }

        if (StringUtils.isBlank(loginData)) {
            Log.e("Login.login: Failed to retrieve login page (1st)");
            return StatusCode.CONNECTION_FAILED; // no loginpage
        }

        if (Login.getLoginStatus(loginData)) {
            Log.i("Already logged in Geocaching.com as " + login.left + " (" + Settings.getMemberStatus() + ')');
            Login.switchToEnglish(loginData);
            return StatusCode.NO_ERROR; // logged in
        }

        Cookies.clearCookies();
        Settings.setCookieStore(null);

        final Parameters params = new Parameters(
                "__EVENTTARGET", "",
                "__EVENTARGUMENT", "",
                "ctl00$ContentBody$tbUsername", login.left,
                "ctl00$ContentBody$tbPassword", login.right,
                "ctl00$ContentBody$cbRememberMe", "on",
                "ctl00$ContentBody$btnSignIn", "Login");
        final String[] viewstates = Login.getViewstates(loginData);
        if (isEmpty(viewstates)) {
            Log.e("Login.login: Failed to find viewstates");
            return StatusCode.LOGIN_PARSE_ERROR; // no viewstates
        }
        Login.putViewstates(params, viewstates);

        loginResponse = Network.postRequest("https://www.geocaching.com/login/default.aspx", params);
        loginData = Network.getResponseData(loginResponse);

        if (StringUtils.isBlank(loginData)) {
            Log.e("Login.login: Failed to retrieve login page (2nd)");
            // FIXME: should it be CONNECTION_FAILED to match the first attempt?
            return StatusCode.COMMUNICATION_ERROR; // no login page
        }

        if (Login.getLoginStatus(loginData)) {
            Log.i("Successfully logged in Geocaching.com as " + login.left + " (" + Settings.getMemberStatus() + ')');

            Login.switchToEnglish(loginData);
            Settings.setCookieStore(Cookies.dumpCookieStore());

            return StatusCode.NO_ERROR; // logged in
        }

        if (loginData.contains("Your username/password combination does not match.")) {
            Log.i("Failed to log in Geocaching.com as " + login.left + " because of wrong username/password");
            return StatusCode.WRONG_LOGIN_DATA; // wrong login
        }

        Log.i("Failed to log in Geocaching.com as " + login.left + " for some unknown reason");
        if (retry) {
            Login.switchToEnglish(loginData);
            return login(false);
        }

        return StatusCode.UNKNOWN_ERROR; // can't login
    }

    public static StatusCode logout() {
        HttpResponse logoutResponse = Network.getRequest("https://www.geocaching.com/login/default.aspx?RESET=Y&redir=http%3a%2f%2fwww.geocaching.com%2fdefault.aspx%3f");
        String logoutData = Network.getResponseData(logoutResponse);
        if (logoutResponse != null && logoutResponse.getStatusLine().getStatusCode() == 503 && BaseUtils.matches(logoutData, GCConstants.PATTERN_MAINTENANCE)) {
            return StatusCode.MAINTENANCE;
        }

        Cookies.clearCookies();
        Settings.setCookieStore(null);
        return StatusCode.NO_ERROR;
    }

    static void setActualCachesFound(final int found) {
        actualCachesFound = found;
    }

    public static String getActualStatus() {
        return actualStatus;
    }

    private static void setActualStatus(final String status) {
        actualStatus = status;
    }

    public static boolean isActualLoginStatus() {
        return actualLoginStatus;
    }

    private static void setActualLoginStatus(boolean loginStatus) {
        actualLoginStatus = loginStatus;
    }

    public static String getActualUserName() {
        return actualUserName;
    }

    private static void setActualUserName(String userName) {
        actualUserName = userName;
    }

    public static int getActualCachesFound() {
        return actualCachesFound;
    }

    /**
     * Check if the user has been logged in when he retrieved the data.
     *
     * @param page
     * @return <code>true</code> if user is logged in, <code>false</code> otherwise
     */
    public static boolean getLoginStatus(final String page) {
        if (StringUtils.isBlank(page)) {
            Log.e("Login.checkLogin: No page given");
            return false;
        }

        setActualStatus(cgeoapplication.getInstance().getString(R.string.init_login_popup_ok));

        // on every page except login page
        setActualLoginStatus(BaseUtils.matches(page, GCConstants.PATTERN_LOGIN_NAME));
        if (isActualLoginStatus()) {
            setActualUserName(BaseUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME, true, "???"));
            setActualCachesFound(Integer.parseInt(BaseUtils.getMatch(page, GCConstants.PATTERN_CACHES_FOUND, true, "0").replaceAll("[,.]", "")));
            Settings.setMemberStatus(BaseUtils.getMatch(page, GCConstants.PATTERN_MEMBER_STATUS, true, null));
            if ( page.contains(GCConstants.MEMBER_STATUS_RENEW) ) {
                Settings.setMemberStatus(GCConstants.MEMBER_STATUS_PM);
            }
            return true;
        }

        // login page
        setActualLoginStatus(BaseUtils.matches(page, GCConstants.PATTERN_LOGIN_NAME_LOGIN_PAGE));
        if (isActualLoginStatus()) {
            setActualUserName(Settings.getUsername());
            // number of caches found is not part of this page
            return true;
        }

        setActualStatus(cgeoapplication.getInstance().getString(R.string.init_login_popup_failed));
        return false;
    }

    private static void switchToEnglish(String previousPage) {
        if (previousPage != null && previousPage.contains(ENGLISH)) {
            Log.i("Geocaching.com language already set to English");
            // get find count
            getLoginStatus(Network.getResponseData(Network.getRequest("http://www.geocaching.com/email/")));
        } else {
            final String page = Network.getResponseData(Network.getRequest(LANGUAGE_CHANGE_URI));
            getLoginStatus(page);
            if (page == null) {
                Log.e("Failed to read viewstates to set geocaching.com language");
            }
            final Parameters params = new Parameters(
                    "__EVENTTARGET", "ctl00$uxLocaleList$uxLocaleList$ctl00$uxLocaleItem", // switch to english
                    "__EVENTARGUMENT", "");
            Login.transferViewstates(page, params);
            final HttpResponse response = Network.postRequest(LANGUAGE_CHANGE_URI, params, new Parameters("Referer", LANGUAGE_CHANGE_URI));
            if (Network.isSuccess(response)) {
                Log.i("changed language on geocaching.com to English");
            } else {
                Log.e("Failed to set geocaching.com language to English");
            }
        }
    }

    public static BitmapDrawable downloadAvatarAndGetMemberStatus() {
        try {
            final String profile = BaseUtils.replaceWhitespace(Network.getResponseData(Network.getRequest("http://www.geocaching.com/my/")));

            Settings.setMemberStatus(BaseUtils.getMatch(profile, GCConstants.PATTERN_MEMBER_STATUS, true, null));
            if (profile.contains(GCConstants.MEMBER_STATUS_RENEW)) {
                Settings.setMemberStatus(GCConstants.MEMBER_STATUS_PM);
            }

            setActualCachesFound(Integer.parseInt(BaseUtils.getMatch(profile, GCConstants.PATTERN_CACHES_FOUND, true, "-1").replaceAll("[,.]", "")));

            final String avatarURL = BaseUtils.getMatch(profile, GCConstants.PATTERN_AVATAR_IMAGE_PROFILE_PAGE, false, null);
            if (null != avatarURL) {
                final HtmlImage imgGetter = new HtmlImage("", false, 0, false);
                return imgGetter.getDrawable(avatarURL);
            }
            // No match? There may be no avatar set by user.
            Log.d("No avatar set for user");
        } catch (Exception e) {
            Log.w("Error when retrieving user avatar", e);
        }
        return null;
    }

    /**
     * Detect user date settings on geocaching.com
     */
    public static void detectGcCustomDate() {

        final String result = Network.getResponseData(Network.getRequest("http://www.geocaching.com/account/ManagePreferences.aspx"));

        if (null == result) {
            Log.w("Login.detectGcCustomDate: result is null");
            return;
        }

        String customDate = BaseUtils.getMatch(result, GCConstants.PATTERN_CUSTOMDATE, true, null);
        if (null != customDate) {
            Settings.setGcCustomDate(customDate);
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
        final MatcherWrapper matcherViewstateCount = new MatcherWrapper(GCConstants.PATTERN_VIEWSTATEFIELDCOUNT, page);
        if (matcherViewstateCount.find()) {
            try {
                count = Integer.parseInt(matcherViewstateCount.group(1));
            } catch (NumberFormatException e) {
                Log.e("getViewStates", e);
            }
        }

        String[] viewstates = new String[count];

        // Get the viewstates
        final MatcherWrapper matcherViewstates = new MatcherWrapper(GCConstants.PATTERN_VIEWSTATES, page);
        while (matcherViewstates.find()) {
            String sno = matcherViewstates.group(1); // number of viewstate
            int no;
            if (StringUtils.isEmpty(sno)) {
                no = 0;
            } else {
                try {
                    no = Integer.parseInt(sno);
                } catch (NumberFormatException e) {
                    Log.e("getViewStates", e);
                    no = 0;
                }
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
    public static void putViewstates(final Parameters params, final String[] viewstates) {
        if (ArrayUtils.isEmpty(viewstates)) {
            return;
        }
        params.put("__VIEWSTATE", viewstates[0]);
        if (viewstates.length > 1) {
            for (int i = 1; i < viewstates.length; i++) {
                params.put("__VIEWSTATE" + i, viewstates[i]);
            }
            params.put("__VIEWSTATEFIELDCOUNT", String.valueOf(viewstates.length));
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
     * POST HTTP request. Do the request a second time if the user is not logged in
     *
     * @param uri
     * @return
     */
    public static String postRequestLogged(final String uri, final Parameters params) {
        HttpResponse response = Network.postRequest(uri, params);
        String data = Network.getResponseData(response);

        if (!getLoginStatus(data)) {
            if (login() == StatusCode.NO_ERROR) {
                response = Network.postRequest(uri, params);
                data = Network.getResponseData(response);
            } else {
                Log.i("Working as guest.");
            }
        }
        return data;
    }

    /**
     * GET HTTP request. Do the request a second time if the user is not logged in
     *
     * @param uri
     * @param params
     * @return
     */
    public static String getRequestLogged(final String uri, final Parameters params) {
        final String data = Network.getResponseData(Network.getRequest(uri, params));

        if (getLoginStatus(data)) {
            return data;
        }

        if (login() == StatusCode.NO_ERROR) {
            return Network.getResponseData(Network.getRequest(uri, params));
        }

        Log.w("Working as guest.");
        return data;
    }

    /** Get user session & session token from the Live Map. Needed for following requests */
    public static String[] getMapTokens() {
        final HttpResponse response = Network.getRequest(GCConstants.URL_LIVE_MAP);
        final String data = Network.getResponseData(response);
        final String userSession = BaseUtils.getMatch(data, GCConstants.PATTERN_USERSESSION, "");
        final String sessionToken = BaseUtils.getMatch(data, GCConstants.PATTERN_SESSIONTOKEN, "");
        return new String[] { userSession, sessionToken };
    }
}
