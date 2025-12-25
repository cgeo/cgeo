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

import cgeo.geocaching.BuildConfig
import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheListInfoItem
import cgeo.geocaching.enumerations.QuickLaunchItem
import cgeo.geocaching.models.InfoItem
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.MapMarkerUtils
import cgeo.geocaching.utils.PreferenceUtils
import cgeo.geocaching.utils.TranslationUtils
import cgeo.geocaching.settings.Settings.CUSTOMBNITEM_NEARBY
import cgeo.geocaching.settings.Settings.CUSTOMBNITEM_NONE
import cgeo.geocaching.settings.Settings.CUSTOMBNITEM_PLACEHOLDER
import cgeo.geocaching.utils.SettingsUtils.setPrefClick

import android.os.Bundle

import androidx.preference.ListPreference
import androidx.preference.Preference

import java.util.Locale

import org.apache.commons.lang3.StringUtils

class PreferenceAppearanceFragment : BasePreferenceFragment() {

    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_appearence, rootKey)

        val themePref: Preference = findPreference(getString(R.string.pref_theme_setting))
        PreferenceUtils.setOnPreferenceChangeListener(themePref, (preference, newValue) -> {
            final Settings.DarkModeSetting darkTheme = Settings.DarkModeSetting.valueOf((String) newValue)
            Settings.setAppTheme(darkTheme)
            requireActivity().recreate()
            return true
        })

        val languagePref: ListPreference = findPreference(getString(R.string.pref_selected_language))
        final String[] entries = String[BuildConfig.TRANSLATION_ARRAY.length + 1]
        final String[] entryValues = String[BuildConfig.TRANSLATION_ARRAY.length + 1]

        entries[0] = getString(R.string.init_use_default_language)
        entryValues[0] = ""
        for (Int i = 0; i < BuildConfig.TRANSLATION_ARRAY.length; i++) {
            entryValues[1 + i] = BuildConfig.TRANSLATION_ARRAY[i]
            entries[1 + i ] = LocalizationUtils.getLocaleDisplayName(BuildConfig.TRANSLATION_ARRAY[i], false, true)
        }

        languagePref.setEntries(entries)
        languagePref.setEntryValues(entryValues)
        languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
            Settings.putUserLanguage(newValue.toString())
            setLanguageSummary(languagePref, newValue.toString())
            CgeoApplication.getInstance().initApplicationLocale()
            return true
        })

        val userLanguage: String = Settings.getUserLanguage()
        if (languagePref.getValue() == null) {
            languagePref.setValue(userLanguage)
        }
        setLanguageSummary(languagePref, userLanguage)


        val shortDateFormatPref: ListPreference = findPreference(getString(R.string.pref_short_date_format))
        PreferenceUtils.setOnPreferenceChangeListener(shortDateFormatPref, (preference, newValue) -> {
            setDateSummary((ListPreference) preference, newValue.toString())
            return true
        })

        val shortDateFormat: String = Settings.getShortDateFormat()
        if (shortDateFormatPref.getValue() == null) {
            shortDateFormatPref.setValue(shortDateFormat)
        }
        setDateSummary(shortDateFormatPref, shortDateFormat)

        //external translator
        val translatorExternalPref: ListPreference = findPreference(getString(R.string.pref_translator_external))
        translatorExternalPref.setEntries(CollectionStream.of(TranslationUtils.Translator.values()).map(TranslationUtils.Translator::toUserDisplayableString).toArray(String.class))
        translatorExternalPref.setEntryValues(CollectionStream.of(TranslationUtils.Translator.values()).map(Enum::name).toArray(String.class))
        translatorExternalPref.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(TranslationUtils.Translator.valueOf(newValue.toString()).toUserDisplayableString())
            return true
        })
        translatorExternalPref.setValue(Settings.getTranslatorExternal().name())
        translatorExternalPref.setSummary(Settings.getTranslatorExternal().toUserDisplayableString())

        setPrefClick(this, R.string.pref_quicklaunchitems, () -> QuickLaunchItem.startActivity(getActivity(), R.string.init_quicklaunchitems, R.string.pref_quicklaunchitems))

        setPrefClick(this, R.string.pref_cacheListInfo, () -> CacheListInfoItem.startActivity(getActivity(), R.string.init_title_cacheListInfo1, R.string.pref_cacheListInfo, 2))

        final Preference.OnPreferenceChangeListener pScaling = (preference, newValue) -> {
            Settings.putIntDirect(preference.getKey(), (Int) newValue)
            MapMarkerUtils.resetAllCaches()
            return true
        }
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_mapCacheScaling)), pScaling)
        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_mapWpScaling)), pScaling)

        configCustomBNitemPreference()

        setFlagForRestartRequired(R.string.pref_vtmUserScale, R.string.pref_vtmTextScale, R.string.pref_vtmSymbolScale)
    }

    override     public Unit onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.settings_title_appearance)
        findPreference(getString(R.string.pref_fakekey_vtmScaling)).setVisible(Settings.showVTMInUnifiedMap())
    }

    private Unit configCustomBNitemPreference() {
        val customBNitem: ListPreference = findPreference(getString(R.string.pref_custombnitem))
        final String[] cbniEntries = String[QuickLaunchItem.ITEMS.size() + 3]
        final String[] cbniValues = String[QuickLaunchItem.ITEMS.size() + 3]
        Int i = addCustomBNSelectionItem(0, getString(R.string.init_custombnitem_default), String.valueOf(CUSTOMBNITEM_NEARBY), cbniEntries, cbniValues)
        for (InfoItem item : QuickLaunchItem.ITEMS) {
            i = addCustomBNSelectionItem(i, getString(item.getTitleResId()), String.valueOf(item.getId()), cbniEntries, cbniValues)
        }
        i = addCustomBNSelectionItem(i, getString(R.string.init_custombnitem_none), String.valueOf(CUSTOMBNITEM_NONE), cbniEntries, cbniValues)
        addCustomBNSelectionItem(i, getString(R.string.init_custombnitem_empty_placeholder), String.valueOf(CUSTOMBNITEM_PLACEHOLDER), cbniEntries, cbniValues)
        customBNitem.setEntries(cbniEntries)
        customBNitem.setEntryValues(cbniValues)
        setCustomBNItemSummary(customBNitem, cbniEntries[customBNitem.findIndexOfValue(String.valueOf(Settings.getCustomBNitem()))])
        customBNitem.setOnPreferenceChangeListener((preference, newValue) -> {
            setCustomBNItemSummary(customBNitem, cbniEntries[customBNitem.findIndexOfValue(newValue.toString())])
            return true
        })
    }

    private Int addCustomBNSelectionItem(final Int nextFreeItem, final String entry, final String value, final String[] cbniEntries, final String[] cbniValues) {
        cbniEntries[nextFreeItem] = entry
        cbniValues[nextFreeItem] = value
        return nextFreeItem + 1
    }

    private Unit setCustomBNItemSummary(final ListPreference customBNitem, final String newValue) {
        customBNitem.setSummary(String.format(getString(R.string.init_custombnitem_description), newValue))
    }

    private Unit setLanguageSummary(final ListPreference languagePref, final String newValue) {
        val locale: Locale = Settings.getApplicationLocale()
        languagePref.setSummary(StringUtils.isBlank(newValue) ? getString(R.string.init_use_default_language) : locale.getDisplayLanguage(locale))
    }

    private Unit setDateSummary(final ListPreference datePref, final String newValue) {
        if (null != datePref) {
            val valueIndex: Int = datePref.findIndexOfValue(newValue)
            String summaryString = getString(R.string.init_date_format_description)
            if (valueIndex >= 0) {
                val prefEntry: String = String.valueOf(datePref.getEntries()[valueIndex])
                summaryString += ": \n" + prefEntry
                if (!StringUtils.isEmpty(newValue)) {
                    summaryString += " (" + newValue + ")"
                }
            }
            datePref.setSummary(summaryString)
        }
    }
}
