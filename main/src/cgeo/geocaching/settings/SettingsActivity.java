package cgeo.geocaching.settings;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import cgeo.geocaching.R;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;
import cgeo.geocaching.network.AndroidBeam;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.ApplicationSettings;
import cgeo.geocaching.utils.BackupUtils;
import cgeo.geocaching.utils.Log;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html"> Android Design: Settings</a> for design
 * guidelines and the <a href="http://developer.android.com/guide/topics/ui/settings.html">Settings API Guide</a> for
 * more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String INTENT_OPEN_SCREEN = "OPEN_SCREEN";
    public static final int NO_RESTART_NEEDED = 1;
    public static final int RESTART_NEEDED = 2;

    public static final String STATE_CSAH = "csah";
    public static final String STATE_BACKUPUTILS = "backuputils";

    private BackupUtils backupUtils = null;

    private ContentStorageActivityHelper contentStorageHelper = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ApplicationSettings.setLocale(this);
        setTheme(Settings.isLightSkin(this) ? R.style.settings_light : R.style.settings);
        super.onCreate(savedInstanceState);

        backupUtils = new BackupUtils(SettingsActivity.this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_BACKUPUTILS));

        this.contentStorageHelper = new ContentStorageActivityHelper(this, savedInstanceState == null ? null : savedInstanceState.getBundle(STATE_CSAH))
            .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FOLDER_PERSISTED, PersistableFolder.class, folder -> {
                //getPreference(folder.getPrefKeyId()).setSummary(folder.toUserDisplayableValue());
                if (PersistableFolder.OFFLINE_MAP_THEMES.equals(folder)) {
                    RenderThemeHelper.resynchronizeOrDeleteMapThemeFolder();
                }
            });

        setContentView(R.layout.layout_settings);

        final Intent intent = getIntent();
        openInitialScreen(intent.getIntExtra(INTENT_OPEN_SCREEN, 0));
        AndroidBeam.disable(this);

        setResult(NO_RESTART_NEEDED);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBundle(STATE_CSAH, contentStorageHelper.getState());
        savedInstanceState.putBundle(STATE_BACKUPUTILS, backupUtils.getState());
    }

    private void openInitialScreen(final int initialScreen) {
        // TODO: Reimplement for Fragment based logic
    }

    @Override
    protected void onPause() {
        Log.i("Requesting settings backup with settings manager");
        BackupManager.dataChanged(getPackageName());
        super.onPause();
    }

    public static void openForScreen(final int preferenceScreenKey, final Context fromActivity) {
        final Intent intent = new Intent(fromActivity, SettingsActivity.class);
        intent.putExtra(INTENT_OPEN_SCREEN, preferenceScreenKey);
        fromActivity.startActivity(intent);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (contentStorageHelper.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        if (backupUtils.onActivityResult(requestCode, resultCode, data)) {
            return;
        }

        if (DownloaderUtils.onActivityResult(this, requestCode, resultCode, data)) {
            return;
        }

        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            // TODO: Reimplement for Fragment based logic
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean onPreferenceStartFragment(final PreferenceFragmentCompat caller, final androidx.preference.Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
            getClassLoader(),
            pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.settings_fragment_root, fragment)
            .addToBackStack(null)
            .commit();
        return true;
    }
}
