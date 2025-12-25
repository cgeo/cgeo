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
import cgeo.geocaching.apps.navi.NavigationAppFactory
import cgeo.geocaching.utils.PreferenceUtils

import android.os.Bundle

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat


class PreferenceNavigationNavigationFragment : PreferenceFragmentCompat() {
    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_navigation_navigation, rootKey)

        initNavigationMenuPreferences()
    }

    override     public Unit onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.settings_title_navigation_menu)
    }

    private Unit initNavigationMenuPreferences() {
        for (final NavigationAppFactory.NavigationAppsEnum appEnum : NavigationAppFactory.NavigationAppsEnum.values()) {
            val preference: Preference = findPreference(getString(appEnum.preferenceKey))
            if (appEnum.app.isInstalled()) {
                PreferenceUtils.setEnabled(preference, true)
            } else {
                PreferenceUtils.setSummary(preference, getString(R.string.settings_navigation_disabled))
            }
        }
    }
}
