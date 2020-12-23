package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.lang3.StringUtils;

/**
 * Wrapper for {@link AlertDialog}. If you want to show a simple text, use one of the
 * {@link #message(Activity, String, String)} variants. If you want the user to confirm using Okay/Cancel or
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
        final AlertDialog.Builder builder = newBuilder(context);
        final AlertDialog dialog = builder.setTitle(title)
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
     * Confirm using two buttons "yourText" and "Cancel", where both buttons have action listener.
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
     * @param cancelListener
     *            listener of the negative button
     */
    public static AlertDialog.Builder confirm(final Activity context, final String title, final String msg, final String positiveButton, final OnClickListener okayListener, final DialogInterface.OnCancelListener cancelListener) {
        final AlertDialog.Builder builder = newBuilder(context);
        final AlertDialog dialog = builder.setTitle(title)
                .setCancelable(true)
                .setOnCancelListener(cancelListener)
                .setMessage(msg)
                .setPositiveButton(positiveButton, okayListener)
                .setNegativeButton(android.R.string.cancel, (dialog1, which) -> dialog1.cancel())
                .create();
        dialog.setOwnerActivity(context);
        dialog.show();
        return builder;
    }

    /**
     * Confirm using two buttons "yourText" and "Cancel", where both buttons have action listener.
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
     * @param cancelListener
     *            listener of the negative button
     */
    public static AlertDialog.Builder confirm(final Activity context, final int title, final int msg, final int positiveButton, final OnClickListener okayListener, final DialogInterface.OnCancelListener cancelListener) {
        return confirm(context, getString(title), getString(msg), getString(positiveButton), okayListener, cancelListener);
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
        final AlertDialog.Builder builder = newBuilder(context);
        final AlertDialog dialog = builder.setTitle(title)
                .setCancelable(true)
                .setMessage(msg)
                .setPositiveButton(R.string.yes, yesListener)
                .setNegativeButton(R.string.no, null)
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

    /**
     * Confirm using up to three configurable buttons "Positive", "Negative" and "Neutral". Buttons text are configurable.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param positiveTextButton
     *            Text for the positive button
     * @param negativeTextButton
     *            Text for the negative button
     * @param neutralTextButton
     *            Text for the neutral button
     * @param positiveListener
     *            listener of the positive button
     * @param negativeListener
     *            listener of the negative button
     * @param neutralListener
     *            listener of the neutral button
     */
    public static AlertDialog.Builder confirmPositiveNegativeNeutral(final Activity context,
                                                                     final String title,
                                                                     final CharSequence msg,
                                                                     final String positiveTextButton,
                                                                     final String negativeTextButton,
                                                                     final String neutralTextButton,
                                                                     final OnClickListener positiveListener,
                                                                     final OnClickListener negativeListener,
                                                                     final OnClickListener neutralListener) {
        final AlertDialog.Builder builder = newBuilder(context)
            .setTitle(title)
            .setCancelable(true)
            .setMessage(msg);
        if (null != positiveTextButton) {
            builder.setPositiveButton(positiveTextButton, positiveListener);
        }
        if (null != negativeTextButton) {
            builder.setNegativeButton(negativeTextButton, negativeListener);
        }
        if (null != neutralTextButton) {
            builder.setNeutralButton(neutralTextButton, neutralListener);
        }
        final AlertDialog dialog = builder.create();
        dialog.setOwnerActivity(context);
        dialog.show();

        makeLinksClickable(dialog);

        return builder;
    }

    private static void makeLinksClickable(final AlertDialog dialog) {
        try {
            // Make the URLs in TextView clickable. Must be called after show()
            // Note: we do NOT use the "setView()" option of AlertDialog because this screws up the layout
            ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        }  catch (Exception e) {
            Log.d("Trying to make dialog Links clickable failed, will be ignored", e);
        }
    }

    /**
     * Confirm using three configurable buttons "Positive", "Negative" and "Neutral". Buttons text are configurable.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param positiveTextButton
     *            Text for the positive button
     * @param negativeTextButton
     *            Text for the negative button
     * @param neutralTextButton
     *            Text for the neutral button
     * @param positiveListener
     *            listener of the positive button
     * @param negativeListener
     *            listener of the negative button
     * @param neutralListener
     *            listener of the neutral button
     */
    public static AlertDialog.Builder confirmPositiveNegativeNeutral(final Activity context,
                                                                     final int title,
                                                                     final String msg,
                                                                     final int positiveTextButton,
                                                                     final int negativeTextButton,
                                                                     final int neutralTextButton,
                                                                     final OnClickListener positiveListener,
                                                                     final OnClickListener negativeListener,
                                                                     final OnClickListener neutralListener) {
        final AlertDialog.Builder builder = newBuilder(context);
        final AlertDialog dialog = builder.setTitle(title)
                .setCancelable(true)
                .setMessage(msg)
                .setPositiveButton(positiveTextButton, positiveListener)
                .setNegativeButton(negativeTextButton, negativeListener)
                .setNeutralButton(neutralTextButton, neutralListener)
                .create();
        dialog.setOwnerActivity(context);
        dialog.show();
        return builder;
    }

    private static String getString(@StringRes final int resourceId) {
        return CgeoApplication.getInstance().getString(resourceId);
    }

    /**
     * Show a message dialog with a single "OK" button.
     *
     * @param context
     *            activity owning the dialog
     * @param msg
     *            message dialog content
     * @param positiveButton
     *            label for positive button
     */
    public static AlertDialog.Builder message(final Activity context, final String title, final String msg, final String positiveButton, final OnClickListener okayListener) {
        final AlertDialog.Builder builder = newBuilder(context);
        final AlertDialog dialog = builder.setTitle(title)
                .setCancelable(true)
                .setMessage(msg)
                .setPositiveButton(positiveButton, okayListener)
                .create();
        dialog.setOwnerActivity(context);
        dialog.show();
        return builder;
    }

    /**
     * Show a message dialog with a single "OK" button.
     *
     * @param context
     *            activity owning the dialog
     * @param msg
     *            message dialog content
     * @param positiveButton
     *            label for positive button
     */
    public static AlertDialog.Builder message(final Activity context, final int title, final int msg, final int positiveButton, final OnClickListener okayListener) {
        return message(context, getString(title), getString(msg), getString(positiveButton), okayListener);
    }

    /**
     * Show a message dialog with a single "OK" button.
     *
     * @param context
     *            activity owning the dialog
     * @param msg
     *            message dialog content
     */
    public static AlertDialog.Builder message(final Activity context, final int title, final int msg, final OnClickListener okayListener) {
        return message(context, getString(title), getString(msg), getString(android.R.string.ok), okayListener);
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
     * @param message
     *            message dialog content
     */
    public static void message(final Activity context, final int message) {
        message(context, null, getString(message));
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
    public static void message(final Activity context, @Nullable final String title, final String message) {
        message(context, title, message, null);
    }

    /**
     * Show a message dialog with a single "OK" button and an eventual icon.
     *
     * @param context
     *            activity owning the dialog
     * @param title
     *            message dialog title
     * @param message
     *            message dialog content
     * @param iconObservable
     *            observable (may be <tt>null</tt>) containing the icon(s) to set
     */
    public static void message(final Activity context, @Nullable final String title, final String message, @Nullable final Observable<Drawable> iconObservable) {
        final AlertDialog.Builder builder = newBuilder(context)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(getString(android.R.string.ok), null);
        if (title != null) {
            builder.setTitle(title);
        }
        builder.setIcon(ImageUtils.getTransparent1x1Drawable(context.getResources()));

        final AlertDialog dialog = builder.create();
        if (iconObservable != null) {
            iconObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(dialog::setIcon);
        }
        dialog.show();
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
     * @param iconObservable
     *            message dialog title icon
     */
    public static void message(final Activity context, final int title, final int message, final Observable<Drawable> iconObservable) {
        message(context, getString(title), getString(message), iconObservable);
    }

    /**
     * Show a onetime message dialog with title icon, "OK" and an "don't shown again" checkbox.
     * The check if the message should be shown is not done by this function, so be aware when using it!
     *
     * @param context
     *            activity owning the dialog
     * @param title
     *            message dialog title
     * @param message
     *            message dialog content
     * @param dialogType
     *            the dialogs individual identification type
     * @param iconObservable
     *            observable (may be <tt>null</tt>) containing the icon(s) to set
     * @param cancellable
     *            if true, a cancel button will be displayed additionally
     * @param runAfterwards
     *            runnable (may be <tt>null</tt>) will be executed when ok button is clicked
     */
    public static void internalOneTimeMessage(final Activity context, @Nullable final String title, final String message, final OneTimeDialogs.DialogType dialogType, @Nullable final Observable<Drawable> iconObservable, final boolean cancellable, final Runnable runAfterwards) {
        final View content = context.getLayoutInflater().inflate(R.layout.dialog_text_checkbox, null);
        final CheckBox checkbox = (CheckBox) content.findViewById(R.id.check_box);
        final TextView textView = (TextView) content.findViewById(R.id.message);
        textView.setText(message);

        if (Settings.isLightSkin()) {
            textView.setTextColor(context.getResources().getColor(R.color.text_light));
            checkbox.setTextColor(context.getResources().getColor(R.color.text_light));
            final int[][] states = {{android.R.attr.state_checked}, {}};
            final int[] colors = {context.getResources().getColor(R.color.colorAccent), context.getResources().getColor(R.color.steel)};
            checkbox.setButtonTintList(new ColorStateList(states, colors));
        }

        final AlertDialog.Builder builder = newBuilder(context)
                .setView(content)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (checkbox.isChecked()) {
                        OneTimeDialogs.setStatus(dialogType, OneTimeDialogs.DialogStatus.DIALOG_HIDE);
                    }
                    if (runAfterwards != null) {
                        runAfterwards.run();
                    }
                });

        if (title != null) {
            builder.setTitle(title);
        }

        if (cancellable) {
            builder.setNeutralButton(android.R.string.cancel, null);
        }

        builder.setIcon(ImageUtils.getTransparent1x1Drawable(context.getResources()));

        final AlertDialog dialog = builder.create();
        dialog.setOwnerActivity(context);

        if (iconObservable != null) {
            iconObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(dialog::setIcon);
        }
        dialog.show();

        if (cancellable) {
            checkbox.setOnClickListener(result -> {
                final Button button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                if (checkbox.isChecked()) {
                    button.setEnabled(false);
                } else {
                    button.setEnabled(true);
                }
            });
        }
    }

    /**
     * Message dialog which is shown max one time each c:geo session, until "don't shown again" is checked.
     * Please define your dialog name/message strings at OneTimeDialogs.DialogType.
     *
     * @param context
     *            activity owning the dialog
     * @param dialogType
     *            for setting title and message of the dialog and for storing the dialog status in DB
     * @param fallbackStatus
     *            if no status is stored in the database use DIALOG_SHOW or DIALOG_HIDE as fallback
     */
    public static void basicOneTimeMessage(final Activity context, final OneTimeDialogs.DialogType dialogType, final OneTimeDialogs.DialogStatus fallbackStatus) {

        if (OneTimeDialogs.getStatus(dialogType, fallbackStatus) == OneTimeDialogs.DialogStatus.DIALOG_SHOW) {
            OneTimeDialogs.setStatus(dialogType, OneTimeDialogs.DialogStatus.DIALOG_HIDE, OneTimeDialogs.DialogStatus.DIALOG_SHOW);
            internalOneTimeMessage(context, getString(dialogType.messageTitle), getString(dialogType.messageText), dialogType, Observable.just(Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_info_blue, context.getTheme()))), false, null);
        }
    }

    /**
     * OK (+ cancel) dialog which is shown, until "don't shown again" is checked. Title, text, icon and runAfterwards can be set.
     * If "don't shown again" is selected for this dialog, runAfterwards will be executed directly.
     *
     * @param dialogType
     *            for setting title and message of the dialog and for storing the dialog status in DB
     * @param fallbackStatus
     *            if no status is stored in the database use DIALOG_SHOW or DIALOG_HIDE as fallback
     */
    public static void advancedOneTimeMessage(final Activity context, final OneTimeDialogs.DialogType dialogType, final OneTimeDialogs.DialogStatus fallbackStatus, final String title, final String message, final boolean cancellable, @Nullable final Observable<Drawable> iconObservable, final Runnable runAfterwards) {
        if (OneTimeDialogs.getStatus(dialogType, fallbackStatus) == OneTimeDialogs.DialogStatus.DIALOG_SHOW) {
            internalOneTimeMessage(context, title, message, dialogType, iconObservable, cancellable, runAfterwards);
        } else {
            runAfterwards.run();
        }
    }

    /**
     * Standard message box + additional neutral button.
     *  @param context
     *            activity hosting the dialog
     * @param msg
     *            dialog message
     * @param neutralTextButton
 *            Text for the neutral button
     * @param neutralListener
     *            listener for neutral button
     */
    public static void messageNeutral(final Context context, final String msg, final int neutralTextButton, final OnClickListener neutralListener) {
        messageNeutral(context, null, msg, neutralTextButton, neutralListener);
    }

    /**
     * Standard message box + additional neutral button.
     *
     * @param context
     *            activity hosting the dialog
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @param neutralTextButton
     *            Text for the neutral button
     * @param neutralListener
     *            listener of the neutral button
     */
    public static AlertDialog.Builder messageNeutral(final Context context, @Nullable final String title, final String msg, final int neutralTextButton, final OnClickListener neutralListener) {
        final AlertDialog.Builder builder = newBuilder(context);
        final AlertDialog dialog = builder.setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(neutralTextButton, neutralListener)
            .create();

        if (title != null) {
            dialog.setTitle(title);
        }

        dialog.show();
        return builder;
    }

    /**
     * Show a message dialog for input from the user. The okay button is only enabled on non empty input.
     *
     * @param context
     *            activity owning the dialog
     * @param title
     *            message dialog title
     * @param defaultValue
     *            default input value
     * @param buttonTitle
     *            title of the okay button
     * @param okayListener
     *            listener to be run on okay
     */
    public static void input(final Activity context, final int title, final String defaultValue, final int buttonTitle, final Action1<String> okayListener) {
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_CLASS_TEXT);
        input.setText(defaultValue);
        input.setTextColor(context.getResources().getColor(Settings.isLightSkin() ? R.color.text_light : R.color.text_dark));

        final AlertDialog.Builder builder = newBuilder(context);
        builder.setTitle(title);
        builder.setView(input);
        builder.setPositiveButton(buttonTitle, (dialog, which) -> okayListener.call(input.getText().toString()));
        builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> dialog.dismiss());
        final AlertDialog dialog = builder.create();

        input.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                // empty
            }

            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                // empty
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                enableDialogButtonIfNotEmpty(dialog, editable.toString());
            }
        });
        // force keyboard
        input.requestFocus();
        new Keyboard(context).showDelayed(input);

        // disable button
        dialog.show();
        enableDialogButtonIfNotEmpty(dialog, defaultValue);

        moveCursorToEnd(input);
    }

    /**
     * Move the cursor to the end of the input field.
     *
     */
    public static void moveCursorToEnd(final EditText input) {
        input.setSelection(input.getText().length(), input.getText().length());
    }

    private static void enableDialogButtonIfNotEmpty(final AlertDialog dialog, final String input) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(StringUtils.isNotBlank(input));
    }

    public interface ItemWithIcon {
        /**
         * @return the drawable resource, or {@code 0} for no drawable
         */
        @DrawableRes
        int getIcon();
    }

    public static <T extends ItemWithIcon> void select(final Activity activity, final String title, final List<T> items, final Action1<T> listener) {
        final ListAdapter adapter = new ArrayAdapter<T>(
                activity,
                android.R.layout.select_dialog_item,
                android.R.id.text1,
                items) {
            @Override
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                // standard list entry
                final View v = super.getView(position, convertView, parent);

                // add image
                final TextView tv = v.findViewById(android.R.id.text1);
                tv.setCompoundDrawablesWithIntrinsicBounds(items.get(position).getIcon(), 0, 0, 0);

                // Add margin between image and text
                final int dp5 = (int) (5 * activity.getResources().getDisplayMetrics().density + 0.5f);
                tv.setCompoundDrawablePadding(dp5);

                return v;
            }
        };

        newBuilder(activity)
                .setTitle(title)
                .setAdapter(adapter, (dialog, item) -> listener.call(items.get(item))).show();
    }

    public static void dismiss(@Nullable final ProgressDialog dialog) {
        if (dialog == null) {
            return;
        }
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public static void selectGlobalTypeFilter(final Activity activity, final Action1<CacheType> okayListener) {
        final List<CacheType> cacheTypes = new ArrayList<>();

        //first add the most used types
        cacheTypes.add(CacheType.ALL);
        cacheTypes.add(CacheType.TRADITIONAL);
        cacheTypes.add(CacheType.MULTI);
        cacheTypes.add(CacheType.MYSTERY);

        // then add all other cache types sorted alphabetically
        final List<CacheType> sorted = new ArrayList<>(Arrays.asList(CacheType.values()));
        sorted.removeAll(cacheTypes);
        Collections.sort(sorted, (left, right) -> TextUtils.COLLATOR.compare(left.getL10n(), right.getL10n()));
        cacheTypes.addAll(sorted);

        final int checkedItem = Math.max(0, cacheTypes.indexOf(Settings.getCacheType()));

        final String[] items = new String[cacheTypes.size()];
        for (int i = 0; i < cacheTypes.size(); i++) {
            items[i] = cacheTypes.get(i).getL10n();
        }

        final AlertDialog.Builder builder = newBuilder(activity);
        builder.setTitle(R.string.menu_filter);
        builder.setSingleChoiceItems(items, checkedItem, (dialog, position) -> {
            final CacheType cacheType = cacheTypes.get(position);
            Settings.setCacheType(cacheType);
            okayListener.call(cacheType);
            dialog.dismiss();
        });
        builder.create().show();
    }

    public static AlertDialog.Builder newBuilder(final Activity activity) {
        return new AlertDialog.Builder(new ContextThemeWrapper(activity, Settings.isLightSkin() ? R.style.Dialog_Alert_light : R.style.Dialog_Alert));
    }

    public static AlertDialog.Builder newBuilder(final Context context) {
        return new AlertDialog.Builder(new ContextThemeWrapper(context, Settings.isLightSkin() ? R.style.Dialog_Alert_light : R.style.Dialog_Alert));
    }
}
