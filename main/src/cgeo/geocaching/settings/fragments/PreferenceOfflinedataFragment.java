package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class PreferenceOfflinedataFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_offlinedata, rootKey);

        // TODO
        findPreference(getString(R.string.pref_fakekey_dataDir)).setSummary(Settings.getExternalPrivateCgeoDirectory());
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings_title_offlinedata);
    }
}
