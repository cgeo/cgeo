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
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.PreferenceUtils

import android.graphics.Color
import android.os.Bundle

import androidx.annotation.NonNull
import androidx.preference.Preference
import androidx.preference.PreferenceScreen

import java.util.Objects

class PreferencesFragmentRoot : BasePreferenceFragment() {
    private var lastPreference: Preference = null

    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override     public Boolean onPreferenceTreeClick(final Preference preference) {
        // toggle icon tint to highlight selected preference screen
        if (preference.getIcon() != null) {
            if (lastPreference != null) {
                Objects.requireNonNull(lastPreference.getIcon()).setTint(Settings.isLightSkin(requireContext()) ? Color.BLACK : Color.WHITE)
            }
            lastPreference = preference
            preference.getIcon().setTint(getResources().getColor(R.color.colorAccent))
        }
        return super.onPreferenceTreeClick(preference)
    }

    override     public Unit onResume() {
        super.onResume()
        adjustScreen(Settings.extendedSettingsAreEnabled())

        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_extended_settings_enabled)), (pref, newValue) -> {
            adjustScreen((Boolean) newValue)
            return true
        })
    }

    private Unit adjustScreen(final Boolean value) {
        val prefScreen: PreferenceScreen = getPreferenceScreen()
        if (prefScreen != null) {
            val pref: Preference = prefScreen.findPreference(getString(R.string.preference_menu_offlinedata))
            if (pref != null) {
                pref.setVisible(value)
            }
        }
    }
}
