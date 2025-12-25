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
import cgeo.geocaching.connector.AbstractLogin
import cgeo.geocaching.databinding.GcManualLoginBinding
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.network.Network
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.settings.Credentials
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.AvatarUtils
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MatcherWrapper
import cgeo.geocaching.utils.TextUtils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.List
import java.util.Locale

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.Response
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class GCLogin : AbstractLogin() {

    private static val LOGIN_URI: String = "https://www.geocaching.com/account/signin?returnUrl=%2Faccount%2Fsettings%2Fhomelocation"
    private static val REQUEST_VERIFICATION_TOKEN: String = "__RequestVerificationToken"
    private static val MAPPER: ObjectMapper = ObjectMapper()

    private var serverParameters: ServerParameters = null
    private var homeLocationPage: String = null

    private static class StatusException : RuntimeException() {
        private static val serialVersionUID: Long = -597420116705938433L
        final StatusCode statusCode

        StatusException(final StatusCode statusCode) {
            super("Status code: " + statusCode)
            this.statusCode = statusCode
        }
    }

    /*
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
     * }
     * </pre>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServerParameters {
        @JsonProperty("user:info")
        UserInfo userInfo
        @JsonProperty("app:options")
        AppOptions appOptions

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class UserInfo {
            @JsonProperty("username")
            String username
            @JsonProperty("referenceCode")
            String referenceCode
            @JsonProperty("userType")
            String userType
            @JsonProperty("isLoggedIn")
            Boolean isLoggedIn
            @JsonProperty("dateFormat")
            String dateFormat
            @JsonProperty("unitSetName")
            String unitSetName
            @JsonProperty("roles")
            String[] roles
            @JsonProperty("publicGuid")
            String publicGuid
            @JsonProperty("avatarUrl")
            String avatarUrl
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class AppOptions {
            @JsonProperty("localRegion")
            String localRegion
            @JsonProperty("endpoints")
            String endpoints
            @JsonProperty("coordInfoUrl")
            String coordInfoUrl
            @JsonProperty("paymentUrl")
            String paymentUrl
        }
    }


    private GCLogin() {
        // singleton
    }

    public static GCLogin getInstance() {
        return SingletonHolder.INSTANCE
    }

    private static class SingletonHolder {
        private static val INSTANCE: GCLogin = GCLogin()
    }

    private static StatusCode resetGcCustomDate(final StatusCode statusCode) {
        Settings.setGcCustomDate(GCConstants.DEFAULT_GC_DATE)
        return statusCode
    }

    override     protected StatusCode login(final Boolean retry) {
        return login(retry, Settings.getCredentials(GCConnector.getInstance()))
    }

    override     protected StatusCode login(final Boolean retry, final Credentials credentials) {
        val status: StatusCode = loginInternal(retry, credentials)
        if (status != StatusCode.NO_ERROR) {
            resetLoginStatus()
        }
        return status
    }

    private Unit logLastLoginError(final String status, final Boolean retry) {
        logLastLoginError(status, retry, "")
    }

    private Unit logLastLoginError(final String status, final Boolean retry, final String additionalLogInfo) {
        val retryMarker: String = " // "
        val currentStatus: String = Settings.getLastLoginErrorGC() == null || Settings.getLastLoginErrorGC().first == null ? "" : Settings.getLastLoginErrorGC().first
        if (!retry && currentStatus.endsWith(retryMarker)) {
            Settings.setLastLoginErrorGC(currentStatus + status)
        } else {
            Settings.setLastLoginErrorGC(status + retryMarker)
        }
        Log.w("Login.login: " + status + " (retry=" + retry + ") [" + additionalLogInfo + "]")
    }

    @WorkerThread
    private StatusCode loginInternal(final Boolean retry, final Credentials credentials) {
        val ctx: Context = CgeoApplication.getInstance()

        if (credentials.isInvalid()) {
            clearLoginInfo()
            logLastLoginError(ctx.getString(R.string.err_auth_gc_missing_login), retry)
            return resetGcCustomDate(StatusCode.NO_LOGIN_INFO_STORED)
        }

        val username: String = credentials.getUserName()

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_working))
        try {
            val tryLoggedInData: String = getLoginPage()

            if (StringUtils.isBlank(tryLoggedInData)) {
                logLastLoginError(ctx.getString(R.string.err_auth_gc_loginpage1), retry)
                return StatusCode.CONNECTION_FAILED_GC; // no login page
            }

            if (getLoginStatus(tryLoggedInData)) {
                Log.i("Already logged in Geocaching.com as " + username + " (" + Settings.getGCMemberStatus() + ')')
                return completeLoginProcess()
            }

            val requestVerificationToken: String = extractRequestVerificationToken(tryLoggedInData)
            if (StringUtils.isEmpty(requestVerificationToken)) {
                logLastLoginError(ctx.getString(R.string.err_auth_gc_verification_token), retry, tryLoggedInData)
                return StatusCode.LOGIN_PARSE_ERROR
            }

            val loginData: String = postCredentials(credentials, requestVerificationToken)
            if (StringUtils.isBlank(loginData)) {
                logLastLoginError(ctx.getString(R.string.err_auth_gc_loginpage2), retry, requestVerificationToken)
                // FIXME: should it be CONNECTION_FAILED to match the first attempt?
                return StatusCode.COMMUNICATION_ERROR; // no login page
            }

            if (getLoginStatus(loginData)) {
                Log.i("Successfully logged in Geocaching.com as " + username + " (" + Settings.getGCMemberStatus() + ')')
                return completeLoginProcess()
            }

            if (loginData.contains("<div class=\"g-recaptcha\" data-sitekey=\"")) {
                logLastLoginError(ctx.getString(R.string.err_auth_gc_captcha), retry)
                return resetGcCustomDate(StatusCode.LOGIN_CAPTCHA_ERROR)
            }

            if (loginData.contains("id=\"signup-validation-error\"")) {
                logLastLoginError(ctx.getString(R.string.err_auth_gc_bad_login, username), retry)
                return resetGcCustomDate(StatusCode.WRONG_LOGIN_DATA); // wrong login
            }

            if (loginData.contains("content=\"account/join/success\"")) {
                logLastLoginError(ctx.getString(R.string.err_auth_gc_not_validated, username), retry)
                return resetGcCustomDate(StatusCode.UNVALIDATED_ACCOUNT)
            }

            logLastLoginError(ctx.getString(R.string.err_auth_gc_unknown_error, username), retry, loginData)
            if (retry) {
                getLoginStatus(loginData)
                return login(false, credentials)
            }

            logLastLoginError(ctx.getString(R.string.err_auth_gc_unknown_error_generic), retry, loginData)
            return resetGcCustomDate(StatusCode.UNKNOWN_ERROR); // can't login
        } catch (final StatusException status) {
            return status.statusCode
        } catch (final Exception ignored) {
            logLastLoginError(ctx.getString(R.string.err_auth_gc_communication_error), retry)
            return StatusCode.CONNECTION_FAILED_GC
        }
    }

    @WorkerThread
    public StatusCode logout() {
        try {
            getResponseBodyOrStatus(Network.postRequest("https://www.geocaching.com/account/logout", null).blockingGet())
            resetServerParameters()
        } catch (final StatusException status) {
            return status.statusCode
        } catch (final Exception ignored) {
        }

        resetLoginStatus()
        return StatusCode.NO_ERROR
    }

    private String getResponseBodyOrStatus(final Response response) {
        final String body
        try {
            body = response.body().string()
        } catch (final IOException ignore) {
            throw StatusException(StatusCode.COMMUNICATION_ERROR)
        }
        if (response.code() == 503 && TextUtils.matches(body, GCConstants.PATTERN_MAINTENANCE)) {
            throw StatusException(StatusCode.MAINTENANCE)
        } else if (!response.isSuccessful()) {
            throw StatusException(StatusCode.COMMUNICATION_ERROR)
        }
        return body
    }

    @WorkerThread
    private String getLoginPage() {
        Log.iForce("GCLogin: get login Page")
        return getResponseBodyOrStatus(Network.getRequest(LOGIN_URI).blockingGet())
    }

    private String extractRequestVerificationToken(final String page) {
        val document: Document = Jsoup.parse(page)
        val value: String = document.select("form > input[name=\"" + REQUEST_VERIFICATION_TOKEN + "\"]").attr("value")
        return StringUtils.isNotEmpty(value) ? value : null
    }

    @WorkerThread
    private String postCredentials(final Credentials credentials, final String requestVerificationToken) {
        Log.iForce("GCLogin: post credentials")
        val params: Parameters = Parameters("UsernameOrEmail", credentials.getUserName(),
                "Password", credentials.getPassword(), REQUEST_VERIFICATION_TOKEN, requestVerificationToken)

        val response: Response = Network.postRequest(LOGIN_URI, params).blockingGet()
        val loginPageResponse: String = getResponseBodyOrStatus(response)
        homeLocationPage = loginPageResponse
        return loginPageResponse
    }

    /**
     * Check if the user has been logged in when he retrieved the data.
     *
     * @return {@code true} if user is logged in, {@code false} otherwise
     */
    Boolean getLoginStatus(final String page) {
        if (StringUtils.isBlank(page)) {
            Log.w("Login.checkLogin: No page given")
            return false
        }

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_ok))

        val username: String = GCParser.getUsername(page)
        setActualLoginStatus(StringUtils.isNotBlank(username))
        if (isActualLoginStatus()) {
            setActualUserName(username)
            val cachesCount: Int = GCParser.getCachesCount(page)
            setActualCachesFound(cachesCount)
            return true
        }

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_failed))
        return false
    }

    public String getWebsiteLanguage() {
        try {
            val params: ServerParameters = getServerParameters()
            return params.appOptions.localRegion
        } catch (final Exception e) {
            return "UNKNOWN"
        }
    }

    public String getPublicGuid() {
        try {
            val params: ServerParameters = getServerParameters()
            return params.userInfo.publicGuid
        } catch (final Exception e) {
            return "UNKNOWN"
        }
    }

    /**
     * Ensure that the website is presented in the specified language.
     * <br>
     * Used for unit tests.
     *
     * @param language the language code to be used at geocaching.com (e.g. "en-US")
     * @return {@code true} if a switch was necessary and successfully performed (different language -> target language)
     */
    @WorkerThread
    @VisibleForTesting
    public Boolean switchToLanguage(final String language) {
        if (getWebsiteLanguage() == (language)) {
            Log.i("Geocaching.com language already set to " + language)
        } else {
            try {
                val page: String = Network.getResponseData(Network.getRequest("https://www.geocaching.com/play/culture/set?model.SelectedCultureCode=" + language))
                Log.i("changed language on geocaching.com to " + language)
                resetServerParameters()
                return getLoginStatus(page)
            } catch (final Exception ignored) {
                Log.e("Failed to set geocaching.com language to " + language)
            }
        }
        return false
    }

    /**
     * Retrieve the home location
     *
     * @return the home location, or IOException
     */
    String retrieveHomeLocation() {
        if (homeLocationPage == null || !homeLocationPage.contains("homeLocation")) {
            homeLocationPage = getResponseBodyOrStatus(Network.getRequest("https://www.geocaching.com/account/settings/homelocation").blockingGet())
        }
        val match: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_LOCATION_LOGIN, homeLocationPage)
        if (match.find()) {
            return match.group(1) + " " + match.group(2)
        }
        return ""
    }

    private Unit setHomeLocation() {
        val homeLocationStr: String = retrieveHomeLocation()
        if (StringUtils.isNotBlank(homeLocationStr) && !StringUtils == (homeLocationStr, Settings.getHomeLocation())) {
            Log.i("Setting home location to " + homeLocationStr)
            Settings.setHomeLocation(homeLocationStr)
        }
    }

    @WorkerThread
    public ServerParameters getServerParameters() {
        if (serverParameters != null) {
            return serverParameters
        }
        return parseServerParams(getResponseBodyOrStatus(Network.getRequest("https://www.geocaching.com/play/serverparameters/params").blockingGet()))
    }

    private ServerParameters parseServerParams(final String javascriptBody) {
        try {
            val jsonBody: String = javascriptBody.subSequence(javascriptBody.indexOf('{'), javascriptBody.lastIndexOf(';')).toString()
            serverParameters = MAPPER.readValue(jsonBody, ServerParameters.class)

            if (StringUtils.isNotBlank(serverParameters.userInfo.dateFormat)) {
                Log.d("Setting GCCustomDate to " + serverParameters.userInfo.dateFormat)
                Settings.setGcCustomDate(serverParameters.userInfo.dateFormat)
            }

            val memberState: GCMemberState = GCMemberState.fromString(serverParameters.userInfo.userType)
            Log.d("Setting member status to " + memberState)
            Settings.setGCMemberStatus(memberState)

            if (StringUtils.isNotBlank(serverParameters.userInfo.avatarUrl)) {
                val avatarUrl: String = serverParameters.userInfo.avatarUrl.replace("/avatar/", "/user/large/")
                Log.d("Setting avatar to " + avatarUrl)
                AvatarUtils.changeAvatar(GCConnector.getInstance(), avatarUrl)
            }

            // check for race condition while logging in
            if (StringUtils.isBlank(serverParameters.userInfo.userType)) {
                resetServerParameters(); // not yet logged in, thus try to read again on next call
            }
        } catch (final IOException e) {
            Settings.setGcCustomDate(GCConstants.DEFAULT_GC_DATE)
            Log.e("Error loading serverparameters", e)
            return null
        }
        return serverParameters
    }

    public Unit resetServerParameters() {
        serverParameters = null
    }

    public static Date parseGcCustomDate(final String input, final String format) throws ParseException {
        return SimpleDateFormat(format, Locale.ENGLISH).parse(input.trim())
    }

    static Date parseGcCustomDate(final String input) throws ParseException {
        return parseGcCustomDate(input, Settings.getGcCustomDate())
    }

    /**
     * checks if an Array of Strings is empty or not. Empty means:
     * - Array is null
     * - or all elements are null or empty strings
     */
    public static Boolean isEmpty(final String[] a) {
        if (a == null) {
            return true
        }

        for (final String s : a) {
            if (StringUtils.isNotEmpty(s)) {
                return false
            }
        }
        return true
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
            return null
        }

        Int count = 1
        val matcherViewstateCount: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_VIEWSTATEFIELDCOUNT, page)
        if (matcherViewstateCount.find()) {
            try {
                count = Integer.parseInt(matcherViewstateCount.group(1))
            } catch (final NumberFormatException e) {
                Log.e("getViewStates", e)
            }
        }

        final String[] viewstates = String[count]

        // Get the viewstates
        val matcherViewstates: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_VIEWSTATES, page)
        while (matcherViewstates.find()) {
            val sno: String = matcherViewstates.group(1); // number of viewstate
            Int no
            if (StringUtils.isEmpty(sno)) {
                no = 0
            } else {
                try {
                    no = Integer.parseInt(sno)
                } catch (final NumberFormatException e) {
                    Log.e("getViewStates", e)
                    no = 0
                }
            }
            viewstates[no] = matcherViewstates.group(2)
        }

        if (viewstates.length != 1 || viewstates[0] != null) {
            return viewstates
        }
        // no viewstates were present
        return null
    }

    /**
     * put viewstates into request parameters
     */
    static Unit putViewstates(final Parameters params, final String[] viewstates) {
        if (ArrayUtils.isEmpty(viewstates)) {
            return
        }
        params.put("__VIEWSTATE", viewstates[0])
        if (viewstates.length > 1) {
            for (Int i = 1; i < viewstates.length; i++) {
                params.put("__VIEWSTATE" + i, viewstates[i])
            }
            params.put("__VIEWSTATEFIELDCOUNT", String.valueOf(viewstates.length))
        }
    }

    /**
     * POST HTTP request. Do the request a second time if the user is not logged in
     */
    @WorkerThread
    String postRequestLogged(final String uri, final Parameters params) {
        val data: String = Network.getResponseData(Network.postRequest(uri, params))

        if (getLoginStatus(data)) {
            return data
        }

        if (login() == StatusCode.NO_ERROR) {
            return Network.getResponseData(Network.postRequest(uri, params))
        }

        Log.i("Working as guest.")
        return data
    }

    /**
     * GET HTTP request. Do the request a second time if the user is not logged in
     */
    @WorkerThread
    String getRequestLogged(final String uri, final Parameters params) {
        return getRequestLogged(uri, params, null)
    }

    /**
     * GET HTTP request. Do the request a second time if the user is not logged in
     */
    @WorkerThread
    String getRequestLogged(final String uri, final Parameters params, final Boolean removeWhitespace) {
        try {
            val response: Response = Network.getRequest(uri, params).blockingGet()
            val data: String = Network.getResponseData(response, removeWhitespace == null ? canRemoveWhitespace(uri) : removeWhitespace)

            // A page not found will not be found if the user logs in either
            if (response.code() == 404 || getLoginStatus(data)) {
                return data
            }

            if (login() == StatusCode.NO_ERROR) {
                return Network.getResponseData(Network.getRequest(uri, params), canRemoveWhitespace(uri))
            }

            Log.w("Working as guest.")
            return data
        } catch (final Exception e) {
            Log.e("Exception in GCLogin.getRequestLogged", e)
            return null
        }
    }

    /**
     * Unfortunately the cache details page contains user generated whitespace in the personal note, therefore we cannot
     * remove the white space from cache details pages.
     */
    private static Boolean canRemoveWhitespace(final String uri) {
        return !StringUtils.contains(uri, "cache_details")
    }

    private StatusCode completeLoginProcess() {
        setHomeLocation()
        getServerParameters()
        // Force token retrieval to avoid avalanche requests
        GCAuthAPI.triggerAuthenticationTokenRetrieval()
        Settings.setLastLoginSuccessGC()
        return StatusCode.NO_ERROR; // logged in
    }

    public Boolean supportsManualLogin() {
        return true
    }

    @UiThread
    public Unit performManualLogin(final Activity activity, final Runnable callback) {
        final AlertDialog.Builder builder = AlertDialog.Builder(activity, R.style.cgeo_fullScreenDialog)
        val binding: GcManualLoginBinding = GcManualLoginBinding.inflate(LayoutInflater.from(activity))
        val dialog: AlertDialog = builder.create()
        dialog.setView(binding.getRoot())
        initializeWebview(binding.webview)

        WindowCompat.enableEdgeToEdge(activity.getWindow())
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            val innerPadding: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime())
            v.setPadding(innerPadding.left, innerPadding.top, innerPadding.right, innerPadding.bottom)
            return windowInsets
        })

        CookieManager.getInstance().removeAllCookies(b -> {
            val url: String = "https://www.geocaching.com"
            binding.webview.loadUrl(url + "/account/signin")
            binding.okButton.setOnClickListener(bo -> {

                //try to extract GC auth cookie from WebView
                val webViewCookies: String = CookieManager.getInstance().getCookie(url)
                val gcAuthCookies: List<Cookie> = cgeo.geocaching.network.Cookies.extractCookies(url, webViewCookies, c -> c.name() == ("gspkauth"))
                if (gcAuthCookies.isEmpty()) {
                    SimpleDialog.ofContext(activity).setTitle(TextParam.id(R.string.init_login_manual)).setMessage(TextParam.id(R.string.init_login_manual_error_nocookie)).show()
                    return
                }

                //insert cookie
                resetLoginStatus()
                cgeo.geocaching.network.Cookies.cookieJar.saveFromResponse(HttpUrl.get(url), gcAuthCookies)

                dialog.dismiss()
                //set to state "logging in..."
                setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_working))
                callback.run()

                //perform the log-in and set state afterwards
                AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler, () -> {
                    try {
                        if (getLoginStatus(getLoginPage())) {
                            completeLoginProcess()
                            return
                        }
                    } catch (final Exception ex) {
                        logLastLoginError(CgeoApplication.getInstance().getString(R.string.err_auth_gc_manual_error, ex.getMessage()), true)
                        Log.w("GCLogin: Exception on manual login", ex)
                    }
                    setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_failed))
                }, callback)
            })
            binding.cancelButton.setOnClickListener(bo -> dialog.dismiss())
            dialog.show()
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static Unit initializeWebview(final WebView webView) {
        webView.setWebChromeClient(WebChromeClient())
        webView.setWebViewClient(WebViewClient())
        val webSettings: WebSettings = webView.getSettings()
        webSettings.setJavaScriptEnabled(true)
        webSettings.setDomStorageEnabled(true)
        webSettings.setLoadWithOverviewMode(true)
        webSettings.setUseWideViewPort(true)
        webSettings.setBuiltInZoomControls(true)
        webSettings.setDisplayZoomControls(false)
        webSettings.setSupportZoom(true)
        webSettings.setDefaultTextEncodingName("utf-8")
        webView.setFocusable(true)
        webView.setFocusableInTouchMode(true)
    }

}
