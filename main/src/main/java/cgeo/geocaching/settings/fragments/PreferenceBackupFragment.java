package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.BackupSeekbarPreference;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.BackupUtils;
import cgeo.geocaching.utils.PreferenceUtils;

import android.os.Bundle;

import androidx.preference.CheckBoxPreference;

public class PreferenceBackupFragment extends BasePreferenceFragment {
    public static final String STATE_BACKUPUTILS = "backuputils";

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_backup, rootKey);

        final BackupUtils backupUtils = ((SettingsActivity) getActivity()).getBackupUtils();

        PreferenceUtils.setOnPreferenceClickListener(findPreference(getString(R.string.pref_fakekey_preference_startbackup)), preference -> {
            backupUtils.backup(this::updateSummary, false);
            return true;
        });

        PreferenceUtils.setOnPreferenceClickListener(findPreference(getString(R.string.pref_fakekey_startrestore)), preference -> {
            backupUtils.restore(BackupUtils.newestBackupFolder(false));
            return true;
        });

        PreferenceUtils.setOnPreferenceClickListener(findPreference(getString(R.string.pref_fakekey_startrestore_dirselect)), preference -> {
            backupUtils.selectBackupDirIntent();
            return true;
        });

        final CheckBoxPreference loginData = findPreference(getString(R.string.pref_backup_logins));
        PreferenceUtils.setOnPreferenceClickListener(loginData, preference -> {
            if (loginData.isChecked()) {
                loginData.setChecked(false);
                SimpleDialog.of(getActivity()).setTitle(R.string.init_backup_settings_logins).setMessage(R.string.init_backup_settings_backup_full_confirm).confirm(() -> loginData.setChecked(true));
            }
            return true;
        });

        updateSummary();

        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_backup_backup_history_length)), (preference, value) -> {
            backupUtils.deleteBackupHistoryDialog((BackupSeekbarPreference) preference, (int) value, false);
            return true;
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.settings_title_backup);
    }

    private void updateSummary() {
        final String textRestore;
        if (BackupUtils.hasBackup(BackupUtils.newestBackupFolder(false))) {
            textRestore = getString(R.string.init_backup_last) + " " + BackupUtils.getNewestBackupDateTime(false);
        } else {
            textRestore = getString(R.string.init_backup_last_no);
        }
        PreferenceUtils.setSummary(findPreference(getString(R.string.pref_fakekey_startrestore)), textRestore);
    }
}
