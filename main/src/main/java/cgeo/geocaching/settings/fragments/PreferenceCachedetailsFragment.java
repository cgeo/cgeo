package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.Settings.FriendLogTabOwnerMode;
import cgeo.geocaching.settings.Settings.NamedFilterDisplayMode;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.OfflineTranslateUtils;
import cgeo.geocaching.utils.PreferenceUtils;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;

public class PreferenceCachedetailsFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_cachedetails, rootKey);

        configTranslationLanguagePreference();
        configNamedFilterDisplayModePreference();
        configFriendLogTabOwnerModePreference();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.settings_title_cachedetails);
    }

    private void configNamedFilterDisplayModePreference() {
        final ListPreference namedFilterDisplayModePref = findPreference(getString(R.string.pref_named_filter_display_mode));
        PreferenceUtils.configEnumListPreference(namedFilterDisplayModePref, NamedFilterDisplayMode.class, Settings.getNamedFilterDisplayMode());
    }

    private void configFriendLogTabOwnerModePreference() {
        final ListPreference friendLogTabOwnerModePref = findPreference(getString(R.string.pref_friend_log_tab_owner_mode));
        PreferenceUtils.configEnumListPreference(friendLogTabOwnerModePref, FriendLogTabOwnerMode.class, Settings.getFriendLogTabOwnerMode());
    }

    private void configTranslationLanguagePreference() {
        final List<OfflineTranslateUtils.Language> languages = OfflineTranslateUtils.getSupportedLanguages();

        final ListPreference translateTargetLngPref = findPreference(getString(R.string.pref_translation_language));
        if (translateTargetLngPref != null) {
            PreferenceUtils.setEntries(translateTargetLngPref, Arrays.asList(
                            Pair.of(OfflineTranslateUtils.LANGUAGE_INVALID, LocalizationUtils.getString(R.string.translator_preference_disable)),
                            Pair.of(OfflineTranslateUtils.LANGUAGE_AUTOMATIC, LocalizationUtils.getString(R.string.translator_preference_application_language))),
                    languages, OfflineTranslateUtils.Language::getCode, OfflineTranslateUtils.Language::toString);
            translateTargetLngPref.setOnPreferenceChangeListener((preference, newValue) -> {
                setTranslateLanguageSummary(translateTargetLngPref, newValue.toString());
                return true;
            });

            setTranslateLanguageSummary(translateTargetLngPref, Settings.getTranslationTargetLanguageRaw().getCode());

            final String rawCode = Settings.getTranslationTargetLanguageRaw().getCode();
            if (translateTargetLngPref.getValue() == null) {
                translateTargetLngPref.setValue(rawCode);
            }
        }

        final MultiSelectListPreference noTranslateLngs = findPreference(getString(R.string.pref_translation_notranslate));
        PreferenceUtils.setEntries(noTranslateLngs, languages, OfflineTranslateUtils.Language::getCode, OfflineTranslateUtils.Language::toString);
    }

    private void setTranslateLanguageSummary(final ListPreference languagePref, final String newValue) {

        final Locale appLocale = Settings.getApplicationLocale();

        if (Strings.CS.equals(newValue, OfflineTranslateUtils.LANGUAGE_INVALID)) {
            languagePref.setSummary(LocalizationUtils.getString(R.string.init_translation_disabled));
        } else if (Strings.CS.equals(newValue, OfflineTranslateUtils.LANGUAGE_AUTOMATIC)) {
            final OfflineTranslateUtils.Language systemLang = OfflineTranslateUtils.getAppLanguageOrDefault();
            final OfflineTranslateUtils.Language appLanguage = Settings.getApplicationLanguage();
            final String appLanguageDisplayName = appLanguage.getDisplayName();

            if (systemLang.equals(appLanguage)) {
                languagePref.setSummary(String.format("%s: %s", LocalizationUtils.getString(R.string.init_use_application_language), appLanguageDisplayName));
            } else {
                languagePref.setSummary(String.format("%s: %s\n%s", LocalizationUtils.getString(R.string.init_use_application_language),
                        LocalizationUtils.getString(R.string.translator_language_unsupported, appLanguageDisplayName),
                        LocalizationUtils.getString(R.string.translator_target_language, systemLang.getDisplayName())));
            }
        } else {
            final Locale newLocale = new Locale(newValue);
            languagePref.setSummary(String.format("%s (%s)", newLocale.getDisplayName(appLocale), newLocale.getDisplayName(newLocale)));
        }
    }
}
