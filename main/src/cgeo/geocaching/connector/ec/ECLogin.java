package cgeo.geocaching.connector.ec;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLogin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.Nullable;

import java.util.regex.Matcher;

public class ECLogin extends AbstractLogin {

    private ECLogin() {
        // singleton
    }

    private static class SingletonHolder {
        private static ECLogin INSTANCE = new ECLogin();
    }

    public static ECLogin getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    protected StatusCode login(boolean retry) {
        final ImmutablePair<String, String> login = Settings.getECLogin();

        if (StringUtils.isEmpty(login.left) || StringUtils.isEmpty(login.right)) {
            clearLoginInfo();
            Log.e("ECLogin.login: No login information stored");
            return StatusCode.NO_LOGIN_INFO_STORED;
        }

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_working));
        HttpResponse loginResponse = Network.getRequest("https://extremcaching.com/community/profil1");
        String loginData = Network.getResponseData(loginResponse);

        if (StringUtils.isBlank(loginData)) {
            Log.e("ECLogin.login: Failed to retrieve login page (1st)");
            return StatusCode.CONNECTION_FAILED_EC; // no login page
        }

        if (getLoginStatus(loginData)) {
            Log.i("Already logged in Extremcaching.com as " + login.left);
            return StatusCode.NO_ERROR; // logged in
        }

        final Parameters params = new Parameters(
                "username", login.left,
                "password", login.right,
                "remember", "yes");

        Matcher m = ECConstants.PATTERN_LOGIN_SECURITY.matcher(loginData);
        if (m.find() && m.groupCount() == 2) {
            params.add("return", m.group(1));
            params.add(m.group(2), "1");
        } else {
            Log.e("ECLogin.login security tokens in login form not found");
            return StatusCode.COMMUNICATION_ERROR;
        }

        loginResponse = Network.postRequest("http://extremcaching.com/component/users/?task=user.login", params);
        loginData = Network.getResponseData(loginResponse);

        if (StringUtils.isBlank(loginData)) {
            Log.e("ECLogin.login: Failed to retrieve login page (2nd)");
            return StatusCode.COMMUNICATION_ERROR; // no login page
        }
        assert loginData != null;  // Caught above

        if (getLoginStatus(loginData)) {
            Log.i("Successfully logged in Extremcaching.com as " + login.left);

            Settings.setCookieStore(Cookies.dumpCookieStore());

            return StatusCode.NO_ERROR; // logged in
        }

        if (loginData.contains("Benutzername und Passwort falsch")) { // Yes, it's hardcoded in German (translation is done using Javascript and Google Translate)
            Log.i("Failed to log in Extremcaching.com as " + login.left + " because of wrong username/password");
            return StatusCode.WRONG_LOGIN_DATA; // wrong login
        }

        Log.i("Failed to log in Extremcaching.com as " + login.left + " for some unknown reason");
        if (retry) {
            return login(false);
        }

        return StatusCode.UNKNOWN_ERROR; // can't login
    }


    /**
     * Check if the user has been logged in when he retrieved the data.
     *
     * @param page
     * @return <code>true</code> if user is logged in, <code>false</code> otherwise
     */
    public boolean getLoginStatus(@Nullable final String page) {
        if (StringUtils.isBlank(page)) {
            Log.e("ECLogin.getLoginStatus: No page given");
            return false;
        }
        assert page != null;

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_ok));

        // on every page except login page
        setActualLoginStatus(TextUtils.matches(page, ECConstants.PATTERN_LOGIN_NAME));
        if (isActualLoginStatus()) {
            setActualUserName(TextUtils.getMatch(page, ECConstants.PATTERN_LOGIN_NAME, true, "???"));
            int cachesCount = 0;
            try {
                cachesCount = Integer.parseInt(TextUtils.getMatch(page, ECConstants.PATTERN_CACHES_FOUND, true, "0"));
            } catch (final NumberFormatException e) {
                Log.e("ECLogin.getLoginStatus: bad cache count", e);
            }
            setActualCachesFound(cachesCount);
            return true;
        }

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_failed));
        return false;
    }

}
