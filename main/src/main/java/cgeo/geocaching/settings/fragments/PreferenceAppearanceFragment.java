package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.BuildConfig;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.QuickLaunchItem;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import static cgeo.geocaching.utils.SettingsUtils.setPrefClick;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public class PreferenceAppearanceFragment extends BasePreferenceFragment {

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
            CgeoApplication.getInstance().initApplicationLocale();
            return true;
        });
        setLanguageSummary(languagePref, Settings.getUserLanguage());

        final MultiSelectListPreference quickLaunchItemsPref = findPreference(getString(R.string.pref_quicklaunchitems));
        final String[] qlEntries = new String[QuickLaunchItem.values().length];
        final String[] qlValues = new String[QuickLaunchItem.values().length];
        int i = 0;
        for (QuickLaunchItem qlItem : QuickLaunchItem.values()) {
            qlEntries[i] = getString(qlItem.info);
            qlValues[i] = qlItem.name();
            i++;
        }
        quickLaunchItemsPref.setEntries(qlEntries);
        quickLaunchItemsPref.setEntryValues(qlValues);

    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings_title_appearance);

        setPrefClick(this, R.string.pref_wallpaper, () -> {
            if (Settings.isWallpaper()) {
                ((SettingsActivity) this.getActivity()).askShowWallpaperPermission();
            }
        });

    }

    private void setLanguageSummary(final ListPreference languagePref, final String newValue) {
        final Locale locale = Settings.getApplicationLocale();
        languagePref.setSummary(StringUtils.isBlank(newValue) ? getString(R.string.init_use_default_language) : locale.getDisplayLanguage(locale));
    }

}
