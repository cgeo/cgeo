package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.oc.OCAuthParams;
import cgeo.geocaching.connector.oc.OCAuthorizationActivity;
import cgeo.geocaching.network.OAuthAuthorizationActivity.OAuthParameters;
import cgeo.geocaching.twitter.TwitterAuthorizationActivity;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

public class OAuthPreference extends AbstractClickablePreference {

    private static final int NO_KEY = -1;

    private enum OAuthActivityMapping {
        NONE(NO_KEY, null, null),
        OCDE(R.string.pref_fakekey_ocde_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_DE_AUTH_PARAMS),
        OCPL(R.string.pref_fakekey_ocpl_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_PL_AUTH_PARAMS),
        OCNL(R.string.pref_fakekey_ocnl_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_NL_AUTH_PARAMS),
        OCUS(R.string.pref_fakekey_ocus_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_US_AUTH_PARAMS),
        OCRO(R.string.pref_fakekey_ocro_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_RO_AUTH_PARAMS),
        TWITTER(R.string.pref_fakekey_twitter_authorization, TwitterAuthorizationActivity.class, TwitterAuthorizationActivity.TWITTER_OAUTH_PARAMS);

        public final int prefKeyId;
        public final Class<?> authActivity;
        public final OAuthParameters authParams;

        OAuthActivityMapping(int prefKeyId, Class<?> authActivity, OAuthParameters authParams) {
            this.prefKeyId = prefKeyId;
            this.authActivity = authActivity;
            this.authParams = authParams;
        }
    }

    private final OAuthActivityMapping oAuthMapping;

    private OAuthActivityMapping getAuthorization() {
        final String prefKey = getKey();
        for (OAuthActivityMapping auth : OAuthActivityMapping.values()) {
            if (auth.prefKeyId != NO_KEY && prefKey.equals(CgeoApplication.getInstance().getString(auth.prefKeyId))) {
                return auth;
            }
        }
        return OAuthActivityMapping.NONE;
    }

    public OAuthPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.oAuthMapping = getAuthorization();
    }

    public OAuthPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.oAuthMapping = getAuthorization();
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity) {
        activity.setOcAuthTitle(oAuthMapping.prefKeyId);
        return new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (oAuthMapping.authActivity != null && oAuthMapping.authParams != null) {
                    Intent authIntent = new Intent(preference.getContext(),
                            oAuthMapping.authActivity);
                    oAuthMapping.authParams.setOAuthExtras(authIntent);
                    activity.startActivityForResult(authIntent,
                            oAuthMapping.prefKeyId);
                }
                return false; // no shared preference has to be changed
            }
        };

    }
}
