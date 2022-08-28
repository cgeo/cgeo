package cgeo.geocaching.activity;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;

import org.apache.commons.lang3.StringUtils;

public final class ActivityMixin {

    private ActivityMixin() {
        // utility class
    }

    public static void setTitle(final Activity activity, final CharSequence text) {
        if (StringUtils.isBlank(text)) {
            return;
        }

        if (activity instanceof AppCompatActivity) {
            final ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(text);
            }
        }
    }

    public static void showHideActionBar(final Activity activity, final boolean show) {
        if (activity instanceof AppCompatActivity) {
            final ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
            if (actionBar != null) {
                if (show) {
                    actionBar.show();
                } else {
                    actionBar.hide();
                }
            }
        }
    }

    private static int getThemeId() {
        return R.style.cgeo;
    }

    public static void setTheme(final Activity activity) {
        setTheme(activity, false);
    }

    public static void setTheme(final Activity activity, final boolean isDialog) {
        activity.setTheme(isDialog ? getDialogTheme() : getThemeId());
    }

    public static int getDialogTheme() {
        return R.style.Theme_AppCompat_Transparent_NoActionBar;
    }

    /**
     * Show a long toast message to the user. This can be called from any thread.
     *
     * @param context the activity the user is facing
     * @param resId   the message
     */
    public static void showToast(final Context context, @StringRes final int resId) {
        showToast(context, context.getString(resId));
    }

    private static void showCgeoToast(final Context context, final String text, final int toastDuration) {
        Log.v("[" + context.getClass().getName() + "].showToast(" + text + "){" + toastDuration + "}");
        try {
            final Toast toast = Toast.makeText(context, text, toastDuration);
            if (Build.VERSION.SDK_INT < 30) {
                toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
            }
            toast.show();
        } catch (RuntimeException re) {
            //this can happen e.g. in Unit tests when thread has no called Looper.prepare()
            Log.w("Could not show toast '" + text + "' to user: " + re);
        }
    }

    private static void postShowToast(final Activity activity, final String text, final int toastDuration) {
        if (StringUtils.isNotBlank(text)) {
            activity.runOnUiThread(() -> showCgeoToast(activity, text, toastDuration));
        }
    }

    /**
     * Show a (long) toast message in application context (e.g. from background threads)
     */
    public static void showApplicationToast(final String message) {
        final Context context = new ContextThemeWrapper(CgeoApplication.getInstance().getApplicationContext(), getThemeId());
        showCgeoToast(context, message, Toast.LENGTH_LONG);
    }

    /**
     * Show a long toast message to the user. This can be called from any thread.
     *
     * @param activity the activity the user is facing
     * @param text     the message
     */
    public static void showToast(final Activity activity, final String text) {
        postShowToast(activity, text, Toast.LENGTH_LONG);
    }

    /**
     * Show a long toast message to the user. This can be called from any thread.
     *
     * @param context any context. If this is not an activity, then the application context will be used.
     * @param text    the message
     */
    public static void showToast(final Context context, final String text) {
        if (context instanceof Activity) {
            showToast((Activity) context, text);
        } else {
            showApplicationToast(text);
        }
    }

    /**
     * Show a short toast message to the user. This can be called from any thread.
     *
     * @param activity the activity the user is facing
     * @param text     the message
     */
    public static void showShortToast(final Activity activity, final String text) {
        postShowToast(activity, text, Toast.LENGTH_SHORT);
    }

    public static void showShortToast(final Activity activity, @StringRes final int resId) {
        postShowToast(activity, activity.getString(resId), Toast.LENGTH_SHORT);
    }

    public static void onCreate(final Activity abstractActivity, final boolean keepScreenOn) {
        final Window window = abstractActivity.getWindow();
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public static void invalidateOptionsMenu(final Activity activity) {
        if (activity instanceof AppCompatActivity) {
            ((AppCompatActivity) activity).supportInvalidateOptionsMenu();
        } else {
            activity.invalidateOptionsMenu();
        }
    }

    /**
     * insert text into the EditText at the current cursor position
     *
     * @param moveCursor place the cursor after the inserted text
     */
    public static void insertAtPosition(final EditText editText, final String insertText, final boolean moveCursor) {
        final int selectionStart = editText.getSelectionStart();
        final int selectionEnd = editText.getSelectionEnd();
        final int start = Math.min(selectionStart, selectionEnd);
        final int end = Math.max(selectionStart, selectionEnd);

        final String content = editText.getText().toString();
        final String completeText;
        if (start > 0 && !Character.isWhitespace(content.charAt(start - 1))) {
            completeText = " " + insertText;
        } else {
            completeText = insertText;
        }

        editText.getText().replace(start, end, completeText);
        final int newCursor = Math.max(0, Math.min(editText.getText().length(), moveCursor ? start + completeText.length() : start));
        editText.setSelection(newCursor);
    }

    /**
     * method should solely be used by class {@link AbstractActivity}
     */
    public static boolean navigateUp(@NonNull final Activity activity) {
        // first check if there is a parent declared in the manifest
        Intent upIntent = NavUtils.getParentActivityIntent(activity);
        // if there is no parent, and if this was not a new task, then just go back to simulate going to a parent
        if (upIntent == null && !activity.isTaskRoot()) {
            activity.finish();
            return true;
        }
        // use the main activity, if there was no back stack and no manifest based parent
        if (upIntent == null) {
            upIntent = new Intent(CgeoApplication.getInstance(), MainActivity.class);
        }
        if (NavUtils.shouldUpRecreateTask(activity, upIntent) || activity.isTaskRoot()) {
            // This activity is NOT part of this app's task, so create a new task
            // when navigating up, with a synthesized back stack.
            TaskStackBuilder.create(activity)
                    // Add all of this activity's parents to the back stack
                    .addNextIntentWithParentStack(upIntent)
                    // Navigate up to the closest parent
                    .startActivities();
        } else {
            // This activity is part of this app's task, so simply
            // navigate up to the logical parent activity.
            NavUtils.navigateUpTo(activity, upIntent);
        }
        return true;
    }

    /**
     * should be used after calling {@link Activity#startActivity(Intent)} when coming from or going to a BottomNavigationActivity
     */
    public static void overrideTransitionToFade(final Activity activity) {
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * should be used after calling {@link Activity#startActivity(Intent)} when switching between two BottomNavigationActivities
     */
    public static void finishWithFadeTransition(final Activity activity) {
        overrideTransitionToFade(activity);
        activity.finish();
    }

    public static void setDisplayHomeAsUpEnabled(final AppCompatActivity activity, final boolean enabled) {
        final ActionBar actionbar = activity.getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(enabled);
        }
    }
}
