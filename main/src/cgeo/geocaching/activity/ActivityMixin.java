package cgeo.geocaching.activity;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.Toast;

public final class ActivityMixin {

    public static void setTitle(final Activity activity, final CharSequence text) {
        if (StringUtils.isBlank(text)) {
            return;
        }

        if (activity instanceof ActionBarActivity) {
            final ActionBar actionBar = ((ActionBarActivity) activity).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(text);
            }
        }
    }

    public static void showProgress(final ActionBarActivity activity, final boolean show) {
        if (activity == null) {
            return;
        }

        activity.setSupportProgressBarIndeterminateVisibility(show);

    }

    public static void setTheme(final Activity activity) {
        if (Settings.isLightSkin()) {
            activity.setTheme(R.style.light);
        } else {
            activity.setTheme(R.style.dark);
        }
    }

    public static int getDialogTheme() {
        // Light theme dialogs don't work on Android Api < 11
        if (Settings.isLightSkin() && VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            return R.style.popup_light;
        }
        return R.style.popup_dark;
    }

    /**
     * Show a long toast message to the user. This can be called from any thread.
     *
     * @param activity the activity the user is facing
     * @param resId the message
     */
    public static void showToast(final Activity activity, final int resId) {
        ActivityMixin.showToast(activity, activity.getString(resId));
    }

    private static void postShowToast(final Activity activity, final String text, final int toastDuration) {
        if (StringUtils.isNotBlank(text)) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final Toast toast = Toast.makeText(activity, text, toastDuration);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
                    toast.show();
                }
            });
        }
    }

    /**
     * Show a long toast message to the user. This can be called from any thread.
     *
     * @param activity the activity the user is facing
     * @param text the message
     */
    public static void showToast(final Activity activity, final String text) {
        postShowToast(activity, text, Toast.LENGTH_LONG);
    }

    /**
     * Show a short toast message to the user. This can be called from any thread.
     *
     * @param activity the activity the user is facing
     * @param text the message
     */
    public static void showShortToast(final Activity activity, final String text) {
        postShowToast(activity, text, Toast.LENGTH_SHORT);
    }

    public static void onCreate(final Activity abstractActivity, final boolean keepScreenOn) {
        final Window window = abstractActivity.getWindow();
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (Settings.useHardwareAcceleration()) {
            enableHardwareAcceleration(window);
        }
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    private static void enableHardwareAcceleration(final Window window) {
        window.addFlags(LayoutParams.FLAG_HARDWARE_ACCELERATED);
    }

    public static void invalidateOptionsMenu(final Activity activity) {
        if (activity instanceof ActionBarActivity) {
            ((ActionBarActivity) activity).supportInvalidateOptionsMenu();
        }
        else {
            ActivityCompat.invalidateOptionsMenu(activity);
        }
    }

    /**
     * insert text into the EditText at the current cursor position
     *
     * @param editText
     * @param insertText
     * @param moveCursor
     *            place the cursor after the inserted text
     */
    public static void insertAtPosition(final EditText editText, final String insertText, final boolean moveCursor) {
        final int selectionStart = editText.getSelectionStart();
        final int selectionEnd = editText.getSelectionEnd();
        final int start = Math.min(selectionStart, selectionEnd);
        final int end = Math.max(selectionStart, selectionEnd);

        final String content = editText.getText().toString();
        String completeText;
        if (start > 0 && !Character.isWhitespace(content.charAt(start - 1))) {
            completeText = " " + insertText;
        } else {
            completeText = insertText;
        }

        editText.getText().replace(start, end, completeText);
        final int newCursor = moveCursor ? start + completeText.length() : start;
        editText.setSelection(newCursor);
    }

    public static boolean navigateUp(@NonNull final Activity activity) {
        // see http://developer.android.com/training/implementing-navigation/ancestral.html
        final Intent upIntent = NavUtils.getParentActivityIntent(activity);
        if (upIntent == null) {
            activity.finish();
            return true;
        }
        if (NavUtils.shouldUpRecreateTask(activity, upIntent)) {
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

    public static void presentShowcase(final IAbstractActivity activity) {
        if (VERSION.SDK_INT < 11) {
            return;
        }
        final ShowcaseViewBuilder builder = activity.getShowcase();
        if (builder != null) {
            builder.setStyle(R.style.ShowcaseView);
            builder.build();
        }
    }
}
