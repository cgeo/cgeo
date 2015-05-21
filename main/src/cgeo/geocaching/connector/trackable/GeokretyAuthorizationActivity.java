package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.TokenAuthorizationActivity;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import android.os.Bundle;

import java.util.regex.Pattern;

public class GeokretyAuthorizationActivity extends TokenAuthorizationActivity {

    private static final Pattern PATTERN_IS_ERROR = Pattern.compile("error ([\\d]+)");
    private static final Pattern PATTERN_TOKEN = Pattern.compile("([\\w]+)");

    public static final TokenAuthParameters GEOKRETY_TOKEN_AUTH_PARAMS = new TokenAuthParameters(
            "http://geokrety.org/api-login2secid.php",
            "http://geokrety.org/adduser.php",
            "login",
            "password");

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    protected String getExtendedErrorMsg(final HttpResponse response) {
        final String line = Network.getResponseData(response);
        return getExtendedErrorMsg(line);
    }

    @Override
    protected String getExtendedErrorMsg(final @Nullable String response) {
        if (StringUtils.equals(response, "1")) {
            return res.getString(R.string.err_auth_geokrety_bad_password);
        }

        return res.getString(R.string.err_auth_geokrety_unknown, getAuthTitle(), response);
    }

}
