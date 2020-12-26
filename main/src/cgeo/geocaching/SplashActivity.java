package cgeo.geocaching;

import cgeo.geocaching.settings.Settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

class SplashActivity extends AppCompatActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        // don't call the super implementation with the layout argument, as that would set the wrong theme
        super.onCreate(savedInstanceState);

        final Intent intent;
        if (Settings.getLastChangelogChecksum() == 0) {
            // new install => run installation wizard
            intent = new Intent(this, InstallWizardActivity.class);
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
