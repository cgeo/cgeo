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

package cgeo.geocaching.settings.fragments

import cgeo.geocaching.R
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.settings.Credentials
import cgeo.geocaching.settings.CredentialsPreference
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.PreferenceUtils
import cgeo.geocaching.utils.SettingsUtils
import cgeo.geocaching.utils.ShareUtils

import android.os.Bundle

import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import java.util.Arrays

import org.apache.commons.lang3.StringUtils

class PreferenceServiceGeocachingComFragment : PreferenceFragmentCompat() {
    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_geocaching_com, rootKey)

        // Open website Preference
        val openWebsite: Preference = findPreference(getString(R.string.pref_fakekey_gc_website))
        val urlOrHost: String = GCConnector.getInstance().getHost()
        PreferenceUtils.setOnPreferenceClickListener(openWebsite, preference -> {
            val url: String = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost
            ShareUtils.openUrl(getContext(), url)
            return true
        })

        // Facebook Login Hint
        val loginFacebook: Preference = findPreference(getString(R.string.pref_gc_fb_login_hint))
        PreferenceUtils.setOnPreferenceClickListener(loginFacebook, preference -> {
            final AlertDialog.Builder builder = Dialogs.newBuilder(getContext())
            builder.setMessage(R.string.settings_info_facebook_login)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle(R.string.settings_info_facebook_login_title)
                    .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                    .setNegativeButton(R.string.more_information,
                            (dialog, id) -> ShareUtils.openUrl(getContext(), getString(R.string.settings_facebook_login_url)))
            builder.create().show()
            return true
        })

        val imageSizePref: ListPreference = findPreference(getString(R.string.pref_gc_imagesize))
        imageSizePref.setEntries(Arrays.stream(ImageUtils.GCImageSize.values()).map(is -> LocalizationUtils.getString(is.getLabel())).toArray(String[]::new))
        imageSizePref.setEntryValues(Arrays.stream(ImageUtils.GCImageSize.values()).map(Enum::name).toArray(String[]::new))
    }

    override     public Unit onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.settings_title_gc)

        // Update authentication preference
        val connector: GCConnector = GCConnector.getInstance()
        val credentials: Credentials = Settings.getCredentials(connector)
        SettingsUtils.setAuthTitle(this, R.string.pref_fakekey_gc_authorization, StringUtils.isNotBlank(credentials.getUsernameRaw()))
        val credentialsPreference: CredentialsPreference = findPreference(getString(R.string.pref_fakekey_gc_authorization))
        assert credentialsPreference != null
        if (credentials.isValid()) {
            credentialsPreference.setIcon(null)
            credentialsPreference.setSummary(getString(R.string.auth_connected_as, credentials.getUserName()))
        } else {
            credentialsPreference.setIcon(R.drawable.attribute_firstaid)
            credentialsPreference.setSummary(R.string.auth_unconnected_tap_here)
        }
        initBasicMemberPreferences()
    }

    Unit initBasicMemberPreferences() {
        findPreference(getString((R.string.preference_screen_basicmembers)))
                .setVisible(!Settings.isGCPremiumMember())
        PreferenceUtils.setEnabled(findPreference(getString((R.string.pref_loaddirectionimg))), !Settings.isGCPremiumMember())
    }
}
