package cgeo.geocaching;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorageActivityHelper;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

class SplashActivity extends AppCompatActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        // don't call the super implementation with the layout argument, as that would set the wrong theme
        super.onCreate(savedInstanceState);

        final Intent intent;
        final boolean firstInstall = Settings.getLastChangelogChecksum() == 0;
        if (firstInstall || !ContentStorageActivityHelper.baseFolderIsSet()) {
            // new install or base folder missing => run installation wizard
            intent = new Intent(this, InstallWizardActivity.class);
            intent.putExtra(InstallWizardActivity.BUNDLE_MIGRATION, !firstInstall);
        } else {
            // otherwise regular startup
            intent = new Intent(this, MainActivity.class);
            intent.putExtras(getIntent());
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}
