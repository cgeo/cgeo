package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.BackupUtils;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

public class PreferenceBackupFragment extends PreferenceFragmentCompat {
    public static final String STATE_BACKUPUTILS = "backuputils";

    private SharedPreferences sharedPrefs;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_backup, rootKey);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        BackupUtils backupUtils = new BackupUtils(getActivity(), savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_BACKUPUTILS));

        final Preference backup = findPreference(getString(R.string.pref_fakekey_preference_backup));
        backup.setOnPreferenceClickListener(preference -> {
            // TODO
            //backupUtils.backup(() -> onPreferenceChange(findPreference(getString(R.string.pref_fakekey_preference_restore))));
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
            if (getBackupLoginData()) {
                loginData.setChecked(false);
                SimpleDialog.of(getActivity()).setTitle(R.string.init_backup_settings_logins).setMessage(R.string.init_backup_settings_backup_full_confirm).confirm((dialog, which) -> loginData.setChecked(true));
            }
            return true;
        });

        // TODO
        //onPreferenceChange(findPreference(getString(R.string.pref_fakekey_preference_restore)));

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

    public boolean getBackupLoginData() {
        return sharedPrefs != null && sharedPrefs.getBoolean(getString(R.string.pref_backup_logins), false);
    }
}
