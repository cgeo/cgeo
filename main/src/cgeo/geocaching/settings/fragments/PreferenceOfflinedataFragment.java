package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import static cgeo.geocaching.utils.SettingsUtils.initPublicFolders;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class PreferenceOfflinedataFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_offlinedata, rootKey);

        // TODO
        findPreference(getString(R.string.pref_fakekey_dataDir)).setSummary(Settings.getExternalPrivateCgeoDirectory());
    }

    @Override
    public void onResume() {
        super.onResume();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        assert activity != null;
        getActivity().setTitle(R.string.settings_title_offlinedata);
        initPublicFolders(this, activity.getCsah());
    }
}
