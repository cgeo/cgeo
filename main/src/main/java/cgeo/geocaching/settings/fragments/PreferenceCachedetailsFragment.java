package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.MlKitTranslateUtil;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;

public class PreferenceCachedetailsFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_cachedetails, rootKey);

        final ListPreference translateTargetLng = findPreference(getString(R.string.pref_translation_language));
        translateTargetLng.setEntries(MlKitTranslateUtil.getSupportedLanguageDisplaynames());
        translateTargetLng.setEntryValues(MlKitTranslateUtil.getSupportedLanguageCodes());

        final MultiSelectListPreference noTranslateLngs = findPreference(getString(R.string.pref_translation_notranslate));
        noTranslateLngs.setEntries(MlKitTranslateUtil.getSupportedLanguageDisplaynames());
        noTranslateLngs.setEntryValues(MlKitTranslateUtil.getSupportedLanguageCodes());

    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.settings_title_cachedetails);
    }
}
