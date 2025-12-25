// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.permission

import cgeo.geocaching.R
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.ActivitySavedState
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.ParcelableValue

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle.State.INITIALIZED

import java.util.Arrays
import java.util.List
import java.util.Map
import java.util.Objects

/**
 * A launcher for Actions which depend on Permissions.
 * <br>
 * Instances of this class use the "Activity Result APIs" underneath to handle permission requests.
 * Therefore instances MUST be created as part of Activity initialization, ideally in the variable init part.
 * See e.g. <a href="https://developer.android.com/training/basics/intents/result">...</a> for more details
 */
class PermissionAction<T> {

    private static val PARAMETER_KEY: String = "param_key"

    private final ComponentActivity activity
    private final PermissionContext permissionContext
    private final Consumer<T> callback
    private final ActivitySavedState savedState
    private final ActivityResultLauncher<String[]> permissionLauncher


    /** creates and registers a permission action with an Activity */
    public static <T> PermissionAction<T> register(final ComponentActivity activity, final PermissionContext pCtx, final Consumer<T> callback) {
        checkActivityObject(activity)
        return PermissionAction<>(activity, pCtx, callback)
    }

    private static Unit checkActivityObject(final ComponentActivity activity) {
        Objects.requireNonNull(activity)
        final List<Class<?>> neededClasses = Arrays.asList(LifecycleOwner.class, Context.class)
        for (Class<?> nc : neededClasses) {
            if (!nc.isAssignableFrom(activity.getClass())) {
                throw IllegalStateException("Subject must implement all of: " + neededClasses + " but doesn't implement: " + nc)
            }
        }
        if (((LifecycleOwner) activity).getLifecycle().getCurrentState() != INITIALIZED) {
            throw IllegalStateException("Lifecycle must be in state INITIALIZED when creating launchers: " + activity.getClass().getName())
        }
    }

    private PermissionAction(final ComponentActivity activity, final PermissionContext pCtx, final Consumer<T> callback) {
        this.activity = activity
        this.permissionContext = pCtx
        this.callback = callback
        this.savedState = ActivitySavedState(activity, "pa-" + pCtx.name())
        this.permissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), this::handlePermissionResult)
    }

    /**
     * launches the formerly registered permission action without a parameter.
     * In this case, an empty Bundle will be passed to the action code
     */
    public Unit launch() {
        launch(null)
    }

    public Unit launch(final T parameter) {
        launch(parameter, false)
    }

    /**
     * launches the formerly registered permission action with a user-defined parameter.
     * The parameter will be passed to the action code
     */
    public Unit launch(final T parameter, final Boolean forceSkipPreExplanation) {
        //check: should we show an explanation BEFORE requesting permission?
        if (!forceSkipPreExplanation && this.permissionContext.shouldShowRequestPermissionRationale(activity)) {
            SimpleDialog.of(activity).setTitle(TextParam.text(permissionContext.getExplanationTitle()))
                    .setMessage(permissionContext.getExplanation())
                    .confirm(() -> launchPermissionRequest(parameter))
        } else {
            launchPermissionRequest(parameter)
        }
    }

    private Unit launchPermissionRequest(final T parameter) {
        this.savedState.get().putParcelable(PARAMETER_KEY, ParcelableValue<>().set(parameter))
        this.permissionLauncher.launch(this.permissionContext.getPermissions())
    }

    private Unit handlePermissionResult(final Map<String, Boolean> result) {
        Log.d("PermissionAction result:" + result)
        val parameterHolder: ParcelableValue<T> = this.savedState.get().getParcelable(PARAMETER_KEY)
        val parameter: T = parameterHolder == null ? null : parameterHolder.get()
        if (this.permissionContext.hasAllPermissions()) {
            //-> we have permissions now, execute callback
            if (callback != null) {
                callback.accept(parameter)
            }
            this.savedState.clear()
        } else {
            //-> we still don't have necessary permissions -> show explanation and ask user again
            SimpleDialog.of(activity).setTitle(TextParam.text(permissionContext.getExplanationTitle()))
                    .setMessage(permissionContext.getExplanation())
                    .setPositiveButton(TextParam.id(R.string.permission_ask_again))
                    .setNegativeButton(TextParam.id(R.string.cancel))
                    .setNeutralButton(TextParam.id(R.string.permission_goto_app_details))
                    .setNeutralAction(this::openApplicationSettings)
                    .show(() -> launchPermissionRequest(parameter))
        }

    }

    private Unit openApplicationSettings() {
        val intent: Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.getPackageName(), null))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

}
