package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.BuildConfig;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListInfoItem;
import cgeo.geocaching.enumerations.QuickLaunchItem;
import cgeo.geocaching.models.InfoItem;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.PreferenceUtils;
import static cgeo.geocaching.settings.Settings.CUSTOMBNITEM_NEARBY;
import static cgeo.geocaching.settings.Settings.CUSTOMBNITEM_NONE;
import static cgeo.geocaching.settings.Settings.CUSTOMBNITEM_PLACEHOLDER;
import static cgeo.geocaching.utils.SettingsUtils.setPrefClick;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public class PreferenceAppearanceFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_appearence, rootKey);

        final Preference themePref = findPreference(getString(R.string.pref_theme_setting));
        PreferenceUtils.setOnPreferenceChangeListener(themePref, (preference, newValue) -> {
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

        final String userLanguage = Settings.getUserLanguage();
        if (languagePref.getValue() == null) {
            languagePref.setValue(userLanguage);
        }
        setLanguageSummary(languagePref, userLanguage);


        final ListPreference shortDateFormatPref = findPreference(getString(R.string.pref_short_date_format));
        PreferenceUtils.setOnPreferenceChangeListener(shortDateFormatPref, (preference, newValue) -> {
            setDateSummary((ListPreference) preference, newValue.toString());
            return true;
        });

        final String shortDateFormat = Settings.getShortDateFormat();
        if (shortDateFormatPref.getValue() == null) {
            shortDateFormatPref.setValue(shortDateFormat);
        }
        setDateSummary(shortDateFormatPref, shortDateFormat);




        setPrefClick(this, R.string.pref_quicklaunchitems, () -> QuickLaunchItem.startActivity(getActivity(), R.string.init_quicklaunchitems, R.string.pref_quicklaunchitems));

        setPrefClick(this, R.string.pref_cacheListInfo, () -> CacheListInfoItem.startActivity(getActivity(), R.string.init_title_cacheListInfo1, R.string.pref_cacheListInfo, 2));

        final Preference.OnPreferenceChangeListener pScaling = (preference, newValue) -> {
            Settings.putIntDirect(preference.getKey(), (int) newValue);
            MapMarkerUtils.resetAllCaches();
            return true;
        };
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_mapCacheScaling)), pScaling);
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_mapWpScaling)), pScaling);

        configCustomBNitemPreference();

        setFlagForRestartRequired(R.string.pref_vtmUserScale, R.string.pref_vtmTextScale, R.string.pref_vtmSymbolScale);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.settings_title_appearance);
        findPreference(getString(R.string.pref_fakekey_vtmScaling)).setVisible(Settings.showVTMInUnifiedMap());
    }

    private void configCustomBNitemPreference() {
        final ListPreference customBNitem = findPreference(getString(R.string.pref_custombnitem));
        final String[] cbniEntries = new String[QuickLaunchItem.ITEMS.size() + 3];
        final String[] cbniValues = new String[QuickLaunchItem.ITEMS.size() + 3];
        int i = addCustomBNSelectionItem(0, getString(R.string.init_custombnitem_default), String.valueOf(CUSTOMBNITEM_NEARBY), cbniEntries, cbniValues);
        for (InfoItem item : QuickLaunchItem.ITEMS) {
            i = addCustomBNSelectionItem(i, getString(item.getTitleResId()), String.valueOf(item.getId()), cbniEntries, cbniValues);
        }
        i = addCustomBNSelectionItem(i, getString(R.string.init_custombnitem_none), String.valueOf(CUSTOMBNITEM_NONE), cbniEntries, cbniValues);
        addCustomBNSelectionItem(i, getString(R.string.init_custombnitem_empty_placeholder), String.valueOf(CUSTOMBNITEM_PLACEHOLDER), cbniEntries, cbniValues);
        customBNitem.setEntries(cbniEntries);
        customBNitem.setEntryValues(cbniValues);
        setCustomBNItemSummary(customBNitem, cbniEntries[customBNitem.findIndexOfValue(String.valueOf(Settings.getCustomBNitem()))]);
        customBNitem.setOnPreferenceChangeListener((preference, newValue) -> {
            setCustomBNItemSummary(customBNitem, cbniEntries[customBNitem.findIndexOfValue(newValue.toString())]);
            return true;
        });
    }

    private int addCustomBNSelectionItem(final int nextFreeItem, final String entry, final String value, final String[] cbniEntries, final String[] cbniValues) {
        cbniEntries[nextFreeItem] = entry;
        cbniValues[nextFreeItem] = value;
        return nextFreeItem + 1;
    }

    private void setCustomBNItemSummary(final ListPreference customBNitem, final String newValue) {
        customBNitem.setSummary(String.format(getString(R.string.init_custombnitem_description), newValue));
    }

    private void setLanguageSummary(final ListPreference languagePref, final String newValue) {
        final Locale locale = Settings.getApplicationLocale();
        languagePref.setSummary(StringUtils.isBlank(newValue) ? getString(R.string.init_use_default_language) : locale.getDisplayLanguage(locale));
    }

    private void setDateSummary(final ListPreference datePref, final String newValue) {
        if (null != datePref) {
            final int valueIndex = datePref.findIndexOfValue(newValue);
            String summaryString = getString(R.string.init_date_format_description);
            if (valueIndex >= 0) {
                final String prefEntry = String.valueOf(datePref.getEntries()[valueIndex]);
                summaryString += ": \n" + prefEntry;
                if (!StringUtils.isEmpty(newValue)) {
                    summaryString += " (" + newValue + ")";
                }
            }
            datePref.setSummary(summaryString);
        }
    }
}
