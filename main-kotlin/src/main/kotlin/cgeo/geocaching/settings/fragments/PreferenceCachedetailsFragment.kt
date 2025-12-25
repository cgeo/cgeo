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
import cgeo.geocaching.utils.OfflineTranslateUtils

import android.os.Bundle

import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference

import java.util.Locale

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils

class PreferenceCachedetailsFragment : BasePreferenceFragment() {
    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_cachedetails, rootKey)

        final CharSequence[] languageNames = OfflineTranslateUtils.getSupportedLanguages().stream().map(OfflineTranslateUtils.Language::toString).toArray(CharSequence[]::new)
        final CharSequence[] languageCodes = OfflineTranslateUtils.getSupportedLanguages().stream().map(OfflineTranslateUtils.Language::getCode).toArray(CharSequence[]::new)

        val translateTargetLngPref: ListPreference = findPreference(getString(R.string.pref_translation_language))
        translateTargetLngPref.setEntries(ArrayUtils.insert(0, languageNames, getString(R.string.translator_preference_disable), getString(R.string.translator_preference_application_language)))
        translateTargetLngPref.setEntryValues(ArrayUtils.insert(0, languageCodes, OfflineTranslateUtils.LANGUAGE_INVALID, OfflineTranslateUtils.LANGUAGE_AUTOMATIC))
        translateTargetLngPref.setOnPreferenceChangeListener((preference, newValue) -> {
            setTranslateLanguageSummary(translateTargetLngPref, newValue.toString())
            return true
        })

        setTranslateLanguageSummary(translateTargetLngPref, Settings.getTranslationTargetLanguageRaw().getCode())

        val rawCode: String = Settings.getTranslationTargetLanguageRaw().getCode()
        if (translateTargetLngPref.getValue() == null) {
            translateTargetLngPref.setValue(rawCode)
        }

        val noTranslateLngs: MultiSelectListPreference = findPreference(getString(R.string.pref_translation_notranslate))
        noTranslateLngs.setEntries(languageNames)
        noTranslateLngs.setEntryValues(languageCodes)
    }

    override     public Unit onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.settings_title_cachedetails)
    }

    private Unit setTranslateLanguageSummary(final ListPreference languagePref, final String newValue) {
        val appLocale: Locale = Settings.getApplicationLocale()

        if (StringUtils == (newValue, OfflineTranslateUtils.LANGUAGE_INVALID)) {
            languagePref.setSummary(getString(R.string.init_translation_disabled))
        } else if (StringUtils == (newValue, OfflineTranslateUtils.LANGUAGE_AUTOMATIC)) {
            final OfflineTranslateUtils.Language systemLang = OfflineTranslateUtils.getAppLanguageOrDefault()
            final OfflineTranslateUtils.Language appLanguage = Settings.getApplicationLanguage()
            val appLanguageDisplayName: String = appLanguage.getDisplayName()

            if (systemLang == (appLanguage)) {
                languagePref.setSummary(String.format("%s: %s", getString(R.string.init_use_application_language), appLanguageDisplayName))
            } else {
                languagePref.setSummary(String.format("%s: %s\n%s", getString(R.string.init_use_application_language),
                        getString(R.string.translator_language_unsupported, appLanguageDisplayName),
                        getString(R.string.translator_target_language, systemLang.getDisplayName())))
            }
        } else {
            val newLocale: Locale = Locale(newValue)
            languagePref.setSummary(String.format("%s (%s)", newLocale.getDisplayName(appLocale), newLocale.getDisplayName(newLocale)))
        }
    }
}
