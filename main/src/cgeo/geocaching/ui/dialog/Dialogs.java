package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.CgeoApplication;

import org.eclipse.jdt.annotation.Nullable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;

/**
 * Wrapper for {@link AlertDialog}. If you want to show a simple text, use one of the
 * {@link #message(Activity, String, String, Drawable)} variants. If you want the user to confirm using Okay/Cancel or
 * Yes/No, select one of the {@link #confirm(Activity, String, String, String, OnClickListener)} or
 * {@link #confirmYesNo(Activity, String, String, OnClickListener)} variants.
 *
 */
public final class Dialogs {
    private Dialogs() {
        // utility class
    }

    /**
     * Confirm using two buttons "yourText" and "Cancel", where "Cancel" just closes the dialog.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param positiveButton
     *            text of the positive button (which would typically be the OK button)
     * @param okayListener
     *            listener of the positive button
     */
    public static AlertDialog.Builder confirm(final Activity context, final String title, final String msg, final String positiveButton, final OnClickListener okayListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.setTitle(title)
                .setCancelable(true)
                .setMessage(msg)
                .setPositiveButton(positiveButton, okayListener)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOwnerActivity(context);
        dialog.show();
        return builder;
    }

    /**
     * Confirm using two buttons "yourText" and "Cancel", where "Cancel" just closes the dialog.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param positiveButton
     *            text of the positive button (which would typically be the OK button)
     * @param okayListener
     *            listener of the positive button
     */
    public static AlertDialog.Builder confirm(final Activity context, final int title, final int msg, final int positiveButton, final OnClickListener okayListener) {
        return confirm(context, getString(title), getString(msg), getString(positiveButton), okayListener);
    }

    /**
     * Confirm using two buttons "Yes" and "No", where "No" just closes the dialog.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param yesListener
     *            listener of the positive button
     */
    public static AlertDialog.Builder confirmYesNo(final Activity context, final String title, final String msg, final OnClickListener yesListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.setTitle(title)
                .setCancelable(true)
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, yesListener)
                .setNegativeButton(android.R.string.no, null)
                .create();
        dialog.setOwnerActivity(context);
        dialog.show();
        return builder;
    }

    /**
     * Confirm using two buttons "Yes" and "No", where "No" just closes the dialog.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param yesListener
     *            listener of the positive button
     */
    public static AlertDialog.Builder confirmYesNo(final Activity context, final String title, final int msg, final OnClickListener yesListener) {
        return confirmYesNo(context, title, getString(msg), yesListener);
    }

    /**
     * Confirm using two buttons "Yes" and "No", where "No" just closes the dialog.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param yesListener
     *            listener of the positive button
     */
    public static AlertDialog.Builder confirmYesNo(final Activity context, final int title, final String msg, final OnClickListener yesListener) {
        return confirmYesNo(context, getString(title), msg, yesListener);
    }

    /**
     * Confirm using two buttons "Yes" and "No", where "No" just closes the dialog.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param yesListener
     *            listener of the positive button
     */
    public static AlertDialog.Builder confirmYesNo(final Activity context, final int title, final int msg, final OnClickListener yesListener) {
        return confirmYesNo(context, getString(title), getString(msg), yesListener);
    }

    /**
     * Confirm using two buttons "OK" and "Cancel", where "Cancel" just closes the dialog.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param okayListener
     *            listener of the positive button
     */
    public static AlertDialog.Builder confirm(final Activity context, final String title, final String msg, final OnClickListener okayListener) {
        return confirm(context, title, msg, getString(android.R.string.ok), okayListener);
    }

    /**
     * Confirm using two buttons "OK" and "Cancel", where "Cancel" just closes the dialog.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param okayListener
     *            listener of the positive button
     */
    public static AlertDialog.Builder confirm(final Activity context, final int title, final String msg, final OnClickListener okayListener) {
        return confirm(context, getString(title), msg, okayListener);
    }

    /**
     * Confirm using two buttons "OK" and "Cancel", where "Cancel" just closes the dialog.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param okayListener
     *            listener of the positive button
     */
    public static AlertDialog.Builder confirm(final Activity context, final int title, final int msg, final OnClickListener okayListener) {
        return confirm(context, getString(title), getString(msg), okayListener);
    }

    private static String getString(int resourceId) {
        return CgeoApplication.getInstance().getString(resourceId);
    }

    /**
     * Show a message dialog with a single "OK" button.
     *
     * @param context
     *            activity owning the dialog
     * @param message
     *            message dialog content
     */
    public static void message(final Activity context, final String message) {
        message(context, null, message);
    }

    /**
     * Show a message dialog with a single "OK" button.
     *
     * @param context
     *            activity owning the dialog
     * @param title
     *            message dialog title
     * @param message
     *            message dialog content
     */
    public static void message(final Activity context, final @Nullable String title, final String message) {
        message(context, title, message, null);
    }

    /**
     * Show a message dialog with a single "OK" button and an icon.
     *
     * @param context
     *            activity owning the dialog
     * @param title
     *            message dialog title
     * @param message
     *            message dialog content
     * @param icon
     *            message dialog title icon
     */
    public static void message(final Activity context, final @Nullable String title, final String message, final @Nullable Drawable icon) {
        Builder builder = new AlertDialog.Builder(context)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(getString(android.R.string.ok), null);
        if (title != null) {
            builder.setTitle(title);
        }
        if (icon != null) {
            builder.setIcon(icon);
        }
        builder.create().show();
    }

    /**
     * Show a message dialog with a single "OK" button and an icon.
     *
     * @param context
     *            activity owning the dialog
     * @param title
     *            message dialog title
     * @param message
     *            message dialog content
     */
    public static void message(final Activity context, final int title, final String message) {
        message(context, getString(title), message);
    }

    /**
     * Show a message dialog with a single "OK" button and an icon.
     *
     * @param context
     *            activity owning the dialog
     * @param title
     *            message dialog title
     * @param message
     *            message dialog content
     */
    public static void message(final Activity context, final int title, final int message) {
        message(context, getString(title), getString(message));
    }

    /**
     * Show a message dialog with a single "OK" button and an icon.
     *
     * @param context
     *            activity owning the dialog
     * @param title
     *            message dialog title
     * @param message
     *            message dialog content
     * @param icon
     *            message dialog title icon
     */
    public static void message(final Activity context, final int title, final int message, final @Nullable Drawable icon) {
        message(context, getString(title), getString(message), icon);
    }

}
