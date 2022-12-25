package cgeo.geocaching.permission;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.ActivitySavedState;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.util.Consumer;
import static androidx.lifecycle.Lifecycle.State.INITIALIZED;

import java.io.Serializable;

/**
 * A launcher for Actions which depend on Permissions.
 *
 * Instances of this class use the "Activity Result APIs" underneath to handle permission requests.
 * Therefore instances MUST be created as part of Activity initialization, ideally in the variable init part.
 * See e.g. https://developer.android.com/training/basics/intents/result for more details
 *
 * @param <T>
 */
public class PermissionAction<T extends Serializable> {

    private static final String PARAMETER_KEY = "parameter_key";

    private final Activity activity;
    private final PermissionContext permissionContext;
    private final Consumer<T> callback;
    private final ActivitySavedState savedState;
    private final ActivityResultLauncher<String[]> permissionLauncher;


    /** creates and registers a permission action with an Activity */
    public static <T extends Serializable> PermissionAction<T> register(final ComponentActivity activity, final PermissionContext pCtx, final Consumer<T> callback) {
        if (activity.getLifecycle().getCurrentState() != INITIALIZED) {
            throw new IllegalStateException("Activity must be in state INITIALIZED when creating launchers: " + activity.getClass().getName());
        }
        return new PermissionAction<>(activity, pCtx, callback);
    }

    private PermissionAction(final ComponentActivity activity, final PermissionContext pCtx, final Consumer<T> callback) {
        this.activity = activity;
        this.permissionContext = pCtx;
        this.callback = callback;
        this.savedState = new ActivitySavedState(activity, "pa-" + pCtx.name());
        this.permissionLauncher = activity.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), p -> handlePermissionResult());
    }

    /**
     * launches the formerly registered permission action with a user-defined parameter.
     * The parameter will be passed to the action code
     */
    public void launch(final T parameter) {
        //check: should we show an explanation BEFORE requesting permission?
        if (this.permissionContext.shouldShowRequestPermissionRationale(activity)) {
            SimpleDialog.of(activity).setTitle(TextParam.id(R.string.permission_dialog_title))
                    .setMessage(permissionContext.getExplanation())
                    .confirm((d, c) -> launchPermissionRequest(parameter));
        } else {
            launchPermissionRequest(parameter);
        }
    }

    private void launchPermissionRequest(final T parameter) {
        this.savedState.get().putSerializable(PARAMETER_KEY, parameter);
        this.permissionLauncher.launch(this.permissionContext.getPermissions());
    }

    @SuppressWarnings("unchecked")
    private void handlePermissionResult() {
        final T parameter = (T) this.savedState.get().getSerializable(PARAMETER_KEY);
        if (this.permissionContext.hasAllPermissions()) {
            //-> we have permissions now, execute callback
            callback.accept(parameter);
            this.savedState.clear();
        } else {
            //-> we still don't have necessary permissions -> show explanation and ask user again
            SimpleDialog.of(activity).setTitle(TextParam.id(R.string.permission_dialog_title))
                    .setMessage(permissionContext.getExplanation())
                    .setPositiveButton(TextParam.id(R.string.ask_again))
                    .setNegativeButton(TextParam.id(R.string.cancel))
                    .setNeutralButton(TextParam.id(R.string.goto_app_details))
                    .show((d, c) -> launchPermissionRequest(parameter),
                            (d, c) -> d.dismiss(),
                            (d, c) -> openApplicationSettings());
        }

    }

    private void openApplicationSettings() {
        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

}
