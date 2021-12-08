package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.SettingsUtils;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class PreferenceLoggingFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_logging, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings_title_logging);
        // Update "Signature" preview
        SettingsUtils.setPrefSummary(this, R.string.pref_signature, Settings.getSignature());
        findPreference(getString(R.string.pref_signature)).setOnPreferenceChangeListener((preference, newValue) -> {
            SettingsUtils.setPrefSummary(this, R.string.pref_signature, Settings.getSignature());
            return true;
        });
        // Update "Log Templates" previews
        for (int i = 1; i <= 5; i++) {
            final int preferenceId = getResources().getIdentifier("pref_logTemplate" + i, "string", getActivity().getPackageName());
            SettingsUtils.setPrefSummary(this, preferenceId, Settings.getLogTemplatePreview("pref_logTemplate" + i));
            findPreference(getString(preferenceId)).setOnPreferenceChangeListener((preference, newValue) -> {
                SettingsUtils.setPrefSummary(this, preferenceId, Settings.getLogTemplatePreview(preference.getKey()));
                return true;
            });
        }

    }
}
