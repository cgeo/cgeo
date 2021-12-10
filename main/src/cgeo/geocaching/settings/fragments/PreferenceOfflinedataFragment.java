package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.SettingsUtils;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class PreferenceOfflinedataFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_offlinedata, rootKey);

        findPreference(getString(R.string.pref_fakekey_preference_maintenance_directories)).setOnPreferenceClickListener(preference -> {
            // disable the button, as the cleanup runs in background and should not be invoked a second time
            preference.setEnabled(false);
            ActivityMixin.showShortToast(getActivity(), R.string.init_maintenance_start);
            AndroidRxUtils.andThenOnUi(Schedulers.io(), DataStore::removeObsoleteGeocacheDataDirectories, () -> ActivityMixin.showShortToast(getActivity(), R.string.init_maintenance_finished));
            return true;
        });

        findPreference(getString(R.string.pref_dbonsdcard)).setOnPreferenceClickListener(preference -> {
            final boolean oldValue = Settings.isDbOnSDCard();
            DataStore.moveDatabase(getActivity());
            return oldValue != Settings.isDbOnSDCard();
        });

        // TODO
        findPreference(getString(R.string.pref_fakekey_dataDir)).setSummary(Settings.getExternalPrivateCgeoDirectory());
    }

    @Override
    public void onResume() {
        super.onResume();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        assert activity != null;
        getActivity().setTitle(R.string.settings_title_offlinedata);
        SettingsUtils.initPublicFolders(this, activity.getCsah());
    }
}
