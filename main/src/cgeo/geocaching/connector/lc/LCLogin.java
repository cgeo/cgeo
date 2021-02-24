package cgeo.geocaching.connector.lc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLogin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

public class LCLogin extends AbstractLogin {

    private Boolean fakeLogin = true;

    private LCLogin() {
        // singleton
    }

    private static class SingletonHolder {
        @NonNull
        private static final LCLogin INSTANCE = new LCLogin();
    }

    @NonNull
    public static LCLogin getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    @NonNull
    protected StatusCode login(final boolean retry) {
        return login(retry, Settings.getCredentials(LCConnector.getInstance()));
    }

    @Override
    @NonNull
    protected StatusCode login(final boolean retry, @NonNull final Credentials credentials) {
        if (credentials.isInvalid()) {
            clearLoginInfo();
            Log.w("LCLogin.login: No login information stored");
            return StatusCode.NO_LOGIN_INFO_STORED;
        }

        setActualStatus(CgeoApplication.getInstance().getString(R.string.init_login_popup_working));

        final Parameters params = new Parameters("user", credentials.getUserName(), "pass", credentials.getPassword());

        String loginData = "";

        if (fakeLogin) {
            loginData = "faking a good login"; // TODO real login disabled by baiti
        } else {
            loginData = Network.getResponseData(Network.postRequest("https://", params));
        }
        if (StringUtils.isBlank(loginData)) {
            Log.e("LCLogin.login: Failed to retrieve login data");
            return StatusCode.CONNECTION_FAILED_LC; // no login page
        }

        if (loginData.contains("Wrong username or password")) { // hardcoded in English
            Log.i("Failed to log in labs.geocaching.com as " + credentials.getUserName() + " because of wrong username/password");
            return StatusCode.WRONG_LOGIN_DATA; // wrong login
        }

        if (getLoginStatus(loginData)) {
            Log.i("Successfully logged in labs.geocaching.com as " + credentials.getUserName());

            return StatusCode.NO_ERROR; // logged in
        }

        Log.i("Failed to log in labs.geocaching.com as " + credentials.getUserName() + " for some unknown reason");
        if (retry) {
            return login(false, credentials);
        }

        return StatusCode.UNKNOWN_ERROR; // can't login
    }


    /**
     * Check if the user has been logged in when he retrieved the data.
     *
     * @return {@code true} if user is logged in, {@code false} otherwise
     */
    private boolean getLoginStatus(@Nullable final String data) {
        if (StringUtils.isBlank(data) || StringUtils.equals(data, "[]")) {
            Log.e("LCLogin.getLoginStatus: No or empty data given");
            return false;
        }

        final Application application = CgeoApplication.getInstance();
        setActualStatus(application.getString(R.string.init_login_popup_ok));

        if (fakeLogin) { // TODO only for debug
            setActualLoginStatus(true);
            setActualUserName("JoeDante");
            setActualCachesFound(0);
            return true;
        } else { // TODO Debug Path
            try {
                final JsonNode json = JsonUtils.reader.readTree(data);

                if (fakeLogin) {
                    setActualLoginStatus(true);
                    setActualUserName(json.get("username").asText());
                    setActualCachesFound(json.get("found").asInt());
                    return true;
                }
                resetLoginStatus();
            } catch (IOException | NullPointerException e) {
                Log.e("LCLogin.getLoginStatus", e);
            }
            setActualStatus(application.getString(R.string.init_login_popup_failed));
            return false;
        }
    }

    @Override
    protected void resetLoginStatus() {
        setActualLoginStatus(false);
    }
}
