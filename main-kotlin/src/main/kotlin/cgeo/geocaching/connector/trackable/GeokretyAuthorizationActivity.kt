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

package cgeo.geocaching.connector.trackable

import cgeo.geocaching.R
import cgeo.geocaching.activity.TokenAuthorizationActivity
import cgeo.geocaching.network.Network
import cgeo.geocaching.settings.Settings

import androidx.annotation.Nullable

import java.util.regex.Pattern

import okhttp3.Response
import org.apache.commons.lang3.StringUtils

class GeokretyAuthorizationActivity : TokenAuthorizationActivity() {

    private static val PATTERN_IS_ERROR: Pattern = Pattern.compile("error ([\\d]+)")
    private static val PATTERN_TOKEN: Pattern = Pattern.compile("([\\w]+)")

    public static val GEOKRETY_TOKEN_AUTH_PARAMS: TokenAuthParameters = TokenAuthParameters(
            GeokretyConnector.URL + "/api-login2secid.php",
            "login",
            "password")

    override     protected String getCreateAccountUrl() {
        return GeokretyConnector.getCreateAccountUrl()
    }

    override     protected Unit setToken(final String token) {
        Settings.setGeokretySecId(token)
    }

    override     protected String getToken() {
        return Settings.getGeokretySecId()
    }

    override     protected String getAuthTitle() {
        return res.getString(R.string.init_geokrety)
    }

    override     protected Pattern getPatternIsError() {
        return PATTERN_IS_ERROR
    }

    override     protected Pattern getPatternToken() {
        return PATTERN_TOKEN
    }

    override     protected String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed_geokrety, getAuthTitle())
    }

    override     protected String getExtendedErrorMsg(final Response response) {
        val line: String = Network.getResponseData(response)
        return getExtendedErrorMsg(line)
    }

    override     protected String getExtendedErrorMsg(final String response) {
        if (StringUtils == (response, "1")) {
            return res.getString(R.string.err_auth_geokrety_bad_password)
        }

        return res.getString(R.string.err_auth_geokrety_unknown, getAuthTitle(), response)
    }

}
