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
import cgeo.geocaching.connector.bettercacher.BetterCacherConnector
import cgeo.geocaching.utils.PreferenceUtils
import cgeo.geocaching.utils.ShareUtils

import android.os.Bundle

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import org.apache.commons.lang3.StringUtils

class PreferenceServiceBetterCacherFragment : PreferenceFragmentCompat() {
    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_bettercacher_org, rootKey)

        // Open website Preference
        val openWebsite: Preference = findPreference(getString(R.string.pref_fakekey_bettercacher_website))
        val urlOrHost: String = BetterCacherConnector.INSTANCE.getHostUrl()
        PreferenceUtils.setOnPreferenceClickListener(openWebsite, preference -> {
            val url: String = StringUtils.startsWith(urlOrHost, "http") ? urlOrHost : "http://" + urlOrHost
            ShareUtils.openUrl(getContext(), url)
            return true
        })
    }

    override     public Unit onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.init_bettercacher)
    }
}
