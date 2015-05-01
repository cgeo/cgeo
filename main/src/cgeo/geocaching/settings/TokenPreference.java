package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.TokenAuthorizationActivity.TokenAuthParameters;
import cgeo.geocaching.connector.trackable.GeokretyAuthorizationActivity;

import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

public class TokenPreference extends AbstractClickablePreference {

    private static final int NO_KEY = -1;

    private enum TokenActivityMapping {
        NONE(NO_KEY, null, null),
        GEOKRETY(R.string.pref_fakekey_geokrety_authorization, GeokretyAuthorizationActivity.class, GeokretyAuthorizationActivity.GEOKRETY_TOKEN_AUTH_PARAMS);

        public final int prefKeyId;
        public final Class<?> authActivity;
        public final TokenAuthParameters authParams;

        TokenActivityMapping(final int prefKeyId, final Class<?> authActivity, final TokenAuthParameters authParams) {
            this.prefKeyId = prefKeyId;
            this.authActivity = authActivity;
            this.authParams = authParams;
        }
    }

    private final TokenActivityMapping tokenMapping;

    private TokenActivityMapping getAuthorization() {
        final String prefKey = getKey();
        for (final TokenActivityMapping auth : TokenActivityMapping.values()) {
            if (auth.prefKeyId != NO_KEY && prefKey.equals(CgeoApplication.getInstance().getString(auth.prefKeyId))) {
                return auth;
            }
        }
        return TokenActivityMapping.NONE;
    }

    public TokenPreference(final SettingsActivity activity, final AttributeSet attrs) {
        super(activity, attrs);
        tokenMapping = getAuthorization();
    }

    public TokenPreference(final SettingsActivity activity, final AttributeSet attrs, final int defStyle) {
        super(activity, attrs, defStyle);
        tokenMapping = getAuthorization();
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity) {
        activity.setAuthTitle(tokenMapping.prefKeyId);
        return new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                if (tokenMapping.authActivity != null) {
                    final Intent authIntent = new Intent(preference.getContext(),
                            tokenMapping.authActivity);
                    tokenMapping.authParams.setTokenAuthExtras(authIntent);
                    activity.startActivityForResult(authIntent,
                            tokenMapping.prefKeyId);
                }
                return false; // no shared preference has to be changed
            }
        };

    }
}
