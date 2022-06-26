package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.databinding.InstallWizardBinding;
import cgeo.geocaching.downloader.DownloadSelectorActivity;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.permission.PermissionGrantedCallback;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.CredentialsAuthorizationContract;
import cgeo.geocaching.settings.GCAuthorizationActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.BackupUtils;
import cgeo.geocaching.wizard.WizardMode;
import cgeo.geocaching.wizard.WizardStep;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class InstallWizardActivity extends AppCompatActivity {

    public static final String BUNDLE_MODE = "wizardmode";
    private static final String BUNDLE_STEP = "step";
    private static final String BUNDLE_CSAH = "csah";
    private static final String BUNDLE_BACKUPUTILS = "backuputils";

    private WizardMode mode = WizardMode.WIZARDMODE_DEFAULT;
    private WizardStep step = WizardStep.WIZARD_START;
    private boolean forceSkipButton = false;
    private ContentStorageActivityHelper contentStorageActivityHelper = null;
    private BackupUtils backupUtils;

    private final ActivityResultLauncher<CredentialsAuthorizationContract.Input> authorize =
        registerForActivityResult(new CredentialsAuthorizationContract(), result -> onAuthorizationResult());

    private InstallWizardBinding binding;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // window without actionbar for a cleaner look
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(R.style.NoActionbarTheme);

        backupUtils = new BackupUtils(this, savedInstanceState == null ? null : savedInstanceState.getBundle(BUNDLE_BACKUPUTILS));
        if (savedInstanceState != null) {
            step = WizardStep.values()[savedInstanceState.getInt(BUNDLE_STEP)];
            mode = WizardMode.values()[savedInstanceState.getInt(BUNDLE_MODE)];
        } else {
            mode = WizardMode.values()[getIntent().getIntExtra(BUNDLE_MODE, WizardMode.WIZARDMODE_DEFAULT.id)];
        }
        binding = InstallWizardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        this.contentStorageActivityHelper = new ContentStorageActivityHelper(this, savedInstanceState == null ? null : savedInstanceState.getBundle(BUNDLE_CSAH))
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FOLDER_PERSISTED, PersistableFolder.class, pf -> {
                    final boolean needsMigration;
                    switch (pf) {
                        case GPX:
                            needsMigration = gpxFolderNeedsMigration();
                            break;
                        case BASE:
                            needsMigration = !ContentStorageActivityHelper.baseFolderIsSet();
                            break;
                        case OFFLINE_MAPS:
                            needsMigration = mapFolderNeedsMigration();
                            break;
                        case OFFLINE_MAP_THEMES:
                            needsMigration = mapThemeFolderNeedsMigration();
                            break;
                        case ROUTING_TILES:
                            needsMigration = broutertilesFolderNeedsMigration();
                            break;
                        default:
                            needsMigration = false;
                            break;
                    }
                    onReturnFromFolderMigration(!needsMigration);
                });

        updateDialog();
    }

    private void updateDialog() {
        binding.wizardLogo.setImageResource(R.mipmap.ic_launcher);
        binding.wizardText.setVisibility(View.VISIBLE);
        setButton(binding.wizardButton1, 0, null, binding.wizardButton1Info, 0);
        setButton(binding.wizardButton2, 0, null, binding.wizardButton2Info, 0);
        setButton(binding.wizardButton3, 0, null, binding.wizardButton3Info, 0);
        switch (step) {
            case WIZARD_START: {
                binding.wizardTitle.setText(mode == WizardMode.WIZARDMODE_MIGRATION ? R.string.wizard_migration_title : R.string.wizard_welcome_title);
                binding.wizardText.setText(mode == WizardMode.WIZARDMODE_RETURNING ? R.string.wizard_intro_returning : mode == WizardMode.WIZARDMODE_MIGRATION ? R.string.wizard_intro_migration : R.string.wizard_intro);
                setNavigation(this::skipWizard, R.string.wizard_not_now, null, 0, this::gotoNext, 0);
                break;
            }
            case WIZARD_PERMISSIONS: {
                binding.wizardTitle.setText(R.string.wizard_permissions_title);
                binding.wizardText.setText(R.string.wizard_permissions_intro);
                setNavigation(this::gotoPrevious, 0, null, 0, this::gotoNext, 0);
                break;
            }
            case WIZARD_PERMISSIONS_STORAGE:
                binding.wizardTitle.setText(R.string.wizard_status_storage_permission);
                binding.wizardText.setText(R.string.storage_permission_request_explanation);
                setNavigation(this::gotoPrevious, 0, null, 0, this::requestStorage, 0);
                break;
            case WIZARD_PERMISSIONS_LOCATION:
                binding.wizardTitle.setText(R.string.wizard_status_location_permission);
                binding.wizardText.setText(R.string.location_permission_request_explanation);
                setNavigation(this::gotoPrevious, 0, null, 0, this::requestLocation, 0);
                break;
            case WIZARD_PERMISSIONS_BASEFOLDER:
                setFolderInfo(PersistableFolder.BASE, R.string.wizard_basefolder_request_explanation, false);
                setNavigation(this::gotoPrevious, 0, forceSkipButton ? this::gotoNext : null, 0, this::requestBasefolder, 0);
                break;
            case WIZARD_PERMISSIONS_MAPFOLDER:
                setFolderInfo(PersistableFolder.OFFLINE_MAPS, R.string.wizard_mapfolder_request_explanation, true);
                setNavigation(this::gotoPrevious, 0, forceSkipButton ? this::gotoNext : null, 0, this::requestMapfolder, 0);
                break;
            case WIZARD_PERMISSIONS_MAPTHEMEFOLDER:
                setFolderInfo(PersistableFolder.OFFLINE_MAP_THEMES, R.string.wizard_mapthemesfolder_request_explanation, true);
                setNavigation(this::gotoPrevious, 0, forceSkipButton ? this::gotoNext : null, 0, this::requestMapthemefolder, 0);
                break;
            case WIZARD_PERMISSIONS_GPXFOLDER:
                setFolderInfo(PersistableFolder.GPX, R.string.wizard_gpxfolder_request_explanation, true);
                setNavigation(this::gotoPrevious, 0, forceSkipButton ? this::gotoNext : null, 0, this::requestGpxfolder, 0);
                break;
            case WIZARD_PERMISSIONS_BROUTERTILESFOLDER:
                setFolderInfo(PersistableFolder.ROUTING_TILES, R.string.wizard_broutertilesfolder_request_explanation, true);
                setNavigation(this::gotoPrevious, 0, forceSkipButton ? this::gotoNext : null, 0, this::requestBroutertilesfolder, 0);
                break;
            case WIZARD_PLATFORMS:
                binding.wizardTitle.setText(R.string.wizard_platforms_title);
                binding.wizardText.setText(R.string.wizard_platforms_intro);
                setNavigation(this::gotoPrevious, 0, null, 0, this::gotoNext, R.string.skip);
                setButton(binding.wizardButton1, R.string.wizard_platforms_gc, v -> {
                    setButtonToDone();
                    authorizeGC();
                }, binding.wizardButton1Info, 0);
                setButton(binding.wizardButton2, R.string.wizard_platforms_others, v -> {
                    setButtonToDone();
                    SettingsActivity.openForScreen(R.string.preference_screen_services, this);
                }, binding.wizardButton2Info, 0);
                break;
            case WIZARD_ADVANCED:
                binding.wizardTitle.setText(R.string.wizard_welcome_advanced);
                binding.wizardText.setVisibility(View.GONE);
                setNavigation(this::gotoPrevious, 0, null, 0, this::gotoNext, R.string.skip);
                setButton(binding.wizardButton1, R.string.wizard_advanced_offlinemaps_label, v -> {
                    setButtonToDone();
                    startActivity(new Intent(this, DownloadSelectorActivity.class));
                }, binding.wizardButton1Info, R.string.wizard_advanced_offlinemaps_info);
                if (!Routing.isAvailable()) {
                    setButton(binding.wizardButton2, R.string.wizard_advanced_routing_label, v -> {
                        setButtonToDone();
                        Settings.setUseInternalRouting(true);
                        Settings.setBrouterAutoTileDownloads(true);
                        setButton(binding.wizardButton2, 0, null, binding.wizardButton2Info, 0);
                    }, binding.wizardButton2Info, R.string.wizard_advanced_routing_info);
                }
                setButton(binding.wizardButton3, R.string.wizard_advanced_restore_label, v -> {
                    setButtonToDone();
                    DataStore.resetNewlyCreatedDatabase();
                    if (BackupUtils.hasBackup(BackupUtils.newestBackupFolder())) {
                        backupUtils.restore(BackupUtils.newestBackupFolder());
                    } else {
                        backupUtils.selectBackupDirIntent();
                    }
                }, binding.wizardButton3Info, R.string.wizard_advanced_restore_info);
                break;
            case WIZARD_END: {
                binding.wizardTitle.setText(R.string.wizard_welcome_title);
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
                binding.wizardButton1Info.setVisibility(View.VISIBLE);
                binding.wizardButton1Info.setText(info);

                binding.wizardText.setText(isConfigurationOk(this) ? R.string.wizard_outro_ok : R.string.wizard_outro_error);

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
        binding.wizardNext.setText(R.string.done);
    }

    private void setNavigation(@Nullable final Runnable listenerPrev, final int prevLabelRes, @Nullable final Runnable listenerSkip, final int skipLabelRes, @Nullable final Runnable listenerNext, final int nextLabelRes) {
        if (listenerPrev == null) {
            binding.wizardPrev.setVisibility(View.GONE);
        } else {
            binding.wizardPrev.setVisibility(View.VISIBLE);
            binding.wizardPrev.setText(prevLabelRes == 0 ? R.string.previous : prevLabelRes);
            binding.wizardPrev.setOnClickListener(v -> listenerPrev.run());
        }
        if (listenerSkip == null) {
            binding.wizardSkip.setVisibility(View.GONE);
        } else {
            binding.wizardSkip.setVisibility(View.VISIBLE);
            binding.wizardSkip.setText(skipLabelRes == 0 ? R.string.skip : skipLabelRes);
            binding.wizardSkip.setOnClickListener(v -> listenerSkip.run());
        }

        final boolean useNextOutlinedButton = nextLabelRes == R.string.skip;
        if (listenerNext == null) {
            binding.wizardNext.setVisibility(View.GONE);
            binding.wizardNextOutlined.setVisibility(View.GONE);
        } else {
            binding.wizardNext.setVisibility(useNextOutlinedButton ? View.GONE : View.VISIBLE);
            binding.wizardNextOutlined.setVisibility(useNextOutlinedButton ? View.VISIBLE : View.GONE);
            if (useNextOutlinedButton) {
                binding.wizardNextOutlined.setText(nextLabelRes);
                binding.wizardNextOutlined.setOnClickListener(v -> listenerNext.run());
            } else {
                binding.wizardNext.setText(nextLabelRes == 0 ? R.string.next : nextLabelRes);
                binding.wizardNext.setOnClickListener(v -> listenerNext.run());
            }
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
                || (step == WizardStep.WIZARD_PERMISSIONS_BROUTERTILESFOLDER && !broutertilesFolderNeedsMigration())
                || (step == WizardStep.WIZARD_PLATFORMS && mode == WizardMode.WIZARDMODE_MIGRATION)
                || (step == WizardStep.WIZARD_ADVANCED && mode == WizardMode.WIZARDMODE_MIGRATION)
                ;
    }

    private void skipWizard() {
        SimpleDialog.of(this).setTitle(R.string.wizard).setMessage(R.string.wizard_skip_wizard_warning).setButtons(0, R.string.back).confirm((dialog, which) -> finishWizard(), (dialog, which) -> updateDialog());
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
        return mapFolderNeedsMigration() || mapThemeFolderNeedsMigration() || gpxFolderNeedsMigration() || broutertilesFolderNeedsMigration();
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        gotoNext();
    }

    // -------------------------------------------------------------------
    // Android SAF-based permissions related methods

    private void setFolderInfo(final PersistableFolder folder, @StringRes final int info, final boolean addSelectOrCreateInfo) {
        binding.wizardTitle.setText(String.format(getString(R.string.wizard_permissions_folder_title), getString(folder.getNameKeyId())));
        final String temp = getString(info) + (addSelectOrCreateInfo ? " " + getString(R.string.wizard_select_or_create) : "");
        binding.wizardText.setText(temp);
    }

    private void requestBasefolder() {
        forceSkipButton = false;
        if (!ContentStorageActivityHelper.baseFolderIsSet()) {
            prepareFolderDefaultValues();
            this.contentStorageActivityHelper.migratePersistableFolder(PersistableFolder.BASE);
        }
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
            this.contentStorageActivityHelper.migratePersistableFolder(PersistableFolder.OFFLINE_MAPS);
        }
    }

    private static boolean mapThemeFolderNeedsMigration() {
        return Settings.legacyFolderNeedsToBeMigrated(R.string.pref_persistablefolder_offlinemapthemes);
    }

    private void requestMapthemefolder() {
        forceSkipButton = false;
        if (mapThemeFolderNeedsMigration()) {
            prepareFolderDefaultValues();
            this.contentStorageActivityHelper.migratePersistableFolder(PersistableFolder.OFFLINE_MAP_THEMES);
        }
    }

    private static boolean gpxFolderNeedsMigration() {
        return Settings.legacyFolderNeedsToBeMigrated(R.string.pref_persistablefolder_gpx);
    }

    private void requestGpxfolder() {
        forceSkipButton = false;
        if (gpxFolderNeedsMigration()) {
            prepareFolderDefaultValues();
            this.contentStorageActivityHelper.migratePersistableFolder(PersistableFolder.GPX);
        }
    }

    private static boolean broutertilesFolderNeedsMigration() {
        return Settings.isBrouterAutoTileDownloads() && PersistableFolder.ROUTING_TILES.isLegacy() && Routing.isExternalRoutingInstalled();
    }

    private void requestBroutertilesfolder() {
        forceSkipButton = false;
        if (broutertilesFolderNeedsMigration()) {
            prepareFolderDefaultValues();
            this.contentStorageActivityHelper.migratePersistableFolder(PersistableFolder.ROUTING_TILES);
        }
    }

    private void prepareFolderDefaultValues() {
        // re-evaluate default folder values, as the public folder may not have been accessible on startup
        PersistableFolder.reevaluateDefaultFolders();
    }

    // -------------------------------------------------------------------
    // services settings

    private boolean hasValidGCCredentials() {
        return Settings.getCredentials(GCConnector.getInstance()).isValid();
    }

    private void authorizeGC() {
        final Credentials credentials = GCConnector.getInstance().getCredentials();
        authorize.launch(new CredentialsAuthorizationContract.Input(credentials, GCAuthorizationActivity.class));
    }

    // -------------------------------------------------------------------

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(BUNDLE_MODE, mode.id);
        savedInstanceState.putInt(BUNDLE_STEP, step.ordinal());
        savedInstanceState.putBundle(BUNDLE_CSAH, contentStorageActivityHelper.getState());
        savedInstanceState.putBundle(BUNDLE_BACKUPUTILS, backupUtils.getState());
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((contentStorageActivityHelper == null || !contentStorageActivityHelper.onActivityResult(requestCode, resultCode, data))) {
            return;
        }
        backupUtils.onActivityResult(requestCode, resultCode, data);
    }

    private void onAuthorizationResult() {
        if (!hasValidGCCredentials()) {
            Toast.makeText(this, R.string.err_auth_process, Toast.LENGTH_SHORT).show();
        } else {
            SimpleDialog.of(this).setTitle(R.string.settings_title_gc).setMessage(R.string.settings_gc_legal_note).confirm((dialog, which) -> {
                Settings.setGCConnectorActive(true);
                gotoNext();
            }, (dialog, i) -> {
            });
        }
    }
}
