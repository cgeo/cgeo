package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.BuildConfig;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public class PreferenceAppearanceFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_appearence, rootKey);

        final Preference themePref = findPreference(getString(R.string.pref_theme_setting));
        themePref.setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
            final Settings.DarkModeSetting darkTheme = Settings.DarkModeSetting.valueOf((String) newValue);
            Settings.setAppTheme(darkTheme);
            requireActivity().recreate();
            return true;
        });

        final ListPreference languagePref = findPreference(getString(R.string.pref_selected_language));
        final String[] entries = new String[BuildConfig.TRANSLATION_ARRAY.length + 1];
        final String[] entryValues = new String[BuildConfig.TRANSLATION_ARRAY.length + 1];
        final Locale currentLocale = Settings.getApplicationLocale();

        entries[0] = getString(R.string.init_use_default_language);
        entryValues[0] = "";
        for (int i = 0; i < BuildConfig.TRANSLATION_ARRAY.length; i++) {
            entryValues[1 + i] = BuildConfig.TRANSLATION_ARRAY[i];
            final Locale l = new Locale(BuildConfig.TRANSLATION_ARRAY[i], "");
            entries[1 + i] = BuildConfig.TRANSLATION_ARRAY[i] + " (" + l.getDisplayLanguage(currentLocale) + ")";
        }

        languagePref.setEntries(entries);
        languagePref.setEntryValues(entryValues);
        languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
            Settings.putUserLanguage(newValue.toString());
            setLanguageSummary(languagePref, newValue.toString());
            return true;
        });
        setLanguageSummary(languagePref, Settings.getUserLanguage());
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings_title_appearance);
    }

    private void setLanguageSummary(final ListPreference languagePref, final String newValue) {
        final Locale locale = Settings.getApplicationLocale();
        languagePref.setSummary(StringUtils.isBlank(newValue) ? getString(R.string.init_use_default_language) : locale.getDisplayLanguage(locale));
    }

}
