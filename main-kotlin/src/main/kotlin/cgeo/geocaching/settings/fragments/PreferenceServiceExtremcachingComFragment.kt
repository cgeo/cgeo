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

import android.os.Bundle

import androidx.preference.PreferenceFragmentCompat

class PreferenceServiceExtremcachingComFragment : PreferenceFragmentCompat() {
    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_services_extremcaching_com, rootKey)
    }

    override     public Unit onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.settings_title_ec)
    }
}
