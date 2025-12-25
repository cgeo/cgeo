// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.settings.fragments

import cgeo.geocaching.R
import cgeo.geocaching.settings.BackupSeekbarPreference
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.BackupUtils
import cgeo.geocaching.utils.PreferenceUtils

import android.os.Bundle

import androidx.preference.CheckBoxPreference

class PreferenceBackupFragment : BasePreferenceFragment() {
    public static val STATE_BACKUPUTILS: String = "backuputils"

    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        initPreferences(R.xml.preferences_backup, rootKey)

        val backupUtils: BackupUtils = ((SettingsActivity) getActivity()).getBackupUtils()

        PreferenceUtils.setOnPreferenceClickListener(findPreference(getString(R.string.pref_fakekey_preference_startbackup)), preference -> {
            backupUtils.backup(this::updateSummary, false)
            return true
        })

        PreferenceUtils.setOnPreferenceClickListener(findPreference(getString(R.string.pref_fakekey_startrestore)), preference -> {
            backupUtils.restore(BackupUtils.newestBackupFolder(false))
            return true
        })

        PreferenceUtils.setOnPreferenceClickListener(findPreference(getString(R.string.pref_fakekey_startrestore_dirselect)), preference -> {
            backupUtils.selectBackupDirIntent()
            return true
        })

        val loginData: CheckBoxPreference = findPreference(getString(R.string.pref_backup_logins))
        PreferenceUtils.setOnPreferenceClickListener(loginData, preference -> {
            if (loginData.isChecked()) {
                loginData.setChecked(false)
                SimpleDialog.of(getActivity()).setTitle(R.string.init_backup_settings_logins).setMessage(R.string.init_backup_settings_backup_full_confirm).confirm(() -> loginData.setChecked(true))
            }
            return true
        })

        updateSummary()

        PreferenceUtils.setOnPreferenceChangeListener(findPreference(getString(R.string.pref_backup_backup_history_length)), (preference, value) -> {
            backupUtils.deleteBackupHistoryDialog((BackupSeekbarPreference) preference, (Int) value, false)
            return true
        })

    }

    override     public Unit onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.settings_title_backup)
    }

    private Unit updateSummary() {
        final String textRestore
        if (BackupUtils.hasBackup(BackupUtils.newestBackupFolder(false))) {
            textRestore = getString(R.string.init_backup_last) + " " + BackupUtils.getNewestBackupDateTime(false)
        } else {
            textRestore = getString(R.string.init_backup_last_no)
        }
        PreferenceUtils.setSummary(findPreference(getString(R.string.pref_fakekey_startrestore)), textRestore)
    }
}
