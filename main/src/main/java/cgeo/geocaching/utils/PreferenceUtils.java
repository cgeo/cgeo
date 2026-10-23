package cgeo.geocaching.utils;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.Arrays;

public class PreferenceUtils {

    private PreferenceUtils() {
        // utility class
    }

    /** fills entries (display names) and entry values (enum names) of a list preference */
    public static <E extends Enum<E> & DisplayNameEnum> void setEnumEntries(final ListPreference preference, final Class<E> enumClass) {
        final E[] constants = enumClass.getEnumConstants();
        preference.setEntries(Arrays.stream(constants).map(DisplayNameEnum::getDisplayName).toArray(String[]::new));
        preference.setEntryValues(Arrays.stream(constants).map(Enum::name).toArray(String[]::new));
    }

    /** fills a list preference from an enum, selects the current value and keeps the summary in sync with the selection */
    public static <E extends Enum<E> & DisplayNameEnum> void configEnumListPreference(final ListPreference preference, final Class<E> enumClass, final E currentValue) {
        setEnumEntries(preference, enumClass);
        preference.setValue(currentValue.name());
        preference.setSummary(currentValue.getDisplayName());
        preference.setOnPreferenceChangeListener((p, newValue) -> {
            p.setSummary(Enum.valueOf(enumClass, newValue.toString()).getDisplayName());
            return true;
        });
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
