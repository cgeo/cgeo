package cgeo.geocaching.connector.ec;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLogin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.Nullable;

import java.io.IOException;

public class ECLogin extends AbstractLogin {

    private final CgeoApplication app = CgeoApplication.getInstance();
    private String sessionId = null;

    private ECLogin() {
        // singleton
    }

    private static class SingletonHolder {
        private final static ECLogin INSTANCE = new ECLogin();
    }

    public static ECLogin getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    protected StatusCode login(final boolean retry) {
        final ImmutablePair<String, String> login = Settings.getCredentials(ECConnector.getInstance());

        if (StringUtils.isEmpty(login.left) || StringUtils.isEmpty(login.right)) {
            clearLoginInfo();
            Log.e("ECLogin.login: No login information stored");
            return StatusCode.NO_LOGIN_INFO_STORED;
        }

        setActualStatus(app.getString(R.string.init_login_popup_working));

        final Parameters params = new Parameters("user", login.left, "pass", login.right);
        final HttpResponse loginResponse = Network.postRequest("https://extremcaching.com/exports/apilogin.php", params);

        final String loginData = Network.getResponseData(loginResponse);

        if (StringUtils.isBlank(loginData)) {
            Log.e("ECLogin.login: Failed to retrieve login data");
            return StatusCode.CONNECTION_FAILED_EC; // no login page
        }

        assert loginData != null;

        if (loginData.contains("Wrong username or password")) { // hardcoded in English
            Log.i("Failed to log in Extremcaching.com as " + login.left + " because of wrong username/password");
            return StatusCode.WRONG_LOGIN_DATA; // wrong login
        }

        if (getLoginStatus(loginData)) {
            Log.i("Successfully logged in Extremcaching.com as " + login.left);

            return StatusCode.NO_ERROR; // logged in
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
     * @param data
     * @return <code>true</code> if user is logged in, <code>false</code> otherwise
     */
    private boolean getLoginStatus(@Nullable final String data) {
        if (StringUtils.isBlank(data) || StringUtils.equals(data, "[]")) {
            Log.e("ECLogin.getLoginStatus: No or empty data given");
            return false;
        }
        assert data != null;

        setActualStatus(app.getString(R.string.init_login_popup_ok));

        try {
            final JsonNode json = JsonUtils.reader.readTree(data);

            final String sid = json.get("sid").asText();
            if (!StringUtils.isBlank(sid)) {
                sessionId = sid;
                setActualLoginStatus(true);
                setActualUserName(json.get("username").asText());
                setActualCachesFound(json.get("found").asInt());
                return true;
            }
            resetLoginStatus();
        } catch (IOException | NullPointerException e) {
            Log.e("ECLogin.getLoginStatus", e);
        }

        setActualStatus(app.getString(R.string.init_login_popup_failed));
        return false;
    }

    @Override
    protected void resetLoginStatus() {
        sessionId = null;
        setActualLoginStatus(false);
    }

    public String getSessionId() {
        if (!StringUtils.isBlank(sessionId) || login() == StatusCode.NO_ERROR) {
            return sessionId;
        }
        return StringUtils.EMPTY;
    }

}
