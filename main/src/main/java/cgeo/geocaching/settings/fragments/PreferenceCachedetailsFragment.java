package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.OfflineTranslateUtils;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;

import org.apache.commons.lang3.ArrayUtils;

public class PreferenceCachedetailsFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_cachedetails, rootKey);

        final CharSequence[] languageNames = OfflineTranslateUtils.getSupportedLanguages().stream().map(OfflineTranslateUtils.Language::toString).toArray(CharSequence[]::new);
        final CharSequence[] languageCodes = OfflineTranslateUtils.getSupportedLanguages().stream().map(OfflineTranslateUtils.Language::getCode).toArray(CharSequence[]::new);

        final ListPreference translateTargetLng = findPreference(getString(R.string.pref_translation_language));
        translateTargetLng.setEntries(ArrayUtils.insert(0, languageNames, getString(R.string.translator_preference_disable)));
        translateTargetLng.setEntryValues(ArrayUtils.insert(0, languageCodes, ""));

        final MultiSelectListPreference noTranslateLngs = findPreference(getString(R.string.pref_translation_notranslate));
        noTranslateLngs.setEntries(languageNames);
        noTranslateLngs.setEntryValues(languageCodes);

    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.settings_title_cachedetails);
    }
}
