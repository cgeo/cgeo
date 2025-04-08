package cgeo.geocaching.connector.wm;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLogin;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import cgeo.geocaching.utils.NetworkUtils;

import okhttp3.OkHttpClient;
import okhttp3.Response;

public class WMLogin extends AbstractLogin {

    private static final String LOGIN_URI = "https://www.waymarking.com/login/default.aspx";
    private static final String LOGOUT_URI = "https://www.waymarking.com/login/default.aspx?RESET=Y";
    private static final String STATS_URI = "https://www.waymarking.com/users/profile.aspx?mypage=6&gt=1";

    private WMLogin() {
        // singleton
    }

    public static WMLogin getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final WMLogin INSTANCE = new WMLogin();
    }

    private void resetLoginStatus() {
        Cookies.removeCookies(WMConnector.getInstance().getHostUrl());
        setActualLoginStatus(false);
    }

    private void clearLoginInfo() {
        resetLoginStatus();

        setActualCachesFound(-1);
        setActualStatus(CgeoApplication.getInstance().getString(R.string.err_login));
    }

    @NonNull
    @Override
    protected StatusCode login(final boolean retry) {
        return login(retry, Settings.getCredentials(GCConnector.getInstance()));
    }

    @NonNull
    @Override
    protected StatusCode login(final boolean retry, @NonNull final Credentials credentials) {
        final StatusCode status = loginInternal(retry, credentials);
        if (status != StatusCode.NO_ERROR) {
            resetLoginStatus();
        }
        return status;
    }

    @WorkerThread
    protected StatusCode loginInternal(final boolean retry, @NonNull final Credentials credentials) {
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);

        final Context ctx = CgeoApplication.getInstance();

        if (credentials.isInvalid()) {
            clearLoginInfo();
            logLastLoginError(ctx.getString(R.string.err_auth_gc_missing_login), retry);
            return StatusCode.NO_LOGIN_INFO_STORED;
        }

        final String username = credentials.getUserName();

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_working));
        try {
            final String tryLoggedInData = getLoginPage();

            if (StringUtils.isBlank(tryLoggedInData)) {
                logLastLoginError(ctx.getString(R.string.err_auth_gc_loginpage1), retry);
                return StatusCode.CONNECTION_FAILED_WM; // no login page
            }

            if (getLoginStatus(tryLoggedInData)) {
                Log.i("Already logged in Waymarking.com as " + username);
                return completeLoginProcess();
            }

            final Document document = Jsoup.parse(tryLoggedInData);

            final String requestVerificationToken = extractFormField("__RequestVerificationToken", document);
            if (StringUtils.isEmpty(requestVerificationToken)) {
                logLastLoginError(ctx.getString(R.string.err_auth_gc_verification_token), retry, tryLoggedInData);
                return StatusCode.LOGIN_PARSE_ERROR;
            }

            final String viewState = extractFormField("__VIEWSTATE", document);
            if (StringUtils.isEmpty(viewState)) {
                logLastLoginError(ctx.getString(R.string.err_auth_gc_verification_token), retry, tryLoggedInData);
                return StatusCode.LOGIN_PARSE_ERROR;
            }

            final String viewStateGenerator = extractFormField("__VIEWSTATEGENERATOR", document);
            if (StringUtils.isEmpty(viewStateGenerator)) {
                logLastLoginError(ctx.getString(R.string.err_auth_gc_verification_token), retry, tryLoggedInData);
                return StatusCode.LOGIN_PARSE_ERROR;
            }

            final Parameters parameters = new Parameters(
                "__VIEWSTATE", viewState,
                "__VIEWSTATEGENERATOR", viewStateGenerator,
                "__RequestVerificationToken", requestVerificationToken
            );

            final String loginData = postCredentials(credentials, parameters);
            if (StringUtils.isBlank(loginData)) {
                logLastLoginError(ctx.getString(R.string.err_auth_gc_loginpage2), retry, requestVerificationToken);
                // FIXME: should it be CONNECTION_FAILED to match the first attempt?
                return StatusCode.COMMUNICATION_ERROR; // no login page
            }

            if (getLoginStatus(loginData)) {
                Log.i("Successfully logged in Waymarking.com as " + username);
                return completeLoginProcess();
            }

            // TODO
            // I don't know common reasons why logins might fail.
            // From what I can predict all errors are returned in #ctl00_ContentBody_ErrorText
            // so we can't do much in terms of finding out what the error is (we can't match
            // the error text itself in case the site is in another language)
            // As such I felt like displaying the error to the user and returning UNKOWN_ERROR
            // was the most appropriate course of action.

            if (loginData.contains("<span id=\"ctl00_ContentBody_ErrorText\"")) {
                final String reason = getFailedLoginReason(loginData);
                if (reason != null) {
                    logLastLoginError(ctx.getString(R.string.err_auth_wm_error_text, username, reason), retry);
                } else {
                    logLastLoginError(ctx.getString(R.string.err_auth_wm_unknown_error, username), retry);
                }
                return StatusCode.UNKNOWN_ERROR;
            }

            logLastLoginError(ctx.getString(R.string.err_auth_wm_unknown_error, username), retry, loginData);
            if (retry) {
                getLoginStatus(loginData);
                return login(false, credentials);
            }

            logLastLoginError(ctx.getString(R.string.err_auth_gc_unknown_error_generic), retry, loginData);
            return StatusCode.UNKNOWN_ERROR;
        } catch (final NetworkUtils.StatusException status) {
            return status.statusCode;
        } catch (final Exception exception) {
            logLastLoginError(ctx.getString(R.string.err_auth_gc_communication_error), retry, exception.toString());
            return StatusCode.CONNECTION_FAILED_WM;
        }
    }

    @WorkerThread
    public StatusCode logout() {
        try {
            NetworkUtils.getResponseBodyOrStatus(Network.getRequest(LOGOUT_URI, null).blockingGet());
        } catch (final NetworkUtils.StatusException status) {
            return status.statusCode;
        } catch (final Exception ignored) {
        }

        resetLoginStatus();
        return StatusCode.NO_ERROR;
    }

    @WorkerThread
    private String getLoginPage() {
        Log.iForce("WMLogin: get login Page");
        return NetworkUtils.getResponseBodyOrStatus(Network.getRequest(LOGIN_URI).blockingGet());
    }

    @NonNull
    private String extractFormField(final String field, final Document document) {
        return document.select("form input[name=\"" + field + "\"]").attr("value");
    }

    @WorkerThread
    private String postCredentials(final Credentials credentials, final Parameters params) {
        Log.iForce("WMLogin: post credentials");
        params.add("__EVENTTARGET", "");
        params.add("__EVENTARGUMENT", "");
        params.add("ctl00$ContentBody$myUsername", credentials.getUserName());
        params.add("ctl00$ContentBody$myPassword", credentials.getPassword());
        params.add("ctl00$ContentBody$Button1", "Log+In");
        params.add("ctl00$ContentBody$cookie", "on");
        return NetworkUtils.getResponseBodyOrStatus(Network.postRequest(LOGIN_URI, params).blockingGet());
    }

    /**
     * Check if the user has been logged in when he retrieved the data.
     *
     * @return {@code true} if user is logged in, {@code false} otherwise
     */
    boolean getLoginStatus(@Nullable final String page) {
        if (StringUtils.isBlank(page)) {
            Log.w("WM Login.checkLogin: No page given");
            return false;
        }

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_ok));

        final String username = WMParser.getUsername(page);
        setActualLoginStatus(StringUtils.isNotBlank(username));
        if (isActualLoginStatus()) {
            setActualUserName(username);

            // The logged in page doesn't actually tell us how many waymarks we've found
            // As a matter of fact almost no page tells us that.
            // The profile page is close, but it has the text embedded in an image.
            // We need to send a second request to the stats page to get the number of finds.
            final String statsData = NetworkUtils.getResponseBodyOrStatus(Network.getRequest(STATS_URI).blockingGet());
            final int waymarksCount = WMParser.getFindsCount(statsData);

            if (waymarksCount == -1) {
                //logLastLoginError(CgeoApplication.getInstance().getString(R.string.err_auth_wm_finds_error), ret);
                setActualCachesFound(0); // best solution for now
            } else {
                setActualCachesFound(waymarksCount);
            }

            return true;
        }

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_failed));
        return false;
    }

    String getFailedLoginReason(@NonNull final String page) {
        final Document document = Jsoup.parse(page);
        final String value = document.select("span#ctl00_ContentBody_ErrorText").text();
        return StringUtils.isNotEmpty(value) ? value : null;
    }

    private StatusCode completeLoginProcess() {
        //TODO GCAuthAPI.triggerAuthenticationTokenRetrieval();
        Settings.setLastLoginSuccessWM();
        return StatusCode.NO_ERROR; // logged in
    }

    private void logLastLoginError(final String status, final boolean retry) {
        logLastLoginError(status, retry, "");
    }

    private void logLastLoginError(final String status, final boolean retry, final String additionalLogInfo) {
        final String retryMarker = " // ";
        final String currentStatus = Settings.getLastLoginErrorWM() == null || Settings.getLastLoginErrorWM().first == null ? "" : Settings.getLastLoginErrorWM().first;
        if (!retry && currentStatus.endsWith(retryMarker)) {
            Settings.setLastLoginErrorWM(currentStatus + status);
        } else {
            Settings.setLastLoginErrorWM(status + retryMarker);
        }
        Log.w("WM Login.login: " + status + " (retry=" + retry + ") [" + additionalLogInfo + "]");
    }
}
