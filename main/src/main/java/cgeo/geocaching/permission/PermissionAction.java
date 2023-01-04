package cgeo.geocaching.permission;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.ActivitySavedState;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ParcelableValue;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;
import static androidx.lifecycle.Lifecycle.State.INITIALIZED;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A launcher for Actions which depend on Permissions.
 *
 * Instances of this class use the "Activity Result APIs" underneath to handle permission requests.
 * Therefore instances MUST be created as part of Activity initialization, ideally in the variable init part.
 * See e.g. https://developer.android.com/training/basics/intents/result for more details
 */
public class PermissionAction<T> {

    private static final String PARAMETER_KEY = "param_key";

    private final ComponentActivity activity;
    private final PermissionContext permissionContext;
    private final Consumer<T> callback;
    private final ActivitySavedState savedState;
    private final ActivityResultLauncher<String[]> permissionLauncher;


    /** creates and registers a permission action with an Activity */
    public static <T> PermissionAction<T> register(@NonNull final ComponentActivity activity, @NonNull final PermissionContext pCtx, @Nullable final Consumer<T> callback) {
        checkActivityObject(activity);
        return new PermissionAction<>(activity, pCtx, callback);
    }

    private static void checkActivityObject(@Nullable final ComponentActivity activity) {
        Objects.requireNonNull(activity);
        final List<Class<?>> neededClasses = Arrays.asList(LifecycleOwner.class, Context.class);
        for (Class<?> nc : neededClasses) {
            if (!nc.isAssignableFrom(activity.getClass())) {
                throw new IllegalStateException("Subject must implement all of: " + neededClasses + " but doesn't implement: " + nc);
            }
        }
        if (((LifecycleOwner) activity).getLifecycle().getCurrentState() != INITIALIZED) {
            throw new IllegalStateException("Lifecycle must be in state INITIALIZED when creating launchers: " + activity.getClass().getName());
        }
    }

    private PermissionAction(final ComponentActivity activity, final PermissionContext pCtx, final Consumer<T> callback) {
        this.activity = activity;
        this.permissionContext = pCtx;
        this.callback = callback;
        this.savedState = new ActivitySavedState(activity, "pa-" + pCtx.name());
        this.permissionLauncher = activity.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::handlePermissionResult);
    }

    /**
     * launches the formerly registered permission action without a parameter.
     * In this case, an empty Bundle will be passed to the action code
     */
    public void launch() {
        launch(null);
    }

    public void launch(final T parameter) {
        launch(parameter, false);
    }

    /**
     * launches the formerly registered permission action with a user-defined parameter.
     * The parameter will be passed to the action code
     */
    public void launch(final T parameter, final boolean forceSkipPreExplanation) {
        //check: should we show an explanation BEFORE requesting permission?
        if (!forceSkipPreExplanation && this.permissionContext.shouldShowRequestPermissionRationale(activity)) {
            SimpleDialog.of(activity).setTitle(TextParam.text(permissionContext.getExplanationTitle()))
                    .setMessage(permissionContext.getExplanation())
                    .confirm((d, c) -> launchPermissionRequest(parameter));
        } else {
            launchPermissionRequest(parameter);
        }
    }

    private void launchPermissionRequest(final T parameter) {
        this.savedState.get().putParcelable(PARAMETER_KEY, new ParcelableValue<>().set(parameter));
        this.permissionLauncher.launch(this.permissionContext.getPermissions());
    }

    private void handlePermissionResult(final Map<String, Boolean> result) {
        Log.d("PermissionAction result:" + result);
        final ParcelableValue<T> parameterHolder = this.savedState.get().getParcelable(PARAMETER_KEY);
        final T parameter = parameterHolder == null ? null : parameterHolder.get();
        if (this.permissionContext.hasAllPermissions()) {
            //-> we have permissions now, execute callback
            if (callback != null) {
                callback.accept(parameter);
            }
            this.savedState.clear();
        } else {
            //-> we still don't have necessary permissions -> show explanation and ask user again
            SimpleDialog.of(activity).setTitle(TextParam.text(permissionContext.getExplanationTitle()))
                    .setMessage(permissionContext.getExplanation())
                    .setPositiveButton(TextParam.id(R.string.permission_ask_again))
                    .setNegativeButton(TextParam.id(R.string.cancel))
                    .setNeutralButton(TextParam.id(R.string.permission_goto_app_details))
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
