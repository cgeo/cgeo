package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.databinding.InstallWizardBinding;
import cgeo.geocaching.downloader.DownloadSelectorActivity;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.permission.PermissionAction;
import cgeo.geocaching.permission.PermissionContext;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.GCAuthorizationActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.BackupUtils;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

public class InstallWizardActivity extends AppCompatActivity {

    public static final String BUNDLE_MODE = "wizardmode";
    private static final String BUNDLE_STEP = "step";
    private static final String BUNDLE_CSAH = "csah";
    private static final String BUNDLE_BACKUPUTILS = "backuputils";

    public enum WizardMode {
        WIZARDMODE_DEFAULT(0),
        WIZARDMODE_RETURNING(1),
        WIZARDMODE_MIGRATION(2);

        public final int id;

        WizardMode(final int id) {
            this.id = id;
        }
    }

    private enum WizardStep {
        WIZARD_START,
        WIZARD_PERMISSIONS, WIZARD_PERMISSIONS_LOCATION,
        WIZARD_PERMISSIONS_BASEFOLDER, WIZARD_PERMISSIONS_MAPFOLDER, WIZARD_PERMISSIONS_MAPTHEMEFOLDER, WIZARD_PERMISSIONS_GPXFOLDER, WIZARD_PERMISSIONS_BROUTERTILESFOLDER,
        WIZARD_PLATFORMS,
        WIZARD_ADVANCED,
        WIZARD_END
    }

    private WizardMode mode = WizardMode.WIZARDMODE_DEFAULT;
    private WizardStep step = WizardStep.WIZARD_START;
    private boolean forceSkipButton = false;
    private ContentStorageActivityHelper contentStorageActivityHelper = null;
    private BackupUtils backupUtils;

    private static final int REQUEST_CODE_WIZARD_GC = 0x7167;

    private final PermissionAction askLocationPermissionAction = PermissionAction.register(this, PermissionContext.LOCATION, b -> gotoNext());

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
    private Button nextOutlined = null;

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
        nextOutlined = binding.wizardNextOutlined;

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
            case WIZARD_PERMISSIONS_LOCATION:
                title.setText(PermissionContext.LOCATION.getExplanationTitle());
                PermissionContext.LOCATION.getExplanation().applyTo(text);
                //text.setText(R.string.location_permission_request_explanation);
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
                    startActivity(new Intent(this, DownloadSelectorActivity.class));
                }, button1Info, R.string.wizard_advanced_offlinemaps_info);
                if (!Routing.isAvailable()) {
                    setButton(button2, R.string.wizard_advanced_routing_label, v -> {
                        setButtonToDone();
                        Settings.setUseInternalRouting(true);
                        Settings.setBrouterAutoTileDownloads(true);
                        setButton(button2, 0, null, button2Info, 0);
                    }, button2Info, R.string.wizard_advanced_routing_info);
                }
                setButton(button3, R.string.wizard_advanced_restore_label, v -> {
                    setButtonToDone();
                    DataStore.resetNewlyCreatedDatabase();
                    if (BackupUtils.hasBackup(BackupUtils.newestBackupFolder())) {
                        backupUtils.restore(BackupUtils.newestBackupFolder());
                    } else {
                        backupUtils.selectBackupDirIntent();
                    }
                }, button3Info, R.string.wizard_advanced_restore_info);
                break;
            case WIZARD_END: {
                title.setText(R.string.wizard_welcome_title);
                final StringBuilder info = new StringBuilder();
                info.append(getString(R.string.wizard_status_title)).append(":\n")
                        .append(getString(R.string.permission_location_explanation_title)).append(": ").append(hasLocationPermission() ? getString(android.R.string.ok) : getString(R.string.status_not_ok)).append("\n")
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

                text.setText(isConfigurationOk() ? R.string.wizard_outro_ok : R.string.wizard_outro_error);

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
        setSkip(listenerSkip, skipLabelRes);

        final boolean useNextOutlinedButton = nextLabelRes == R.string.skip;
        if (listenerNext == null) {
            next.setVisibility(View.GONE);
            nextOutlined.setVisibility(View.GONE);
        } else {
            next.setVisibility(useNextOutlinedButton ? View.GONE : View.VISIBLE);
            nextOutlined.setVisibility(useNextOutlinedButton ? View.VISIBLE : View.GONE);
            if (useNextOutlinedButton) {
                nextOutlined.setText(nextLabelRes);
                nextOutlined.setOnClickListener(v -> listenerNext.run());
            } else {
                next.setText(nextLabelRes == 0 ? R.string.next : nextLabelRes);
                next.setOnClickListener(v -> listenerNext.run());
            }
        }
    }

    private void setSkip(@Nullable final Runnable listenerSkip, final int skipLabelRes) {
        if (listenerSkip == null) {
            skip.setVisibility(View.GONE);
        } else {
            skip.setVisibility(View.VISIBLE);
            skip.setText(skipLabelRes == 0 ? R.string.skip : skipLabelRes);
            skip.setOnClickListener(v -> listenerSkip.run());
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
        return (step == WizardStep.WIZARD_PERMISSIONS && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasLocationPermission()))
                || (step == WizardStep.WIZARD_PERMISSIONS_LOCATION && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasLocationPermission()))
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

    public static boolean isConfigurationOk() {
        final boolean isPlatformConfigured = ConnectorFactory.getActiveConnectorsWithValidCredentials().length > 0;
        return hasLocationPermission() && isPlatformConfigured && ContentStorageActivityHelper.baseFolderIsSet();
    }

    public static boolean needsFolderMigration() {
        return mapFolderNeedsMigration() || mapThemeFolderNeedsMigration() || gpxFolderNeedsMigration() || broutertilesFolderNeedsMigration();
    }

    // -------------------------------------------------------------------
    // old Android permissions related methods

    private static boolean hasLocationPermission() {
        return PermissionContext.LOCATION.hasAllPermissions();
    }

    private void requestLocation() {
        setSkip(this::gotoNext, 0);
        askLocationPermissionAction.launch(null, true);
    }


    // -------------------------------------------------------------------
    // Android SAF-based permissions related methods

    private void setFolderInfo(final PersistableFolder folder, @StringRes final int info, final boolean addSelectOrCreateInfo) {
        title.setText(String.format(getString(R.string.wizard_permissions_folder_title), getString(folder.getNameKeyId())));
        final String temp = getString(info) + (addSelectOrCreateInfo ? " " + getString(R.string.wizard_select_or_create) : "");
        text.setText(temp);
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
        savedInstanceState.putBundle(BUNDLE_CSAH, contentStorageActivityHelper.getState());
        savedInstanceState.putBundle(BUNDLE_BACKUPUTILS, backupUtils.getState());
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_WIZARD_GC) {
            if (!hasValidGCCredentials()) {
                Toast.makeText(this, R.string.err_auth_process, Toast.LENGTH_SHORT).show();
            } else {
                SimpleDialog.of(this).setTitle(R.string.settings_title_gc).setMessage(R.string.settings_gc_legal_note).confirm((dialog, which) -> {
                    Settings.setGCConnectorActive(true);
                    gotoNext();
                }, (dialog, i) -> {
                });
            }
            return;
        }
        if ((contentStorageActivityHelper == null || !contentStorageActivityHelper.onActivityResult(requestCode, resultCode, data))) {
            return;
        }
        backupUtils.onActivityResult(requestCode, resultCode, data);
    }
}
