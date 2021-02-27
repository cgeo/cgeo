package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.databinding.InstallWizardBinding;
import cgeo.geocaching.downloader.DownloaderUtils;
import cgeo.geocaching.downloader.MapDownloadSelectorActivity;
import cgeo.geocaching.permission.PermissionGrantedCallback;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.GCAuthorizationActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.BackupUtils;
import cgeo.geocaching.utils.ProcessUtils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class InstallWizardActivity extends AppCompatActivity {

    public static final String BUNDLE_MODE = "wizardmode";
    private static final String BUNDLE_STEP = "step";

    public enum WizardMode {
        WIZARDMODE_DEFAULT(0),
        WIZARDMODE_RETURNING(1),
        WIZARDMODE_MIGRATION(2);

        public int id;

        WizardMode(final int id) {
            this.id = id;
        }
    }

    private enum WizardStep {
        WIZARD_START,
        WIZARD_PERMISSIONS, WIZARD_PERMISSIONS_STORAGE, WIZARD_PERMISSIONS_LOCATION,
        WIZARD_PERMISSIONS_BASEFOLDER, WIZARD_PERMISSIONS_MAPFOLDER, WIZARD_PERMISSIONS_MAPTHEMEFOLDER, WIZARD_PERMISSIONS_GPXFOLDER,
        WIZARD_PLATFORMS,
        WIZARD_ADVANCED,
        WIZARD_END
    }

    private WizardMode mode = WizardMode.WIZARDMODE_DEFAULT;
    private WizardStep step = WizardStep.WIZARD_START;
    private boolean forceSkipButton = false;
    private ContentStorageActivityHelper contentStorageActivityHelper = null;

    private static final int REQUEST_CODE_WIZARD_GC = 0x7167;

    // dialog elements
    private ImageView logo = null;
    private TextView title = null;
    private TextView text = null;

    private TextView button1Info = null;
    private Button button1 = null;
    private TextView button2Info = null;
    private Button button2 = null;
    private TextView button3Info = null;
    private Button button3 = null;

    private Button prev = null;
    private Button skip = null;
    private Button next = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.dark);
        if (savedInstanceState != null) {
            step = WizardStep.values()[savedInstanceState.getInt(BUNDLE_STEP)];
            mode = WizardMode.values()[savedInstanceState.getInt(BUNDLE_MODE)];
        } else {
            mode = WizardMode.values()[getIntent().getIntExtra(BUNDLE_MODE, WizardMode.WIZARDMODE_DEFAULT.id)];
        }
        final InstallWizardBinding binding = InstallWizardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        logo = binding.wizardLogo;
        title = binding.wizardTitle;
        text = binding.wizardText;

        button1Info = binding.wizardButton1Info;
        button1 = binding.wizardButton1;
        button2Info = binding.wizardButton2Info;
        button2 = binding.wizardButton2;
        button3Info = binding.wizardButton3Info;
        button3 = binding.wizardButton3;

        prev = binding.wizardPrev;
        skip = binding.wizardSkip;
        next = binding.wizardNext;

        updateDialog();
    }

    private void updateDialog() {
        logo.setImageResource(R.mipmap.ic_launcher);
        text.setVisibility(View.VISIBLE);
        setButton(button1, 0, null, button1Info, 0);
        setButton(button2, 0, null, button2Info, 0);
        setButton(button3, 0, null, button3Info, 0);
        switch (step) {
            case WIZARD_START: {
                title.setText(mode == WizardMode.WIZARDMODE_MIGRATION ? R.string.wizard_migration_title : R.string.wizard_welcome_title);
                text.setText(mode == WizardMode.WIZARDMODE_RETURNING ? R.string.wizard_intro_returning : mode == WizardMode.WIZARDMODE_MIGRATION ? R.string.wizard_intro_migration : R.string.wizard_intro);
                setNavigation(this::skipWizard, R.string.wizard_not_now, null, 0, this::gotoNext, 0);
                break;
            }
            case WIZARD_PERMISSIONS: {
                title.setText(R.string.wizard_permissions_title);
                text.setText(R.string.wizard_permissions_intro);
                setNavigation(this::gotoPrevious, 0, null, 0, this::gotoNext, 0);
                break;
            }
            case WIZARD_PERMISSIONS_STORAGE:
                title.setText(R.string.wizard_permissions_title);
                text.setText(R.string.storage_permission_request_explanation);
                setNavigation(this::gotoPrevious, 0, null, 0, this::requestStorage, 0);
                break;
            case WIZARD_PERMISSIONS_LOCATION:
                title.setText(R.string.wizard_permissions_title);
                text.setText(R.string.location_permission_request_explanation);
                setNavigation(this::gotoPrevious, 0, null, 0, this::requestLocation, 0);
                break;
            case WIZARD_PERMISSIONS_BASEFOLDER:
                setFolderTitle(PersistableFolder.BASE);
                text.setText(R.string.wizard_basefolder_request_explanation);
                setNavigation(this::gotoPrevious, 0, forceSkipButton ? this::gotoNext : null, 0, this::requestBasefolder, 0);
                break;
            case WIZARD_PERMISSIONS_MAPFOLDER:
                setFolderTitle(PersistableFolder.OFFLINE_MAPS);
                text.setText(R.string.wizard_mapfolder_request_explanation);
                setNavigation(this::gotoPrevious, 0, forceSkipButton ? this::gotoNext : null, 0, this::requestMapfolder, 0);
                break;
            case WIZARD_PERMISSIONS_MAPTHEMEFOLDER:
                setFolderTitle(PersistableFolder.OFFLINE_MAP_THEMES);
                text.setText(R.string.wizard_mapthemesfolder_request_explanation);
                setNavigation(this::gotoPrevious, 0, forceSkipButton ? this::gotoNext : null, 0, this::requestMapthemefolder, 0);
                break;
            case WIZARD_PERMISSIONS_GPXFOLDER:
                setFolderTitle(PersistableFolder.GPX);
                text.setText(R.string.wizard_gpxfolder_request_explanation);
                setNavigation(this::gotoPrevious, 0, forceSkipButton ? this::gotoNext : null, 0, this::requestGpxfolder, 0);
                break;
            case WIZARD_PLATFORMS:
                title.setText(R.string.wizard_platforms_title);
                text.setText(R.string.wizard_platforms_intro);
                setNavigation(this::gotoPrevious, 0, null, 0, this::gotoNext, R.string.skip);
                setButton(button1, R.string.wizard_platforms_gc, v -> {
                    setButtonToDone();
                    authorizeGC();
                }, button1Info, 0);
                setButton(button2, R.string.wizard_platforms_others, v -> {
                    setButtonToDone();
                    SettingsActivity.openForScreen(R.string.preference_screen_services, this);
                }, button2Info, 0);
                break;
            case WIZARD_ADVANCED:
                title.setText(R.string.wizard_welcome_advanced);
                text.setVisibility(View.GONE);
                setNavigation(this::gotoPrevious, 0, null, 0, this::gotoNext, R.string.skip);
                setButton(button1, R.string.wizard_advanced_offlinemaps_label, v -> {
                    setButtonToDone();
                    startActivityForResult(new Intent(this, MapDownloadSelectorActivity.class), DownloaderUtils.REQUEST_CODE);
                }, button1Info, R.string.wizard_advanced_offlinemaps_info);
                setButton(button2, R.string.wizard_advanced_brouter_label, v -> {
                    setButtonToDone();
                    ProcessUtils.openMarket(this, getString(R.string.package_brouter));
                }, button2Info, R.string.wizard_advanced_brouter_info);
                setButton(button3, R.string.wizard_advanced_restore_label, v -> {
                    setButtonToDone();
                    DataStore.resetNewlyCreatedDatabase();
                    final BackupUtils backupUtils = new BackupUtils(this);
                    if (BackupUtils.hasBackup(BackupUtils.newestBackupFolder())) {
                        backupUtils.restore(BackupUtils.newestBackupFolder(), getContentStorageHelper());
                    } else {
                        backupUtils.selectBackupDirIntent(getContentStorageHelper());
                    }
                }, button3Info, R.string.wizard_advanced_restore_info);
                break;
            case WIZARD_END: {
                title.setText(R.string.wizard_welcome_title);
                final StringBuilder info = new StringBuilder();
                info.append(getString(R.string.wizard_status_title)).append(":\n")
                    .append(getString(R.string.wizard_status_storage_permission)).append(": ").append(hasStoragePermission(this) ? getString(android.R.string.ok) : getString(R.string.status_not_ok)).append("\n")
                    .append(getString(R.string.wizard_status_location_permission)).append(": ").append(hasLocationPermission(this) ? getString(android.R.string.ok) : getString(R.string.status_not_ok)).append("\n")
                    .append(getString(R.string.wizard_status_basefolder)).append(": ").append(ContentStorageActivityHelper.baseFolderIsSet() ? getString(android.R.string.ok) : getString(R.string.status_not_ok)).append("\n")
                    .append(getString(R.string.wizard_status_platform));
                boolean platformConfigured = false;
                final StringBuilder platforms = new StringBuilder();
                for (final IConnector conn : ConnectorFactory.getActiveConnectorsWithValidCredentials()) {
                    if (platformConfigured) {
                        platforms.append(", ");
                    }
                    platforms.append(conn.getName());
                    platformConfigured = true;
                }
                if (platformConfigured) {
                    info.append(": ").append(getString(android.R.string.ok)).append("\n(").append(platforms).append(")\n");
                } else {
                    info.append(": ").append(getString(R.string.status_not_ok)).append("\n");
                }
                button1Info.setVisibility(View.VISIBLE);
                button1Info.setText(info);

                text.setText(isConfigurationOk(this) ? R.string.wizard_outro_ok : R.string.wizard_outro_error);

                setNavigation(this::gotoPrevious, 0, null, 0, this::finishWizard, R.string.finish);
                break;
            }
            default: {
                // never should happen!
                step = WizardStep.WIZARD_START;
                updateDialog();
                break;
            }
        }
    }

    private void setButtonToDone() {
        next.setText(R.string.done);
    }

    private void setNavigation(@Nullable final Runnable listenerPrev, final int prevLabelRes, @Nullable final Runnable listenerSkip, final int skipLabelRes, @Nullable final Runnable listenerNext, final int nextLabelRes) {
        if (listenerPrev == null) {
            prev.setVisibility(View.GONE);
        } else {
            prev.setVisibility(View.VISIBLE);
            prev.setText(prevLabelRes == 0 ? R.string.previous : prevLabelRes);
            prev.setOnClickListener(v -> listenerPrev.run());
        }
        if (listenerSkip == null) {
            skip.setVisibility(View.GONE);
        } else {
            skip.setVisibility(View.VISIBLE);
            skip.setText(skipLabelRes == 0 ? R.string.skip : skipLabelRes);
            skip.setOnClickListener(v -> listenerSkip.run());
        }
        if (listenerNext == null) {
            next.setVisibility(View.GONE);
        } else {
            next.setVisibility(View.VISIBLE);
            next.setText(nextLabelRes == 0 ? R.string.next : nextLabelRes);
            next.setOnClickListener(v -> listenerNext.run());
        }
    }

    private void setButton(final Button button, final int labelResId, @Nullable final View.OnClickListener listener, final TextView buttonInfo, final int infoResId) {
        if (button != null) {
            if (listener == null) {
                button.setVisibility(View.GONE);
            } else {
                button.setVisibility(View.VISIBLE);
                button.setText(labelResId);
                button.setOnClickListener(listener);
            }
        }
        if (buttonInfo != null) {
            if (infoResId == 0) {
                buttonInfo.setVisibility(View.GONE);
            } else {
                buttonInfo.setVisibility(View.VISIBLE);
                buttonInfo.setText(infoResId);
            }
        }
    }

    private void gotoPrevious() {
        if (step.ordinal() > 0) {
            step = WizardStep.values()[step.ordinal() - 1];
            if (stepCanBeSkipped()) {
                gotoPrevious();
            } else {
                updateDialog();
            }
        }
    }

    private void gotoNext() {
        final int max = WizardStep.values().length - 1;
        if (step.ordinal() < max) {
            step = WizardStep.values()[step.ordinal() + 1];
            if (stepCanBeSkipped()) {
                gotoNext();
            } else {
                updateDialog();
            }
        }
    }

    private boolean stepCanBeSkipped() {
        return (step == WizardStep.WIZARD_PERMISSIONS && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (hasStoragePermission(this) && hasLocationPermission(this))))
            || (step == WizardStep.WIZARD_PERMISSIONS_STORAGE && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasStoragePermission(this)))
            || (step == WizardStep.WIZARD_PERMISSIONS_LOCATION && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasLocationPermission(this)))
            || (step == WizardStep.WIZARD_PERMISSIONS_BASEFOLDER && ContentStorageActivityHelper.baseFolderIsSet())
            || (step == WizardStep.WIZARD_PERMISSIONS_MAPFOLDER && !mapFolderNeedsMigration())
            || (step == WizardStep.WIZARD_PERMISSIONS_MAPTHEMEFOLDER && !mapThemeFolderNeedsMigration())
            || (step == WizardStep.WIZARD_PERMISSIONS_GPXFOLDER && !gpxFolderNeedsMigration())
            || (step == WizardStep.WIZARD_PLATFORMS && mode == WizardMode.WIZARDMODE_MIGRATION)
            || (step == WizardStep.WIZARD_ADVANCED && mode == WizardMode.WIZARDMODE_MIGRATION)
            ;
    }

    private void skipWizard() {
        Dialogs.confirmPositiveNegativeNeutral(this, getString(R.string.wizard), getString(R.string.wizard_skip_wizard_warning), getString(android.R.string.ok), getString(R.string.back), "", (dialog, which) -> finishWizard(), (dialog, which) -> updateDialog(), null);
    }

    private void finishWizard() {
        // call MainActivity (if not returning) and finish this Activity
        if (mode != WizardMode.WIZARDMODE_RETURNING) {
            final Intent main = new Intent(this, MainActivity.class);
            main.putExtras(getIntent());
            startActivity(main);
        }
        finish();
    }

    public static boolean isConfigurationOk(final Context context) {
        final boolean isPlatformConfigured = ConnectorFactory.getActiveConnectorsWithValidCredentials().length > 0;
        return hasStoragePermission(context) && hasLocationPermission(context) && isPlatformConfigured && ContentStorageActivityHelper.baseFolderIsSet();
    }

    public static boolean needsFolderMigration() {
        return mapFolderNeedsMigration() || mapThemeFolderNeedsMigration() || gpxFolderNeedsMigration();
    }

    // -------------------------------------------------------------------
    // old Android permissions related methods

    private static boolean hasStoragePermission(final Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasLocationPermission(final Context context) {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            || (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestStorage() {
        if (!hasStoragePermission(this)) {
            LocalStorage.resetExternalPublicCgeoDirectory(); // workaround for permission handler callback not being called, see #9850, needs to be investigated further
            PermissionHandler.requestStoragePermission(this, new PermissionGrantedCallback(PermissionRequestContext.InstallWizardActivity) {
                @Override
                protected void execute() {
                    LocalStorage.resetExternalPublicCgeoDirectory();
                }
            });
        }
    }

    private void requestLocation() {
        if (!hasLocationPermission(this)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PermissionRequestContext.InstallWizardActivity.getRequestCode());
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        gotoNext();
    }

    // -------------------------------------------------------------------
    // Android SAF-based permissions related methods

    private void setFolderTitle(final PersistableFolder folder) {
        title.setText(String.format(getString(R.string.wizard_permissions_folder_title), getString(folder.getNameKeyId())));
    }

    private void requestBasefolder() {
        forceSkipButton = false;
        if (!ContentStorageActivityHelper.baseFolderIsSet()) {
            prepareFolderDefaultValues();
            getContentStorageHelper().migratePersistableFolder(PersistableFolder.BASE, folder -> onReturnFromFolderMigration(ContentStorageActivityHelper.baseFolderIsSet()));
        }
    }

    private ContentStorageActivityHelper getContentStorageHelper() {
        if (contentStorageActivityHelper == null) {
            contentStorageActivityHelper = new ContentStorageActivityHelper(this);
        }
        return contentStorageActivityHelper;
    }

    private void onReturnFromFolderMigration(final boolean resultOk) {
        if (resultOk) {
            gotoNext();
        } else {
            forceSkipButton = true;
            updateDialog();
        }
    }

    private static boolean mapFolderNeedsMigration() {
        return Settings.legacyFolderNeedsToBeMigrated(R.string.pref_persistablefolder_offlinemaps);
    }

    private void requestMapfolder() {
        forceSkipButton = false;
        if (mapFolderNeedsMigration()) {
            prepareFolderDefaultValues();
            getContentStorageHelper().migratePersistableFolder(PersistableFolder.OFFLINE_MAPS, v -> onReturnFromFolderMigration(!mapFolderNeedsMigration()));
        }
    }

    private static boolean mapThemeFolderNeedsMigration() {
        return Settings.legacyFolderNeedsToBeMigrated(R.string.pref_persistablefolder_offlinemapthemes);
    }

    private void requestMapthemefolder() {
        forceSkipButton = false;
        if (mapThemeFolderNeedsMigration()) {
            prepareFolderDefaultValues();
            getContentStorageHelper().migratePersistableFolder(PersistableFolder.OFFLINE_MAP_THEMES, v -> onReturnFromFolderMigration(!mapThemeFolderNeedsMigration()));
        }
    }

    private static boolean gpxFolderNeedsMigration() {
        return Settings.legacyFolderNeedsToBeMigrated(R.string.pref_persistablefolder_gpx);
    }

    private void requestGpxfolder() {
        forceSkipButton = false;
        if (gpxFolderNeedsMigration()) {
            prepareFolderDefaultValues();
            getContentStorageHelper().migratePersistableFolder(PersistableFolder.GPX, v -> onReturnFromFolderMigration(!gpxFolderNeedsMigration()));
        }
    }

    private void prepareFolderDefaultValues() {
        // re-evaluate default folder values, as the public folder may not have been accessible on startup
        ContentStorage.get().reevaluateFolderDefaults();
    }

    // -------------------------------------------------------------------
    // services settings

    private boolean hasValidGCCredentials() {
        return Settings.getCredentials(GCConnector.getInstance()).isValid();
    }

    private void authorizeGC() {
        final Intent checkIntent = new Intent(this, GCAuthorizationActivity.class);
        final Credentials credentials = GCConnector.getInstance().getCredentials();
        checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_USERNAME, credentials.getUsernameRaw());
        checkIntent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_PASSWORD, credentials.getPasswordRaw());
        startActivityForResult(checkIntent, REQUEST_CODE_WIZARD_GC);
    }

    // -------------------------------------------------------------------

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(BUNDLE_MODE, mode.id);
        savedInstanceState.putInt(BUNDLE_STEP, step.ordinal());
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_WIZARD_GC) {
            if (!hasValidGCCredentials()) {
                Toast.makeText(this, R.string.err_auth_process, Toast.LENGTH_SHORT).show();
            } else {
                Dialogs.confirm(this, R.string.settings_title_gc, R.string.settings_gc_legal_note, android.R.string.ok, (dialog, which) -> {
                    Settings.setGCConnectorActive(true);
                    gotoNext();
                }, dialog -> { });
            }
        } else if (contentStorageActivityHelper == null || !contentStorageActivityHelper.onActivityResult(requestCode, resultCode, data)) {
            DownloaderUtils.onActivityResult(this, requestCode, resultCode, data);
        }
    }
}
