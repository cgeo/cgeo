package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.OAuthAuthorizationActivity.OAuthParameters;
import cgeo.geocaching.connector.oc.OCAuthParams;
import cgeo.geocaching.connector.oc.OCAuthorizationActivity;
import cgeo.geocaching.connector.su.SuAuthorizationActivity;
import cgeo.geocaching.connector.su.SuConnector;
import cgeo.geocaching.twitter.TwitterAuthorizationActivity;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

public class OAuthPreference extends AbstractClickablePreference {

    private static final int NO_KEY = -1;

    private enum OAuthActivityMapping {
        NONE(NO_KEY, null, null, -1, -1),
        OCDE(R.string.pref_fakekey_ocde_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_DE_AUTH_PARAMS, -1, -1),
        OCPL(R.string.pref_fakekey_ocpl_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_PL_AUTH_PARAMS, -1, -1),
        OCNL(R.string.pref_fakekey_ocnl_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_NL_AUTH_PARAMS, -1, -1),
        OCUS(R.string.pref_fakekey_ocus_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_US_AUTH_PARAMS, -1, -1),
        OCRO(R.string.pref_fakekey_ocro_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_RO_AUTH_PARAMS, -1, -1),
        OCUK(R.string.pref_fakekey_ocuk_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_UK_AUTH_PARAMS, -1, -1),
        SU(R.string.pref_fakekey_su_authorization, SuAuthorizationActivity.class, SuAuthorizationActivity.SU_OAUTH_PARAMS, SuConnector.getInstance().getTokenPublicPrefKeyId(), SuConnector.getInstance().getTokenPublicPrefKeyId()),
        TWITTER(R.string.pref_fakekey_twitter_authorization, TwitterAuthorizationActivity.class, TwitterAuthorizationActivity.TWITTER_OAUTH_PARAMS, R.string.pref_twitter_token_public, R.string.pref_twitter_token_secret);

        public final int prefKeyId;
        public final int publicKeyId;
        public final int secretKeyId;
        public final Class<?> authActivity;
        public final OAuthParameters authParams;

        OAuthActivityMapping(final int prefKeyId, final Class<?> authActivity, final OAuthParameters authParams, final int publicKeyId, final int secretKeyId) {
            this.prefKeyId = prefKeyId;
            this.authActivity = authActivity;
            this.authParams = authParams;

            final OCPreferenceKeys key = OCPreferenceKeys.getByAuthId(prefKeyId);
            // Extract keys IDs from key for OC-based services
            if (key != null) {
                this.publicKeyId = key.publicTokenPrefId;
                this.secretKeyId = key.privateTokenPrefId;
            } else {
                this.publicKeyId = publicKeyId;
                this.secretKeyId = secretKeyId;
            }
        }
    }

    private final OAuthActivityMapping oAuthMapping;

    private OAuthActivityMapping getAuthorization() {
        final String prefKey = getKey();
        for (final OAuthActivityMapping auth : OAuthActivityMapping.values()) {
            if (auth.prefKeyId != NO_KEY && prefKey.equals(CgeoApplication.getInstance().getString(auth.prefKeyId))) {
                return auth;
            }
        }
        return OAuthActivityMapping.NONE;
    }

    public OAuthPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.oAuthMapping = getAuthorization();
    }

    public OAuthPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        this.oAuthMapping = getAuthorization();
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity) {
        return preference -> {
            if (oAuthMapping.authActivity != null && oAuthMapping.authParams != null) {
                final Intent authIntent = new Intent(preference.getContext(),
                        oAuthMapping.authActivity);
                oAuthMapping.authParams.setOAuthExtras(authIntent);
                activity.startActivityForResult(authIntent,
                        oAuthMapping.prefKeyId);
            }
            return false; // no shared preference has to be changed
        };

    }

    @Override
    protected boolean isAuthorized() {
        if (oAuthMapping.publicKeyId < 0 || oAuthMapping.secretKeyId < 0) {
            return false;
        }
        return Settings.hasOAuthAuthorization(oAuthMapping.publicKeyId, oAuthMapping.secretKeyId);
    }

    @Override
    protected void revokeAuthorization() {
        if (oAuthMapping.publicKeyId < 0 || oAuthMapping.secretKeyId < 0) {
            return;
        }

        Settings.setTokens(oAuthMapping.publicKeyId, null, oAuthMapping.secretKeyId, null);
    }
}
