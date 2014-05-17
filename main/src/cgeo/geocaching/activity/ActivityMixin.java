package cgeo.geocaching.activity;

import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.settings.Settings;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public final class ActivityMixin {

    public static void setTitle(final Activity activity, final CharSequence text) {
        if (StringUtils.isBlank(text)) {
            return;
        }

        if  (((ActionBarActivity) activity).getSupportActionBar() != null) {
                ((ActionBarActivity) activity).getSupportActionBar().setTitle(text);
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
        // The compat theme should fix this
        if (Settings.isLightSkin()) {
            return R.style.popup_light;
        }
        return R.style.popup_dark;
    }

    public static void showToast(final Activity activity, final int resId) {
        ActivityMixin.showToast(activity, activity.getString(resId));
    }

    public static void showToast(final Activity activity, final String text) {
        if (StringUtils.isNotBlank(text)) {
            Toast toast = Toast.makeText(activity, text, Toast.LENGTH_LONG);

            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
            toast.show();
        }
    }

    public static void showShortToast(final Activity activity, final String text) {
        if (StringUtils.isNotBlank(text)) {
            Toast toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT);

            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
            toast.show();
        }
    }

    public static void keepScreenOn(final Activity abstractActivity, boolean keepScreenOn) {
        if (keepScreenOn) {
            abstractActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public static void invalidateOptionsMenu(Activity activity) {
        Compatibility.invalidateOptionsMenu(activity);
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
        int selectionStart = editText.getSelectionStart();
        int selectionEnd = editText.getSelectionEnd();
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);

        final String content = editText.getText().toString();
        String completeText;
        if (start > 0 && !Character.isWhitespace(content.charAt(start - 1))) {
            completeText = " " + insertText;
        } else {
            completeText = insertText;
        }

        editText.getText().replace(start, end, completeText);
        int newCursor = moveCursor ? start + completeText.length() : start;
        editText.setSelection(newCursor);
    }

    public static void navigateToMain(Activity activity) {
        final Intent main = new Intent(activity, MainActivity.class);
        NavUtils.navigateUpTo(activity, main);
    }
}
