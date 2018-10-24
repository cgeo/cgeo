package cgeo.geocaching.connector.su;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLogin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.OAuthTokens;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import android.app.Application;
import android.support.annotation.NonNull;

import java.util.regex.Pattern;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class SuLogin extends AbstractLogin {

    private static final Pattern PARAMS_PATTERN_1 = Pattern.compile("oauth_token=([\\w_.-]+)");
    private static final Pattern PARAMS_PATTERN_2 = Pattern.compile("oauth_token_secret=([\\w_.-]+)");
    private static final String host = "www.geocaching.su";
    private static final String FULL_HOST_URL = "http://" + host;
    private static final String LOGON_PATH = "/api/oauth/logon.php";
    private static final String REQUEST_TOKEN_PATH = "/api/oauth/request_token.php";
    private static final String ACCESS_TOKEN_PATH = "/api/oauth/access_token.php";
    private static final String AUTHORIZE_TOKEN_PATH = "/api/oauth/authorize.php";
    private static String oAtoken;
    private static String oAtokenSecret;

    private SuLogin() {
        // singleton
    }

    @NonNull
    public static SuLogin getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    @NonNull
    protected StatusCode login(final boolean retry) {
        return login(retry, Settings.getCredentials(SuConnector.getInstance()));
    }

    @NonNull
    public OAuthTokens getOAuthTokens() {
        return new OAuthTokens(oAtoken, oAtokenSecret);
    }

    @Override
    @NonNull
    protected StatusCode login(final boolean retry, @NonNull final Credentials credentials) {
        if (credentials.isInvalid()) {
            clearLoginInfo();
            Log.w("SuLogin.login: No login information stored");
            return StatusCode.NO_LOGIN_INFO_STORED;
        }

        final Application application = CgeoApplication.getInstance();
        setActualStatus(application.getString(R.string.init_login_popup_working));

        final Parameters params = new Parameters("username", credentials.getUserName(), "password", credentials.getPassword());

        final String loginData = Network.getResponseData(Network.postRequest(FULL_HOST_URL + LOGON_PATH, params));

        if (StringUtils.isBlank(loginData)) {
            Log.e("SuLogin.login: Failed to retrieve login data");
            setActualStatus(application.getString(R.string.init_login_popup_failed));
            return StatusCode.CONNECTION_FAILED_SU; // no login page
        }

        if (!loginData.contains("Login succesful")) { // hardcoded in English
            Log.i("Failed to log in Geocaching.su as " + credentials.getUserName() + " because of wrong username/password");
            setActualStatus(application.getString(R.string.init_login_popup_failed));
            return StatusCode.WRONG_LOGIN_DATA; // wrong login
        }

        setActualUserName(credentials.getUserName());

        Log.i("Successfully logged in Geocaching.su as " + credentials.getUserName());
        requestToken();
        authorizeToken();
        accessToken();

        if (oAtoken != null) {
            Log.i("Successfully logged in Geocaching.su as " + credentials.getUserName());
            setActualStatus(application.getString(R.string.init_login_popup_ok));
            setActualLoginStatus(true);

            return StatusCode.NO_ERROR; // logged in
        } else {
            Log.i("Failed to log in Geocaching.su as " + credentials.getUserName() + " for some unknown reason");
            if (retry) {
                return login(false, credentials);
            }

            return StatusCode.UNKNOWN_ERROR; // can't login
        }
    }

    private void requestToken() {
        final Parameters params = new Parameters();
        final String method = "GET";
        OAuth.signOAuth(host, REQUEST_TOKEN_PATH, method, false, params, new OAuthTokens(null, null), CgeoApplication.getInstance().getString(R.string.su_consumer_key), CgeoApplication.getInstance().getString(R.string.su_consumer_secret));

        try {
            final Response response = Network.getRequest(FULL_HOST_URL + REQUEST_TOKEN_PATH, params).blockingGet();

            if (response.isSuccessful()) {
                final String line = Network.getResponseData(response);

                if (line != null && StringUtils.isNotBlank(line)) {
                    final MatcherWrapper paramsMatcher1 = new MatcherWrapper(PARAMS_PATTERN_1, line);
                    if (paramsMatcher1.find()) {
                        oAtoken = paramsMatcher1.group(1);
                    }
                    final MatcherWrapper paramsMatcher2 = new MatcherWrapper(PARAMS_PATTERN_2, line);
                    if (paramsMatcher2.find()) {
                        oAtokenSecret = paramsMatcher2.group(1);
                    }
                }
            }
        } catch (final RuntimeException ignored) {
            Log.e("requestToken: cannot get token");
        }
    }

    private void authorizeToken() {
        final Parameters params = new Parameters();
        final String method = "GET";
        OAuth.signOAuth(host, AUTHORIZE_TOKEN_PATH, method, false, params, new OAuthTokens(oAtoken, oAtokenSecret), CgeoApplication.getInstance().getString(R.string.su_consumer_key), CgeoApplication.getInstance().getString(R.string.su_consumer_secret));

        try {
            //TODO: Maybe check result?
            Network.getRequest(FULL_HOST_URL + AUTHORIZE_TOKEN_PATH, params).blockingGet();
        } catch (final RuntimeException ignored) {
            Log.e("requestToken: cannot get token");
        }
    }

    private void accessToken() {
        final Parameters params = new Parameters();
        final String method = "GET";
        OAuth.signOAuth(host, ACCESS_TOKEN_PATH, method, false, params, new OAuthTokens(oAtoken, oAtokenSecret), CgeoApplication.getInstance().getString(R.string.su_consumer_key), CgeoApplication.getInstance().getString(R.string.su_consumer_secret));

        try {
            final Response response = Network.getRequest(FULL_HOST_URL + ACCESS_TOKEN_PATH, params).blockingGet();

            if (response.isSuccessful()) {
                final String line = Network.getResponseData(response);

                if (line != null && StringUtils.isNotBlank(line)) {
                    final MatcherWrapper paramsMatcher1 = new MatcherWrapper(PARAMS_PATTERN_1, line);
                    if (paramsMatcher1.find()) {
                        oAtoken = paramsMatcher1.group(1);
                    }
                    final MatcherWrapper paramsMatcher2 = new MatcherWrapper(PARAMS_PATTERN_2, line);
                    if (paramsMatcher2.find()) {
                        oAtokenSecret = paramsMatcher2.group(1);
                    }
                }
            }
        } catch (final RuntimeException ignored) {
            Log.e("requestToken: cannot get token");
        }
    }

    @Override
    protected void resetLoginStatus() {
        setActualLoginStatus(false);
    }


    private static class SingletonHolder {
        @NonNull
        private static final SuLogin INSTANCE = new SuLogin();
    }

}
