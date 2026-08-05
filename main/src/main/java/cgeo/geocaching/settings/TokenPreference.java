package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.TokenAuthorizationActivity.TokenAuthParameters;
import cgeo.geocaching.connector.trackable.GeokretyAuthorizationActivity;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import org.apache.commons.lang3.StringUtils;

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

    public TokenPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.tokenMapping = getAuthorization();
    }

    public TokenPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        this.tokenMapping = getAuthorization();
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity) {
        return preference -> {
            if (tokenMapping.authActivity != null) {
                final Intent authIntent = new Intent(preference.getContext(),
                        tokenMapping.authActivity);
                tokenMapping.authParams.setTokenAuthExtras(authIntent);
                activity.startActivityForResult(authIntent,
                        tokenMapping.prefKeyId);
            }
            return false; // no shared preference has to be changed
        };
    }

    @Override
    protected boolean isAuthorized() {
        return StringUtils.isNotEmpty(Settings.getTokenSecret(tokenMapping.prefKeyId));
    }

    @Override
    protected void revokeAuthorization() {
        Settings.setTokenSecret(tokenMapping.prefKeyId, StringUtils.EMPTY);
    }
}
