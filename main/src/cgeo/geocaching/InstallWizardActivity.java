package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
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
import cgeo.geocaching.wizard.InstallWizardViewModel;
import cgeo.geocaching.wizard.NextButton;
import cgeo.geocaching.wizard.PreviousButton;
import cgeo.geocaching.wizard.WizardMode;
import cgeo.geocaching.wizard.WizardStep;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.Objects;

public class InstallWizardActivity extends AppCompatActivity {

    public static final String BUNDLE_MODE = "wizardmode";
    private static final String BUNDLE_CSAH = "csah";
    private static final String BUNDLE_BACKUPUTILS = "backuputils";

    private InstallWizardViewModel viewModel;

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

        viewModel = new ViewModelProvider(this).get(InstallWizardViewModel.class);

        backupUtils = new BackupUtils(this, savedInstanceState == null ? null : savedInstanceState.getBundle(BUNDLE_BACKUPUTILS));
        if (savedInstanceState == null) {
            viewModel.setMode(WizardMode.values()[getIntent().getIntExtra(BUNDLE_MODE, WizardMode.WIZARDMODE_DEFAULT.id)]);
        }
        binding = InstallWizardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        this.contentStorageActivityHelper = new ContentStorageActivityHelper(this, savedInstanceState == null ? null : savedInstanceState.getBundle(BUNDLE_CSAH))
                .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FOLDER_PERSISTED, PersistableFolder.class, pf -> {
                    final boolean needsMigration;
                    switch (pf) {
                        case GPX:
                            needsMigration = InstallWizardViewModel.gpxFolderNeedsMigration();
                            break;
                        case BASE:
                            needsMigration = !ContentStorageActivityHelper.baseFolderIsSet();
                            break;
                        case OFFLINE_MAPS:
                            needsMigration = InstallWizardViewModel.mapFolderNeedsMigration();
                            break;
                        case OFFLINE_MAP_THEMES:
                            needsMigration = InstallWizardViewModel.mapThemeFolderNeedsMigration();
                            break;
                        case ROUTING_TILES:
                            needsMigration = InstallWizardViewModel.broutertilesFolderNeedsMigration();
                            break;
                        default:
                            needsMigration = false;
                            break;
                    }
                    onReturnFromFolderMigration(!needsMigration);
                });

        viewModel.getStep().observe(this, (step) -> {
            final boolean forceSkipButton = Boolean.TRUE.equals(viewModel.getForceSkipButton().getValue());
            updateDialog(step, forceSkipButton);
        });
        viewModel.getForceSkipButton().observe(this, (forceSkipButton) -> {
            final WizardStep step = Objects.requireNonNull(viewModel.getStep().getValue());
            updateDialog(step, forceSkipButton);
        });

        viewModel.getPreviousButton().observe(this, this::setNavigationButtonPrevious);
    }

    private void updateDialog(@NonNull final WizardStep step, final boolean forceSkipButton) {
        binding.wizardLogo.setImageResource(R.mipmap.ic_launcher);
        binding.wizardText.setVisibility(View.VISIBLE);
        setButton(binding.wizardButton1, 0, null, binding.wizardButton1Info, 0);
        setButton(binding.wizardButton2, 0, null, binding.wizardButton2Info, 0);
        setButton(binding.wizardButton3, 0, null, binding.wizardButton3Info, 0);
        switch (step) {
            case WIZARD_START:
                final WizardMode mode = viewModel.getMode();
                binding.wizardTitle.setText(mode == WizardMode.WIZARDMODE_MIGRATION ? R.string.wizard_migration_title : R.string.wizard_welcome_title);
                binding.wizardText.setText(mode == WizardMode.WIZARDMODE_RETURNING ? R.string.wizard_intro_returning : mode == WizardMode.WIZARDMODE_MIGRATION ? R.string.wizard_intro_migration : R.string.wizard_intro);
                setNavigationButtonNext(viewModel::gotoNext, NextButton.NEXT, forceSkipButton);
                break;
            case WIZARD_PERMISSIONS:
                binding.wizardTitle.setText(R.string.wizard_permissions_title);
                binding.wizardText.setText(R.string.wizard_permissions_intro);
                setNavigationButtonNext(viewModel::gotoNext, NextButton.NEXT, forceSkipButton);
                break;
            case WIZARD_PERMISSIONS_STORAGE:
                binding.wizardTitle.setText(R.string.wizard_status_storage_permission);
                binding.wizardText.setText(R.string.storage_permission_request_explanation);
                setNavigationButtonNext(this::requestStorage, NextButton.NEXT, forceSkipButton);
                break;
            case WIZARD_PERMISSIONS_LOCATION:
                binding.wizardTitle.setText(R.string.wizard_status_location_permission);
                binding.wizardText.setText(R.string.location_permission_request_explanation);
                setNavigationButtonNext(this::requestLocation, NextButton.NEXT, forceSkipButton);
                break;
            case WIZARD_PERMISSIONS_BASEFOLDER:
                setFolderInfo(PersistableFolder.BASE, R.string.wizard_basefolder_request_explanation, false);
                setNavigationButtonNext(this::requestBasefolder, NextButton.NEXT, forceSkipButton);
                break;
            case WIZARD_PERMISSIONS_MAPFOLDER:
                setFolderInfo(PersistableFolder.OFFLINE_MAPS, R.string.wizard_mapfolder_request_explanation, true);
                setNavigationButtonNext(this::requestMapfolder, NextButton.NEXT, forceSkipButton);
                break;
            case WIZARD_PERMISSIONS_MAPTHEMEFOLDER:
                setFolderInfo(PersistableFolder.OFFLINE_MAP_THEMES, R.string.wizard_mapthemesfolder_request_explanation, true);
                setNavigationButtonNext(this::requestMapthemefolder, NextButton.NEXT, forceSkipButton);
                break;
            case WIZARD_PERMISSIONS_GPXFOLDER:
                setFolderInfo(PersistableFolder.GPX, R.string.wizard_gpxfolder_request_explanation, true);
                setNavigationButtonNext(this::requestGpxfolder, NextButton.NEXT, forceSkipButton);
                break;
            case WIZARD_PERMISSIONS_BROUTERTILESFOLDER:
                setFolderInfo(PersistableFolder.ROUTING_TILES, R.string.wizard_broutertilesfolder_request_explanation, true);
                setNavigationButtonNext(this::requestBroutertilesfolder, NextButton.NEXT, forceSkipButton);
                break;
            case WIZARD_PLATFORMS:
                binding.wizardTitle.setText(R.string.wizard_platforms_title);
                binding.wizardText.setText(R.string.wizard_platforms_intro);
                setNavigationButtonNext(null, NextButton.NEXT, false);
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
                setNavigationButtonNext(null, NextButton.NEXT, false);
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
                    restoreBackup();
                }, binding.wizardButton3Info, R.string.wizard_advanced_restore_info);
                break;
            case WIZARD_END: {
                binding.wizardTitle.setText(R.string.wizard_welcome_title);
                binding.wizardButton1Info.setVisibility(View.VISIBLE);
                binding.wizardButton1Info.setText(viewModel.getWizardEndInfo());

                binding.wizardText.setText(isConfigurationOk(this) ? R.string.wizard_outro_ok : R.string.wizard_outro_error);

                setNavigationButtonNext(this::finishWizard, NextButton.FINISH, false);
                break;
            }
            default: {
                // never should happen!
                viewModel.resetStep();
                break;
            }
        }
    }

    private void setButtonToDone() {
        setNavigationButtonNext(viewModel::gotoNext, NextButton.DONE, false);
    }

    /**
     * Configure the previous button at the bottom of the screen.
     */
    private void setNavigationButtonPrevious(final PreviousButton mode) {
        binding.wizardPrev.setText(mode.string);
        binding.wizardPrev.setOnClickListener(mode == PreviousButton.NOT_NOW ? v -> skipWizard() : v -> viewModel.gotoPrevious());
    }

    /**
     * Configure the next and skip buttons at the bottom of the screen.
     * @param listenerNext Listener for the next button.
     *                     If null, the button is hidden and the skip button takes its place.
     * @param forceSkipButton If true, a skip button will be shown.
     *                        If false, a skip button is only shown when listenerNext is null.
     */
    private void setNavigationButtonNext(@Nullable final Runnable listenerNext, final NextButton mode, final boolean forceSkipButton) {
        final boolean skipButtonVisible = listenerNext == null || forceSkipButton;

        if (listenerNext == null) {
            binding.wizardNext.setVisibility(View.GONE);
            binding.wizardNextSpace.setVisibility(View.GONE);
        } else {
            binding.wizardNext.setVisibility(View.VISIBLE);
            binding.wizardNextSpace.setVisibility(View.VISIBLE);
            binding.wizardNext.setText(mode.string);
            binding.wizardNext.setOnClickListener(v -> listenerNext.run());
        }

        if (skipButtonVisible) {
            binding.wizardSkip.setVisibility(View.VISIBLE);
            binding.wizardSkip.setText(R.string.skip);
            binding.wizardSkip.setOnClickListener(v -> viewModel.gotoNext());
        } else {
            binding.wizardSkip.setVisibility(View.GONE);
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

    private void skipWizard() {
        SimpleDialog.of(this)
            .setTitle(R.string.wizard)
            .setMessage(R.string.wizard_skip_wizard_warning)
            .setButtons(0, R.string.back)
            .confirm((dialog, which) -> finishWizard(), (dialog, which) -> {
            });
    }

    private void finishWizard() {
        // call MainActivity (if not returning) and finish this Activity
        if (viewModel.getMode() != WizardMode.WIZARDMODE_RETURNING) {
            final Intent main = new Intent(this, MainActivity.class);
            main.putExtras(getIntent());
            startActivity(main);
        }
        finish();
    }

    private void restoreBackup() {
        DataStore.resetNewlyCreatedDatabase();
        if (BackupUtils.hasBackup(BackupUtils.newestBackupFolder())) {
            backupUtils.restore(BackupUtils.newestBackupFolder());
        } else {
            backupUtils.selectBackupDirIntent();
        }
    }

    public static boolean isConfigurationOk(final Context context) {
        final boolean isPlatformConfigured = ConnectorFactory.getActiveConnectorsWithValidCredentials().length > 0;
        return hasStoragePermission(context) && hasLocationPermission(context) && isPlatformConfigured && ContentStorageActivityHelper.baseFolderIsSet();
    }

    public static boolean needsFolderMigration() {
        return InstallWizardViewModel.mapFolderNeedsMigration()
            || InstallWizardViewModel.mapThemeFolderNeedsMigration()
            || InstallWizardViewModel.gpxFolderNeedsMigration()
            || InstallWizardViewModel.broutertilesFolderNeedsMigration();
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
        viewModel.gotoNext();
    }

    // -------------------------------------------------------------------
    // Android SAF-based permissions related methods

    private void setFolderInfo(final PersistableFolder folder, @StringRes final int info, final boolean addSelectOrCreateInfo) {
        binding.wizardTitle.setText(String.format(getString(R.string.wizard_permissions_folder_title), getString(folder.getNameKeyId())));
        final String temp = getString(info) + (addSelectOrCreateInfo ? " " + getString(R.string.wizard_select_or_create) : "");
        binding.wizardText.setText(temp);
    }

    private void requestBasefolder() {
        viewModel.setForceSkipButton(false);
        if (!ContentStorageActivityHelper.baseFolderIsSet()) {
            prepareFolderDefaultValues();
            this.contentStorageActivityHelper.migratePersistableFolder(PersistableFolder.BASE);
        }
    }


    private void onReturnFromFolderMigration(final boolean resultOk) {
        if (resultOk) {
            viewModel.gotoNext();
        } else {
            viewModel.setForceSkipButton(true);
        }
    }

    private void requestMapfolder() {
        viewModel.setForceSkipButton(false);
        if (InstallWizardViewModel.mapFolderNeedsMigration()) {
            prepareFolderDefaultValues();
            this.contentStorageActivityHelper.migratePersistableFolder(PersistableFolder.OFFLINE_MAPS);
        }
    }

    private void requestMapthemefolder() {
        viewModel.setForceSkipButton(false);
        if (InstallWizardViewModel.mapThemeFolderNeedsMigration()) {
            prepareFolderDefaultValues();
            this.contentStorageActivityHelper.migratePersistableFolder(PersistableFolder.OFFLINE_MAP_THEMES);
        }
    }

    private void requestGpxfolder() {
        viewModel.setForceSkipButton(false);
        if (InstallWizardViewModel.gpxFolderNeedsMigration()) {
            prepareFolderDefaultValues();
            this.contentStorageActivityHelper.migratePersistableFolder(PersistableFolder.GPX);
        }
    }

    private void requestBroutertilesfolder() {
        viewModel.setForceSkipButton(false);
        if (InstallWizardViewModel.broutertilesFolderNeedsMigration()) {
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
                viewModel.gotoNext();
            }, (dialog, i) -> {
            });
        }
    }
}
