package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.BackupSeekbarPreference;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.BackupUtils;

import android.os.Bundle;

import androidx.preference.CheckBoxPreference;

public class PreferenceBackupFragment extends BasePreferenceFragment {
    public static final String STATE_BACKUPUTILS = "backuputils";

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_backup, rootKey);

        final BackupUtils backupUtils = ((SettingsActivity) getActivity()).getBackupUtils();

        findPreference(getString(R.string.pref_fakekey_preference_startbackup)).setOnPreferenceClickListener(preference -> {
            backupUtils.backup(this::updateSummary, false);
            return true;
        });

        findPreference(getString(R.string.pref_fakekey_startrestore)).setOnPreferenceClickListener(preference -> {
            backupUtils.restore(BackupUtils.newestBackupFolder(false));
            return true;
        });

        findPreference(getString(R.string.pref_fakekey_startrestore_dirselect)).setOnPreferenceClickListener(preference -> {
            backupUtils.selectBackupDirIntent();
            return true;
        });

        final CheckBoxPreference loginData = findPreference(getString(R.string.pref_backup_logins));
        loginData.setOnPreferenceClickListener(preference -> {
            if (loginData.isChecked()) {
                loginData.setChecked(false);
                SimpleDialog.of(getActivity()).setTitle(R.string.init_backup_settings_logins).setMessage(R.string.init_backup_settings_backup_full_confirm).confirm(() -> loginData.setChecked(true));
            }
            return true;
        });

        updateSummary();

        findPreference(getString(R.string.pref_backup_backup_history_length)).setOnPreferenceChangeListener((preference, value) -> {
            backupUtils.deleteBackupHistoryDialog((BackupSeekbarPreference) preference, (int) value, false);
            return true;
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings_title_backup);
    }

    private void updateSummary() {
        final String textRestore;
        if (BackupUtils.hasBackup(BackupUtils.newestBackupFolder(false))) {
            textRestore = getString(R.string.init_backup_last) + " " + BackupUtils.getNewestBackupDateTime(false);
        } else {
            textRestore = getString(R.string.init_backup_last_no);
        }
        findPreference(getString(R.string.pref_fakekey_startrestore)).setSummary(textRestore);
    }
}
