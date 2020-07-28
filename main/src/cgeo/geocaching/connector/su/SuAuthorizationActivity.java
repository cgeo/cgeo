package cgeo.geocaching.connector.su;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.OAuthAuthorizationActivity;
import cgeo.geocaching.settings.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class SuAuthorizationActivity extends OAuthAuthorizationActivity {

    public static final OAuthParameters SU_OAUTH_PARAMS = new OAuthParameters(
            SuConnector.getInstance().getHost(),
            "/api/oauth/request_token.php",
            "/api/oauth/authorize.php",
            "/api/oauth/access_token.php",
            SuConnector.getInstance().isHttps(),
            CgeoApplication.getInstance().getString(R.string.su_consumer_key),
            CgeoApplication.getInstance().getString(R.string.su_consumer_secret),
            "callback://www.cgeo.org/geocachingsu/");

    @StringRes
    private final int titleResId = R.string.auth_su;
    private final int tokenPublicPrefKey = R.string.pref_su_tokenpublic;
    private final int tokenSecretPrefKey = R.string.pref_su_tokensecret;
    private final int tempTokenPublicPrefKey = R.string.pref_temp_su_token_public;
    private final int tempTokenSecretPrefKey = R.string.pref_temp_su_token_secret;

    @Override
    protected String getCreateAccountUrl() {
        return SuConnector.getInstance().getCreateAccountUrl();
    }

    @Override
    @NonNull
    protected ImmutablePair<String, String> getTempTokens() {
        return Settings.getTokenPair(tempTokenPublicPrefKey, tempTokenSecretPrefKey);
    }

    @Override
    protected void setTempTokens(@Nullable final String tokenPublic, @Nullable final String tokenSecret) {
        Settings.setTokens(tempTokenPublicPrefKey, tokenPublic, tempTokenSecretPrefKey, tokenSecret);
    }

    @Override
    protected void setTokens(@Nullable final String tokenPublic, @Nullable final String tokenSecret, final boolean enable) {
        Settings.setTokens(tokenPublicPrefKey, tokenPublic, tokenSecretPrefKey, tokenSecret);
        if (tokenPublic != null) {
            Settings.setTokens(tempTokenPublicPrefKey, null, tempTokenSecretPrefKey, null);
        }
    }

    @Override
    @NonNull
    protected final String getAuthTitle() {
        return res.getString(titleResId);
    }

    @Override
    @NonNull
    protected String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed_oc, getAuthTitle());
    }

}
