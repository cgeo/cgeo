package cgeo.geocaching.wizard;

import cgeo.geocaching.R;
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
    private final LiveData<WizardMode> mode;
    @NonNull
    private final LiveData<WizardStep> step;
    @NonNull
    private final MutableLiveData<Boolean> forceSkipButton = new MutableLiveData<>(false);

    public InstallWizardViewModel(final Application application, final SavedStateHandle savedStateHandle) {
        super(application);
        state = savedStateHandle;
        mode = Transformations.map(
            savedStateHandle.getLiveData(BUNDLE_MODE, WizardMode.WIZARDMODE_DEFAULT.ordinal()),
            (ordinal) -> WizardMode.values()[ordinal]
        );
        step = Transformations.distinctUntilChanged(Transformations.map(
            savedStateHandle.getLiveData(BUNDLE_STEP, WizardStep.WIZARD_START.ordinal()),
            (ordinal) -> WizardStep.values()[ordinal]
        ));
    }

    @NonNull
    public LiveData<WizardMode> getMode() {
        return mode;
    }

    public void setMode(@NonNull final WizardMode mode) {
        this.state.set(BUNDLE_MODE, mode.ordinal());
    }

    @NonNull
    public LiveData<WizardStep> getStep() {
        return step;
    }

    private void setStep(@NonNull final WizardStep step) {
        state.set(BUNDLE_STEP, step.ordinal());
    }

    public void resetStep() {
        setStep(WizardStep.WIZARD_START);
    }

    @NonNull
    public LiveData<Boolean> getForceSkipButton() {
        return forceSkipButton;
    }

    public void setForceSkipButton(final boolean forceSkipButton) {
        this.forceSkipButton.setValue(forceSkipButton);
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
        final WizardMode mode = Objects.requireNonNull(this.mode.getValue());

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
