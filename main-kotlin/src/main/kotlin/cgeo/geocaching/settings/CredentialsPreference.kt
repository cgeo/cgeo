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
import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.connector.capability.IAvatar
import cgeo.geocaching.connector.capability.ICredentials
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.network.Cookies
import cgeo.geocaching.ui.AvatarUtils
import cgeo.geocaching.utils.AndroidRxUtils

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout

import androidx.annotation.NonNull
import androidx.preference.PreferenceViewHolder

import org.apache.commons.lang3.StringUtils

class CredentialsPreference : AbstractClickablePreference() {

    private static val NO_KEY: Int = -1

    private final CredentialActivityMapping credentialsMapping

    private LinearLayout avatarFrame

    private enum class CredentialActivityMapping {
        GEOCACHING(R.string.pref_fakekey_gc_authorization, GCAuthorizationActivity.class, GCConnector.getInstance())

        public final Int prefKeyId
        private final Class<?> authActivity
        private final ICredentials connector

        CredentialActivityMapping(final Int prefKeyId, final Class<?> authActivity, final ICredentials connector) {
            this.prefKeyId = prefKeyId
            this.authActivity = authActivity
            this.connector = connector
        }

        public Class<?> getAuthActivity() {
            return authActivity
        }

        public ICredentials getConnector() {
            return connector
        }
    }

    private CredentialActivityMapping getAuthorization() {
        val prefKey: String = getKey()
        for (final CredentialActivityMapping auth : CredentialActivityMapping.values()) {
            if (auth.prefKeyId != NO_KEY && prefKey == (CgeoApplication.getInstance().getString(auth.prefKeyId))) {
                return auth
            }
        }
        throw IllegalStateException("Invalid authorization preference")
    }

    public CredentialsPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        this.credentialsMapping = getAuthorization()
    }

    public CredentialsPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        this.credentialsMapping = getAuthorization()
    }

    override     protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity settingsActivity) {
        return preference -> {
            val checkIntent: Intent = Intent(preference.getContext(), credentialsMapping.getAuthActivity())

            val credentials: Credentials = credentialsMapping.getConnector().getCredentials()
            checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_USERNAME, credentials.getUsernameRaw())
            checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_PASSWORD, credentials.getPasswordRaw())

            settingsActivity.startActivityForResult(checkIntent, credentialsMapping.prefKeyId)
            return false; // no shared preference has to be changed
        }
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)
        avatarFrame = (LinearLayout) holder.findViewById(android.R.id.widget_frame)
    }

    public Unit resetAvatarImage() {
        if (avatarFrame == null) {
            return
        }

        if (credentialsMapping.getConnector() is IAvatar) {
            AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler,
                    () -> AvatarUtils.getAvatar((IAvatar) credentialsMapping.getConnector()),
                    img -> {
                        if (img != null) {
                            val iconView: ImageView = ImageView(getContext())
                            iconView.setImageDrawable(img)

                            avatarFrame.removeAllViews()
                            avatarFrame.addView(iconView)
                            avatarFrame.setVisibility(View.VISIBLE)

                            final LinearLayout.LayoutParams param = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.5f)
                            avatarFrame.setLayoutParams(param)
                        } else {
                            avatarFrame.setVisibility(View.GONE)
                        }
                    })
        } else {
            avatarFrame.setVisibility(View.GONE)
        }
    }

    override     protected Boolean isAuthorized() {
        return Settings.getCredentials(credentialsMapping.getConnector()).isValid()
    }

    override     protected Unit revokeAuthorization() {
        if (credentialsMapping == CredentialActivityMapping.GEOCACHING) {
            Cookies.clearCookies()
        }
        Settings.setCredentials(credentialsMapping.getConnector(), Credentials.EMPTY)

        if (credentialsMapping.getConnector() is IAvatar) {
            AvatarUtils.changeAvatar((IAvatar) credentialsMapping.getConnector(), StringUtils.EMPTY)
            resetAvatarImage()
        }
    }
}
