package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;

import com.google.mlkit.nl.translate.TranslateLanguage;

public class PreferenceCachedetailsFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_cachedetails, rootKey);

        CharSequence[] languages = TranslateLanguage.getAllLanguages().stream().sorted().toArray(CharSequence[]::new);

        final ListPreference translateTargetLng = findPreference(getString(R.string.pref_translation_language));
        translateTargetLng.setEntries(languages);
        translateTargetLng.setEntryValues(languages);

        final MultiSelectListPreference noTranslateLngs = findPreference(getString(R.string.pref_translation_notranslate));
        noTranslateLngs.setEntries(languages);
        noTranslateLngs.setEntryValues(languages);
        noTranslateLngs.setSummary(Settings.getLanguagesToNotTranslate().toString());

    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.settings_title_cachedetails);
    }
}
