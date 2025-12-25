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

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorageActivityHelper
import cgeo.geocaching.storage.extension.OneTimeDialogs
import cgeo.geocaching.utils.ContextLogger
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.ProcessUtils
import cgeo.geocaching.utils.TextUtils

import android.content.Intent
import android.os.Bundle

class SplashActivity : AbstractActivity() {

    override     public Unit onCreate(final Bundle savedInstanceState) {
        try (ContextLogger cLog = ContextLogger(Log.LogLevel.DEBUG, "SplashActivity.onCreate")) {
            // don't call the super implementation with the layout argument, as that would set the wrong theme
            super.onCreate(savedInstanceState)

            final Intent intent
            val firstInstall: Boolean = Settings.getLastChangelogChecksum() == 0
            val folderMigrationNeeded: Boolean = InstallWizardActivity.needsFolderMigration()
            if (firstInstall || !ContentStorageActivityHelper.baseFolderIsSet() || folderMigrationNeeded) {
                // install, base folder missing or folder migration needed => run installation wizard
                intent = Intent(this, InstallWizardActivity.class)
                intent.putExtra(InstallWizardActivity.BUNDLE_MODE, firstInstall ? InstallWizardActivity.WizardMode.WIZARDMODE_DEFAULT.id : InstallWizardActivity.WizardMode.WIZARDMODE_MIGRATION.id)
            } else {
                // otherwise regular startup
                intent = Settings.getStartscreenIntent(this)
                intent.putExtras(getIntent())
            }
            cLog.add("fi")

            // reactivate dialogs which are set to show later
            OneTimeDialogs.nextStatus()
            cLog.add("otd")

            startActivity(intent)
            cLog.add("sa")

            checkChangedInstall()
            cLog.add("cci")

            finish()
        }
    }

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data)
        finish()
    }

    private Unit checkChangedInstall() {
        // temporary workaround for #4143
        //TODO: understand and avoid if possible
        try {
            val lastChecksum: Long = Settings.getLastChangelogChecksum()
            val checksum: Long = TextUtils.checksum(FileUtils.getChangelogMaster(this) + FileUtils.getChangelogRelease(this))
            Settings.setLastChangelogChecksum(checksum)

            if (lastChecksum == 0) {
                // initialize oneTimeMessages after fresh install
                OneTimeDialogs.initializeOnFreshInstall()
                // initialize useInternalRouting setting depending on whether BRouter app is installed or not
                Settings.setUseInternalRouting(!ProcessUtils.isInstalled(getString(R.string.package_brouter)))
            } else if (lastChecksum != checksum) {
                // show change log page after update
                AboutActivity.showChangeLog(this)
            }
        } catch (final Exception ex) {
            Log.e("Error checking/showing changelog!", ex)
        }
    }
}
