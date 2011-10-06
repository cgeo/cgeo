package cgeo.geocaching.backup;

import cgeo.geocaching.Settings;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class CentralBackupAgent extends BackupAgentHelper {

    static final String PREFS_BACKUP_KEY = "prefs";

    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, Settings.preferences);
        addHelper(PREFS_BACKUP_KEY, helper);
    }

}
