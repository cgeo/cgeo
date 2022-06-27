package cgeo.geocaching.wizard;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.PersistableFolder;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;

import java.util.Objects;

public class InstallWizardViewModel extends AndroidViewModel {

    private static final String BUNDLE_MODE = "wizardmode";
    private static final String BUNDLE_STEP = "step";

    private final SavedStateHandle state;

    @NonNull
    private WizardMode mode;

    @NonNull
    private final LiveData<WizardStep> step;
    @NonNull
    private final MutableLiveData<Boolean> forceSkipButton = new MutableLiveData<>(false);
    @NonNull
    private final LiveData<PreviousButton> previousButton;

    public InstallWizardViewModel(final Application application, final SavedStateHandle savedStateHandle) {
        super(application);
        state = savedStateHandle;

        final Integer modeOrdinal = savedStateHandle.get(BUNDLE_MODE);
        mode = modeOrdinal == null ? WizardMode.WIZARDMODE_DEFAULT : WizardMode.values()[modeOrdinal];

        step = Transformations.distinctUntilChanged(Transformations.map(
            savedStateHandle.getLiveData(BUNDLE_STEP, WizardStep.WIZARD_START.ordinal()),
            (ordinal) -> WizardStep.values()[ordinal]
        ));
        previousButton = Transformations.distinctUntilChanged(Transformations.map(
            step,
            (step) -> step == WizardStep.WIZARD_START ? PreviousButton.NOT_NOW : PreviousButton.PREVIOUS
        ));
    }

    /**
     * Get the wizard mode. This value is set by the activity based on intent data and is not reactive.
     */
    @NonNull
    public WizardMode getMode() {
        return mode;
    }

    public void setMode(@NonNull final WizardMode mode) {
        this.mode = mode;
        this.state.set(BUNDLE_MODE, mode.ordinal());
    }

    @NonNull
    public LiveData<Boolean> getForceSkipButton() {
        return forceSkipButton;
    }

    public void setForceSkipButton(final boolean forceSkipButton) {
        this.forceSkipButton.setValue(forceSkipButton);
    }

    /**
     * State for the previous button at the bottom of the screen.
     */
    @NonNull
    public LiveData<PreviousButton> getPreviousButton() {
        return previousButton;
    }

    /**
     * The current step in the wizard flow.
     */
    @NonNull
    public LiveData<WizardStep> getStep() {
        return step;
    }

    private void setStep(@NonNull final WizardStep step) {
        state.set(BUNDLE_STEP, step.ordinal());
    }

    /**
     * Reset the wizard - used in case of invalid state.
     */
    public void resetStep() {
        setStep(WizardStep.WIZARD_START);
    }

    public void gotoPrevious() {
        final WizardStep currentStep = Objects.requireNonNull(this.step.getValue());
        if (currentStep.ordinal() > 0) {
            setStep(WizardStep.values()[currentStep.ordinal() - 1]);
            if (stepCanBeSkipped()) {
                gotoPrevious();
            }
        }
    }

    public void gotoNext() {
        final WizardStep currentStep = Objects.requireNonNull(this.step.getValue());
        final int max = WizardStep.values().length - 1;
        if (currentStep.ordinal() < max) {
            setStep(WizardStep.values()[currentStep.ordinal() + 1]);
            if (stepCanBeSkipped()) {
                gotoNext();
            }
        }
    }

    private boolean stepCanBeSkipped() {
        final WizardStep step = Objects.requireNonNull(this.step.getValue());
        final WizardMode mode = this.mode;

        return (step == WizardStep.WIZARD_PERMISSIONS && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (hasStoragePermission() && hasLocationPermission())))
            || (step == WizardStep.WIZARD_PERMISSIONS_STORAGE && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasStoragePermission()))
            || (step == WizardStep.WIZARD_PERMISSIONS_LOCATION && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasLocationPermission()))
            || (step == WizardStep.WIZARD_PERMISSIONS_BASEFOLDER && ContentStorageActivityHelper.baseFolderIsSet())
            || (step == WizardStep.WIZARD_PERMISSIONS_MAPFOLDER && !mapFolderNeedsMigration())
            || (step == WizardStep.WIZARD_PERMISSIONS_MAPTHEMEFOLDER && !mapThemeFolderNeedsMigration())
            || (step == WizardStep.WIZARD_PERMISSIONS_GPXFOLDER && !gpxFolderNeedsMigration())
            || (step == WizardStep.WIZARD_PERMISSIONS_BROUTERTILESFOLDER && !broutertilesFolderNeedsMigration())
            || (step == WizardStep.WIZARD_PLATFORMS && mode == WizardMode.WIZARDMODE_MIGRATION)
            || (step == WizardStep.WIZARD_ADVANCED && mode == WizardMode.WIZARDMODE_MIGRATION);
    }

    @NonNull
    private String okOrError(final boolean ok) {
        return getApplication().getString(ok ? android.R.string.ok : R.string.status_not_ok);
    }

    @NonNull
    public String getWizardEndInfo() {
        final Context context = getApplication();
        final StringBuilder info = new StringBuilder();
        info.append(context.getString(R.string.wizard_status_title)).append(":\n")
                .append(context.getString(R.string.wizard_status_storage_permission)).append(": ").append(okOrError(hasStoragePermission())).append("\n")
                .append(context.getString(R.string.wizard_status_location_permission)).append(": ").append(okOrError(hasLocationPermission())).append("\n")
                .append(context.getString(R.string.wizard_status_basefolder)).append(": ").append(okOrError(ContentStorageActivityHelper.baseFolderIsSet())).append("\n")
                .append(context.getString(R.string.wizard_status_platform));

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
            info.append(": ").append(context.getString(android.R.string.ok)).append("\n(").append(platforms).append(")\n");
        } else {
            info.append(": ").append(context.getString(R.string.status_not_ok)).append("\n");
        }
        return info.toString();
    }

    private boolean hasStoragePermission() {
        final Context context = getApplication();
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermission() {
        final Context context = getApplication();
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean mapFolderNeedsMigration() {
        return Settings.legacyFolderNeedsToBeMigrated(R.string.pref_persistablefolder_offlinemaps);
    }

    public static boolean mapThemeFolderNeedsMigration() {
        return Settings.legacyFolderNeedsToBeMigrated(R.string.pref_persistablefolder_offlinemapthemes);
    }

    public static boolean gpxFolderNeedsMigration() {
        return Settings.legacyFolderNeedsToBeMigrated(R.string.pref_persistablefolder_gpx);
    }

    public static boolean broutertilesFolderNeedsMigration() {
        return Settings.isBrouterAutoTileDownloads() && PersistableFolder.ROUTING_TILES.isLegacy() && Routing.isExternalRoutingInstalled();
    }
}
