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

package cgeo.geocaching.backup

import cgeo.geocaching.utils.ApplicationSettings

import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper

class CentralBackupAgent : BackupAgentHelper() {

    private static val PREFS_BACKUP_KEY: String = "prefs"

    override     public Unit onCreate() {
        val helper: SharedPreferencesBackupHelper = SharedPreferencesBackupHelper(this, ApplicationSettings.getPreferencesName())
        addHelper(PREFS_BACKUP_KEY, helper)
    }

}
