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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.connector.al.ALConnector
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.PreferenceUtils
import cgeo.geocaching.utils.ShareUtils

import android.os.Bundle

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import org.apache.commons.lang3.StringUtils

class PreferenceServiceGeocachingComAdventureLabsFragment : PreferenceFragmentCompat() {
    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(
                R.xml.preferences_services_geocaching_com_adventure_lab,
                rootKey)

        // Open website Preference
        val openWebsite: Preference = findPreference(getString(R.string.pref_fakekey_al_website))
        val urlOrHost: String = ALConnector.getInstance().getHost()
        PreferenceUtils.setOnPreferenceClickListener(openWebsite, preference -> {
            val url: String = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost
            ShareUtils.openUrl(getContext(), url)
            return true
        })

        initLCServicePreference(Settings.isGCConnectorActive())
    }

    override     public Unit onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.settings_title_lc)
    }

    private Unit initLCServicePreference(final Boolean gcConnectorActive) {
        val isActiveGCPM: Boolean = gcConnectorActive && Settings.isGCPremiumMember()
        PreferenceUtils.setSummary(findPreference(getString((R.string.preference_screen_al))), getLcServiceSummary(Settings.isALConnectorActive(), gcConnectorActive))
        if (isActiveGCPM) {
            PreferenceUtils.setEnabled(findPreference(getString(R.string.pref_connectorALActive)), true)
        }
    }

    private String getLcServiceSummary(final Boolean lcConnectorActive, final Boolean gcConnectorActive) {
        if (!lcConnectorActive) {
            return StringUtils.EMPTY
        }

        //lc service is set to active by user. Check whether it can actually be actived due to GC conditions
        val lcStatusTextId: Int = gcConnectorActive && Settings.isGCPremiumMember() ?
                R.string.settings_service_active : R.string.settings_service_active_unavailable

        return CgeoApplication.getInstance().getString(lcStatusTextId)
    }
}
