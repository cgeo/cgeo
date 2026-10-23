package cgeo.geocaching.utils;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

public class PreferenceUtils {

    private PreferenceUtils() {
        // utility class
    }

    /** fills entries and entry values of a list preference from the given items */
    public static <T> void setEntries(final ListPreference preference, final Collection<? extends T> items,
                                      final Function<? super T, String> valueMapper, final Function<? super T, String> labelMapper) {
        setEntries(preference, Collections.emptyList(), items, valueMapper, labelMapper);
    }

    /** like {@link #setEntries(ListPreference, Collection, Function, Function)}, but with additional leading entries (pairs of value and label) */
    public static <T> void setEntries(final ListPreference preference, final List<Pair<String, String>> leadingEntries, final Collection<? extends T> items,
                                      final Function<? super T, String> valueMapper, final Function<? super T, String> labelMapper) {
        fillEntries((entries, values) -> {
            preference.setEntries(entries);
            preference.setEntryValues(values);
        }, leadingEntries, items, valueMapper, labelMapper);
    }

    /** fills entries and entry values of a multi select list preference from the given items */
    public static <T> void setEntries(final MultiSelectListPreference preference, final Collection<? extends T> items,
                                      final Function<? super T, String> valueMapper, final Function<? super T, String> labelMapper) {
        fillEntries((entries, values) -> {
            preference.setEntries(entries);
            preference.setEntryValues(values);
        }, Collections.emptyList(), items, valueMapper, labelMapper);
    }

    private static <T> void fillEntries(final BiConsumer<CharSequence[], CharSequence[]> target, final List<Pair<String, String>> leadingEntries,
                                        final Collection<? extends T> items, final Function<? super T, String> valueMapper, final Function<? super T, String> labelMapper) {
        final List<String> entries = new ArrayList<>();
        final List<String> values = new ArrayList<>();
        for (Pair<String, String> leading : leadingEntries) {
            values.add(leading.getLeft());
            entries.add(leading.getRight());
        }
        for (T item : items) {
            values.add(valueMapper.apply(item));
            entries.add(labelMapper.apply(item));
        }
        target.accept(entries.toArray(new CharSequence[0]), values.toArray(new CharSequence[0]));
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
