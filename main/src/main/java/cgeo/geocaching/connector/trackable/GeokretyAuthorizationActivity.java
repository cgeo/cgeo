package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.TokenAuthorizationActivity;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class GeokretyAuthorizationActivity extends TokenAuthorizationActivity {

    private static final Pattern PATTERN_IS_ERROR = Pattern.compile("error ([\\d]+)");
    private static final Pattern PATTERN_TOKEN = Pattern.compile("([\\w]+)");

    public static final TokenAuthParameters GEOKRETY_TOKEN_AUTH_PARAMS = new TokenAuthParameters(
            GeokretyConnector.URL + "/api-login2secid.php",
            "login",
            "password");

    @Override
    protected String getCreateAccountUrl() {
        return GeokretyConnector.getCreateAccountUrl();
    }

    @Override
    protected void setToken(final String token) {
        Settings.setGeokretySecId(token);
    }

    @Override
    protected String getToken() {
        return Settings.getGeokretySecId();
    }

    @Override
    protected String getAuthTitle() {
        return res.getString(R.string.init_geokrety);
    }

    @Override
    protected Pattern getPatternIsError() {
        return PATTERN_IS_ERROR;
    }

    @Override
    protected Pattern getPatternToken() {
        return PATTERN_TOKEN;
    }

    @Override
    protected String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed_geokrety, getAuthTitle());
    }

    @Override
    protected String getExtendedErrorMsg(final Response response) {
        final String line = Network.getResponseData(response);
        return getExtendedErrorMsg(line);
    }

    @Override
    protected String getExtendedErrorMsg(@Nullable final String response) {
        if (StringUtils.equals(response, "1")) {
            return res.getString(R.string.err_auth_geokrety_bad_password);
        }

        return res.getString(R.string.err_auth_geokrety_unknown, getAuthTitle(), response);
    }

}
