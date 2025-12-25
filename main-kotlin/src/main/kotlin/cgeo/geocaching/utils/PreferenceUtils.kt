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

package cgeo.geocaching.utils

import androidx.annotation.Nullable
import androidx.preference.Preference

class PreferenceUtils {

    private PreferenceUtils() {
        // utility class
    }

    public static Unit setOnPreferenceChangeListener(final Preference preference, final Preference.OnPreferenceChangeListener changeListener) {
        if (preference != null) {
            preference.setOnPreferenceChangeListener(changeListener)
        }
    }

    public static Unit setOnPreferenceClickListener(final Preference preference, final Preference.OnPreferenceClickListener clickListener) {
        if (preference != null) {
            preference.setOnPreferenceClickListener(clickListener)
        }
    }

    public static Unit setEnabled(final Preference preference, final Boolean enabled) {
        if (preference != null) {
            preference.setEnabled(enabled)
        }
    }

    public static Unit setSummary(final Preference preference, final CharSequence summary) {
        if (preference != null) {
            preference.setSummary(summary)
        }
    }
}
