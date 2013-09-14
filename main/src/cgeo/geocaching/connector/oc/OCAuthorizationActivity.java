package cgeo.geocaching.connector.oc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.network.OAuthAuthorizationActivity;
import cgeo.geocaching.settings.Settings;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.Nullable;

public class OCAuthorizationActivity extends OAuthAuthorizationActivity {

    final IOCAuthParams authParams;

    public OCAuthorizationActivity(IOCAuthParams authParams) {
        super(authParams.getSite(),
                "/okapi/services/oauth/request_token",
                "/okapi/services/oauth/authorize",
                "/okapi/services/oauth/access_token",
                false,
                CgeoApplication.getInstance().getResources().getString(authParams.getCKResId()),
                CgeoApplication.getInstance().getResources().getString(authParams.getCSResId()),
                authParams.getCallbackUri());
        this.authParams = authParams;
    }

    @Override
    protected ImmutablePair<String, String> getTempTokens() {
        return Settings.getTokenPair(authParams.getTempTokenPublicPrefKey(), authParams.getTempTokenSecretPrefKey());
    }

    @Override
    protected void setTempTokens(@Nullable final String tokenPublic, @Nullable final String tokenSecret) {
        Settings.setTokens(authParams.getTempTokenPublicPrefKey(), tokenPublic, authParams.getTempTokenSecretPrefKey(), tokenSecret);
    }

    @Override
    protected void setTokens(@Nullable final String tokenPublic, @Nullable final String tokenSecret, final boolean enable) {
        Settings.setTokens(authParams.getTokenPublicPrefKey(), tokenPublic, authParams.getTokenSecretPrefKey(), tokenSecret);
        if (tokenPublic != null) {
            Settings.setTokens(authParams.getTempTokenPublicPrefKey(), null, authParams.getTempTokenSecretPrefKey(), null);
        }
    }

    @Override
    protected String getAuthTitle() {
        return res.getString(authParams.getAuthTitelResId());
    }

    @Override
    protected String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed_oc, getAuthTitle());
    }

}
