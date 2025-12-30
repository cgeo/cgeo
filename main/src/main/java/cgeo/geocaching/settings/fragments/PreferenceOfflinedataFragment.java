package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.PreferenceUtils;
import cgeo.geocaching.utils.SettingsUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class PreferenceOfflinedataFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_offlinedata, rootKey);

        // File handling preferences
        setupFileHandlingPreference(R.string.pref_localfile_handler_by_extension, "cgeo.geocaching.HandleLocalFilesByExtensionAlias");
        setupFileHandlingPreference(R.string.pref_localfile_handler_by_mimetype, "cgeo.geocaching.HandleLocalFilesByMimeTypeAlias");

        PreferenceUtils.setOnPreferenceClickListener(findPreference(getString(R.string.pref_fakekey_preference_maintenance_directories)), preference -> {
            // disable the button, as the cleanup runs in background and should not be invoked a second time
            preference.setEnabled(false);

            final ProgressDialog waitDialog = new ProgressDialog(getActivity());
            waitDialog.setTitle(getString(R.string.init_maintenance_start));
            waitDialog.setMessage(getString(R.string.init_maintenance_ongoing));
            waitDialog.setCancelable(false);
            waitDialog.show();

            AndroidRxUtils.andThenOnUi(Schedulers.io(), DataStore::removeObsoleteGeocacheDataDirectories, () -> {
                final Activity activity = getActivity();
                if (activity != null) {
                    ActivityMixin.showShortToast(activity, R.string.init_maintenance_finished);
                }
                waitDialog.dismiss();
            });
            return true;
        });

        final Preference isDbOnSdCard = findPreference(getString(R.string.pref_dbonsdcard));
        assert isDbOnSdCard != null;
        isDbOnSdCard.setPersistent(false);
        isDbOnSdCard.setOnPreferenceClickListener(preference -> {
            final boolean oldValue = Settings.isDbOnSDCard();
            DataStore.moveDatabase(getActivity());
            return oldValue != Settings.isDbOnSDCard();
        });

        final Preference dataDirPreference = findPreference(getString(R.string.pref_fakekey_dataDir));
        assert dataDirPreference != null;
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

    /**
     * Setup a preference to enable/disable an activity alias.
     *
     * @param preferenceKeyId The preference key resource ID
     * @param aliasClassName  The fully qualified activity alias class name
     */
    private void setupFileHandlingPreference(final int preferenceKeyId, final String aliasClassName) {
        final SwitchPreference preference = findPreference(getString(preferenceKeyId));
        if (preference != null) {
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                final boolean enabled = (Boolean) newValue;
                setActivityAliasEnabled(aliasClassName, enabled);
                return true;
            });
        }
    }

    /**
     * Enable or disable an activity alias.
     *
     * @param aliasClassName The fully qualified activity alias class name
     * @param enabled        Whether to enable or disable the alias
     */
    private void setActivityAliasEnabled(final String aliasClassName, final boolean enabled) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        try {
            final PackageManager pm = activity.getPackageManager();
            final ComponentName alias = new ComponentName(activity, aliasClassName);

            pm.setComponentEnabledSetting(
                    alias,
                    enabled
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (final Exception e) {
            // Handle SecurityException or other potential issues
            ActivityMixin.showToast(activity, "Error changing file handler setting: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        assert activity != null;
        activity.setTitle(R.string.settings_title_offlinedata);
        SettingsUtils.initPublicFolders(this, activity.getCsah());
    }
}
