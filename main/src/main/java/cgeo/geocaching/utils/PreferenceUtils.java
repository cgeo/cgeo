package cgeo.geocaching.utils;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

public class PreferenceUtils {

    private PreferenceUtils() {
        // utility class
    }

    public static void setOnPreferenceChangeListener(@Nullable final Preference preference, @Nullable final Preference.OnPreferenceChangeListener changeListener) {
        if (preference != null) {
            preference.setOnPreferenceChangeListener(changeListener);
        }
    }

    public static void setOnPreferenceClickListener(@Nullable final Preference preference, @Nullable final Preference.OnPreferenceClickListener clickListener) {
        if (preference != null) {
            preference.setOnPreferenceClickListener(clickListener);
        }
    }

    public static void setEnabled(@Nullable final Preference preference, final boolean enabled) {
        if (preference != null) {
            preference.setEnabled(enabled);
        }
    }

    public static void setSummary(@Nullable final Preference preference, @Nullable final CharSequence summary) {
        if (preference != null) {
            preference.setSummary(summary);
        }
    }
}
