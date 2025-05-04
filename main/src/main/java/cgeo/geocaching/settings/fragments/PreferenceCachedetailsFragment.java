package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.OfflineTranslateUtils;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;

import java.util.Locale;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class PreferenceCachedetailsFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_cachedetails, rootKey);

        final CharSequence[] languageNames = OfflineTranslateUtils.getSupportedLanguages().stream().map(OfflineTranslateUtils.Language::toString).toArray(CharSequence[]::new);
        final CharSequence[] languageCodes = OfflineTranslateUtils.getSupportedLanguages().stream().map(OfflineTranslateUtils.Language::getCode).toArray(CharSequence[]::new);

        final ListPreference translateTargetLngPref = findPreference(getString(R.string.pref_translation_language));
        translateTargetLngPref.setEntries(ArrayUtils.insert(0, languageNames, getString(R.string.translator_preference_disable), getString(R.string.translator_preference_application_language)));
        translateTargetLngPref.setEntryValues(ArrayUtils.insert(0, languageCodes, OfflineTranslateUtils.LANGUAGE_INVALID, OfflineTranslateUtils.LANGUAGE_AUTOMATIC));
        translateTargetLngPref.setOnPreferenceChangeListener((preference, newValue) -> {
            setTranslateLanguageSummary(translateTargetLngPref, newValue.toString());
            return true;
        });

        setTranslateLanguageSummary(translateTargetLngPref, Settings.getTranslationTargetLanguageRaw().getCode());

        final String rawCode = Settings.getTranslationTargetLanguageRaw().getCode();
        if (translateTargetLngPref.getValue() == null) {
            translateTargetLngPref.setValue(rawCode);
        }

        final MultiSelectListPreference noTranslateLngs = findPreference(getString(R.string.pref_translation_notranslate));
        noTranslateLngs.setEntries(languageNames);
        noTranslateLngs.setEntryValues(languageCodes);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.settings_title_cachedetails);
    }

    private void setTranslateLanguageSummary(final ListPreference languagePref, final String newValue) {
        final Locale appLocale = Settings.getApplicationLocale();

        if (StringUtils.equals(newValue, OfflineTranslateUtils.LANGUAGE_INVALID)) {
            languagePref.setSummary(getString(R.string.init_translation_disabled));
        } else if (StringUtils.equals(newValue, OfflineTranslateUtils.LANGUAGE_AUTOMATIC)) {
            final OfflineTranslateUtils.Language systemLang = OfflineTranslateUtils.getAppLanguageOrDefault();
            final OfflineTranslateUtils.Language appLanguage = Settings.getApplicationLanguage();
            final String appLanguageDisplayName = appLanguage.getDisplayName(appLocale);

            if (systemLang.equals(appLanguage)) {
                languagePref.setSummary(String.format("%s: %s", getString(R.string.init_use_application_language), appLanguageDisplayName));
            } else {
                languagePref.setSummary(String.format("%s: %s\n%s", getString(R.string.init_use_application_language),
                        getString(R.string.translator_language_unsupported, appLanguageDisplayName),
                        getString(R.string.translator_target_language, systemLang.getDisplayName(appLocale))));
            }
        } else {
            final Locale newLocale = new Locale(newValue);
            languagePref.setSummary(String.format("%s (%s)", newLocale.getDisplayName(appLocale), newLocale.getDisplayName(newLocale)));
        }
    }
}
