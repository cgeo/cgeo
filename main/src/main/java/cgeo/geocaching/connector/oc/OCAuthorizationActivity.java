package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.OAuthAuthorizationActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.oc.OkapiError.OkapiErrors;
import cgeo.geocaching.settings.Settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class OCAuthorizationActivity extends OAuthAuthorizationActivity {

    @StringRes
    private int titleResId;
    private int tokenPublicPrefKey;
    private int tokenSecretPrefKey;
    private int tempTokenPublicPrefKey;
    private int tempTokenSecretPrefKey;
    private String urlHost;

    @Override
    public void onCreate(final Bundle savedInstanceState) {

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            titleResId = extras.getInt(Intents.EXTRA_OAUTH_TITLE_RES_ID);
            tokenPublicPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TOKEN_PUBLIC_KEY);
            tokenSecretPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TOKEN_SECRET_KEY);
            tempTokenPublicPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TEMP_TOKEN_KEY_PREF);
            tempTokenSecretPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TEMP_TOKEN_SECRET_PREF);
            urlHost = extras.getString(Intents.EXTRA_OAUTH_HOST);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String getCreateAccountUrl() {
        return getConnector().getCreateAccountUrl();
    }

    private IConnector getConnector() {
        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            if (connector.getHost().equalsIgnoreCase(urlHost)) {
                return connector;
            }
        }
        throw new IllegalStateException("Cannot find connector for host " + urlHost);
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
    protected String getAuthTitle() {
        return res.getString(titleResId);
    }

    @Override
    @NonNull
    protected String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed_oc, getAuthTitle());
    }

    /**
     * Return an extended error in case of an invalid time stamp
     *
     * @param response network response
     */
    @Override
    @NonNull
    protected String getExtendedErrorMsg(final Response response) {
        final OkapiError error = OkapiClient.decodeErrorResponse(response);
        if (error.getResult() == OkapiErrors.INVALID_TIMESTAMP) {
            return res.getString(R.string.init_login_popup_invalid_timestamp);
        }
        return StringUtils.EMPTY;
    }
}
