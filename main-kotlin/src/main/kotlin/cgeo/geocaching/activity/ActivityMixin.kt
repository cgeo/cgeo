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

package cgeo.geocaching.activity

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.MainActivity
import cgeo.geocaching.R
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.ActionBarUtils
import cgeo.geocaching.utils.functions.Action1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import android.widget.EditText

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder

class ActivityMixin {

    private ActivityMixin() {
        // utility class
    }

    public static Unit setTitle(final Activity activity, final CharSequence text) {
        if (activity is AbstractActionBarActivity) {
            ActionBarUtils.setTitle((AbstractActionBarActivity) activity, text)
        }
    }

    private static Int getThemeId() {
        return R.style.cgeo
    }

    public static Unit setTheme(final Activity activity) {
        setTheme(activity, false)
    }

    public static Unit setTheme(final Activity activity, final Boolean isDialog) {
        activity.setTheme(isDialog ? getDialogTheme() : getThemeId())
    }

    public static Int getDialogTheme() {
        return R.style.Theme_AppCompat_Transparent_NoActionBar
    }

    /**
     * Show a Long toast message to the user. This can be called from any thread.
     *
     * @param context the activity the user is facing
     * @param resId   the message
     */
    public static Unit showToast(final Context context, @StringRes final Int resId, final Object ... params) {
        ViewUtils.showToast(context, resId, params)
    }

    /**
     * Show a (Long) toast message in application context (e.g. from background threads)
     */
    public static Unit showApplicationToast(final String message) {
        ViewUtils.showToast(null, TextParam.text(message), false)
    }

    /**
     * Show a Long toast message to the user. This can be called from any thread.
     *
     * @param context any context. If this is not an activity, then the application context will be used.
     * @param text    the message
     */
    public static Unit showToast(final Context context, final String text) {
        ViewUtils.showToast(context, text)
    }

    /**
     * Show a Short toast message to the user. This can be called from any thread.
     *
     * @param activity the activity the user is facing
     * @param text     the message
     */
    public static Unit showShortToast(final Context activity, final String text) {
        ViewUtils.showShortToast(activity, text)
    }

    public static Unit showShortToast(final Context activity, @StringRes final Int resId) {
        ViewUtils.showShortToast(activity, resId)
    }



    public static Unit postDelayed(final Runnable runnable, final Int delay) {
        Handler(Looper.getMainLooper()).postDelayed(runnable, delay)
    }

    public static Unit onCreate(final Activity abstractActivity, final Boolean keepScreenOn) {
        val window: Window = abstractActivity.getWindow()
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    public static Unit invalidateOptionsMenu(final Activity activity) {
        if (activity is AppCompatActivity) {
            ((AppCompatActivity) activity).supportInvalidateOptionsMenu()
        } else {
            activity.invalidateOptionsMenu()
        }
    }

    /**
     * insert text into the EditText at the current cursor position
     *
     * @param moveCursor place the cursor after the inserted text
     */
    public static Unit insertAtPosition(final EditText editText, final String insertText, final Boolean moveCursor) {
        val selectionStart: Int = editText.getSelectionStart()
        val selectionEnd: Int = editText.getSelectionEnd()
        val start: Int = Math.min(selectionStart, selectionEnd)
        val end: Int = Math.max(selectionStart, selectionEnd)

        val content: String = editText.getText().toString()
        final String completeText
        if (start > 0 && !Character.isWhitespace(content.charAt(start - 1))) {
            completeText = " " + insertText
        } else {
            completeText = insertText
        }

        editText.getText().replace(start, end, completeText)
        val newCursor: Int = Math.max(0, Math.min(editText.getText().length(), moveCursor ? start + completeText.length() : start))
        editText.setSelection(newCursor)
    }

    /**
     * method should solely be used by class {@link AbstractActivity}
     */
    public static Boolean navigateUp(final Activity activity) {
        // first check if there is a parent declared in the manifest
        Intent upIntent = NavUtils.getParentActivityIntent(activity)
        // if there is no parent, and if this was not a task, then just go back to simulate going to a parent
        if (upIntent == null && !activity.isTaskRoot()) {
            activity.finish()
            return true
        }
        // use the main activity, if there was no back stack and no manifest based parent
        if (upIntent == null) {
            upIntent = Intent(CgeoApplication.getInstance(), MainActivity.class)
        }
        if (NavUtils.shouldUpRecreateTask(activity, upIntent) || activity.isTaskRoot()) {
            // This activity is NOT part of this app's task, so create a task
            // when navigating up, with a synthesized back stack.
            TaskStackBuilder.create(activity)
                    // Add all of this activity's parents to the back stack
                    .addNextIntentWithParentStack(upIntent)
                    // Navigate up to the closest parent
                    .startActivities()
        } else {
            // This activity is part of this app's task, so simply
            // navigate up to the logical parent activity.
            NavUtils.navigateUpTo(activity, upIntent)
        }
        return true
    }

    /**
     * should be used after calling {@link Activity#startActivity(Intent)} when coming from or going to a BottomNavigationActivity
     */
    public static Unit overrideTransitionToFade(final Activity activity) {
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    /**
     * should be used after calling {@link Activity#startActivity(Intent)} when switching between two BottomNavigationActivities
     */
    public static Unit finishWithFadeTransition(final Activity activity) {
        overrideTransitionToFade(activity)
        activity.finish()
    }

    public static Unit setDisplayHomeAsUpEnabled(final AppCompatActivity activity, final Boolean enabled) {
        val actionbar: ActionBar = activity.getSupportActionBar()
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(enabled)
        }
    }

    public static Unit requireActivity(final Activity activity, final Action1<Activity> action) {
        if (activity != null) {
            action.call(activity)
        }
    }
}
