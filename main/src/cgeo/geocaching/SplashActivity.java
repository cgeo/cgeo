package cgeo.geocaching;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.TextUtils;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class SplashActivity extends AppCompatActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "SplashActivity.onCreate")) {
            // Handle the splash screen transition
            SplashScreen.installSplashScreen(this);

            // don't call the super implementation with the layout argument, as that would set the wrong theme
            super.onCreate(savedInstanceState);

            final Intent intent;
            final boolean firstInstall = Settings.getLastChangelogChecksum() == 0;
            final boolean folderMigrationNeeded = InstallWizardActivity.needsFolderMigration();
            if (firstInstall || !ContentStorageActivityHelper.baseFolderIsSet() || folderMigrationNeeded) {
                // new install, base folder missing or folder migration needed => run installation wizard
                intent = new Intent(this, InstallWizardActivity.class);
                intent.putExtra(InstallWizardActivity.BUNDLE_MODE, firstInstall ? InstallWizardActivity.WizardMode.WIZARDMODE_DEFAULT.id : InstallWizardActivity.WizardMode.WIZARDMODE_MIGRATION.id);
            } else {
                // otherwise regular startup
                intent = new Intent(this, MainActivity.class);
                intent.putExtras(getIntent());
            }
            cLog.add("fi");

            // reactivate dialogs which are set to show later
            OneTimeDialogs.nextStatus();
            cLog.add("otd");

            startActivity(intent);
            cLog.add("sa");

            checkChangedInstall();
            cLog.add("cci");

            finish();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }

    private void checkChangedInstall() {
        // temporary workaround for #4143
        //TODO: understand and avoid if possible
        try {
            final long lastChecksum = Settings.getLastChangelogChecksum();
            final long checksum = TextUtils.checksum(FileUtils.getChangelogMaster(this) + FileUtils.getChangelogRelease(this));
            Settings.setLastChangelogChecksum(checksum);

            if (lastChecksum == 0) {
                // initialize oneTimeMessages after fresh install
                OneTimeDialogs.initializeOnFreshInstall();
                // initialize useInternalRouting setting depending on whether BRouter app is installed or not
                Settings.setUseInternalRouting(!ProcessUtils.isInstalled(getString(R.string.package_brouter)));
            } else if (lastChecksum != checksum) {
                // show change log page after update
                AboutActivity.showChangeLog(this);
            }
        } catch (final Exception ex) {
            Log.e("Error checking/showing changelog!", ex);
        }
    }
}
