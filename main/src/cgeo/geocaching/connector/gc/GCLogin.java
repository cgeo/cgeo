package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLogin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.AvatarUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Single;
import okhttp3.Response;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class GCLogin extends AbstractLogin {

    private static final String LOGIN_URI = "https://www.geocaching.com/account/signin";
    private static final String REQUEST_VERIFICATION_TOKEN = "__RequestVerificationToken";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ServerParameters serverParameters = null;

    private static class StatusException extends RuntimeException {
        private static final long serialVersionUID = -597420116705938433L;
        final StatusCode statusCode;

        StatusException(final StatusCode statusCode) {
            super("Status code: " + statusCode);
            this.statusCode = statusCode;
        }
    }

    /**
     * <pre>
     * var serverParameters = {
     *   "user:info": {
     *      "username": "gc-user-name",
     *      "referenceCode": "PR....",
     *      "userType": "Premium",
     *      "isLoggedIn": true,
     *      "dateFormat": "dd.MM.yyyy",
     *      "unitSetName": "Metric",
     *      "roles": [
     *         "Public",
     *         "Premium"
     *         ],
     *     "publicGuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
     *     "avatarUrl": "https://img.geocaching.com/avatar/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.png"
     *      },
     *  "app:options": {
     *       "localRegion": "en-US",
     *       "endpoints": null,
     *       "coordInfoUrl": "https://coord.info",
     *       "paymentUrl": "https://payments.geocaching.com"
     *   }
     * };
     * </pre>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServerParameters {
        @JsonProperty("user:info")
        UserInfo userInfo;
        @JsonProperty("app:options")
        AppOptions appOptions;

        @JsonIgnoreProperties(ignoreUnknown = true)
        static final class UserInfo {
            @JsonProperty("username")
            String username;
            @JsonProperty("referenceCode")
            String referenceCode;
            @JsonProperty("userType")
            String userType;
            @JsonProperty("isLoggedIn")
            boolean isLoggedIn;
            @JsonProperty("dateFormat")
            String dateFormat;
            @JsonProperty("unitSetName")
            String unitSetName;
            @JsonProperty("roles")
            String[] roles;
            @JsonProperty("publicGuid")
            String publicGuid;
            @JsonProperty("avatarUrl")
            String avatarUrl;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static final class AppOptions {
            @JsonProperty("localRegion")
            String localRegion;
            @JsonProperty("endpoints")
            String endpoints;
            @JsonProperty("coordInfoUrl")
            String coordInfoUrl;
            @JsonProperty("paymentUrl")
            String paymentUrl;
        }
    }


    private GCLogin() {
        // singleton
    }

    public static GCLogin getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final GCLogin INSTANCE = new GCLogin();
    }

    private static StatusCode resetGcCustomDate(final StatusCode statusCode) {
        Settings.setGcCustomDate(GCConstants.DEFAULT_GC_DATE);
        return statusCode;
    }

    @Override
    @NonNull
    protected StatusCode login(final boolean retry) {
        return login(retry, Settings.getCredentials(GCConnector.getInstance()));
    }

    @Override
    @NonNull
    protected StatusCode login(final boolean retry, @NonNull final Credentials credentials) {
        final StatusCode status = loginInternal(retry, credentials);
        if (status != StatusCode.NO_ERROR) {
            resetLoginStatus();
        }
        return status;
    }

    private void logLastLoginError(final String status) {
        Settings.setLastLoginErrorGC(status);
        Log.w("Login.login: " + status);
    }

    @WorkerThread
    private StatusCode loginInternal(final boolean retry, @NonNull final Credentials credentials) {
        if (credentials.isInvalid()) {
            clearLoginInfo();
            logLastLoginError("No login information stored");
            return resetGcCustomDate(StatusCode.NO_LOGIN_INFO_STORED);
        }

        final String username = credentials.getUserName();

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_working));
        try {
            final String tryLoggedInData = getLoginPage();

            if (StringUtils.isBlank(tryLoggedInData)) {
                logLastLoginError("Failed to retrieve login page (1st)");
                return StatusCode.CONNECTION_FAILED; // no login page
            }

            if (getLoginStatus(tryLoggedInData)) {
                Log.i("Already logged in Geocaching.com as " + username + " (" + Settings.getGCMemberStatus() + ')');
                return completeLoginProcess();
            }

            final String requestVerificationToken = extractRequestVerificationToken(tryLoggedInData);
            if (StringUtils.isEmpty(requestVerificationToken)) {
                logLastLoginError("failed to find request verification token");
                return StatusCode.LOGIN_PARSE_ERROR;
            }

            final String loginData = postCredentials(credentials, requestVerificationToken);
            if (StringUtils.isBlank(loginData)) {
                logLastLoginError("Failed to retrieve login page (2nd)");
                // FIXME: should it be CONNECTION_FAILED to match the first attempt?
                return StatusCode.COMMUNICATION_ERROR; // no login page
            }

            if (getLoginStatus(loginData)) {
                Log.i("Successfully logged in Geocaching.com as " + username + " (" + Settings.getGCMemberStatus() + ')');
                return completeLoginProcess();
            }

            if (loginData.contains("<div class=\"g-recaptcha\" data-sitekey=\"")) {
                logLastLoginError("Failed to log in to geocaching.com due to captcha required");
                return resetGcCustomDate(StatusCode.LOGIN_CAPTCHA_ERROR);
            }

            if (loginData.contains("id=\"signup-validation-error\"")) {
                logLastLoginError("Failed to log in to geocaching.com as " + username + " because of wrong username/password");
                return resetGcCustomDate(StatusCode.WRONG_LOGIN_DATA); // wrong login
            }

            if (loginData.contains("content=\"account/join/success\"")) {
                logLastLoginError("Failed to log in Geocaching.com as " + username + " because account needs to be validated first");
                return resetGcCustomDate(StatusCode.UNVALIDATED_ACCOUNT);
            }

            logLastLoginError("Failed to log in Geocaching.com as " + username + " for some unknown reason");
            if (retry) {
                getLoginStatus(loginData);
                return login(false, credentials);
            }

            logLastLoginError("Unknown error");
            return resetGcCustomDate(StatusCode.UNKNOWN_ERROR); // can't login
        } catch (final StatusException status) {
            return status.statusCode;
        } catch (final Exception ignored) {
            logLastLoginError("communication error");
            return StatusCode.CONNECTION_FAILED;
        }
    }

    @WorkerThread
    public StatusCode logout() {
        try {
            getResponseBodyOrStatus(Network.postRequest("https://www.geocaching.com/account/logout", null).blockingGet());
            resetServerParameters();
        } catch (final StatusException status) {
            return status.statusCode;
        } catch (final Exception ignored) {
        }

        resetLoginStatus();
        return StatusCode.NO_ERROR;
    }

    private String getResponseBodyOrStatus(final Response response) {
        final String body;
        try {
            body = response.body().string();
        } catch (final IOException ignore) {
            throw new StatusException(StatusCode.COMMUNICATION_ERROR);
        }
        if (response.code() == 503 && TextUtils.matches(body, GCConstants.PATTERN_MAINTENANCE)) {
            throw new StatusException(StatusCode.MAINTENANCE);
        } else if (!response.isSuccessful()) {
            throw new StatusException(StatusCode.COMMUNICATION_ERROR);
        }
        return body;
    }

    @WorkerThread
    private String getLoginPage() {
        return getResponseBodyOrStatus(Network.getRequest(LOGIN_URI).blockingGet());
    }

    @Nullable
    private String extractRequestVerificationToken(final String page) {
        final Document document = Jsoup.parse(page);
        final String value = document.select("form > input[name=\"" + REQUEST_VERIFICATION_TOKEN + "\"]").attr("value");
        return StringUtils.isNotEmpty(value) ? value : null;
    }

    @WorkerThread
    private String postCredentials(final Credentials credentials, final String requestVerificationToken) {
        final Parameters params = new Parameters("UsernameOrEmail", credentials.getUserName(),
                "Password", credentials.getPassword(), REQUEST_VERIFICATION_TOKEN, requestVerificationToken);
        return getResponseBodyOrStatus(Network.postRequest(LOGIN_URI, params).blockingGet());
    }

    /**
     * Check if the user has been logged in when he retrieved the data.
     *
     * @return {@code true} if user is logged in, {@code false} otherwise
     */
    boolean getLoginStatus(@Nullable final String page) {
        if (StringUtils.isBlank(page)) {
            Log.w("Login.checkLogin: No page given");
            return false;
        }

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_ok));

        final String username = GCParser.getUsername(page);
        setActualLoginStatus(StringUtils.isNotBlank(username));
        if (isActualLoginStatus()) {
            setActualUserName(username);
            final int cachesCount = GCParser.getCachesCount(page);
            setActualCachesFound(cachesCount);
            return true;
        }

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_failed));
        return false;
    }

    public String getWebsiteLanguage() {
        try {
            final ServerParameters params = getServerParameters();
            return params.appOptions.localRegion;
        } catch (final Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * Ensure that the website is presented in the specified language.
     *
     * Used for unit tests.
     *
     * @param language the language code to be used at geocaching.com (e.g. "en-US")
     * @return {@code true} if a switch was necessary and successfully performed (different language -> target language)
     */
    @WorkerThread
    @VisibleForTesting
    public boolean switchToLanguage(final String language) {
        if (getWebsiteLanguage().equals(language)) {
            Log.i("Geocaching.com language already set to " + language);
        } else {
            try {
                final String page = Network.getResponseData(Network.getRequest("https://www.geocaching.com/play/culture/set?model.SelectedCultureCode=" + language));
                Log.i("changed language on geocaching.com to " + language);
                resetServerParameters();
                return getLoginStatus(page);
            } catch (final Exception ignored) {
                Log.e("Failed to set geocaching.com language to " + language);
            }
        }
        return false;
    }

    /**
     * Retrieve the home location
     *
     * @return a Single containing the home location, or IOException
     */
    static Single<String> retrieveHomeLocation() {
        return Network.getResponseDocument(Network.getRequest("https://www.geocaching.com/account/settings/homelocation"))
                .map(document -> {
                    final Document innerHtml = Jsoup.parse(document.getElementById("tplSearchCoords").html());
                    return innerHtml.select("input.search-coordinates").attr("value");
                });
    }

    private static void setHomeLocation() {
        retrieveHomeLocation().subscribe(homeLocationStr -> {
            if (StringUtils.isNotBlank(homeLocationStr) && !StringUtils.equals(homeLocationStr, Settings.getHomeLocation())) {
                Log.i("Setting home location to " + homeLocationStr);
                Settings.setHomeLocation(homeLocationStr);
            }
        }, throwable -> Log.w("Unable to retrieve the home location"));
    }

    @WorkerThread
    public ServerParameters getServerParameters() {
        if (serverParameters != null) {
            return serverParameters;
        }

        final Response response = Network.getRequest("https://www.geocaching.com/play/serverparameters/params").blockingGet();
        try {
            final String javascriptBody = response.body().string();
            final String jsonBody = javascriptBody.subSequence(javascriptBody.indexOf('{'), javascriptBody.lastIndexOf(';')).toString();
            serverParameters = MAPPER.readValue(jsonBody, ServerParameters.class);

            if (StringUtils.isNotBlank(serverParameters.userInfo.dateFormat)) {
                Log.d("Setting GCCustomDate to " + serverParameters.userInfo.dateFormat);
                Settings.setGcCustomDate(serverParameters.userInfo.dateFormat);
            }

            final GCMemberState memberState = GCMemberState.fromString(serverParameters.userInfo.userType);
            Log.d("Setting member status to " + memberState);
            Settings.setGCMemberStatus(memberState);

            if (StringUtils.isNotBlank(serverParameters.userInfo.avatarUrl)) {
                final String avatarUrl = serverParameters.userInfo.avatarUrl.replace("/avatar/", "/user/large/");
                Log.d("Setting avatar to " + avatarUrl);
                AvatarUtils.changeAvatar(GCConnector.getInstance(), avatarUrl);
            }

        } catch (final IOException e) {
            Settings.setGcCustomDate(GCConstants.DEFAULT_GC_DATE);
            Log.e("Error loading serverparameters", e);
            return null;
        }

        return serverParameters;
    }

    public void resetServerParameters() {
        serverParameters = null;
    }

    public static Date parseGcCustomDate(final String input, final String format) throws ParseException {
        return new SimpleDateFormat(format, Locale.ENGLISH).parse(input.trim());
    }

    static Date parseGcCustomDate(final String input) throws ParseException {
        return parseGcCustomDate(input, Settings.getGcCustomDate());
    }

    static String formatGcCustomDate(final int year, final int month, final int day) {
        return new SimpleDateFormat(Settings.getGcCustomDate(), Locale.ENGLISH).format(new GregorianCalendar(year, month - 1, day).getTime());
    }

    /**
     * checks if an Array of Strings is empty or not. Empty means:
     * - Array is null
     * - or all elements are null or empty strings
     */
    public static boolean isEmpty(final String[] a) {
        if (a == null) {
            return true;
        }

        for (final String s : a) {
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
    public static String[] getViewstates(final String page) {
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
            } catch (final NumberFormatException e) {
                Log.e("getViewStates", e);
            }
        }

        final String[] viewstates = new String[count];

        // Get the viewstates
        final MatcherWrapper matcherViewstates = new MatcherWrapper(GCConstants.PATTERN_VIEWSTATES, page);
        while (matcherViewstates.find()) {
            final String sno = matcherViewstates.group(1); // number of viewstate
            int no;
            if (StringUtils.isEmpty(sno)) {
                no = 0;
            } else {
                try {
                    no = Integer.parseInt(sno);
                } catch (final NumberFormatException e) {
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
    static void putViewstates(final Parameters params, final String[] viewstates) {
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
    static void transferViewstates(final String page, final Parameters params) {
        putViewstates(params, getViewstates(page));
    }

    /**
     * POST HTTP request. Do the request a second time if the user is not logged in
     */
    @WorkerThread
    String postRequestLogged(final String uri, final Parameters params) {
        final String data = Network.getResponseData(Network.postRequest(uri, params));

        if (getLoginStatus(data)) {
            return data;
        }

        if (login() == StatusCode.NO_ERROR) {
            return Network.getResponseData(Network.postRequest(uri, params));
        }

        Log.i("Working as guest.");
        return data;
    }

    /**
     * GET HTTP request. Do the request a second time if the user is not logged in
     */
    @Nullable
    @WorkerThread
    String getRequestLogged(@NonNull final String uri, @Nullable final Parameters params) {
        try {
            final Response response = Network.getRequest(uri, params).blockingGet();
            final String data = Network.getResponseData(response, canRemoveWhitespace(uri));

            // A page not found will not be found if the user logs in either
            if (response.code() == 404 || getLoginStatus(data)) {
                return data;
            }

            if (login() == StatusCode.NO_ERROR) {
                return Network.getResponseData(Network.getRequest(uri, params), canRemoveWhitespace(uri));
            }

            Log.w("Working as guest.");
            return data;
        } catch (final Exception e) {
            Log.e("Exception in GCLogin.getRequestLogged", e);
            return null;
        }
    }

    /**
     * Unfortunately the cache details page contains user generated whitespace in the personal note, therefore we cannot
     * remove the white space from cache details pages.
     */
    private static boolean canRemoveWhitespace(final String uri) {
        return !StringUtils.contains(uri, "cache_details");
    }

    private StatusCode completeLoginProcess() {
        setHomeLocation();
        getServerParameters();
        // Force token retrieval to avoid avalanche requests
        GCWebAPI.getAuthorizationHeader().subscribe();
        Settings.setLastLoginSuccessGC();
        return StatusCode.NO_ERROR; // logged in
    }

}
