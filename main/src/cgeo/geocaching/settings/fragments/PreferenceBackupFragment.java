package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.BackupUtils;

import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

public class PreferenceBackupFragment extends PreferenceFragmentCompat {
    public static final String STATE_BACKUPUTILS = "backuputils";

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences_backup, rootKey);

        final BackupUtils backupUtils = new BackupUtils(getActivity(), savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_BACKUPUTILS));

        final Preference backup = findPreference(getString(R.string.pref_fakekey_preference_backup));
        backup.setOnPreferenceClickListener(preference -> {
            backupUtils.backup(this::updateSummary);
            return true;
        });

        final Preference restore = findPreference(getString(R.string.pref_fakekey_preference_restore));
        restore.setOnPreferenceClickListener(preference -> {
            backupUtils.restore(BackupUtils.newestBackupFolder());
            return true;
        });

        final Preference restoreFromDir = findPreference(getString(R.string.pref_fakekey_preference_restore_dirselect));
        restoreFromDir.setOnPreferenceClickListener(preference -> {
            backupUtils.selectBackupDirIntent();
            return true;
        });

        final CheckBoxPreference loginData = findPreference(getString(R.string.pref_backup_logins));
        loginData.setOnPreferenceClickListener(preference -> {
            if (loginData.isChecked()) {
                loginData.setChecked(false);
                SimpleDialog.of(getActivity()).setTitle(R.string.init_backup_settings_logins).setMessage(R.string.init_backup_settings_backup_full_confirm).confirm((dialog, which) -> loginData.setChecked(true));
            }
            return true;
        });

        updateSummary();

        final SeekBarPreference keepOld = (SeekBarPreference) findPreference(getString(R.string.pref_backups_backup_history_length));

        keepOld.setOnPreferenceChangeListener((preference, value) -> {
            backupUtils.deleteBackupHistoryDialog((SeekBarPreference) preference, (int) value);
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
        if (BackupUtils.hasBackup(BackupUtils.newestBackupFolder())) {
            textRestore = getString(R.string.init_backup_last) + " " + BackupUtils.getNewestBackupDateTime();
        } else {
            textRestore = getString(R.string.init_backup_last_no);
        }
        findPreference(getString(R.string.pref_fakekey_preference_restore)).setSummary(textRestore);
    }
}
