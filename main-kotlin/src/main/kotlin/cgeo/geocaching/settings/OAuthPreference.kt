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
import cgeo.geocaching.activity.OAuthAuthorizationActivity.OAuthParameters
import cgeo.geocaching.connector.oc.OCAuthParams
import cgeo.geocaching.connector.oc.OCAuthorizationActivity
import cgeo.geocaching.connector.su.SuAuthorizationActivity
import cgeo.geocaching.connector.su.SuConnector

import android.content.Context
import android.content.Intent
import android.util.AttributeSet

class OAuthPreference : AbstractClickablePreference() {

    private static val NO_KEY: Int = -1

    private enum class OAuthActivityMapping {
        NONE(NO_KEY, null, null, -1, -1),
        OCDE(R.string.pref_fakekey_ocde_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_DE_AUTH_PARAMS, -1, -1),
        OCPL(R.string.pref_fakekey_ocpl_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_PL_AUTH_PARAMS, -1, -1),
        OCNL(R.string.pref_fakekey_ocnl_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_NL_AUTH_PARAMS, -1, -1),
        OCUS(R.string.pref_fakekey_ocus_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_US_AUTH_PARAMS, -1, -1),
        OCRO(R.string.pref_fakekey_ocro_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_RO_AUTH_PARAMS, -1, -1),
        OCUK(R.string.pref_fakekey_ocuk_authorization, OCAuthorizationActivity.class, OCAuthParams.OC_UK_AUTH_PARAMS, -1, -1),
        SU(R.string.pref_fakekey_su_authorization, SuAuthorizationActivity.class, SuAuthorizationActivity.SU_OAUTH_PARAMS, SuConnector.getInstance().getTokenPublicPrefKeyId(), SuConnector.getInstance().getTokenPublicPrefKeyId())

        public final Int prefKeyId
        public final Int publicKeyId
        public final Int secretKeyId
        public final Class<?> authActivity
        public final OAuthParameters authParams

        OAuthActivityMapping(final Int prefKeyId, final Class<?> authActivity, final OAuthParameters authParams, final Int publicKeyId, final Int secretKeyId) {
            this.prefKeyId = prefKeyId
            this.authActivity = authActivity
            this.authParams = authParams

            val key: OCPreferenceKeys = OCPreferenceKeys.getByAuthId(prefKeyId)
            // Extract keys IDs from key for OC-based services
            if (key != null) {
                this.publicKeyId = key.publicTokenPrefId
                this.secretKeyId = key.privateTokenPrefId
            } else {
                this.publicKeyId = publicKeyId
                this.secretKeyId = secretKeyId
            }
        }
    }

    private final OAuthActivityMapping oAuthMapping

    private OAuthActivityMapping getAuthorization() {
        val prefKey: String = getKey()
        for (final OAuthActivityMapping auth : OAuthActivityMapping.values()) {
            if (auth.prefKeyId != NO_KEY && prefKey == (CgeoApplication.getInstance().getString(auth.prefKeyId))) {
                return auth
            }
        }
        return OAuthActivityMapping.NONE
    }

    public OAuthPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        this.oAuthMapping = getAuthorization()
    }

    public OAuthPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        this.oAuthMapping = getAuthorization()
    }

    override     protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity) {
        return preference -> {
            if (oAuthMapping.authActivity != null && oAuthMapping.authParams != null) {
                val authIntent: Intent = Intent(preference.getContext(),
                        oAuthMapping.authActivity)
                oAuthMapping.authParams.setOAuthExtras(authIntent)
                activity.startActivityForResult(authIntent,
                        oAuthMapping.prefKeyId)
            }
            return false; // no shared preference has to be changed
        }

    }

    override     protected Boolean isAuthorized() {
        if (oAuthMapping.publicKeyId < 0 || oAuthMapping.secretKeyId < 0) {
            return false
        }
        return Settings.hasOAuthAuthorization(oAuthMapping.publicKeyId, oAuthMapping.secretKeyId)
    }

    override     protected Unit revokeAuthorization() {
        if (oAuthMapping.publicKeyId < 0 || oAuthMapping.secretKeyId < 0) {
            return
        }

        Settings.setTokens(oAuthMapping.publicKeyId, null, oAuthMapping.secretKeyId, null)
    }
}
