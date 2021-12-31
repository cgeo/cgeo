package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.SettingsUtils;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.preference.Preference;

import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class PreferenceOfflinedataFragment extends BasePreferenceFragment {
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

        final Preference isDbOnSdCard = findPreference(getString(R.string.pref_dbonsdcard));
        isDbOnSdCard.setPersistent(false);
        isDbOnSdCard.setOnPreferenceClickListener(preference -> {
            final boolean oldValue = Settings.isDbOnSDCard();
            DataStore.moveDatabase(getActivity());
            return oldValue != Settings.isDbOnSDCard();
        });

        final Preference dataDirPreference = findPreference(getString(R.string.pref_fakekey_dataDir));
        dataDirPreference.setSummary(Settings.getExternalPrivateCgeoDirectory());
        if (LocalStorage.getAvailableExternalPrivateCgeoDirectories().size() < 2) {
            dataDirPreference.setEnabled(false);
        } else {
            final AtomicLong usedBytes = new AtomicLong();
            dataDirPreference.setOnPreferenceClickListener(preference -> {
                final ProgressDialog progress = ProgressDialog.show(getActivity(), getString(R.string.calculate_dataDir_title), getString(R.string.calculate_dataDir), true, false);
                AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
                    // calculate disk usage
                    usedBytes.set(FileUtils.getSize(LocalStorage.getExternalPrivateCgeoDirectory()));
                }, () -> {
                    progress.dismiss();
                    SettingsUtils.showExtCgeoDirChooser(this, usedBytes.get());
                });
                return true;
            });
        }
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
