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

package cgeo.geocaching.settings

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.TokenAuthorizationActivity.TokenAuthParameters
import cgeo.geocaching.connector.trackable.GeokretyAuthorizationActivity

import android.content.Context
import android.content.Intent
import android.util.AttributeSet

import org.apache.commons.lang3.StringUtils

class TokenPreference : AbstractClickablePreference() {

    private static val NO_KEY: Int = -1

    private enum class TokenActivityMapping {
        NONE(NO_KEY, null, null),
        GEOKRETY(R.string.pref_fakekey_geokrety_authorization, GeokretyAuthorizationActivity.class, GeokretyAuthorizationActivity.GEOKRETY_TOKEN_AUTH_PARAMS)

        public final Int prefKeyId
        public final Class<?> authActivity
        public final TokenAuthParameters authParams

        TokenActivityMapping(final Int prefKeyId, final Class<?> authActivity, final TokenAuthParameters authParams) {
            this.prefKeyId = prefKeyId
            this.authActivity = authActivity
            this.authParams = authParams
        }
    }

    private final TokenActivityMapping tokenMapping

    private TokenActivityMapping getAuthorization() {
        val prefKey: String = getKey()
        for (final TokenActivityMapping auth : TokenActivityMapping.values()) {
            if (auth.prefKeyId != NO_KEY && prefKey == (CgeoApplication.getInstance().getString(auth.prefKeyId))) {
                return auth
            }
        }
        return TokenActivityMapping.NONE
    }

    public TokenPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        this.tokenMapping = getAuthorization()
    }

    public TokenPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        this.tokenMapping = getAuthorization()
    }

    override     protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity) {
        return preference -> {
            if (tokenMapping.authActivity != null) {
                val authIntent: Intent = Intent(preference.getContext(),
                        tokenMapping.authActivity)
                tokenMapping.authParams.setTokenAuthExtras(authIntent)
                activity.startActivityForResult(authIntent,
                        tokenMapping.prefKeyId)
            }
            return false; // no shared preference has to be changed
        }
    }

    override     protected Boolean isAuthorized() {
        return StringUtils.isNotEmpty(Settings.getTokenSecret(tokenMapping.prefKeyId))
    }

    override     protected Unit revokeAuthorization() {
        Settings.setTokenSecret(tokenMapping.prefKeyId, StringUtils.EMPTY)
    }
}
