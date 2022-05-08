package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.BottomsheetDialogWithActionbarBinding;
import cgeo.geocaching.databinding.DialogTextCheckboxBinding;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func1;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Consumer;

import java.util.List;
import java.util.Objects;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class providing methods when constructing custom Dialogs.
 *
 * To create simple dialogs, consider using {@link SimpleDialog} instead.
 */
public final class Dialogs {


    private Dialogs() {
        // utility class
    }

    /**
     * Show a onetime message dialog with title icon, "OK" and an "don't shown again" checkbox.
     * The check if the message should be shown is not done by this function, so be aware when using it!
     *
     * @param context        activity owning the dialog
     * @param title          message dialog title
     * @param message        message dialog content
     * @param dialogType     the dialogs individual identification type
     * @param iconObservable observable (may be <tt>null</tt>) containing the icon(s) to set
     * @param cancellable    if true, a cancel button will be displayed additionally
     * @param runAfterwards  runnable (may be <tt>null</tt>) will be executed when ok button is clicked
     */
    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private static void internalOneTimeMessage(@NonNull final Context context, @Nullable final String title, final String message, @Nullable final String moreInfoURL, final OneTimeDialogs.DialogType dialogType, @Nullable final Observable<Drawable> iconObservable, final boolean cancellable, final Runnable runAfterwards) {
        final DialogTextCheckboxBinding content = DialogTextCheckboxBinding.inflate(LayoutInflater.from(context));

        content.message.setText(message);

        /* @todo Check if any adaptions to Material theme necessary
        if (Settings.isLightSkin()) {
            final int[][] states = {{android.R.attr.state_checked}, {}};
            final int[] colors = {context.getResources().getColor(R.color.colorAccent), context.getResources().getColor(R.color.steel)};
            content.checkBox.setButtonTintList(new ColorStateList(states, colors));
        }
        */

        final AlertDialog.Builder builder = newBuilder(context)
                .setView(content.getRoot())
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (content.checkBox.isChecked()) {
                        OneTimeDialogs.setStatus(dialogType, OneTimeDialogs.DialogStatus.DIALOG_HIDE);
                    }
                    if (runAfterwards != null) {
                        runAfterwards.run();
                    }
                });

        if (title != null) {
            builder.setTitle(title);
        }

        if (StringUtils.isNotBlank(moreInfoURL)) {
            builder.setNeutralButton(R.string.more_information, (dialog, which) -> ShareUtils.openUrl(context, moreInfoURL));
        }

        if (cancellable) {
            builder.setNegativeButton(android.R.string.cancel, null);
        }

        builder.setIcon(ImageUtils.getTransparent1x1Drawable(context.getResources()));

        final AlertDialog dialog = builder.create();

        if (iconObservable != null) {
            iconObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(dialog::setIcon);
        }
        dialog.show();

        if (cancellable) {
            content.checkBox.setOnClickListener(result -> {
                final Button button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                button.setEnabled(!content.checkBox.isChecked());
            });
        }
    }

    /**
     * Message dialog which is shown max one time each c:geo session, until "don't shown again" is checked.
     * Please define your dialog name/message strings at OneTimeDialogs.DialogType.
     *
     * @param context    activity owning the dialog
     * @param dialogType sets title and message of the dialog
     *                   used for storing the dialog status in the DB
     */
    public static void basicOneTimeMessage(@NonNull final Context context, final OneTimeDialogs.DialogType dialogType) {

        if (OneTimeDialogs.showDialog(dialogType)) {
            OneTimeDialogs.setStatus(dialogType, OneTimeDialogs.DialogStatus.DIALOG_HIDE, OneTimeDialogs.DialogStatus.DIALOG_SHOW);
            internalOneTimeMessage(context, LocalizationUtils.getString(dialogType.messageTitle), LocalizationUtils.getString(dialogType.messageText), dialogType.moreInfoURLResId > 0 ? LocalizationUtils.getString(dialogType.moreInfoURLResId) : null, dialogType,
                    Observable.just(Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_info_blue, context.getTheme()))), false, null);
        }
    }


    /**
     * OK (+ cancel) dialog which is shown, until "don't shown again" is checked. Title, text, icon and runAfterwards can be set.
     * If "don't shown again" is selected for this dialog, runAfterwards will be executed directly.
     *
     * @param dialogType used for storing the dialog status in the DB, title and message defined in the dialogType are ignored
     */
    public static void advancedOneTimeMessage(final Context context, final OneTimeDialogs.DialogType dialogType, final String title, final String message, final String moreInfoURL, final boolean cancellable, @Nullable final Observable<Drawable> iconObservable, @Nullable final Runnable runAfterwards) {
        if (OneTimeDialogs.showDialog(dialogType)) {
            internalOneTimeMessage(context, title, message, moreInfoURL, dialogType, iconObservable, cancellable, runAfterwards);
        } else if (runAfterwards != null) {
            runAfterwards.run();
        }
    }

    /**
     * Show dialog with title, message and checkbox
     * Checkbox value is returned to the listeners
     */
    public static void confirmWithCheckbox(final Context context, final String title, final String message, final CheckboxDialogConfig checkboxConfig, final Action1<Boolean> onConfirm, @Nullable final Action1<Boolean> onCancel) {
        final View content = LayoutInflater.from(context).inflate(R.layout.dialog_text_checkbox, null);
        final CheckBox checkbox = (CheckBox) content.findViewById(R.id.check_box);
        final TextView textView = (TextView) content.findViewById(R.id.message);
        textView.setText(message);
        checkbox.setText(checkboxConfig.getTextRes());
        checkbox.setChecked(checkboxConfig.isCheckedOnInit());
        checkbox.setVisibility(checkboxConfig.isVisible() ? View.VISIBLE : View.GONE);

        final boolean yesNoLabel = checkboxConfig.getActionButtonLabel() == CheckboxDialogConfig.ActionButtonLabel.YES_NO;

        final AlertDialog alertDialog = Dialogs.newBuilder(context)
                .setView(content)
                .setTitle(title)
                .setCancelable(true)
                .setPositiveButton(yesNoLabel ? R.string.yes : android.R.string.ok, (dialog, which) -> onConfirm.call(checkbox.isChecked()))
                .setNegativeButton(yesNoLabel ? R.string.no : android.R.string.cancel, (dialog2, which2) -> {
                    if (onCancel != null) {
                        onCancel.call(checkbox.isChecked());
                    } else {
                        dialog2.dismiss();
                    }
                })
                .create();
        alertDialog.show();

        final Button buttonPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        final Button buttonNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        buttonPositive.setEnabled(checkboxConfig.getPositiveCheckCondition().eval(checkbox.isChecked()));
        buttonNegative.setEnabled(checkboxConfig.getNegativeCheckCondition().eval(checkbox.isChecked()));

        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonPositive.setEnabled(checkboxConfig.getPositiveCheckCondition().eval(isChecked));
            buttonNegative.setEnabled(checkboxConfig.getNegativeCheckCondition().eval(isChecked));
        });

        if (context instanceof Activity) {
            alertDialog.setOwnerActivity((Activity) context);
        }
    }

    /**
     * Show a select dialog with additional delete item button
     */
    public static <T> void selectItemDialogWithAdditionalDeleteButton(final Context context, final @StringRes int title, @NonNull final List<T> items, @NotNull final Func1<T, TextParam> displayMapper, final Action1<T> onSelectListener, final Action1<T> onDeleteListener) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(context).setTitle(title);
        final AlertDialog[] dialog = new AlertDialog[]{null};

        final ListAdapter adapter = new ArrayAdapter<T>(context, R.layout.select_or_delete_item, android.R.id.text1, items) {
            public View getView(final int position, final View convertView, final ViewGroup parent) {
                //Use super class to create the View.
                final View v = super.getView(position, convertView, parent);

                // set text
                final TextView tv = v.findViewById(android.R.id.text1);
                displayMapper.call(getItem(position)).applyTo(tv);

                // register delete listener
                final View deleteButton = v.findViewById(R.id.delete);
                deleteButton.setOnClickListener(v1 -> {
                    onDeleteListener.call(items.get(position));

                    // dismiss dialog
                    if (dialog[0] != null) {
                        dialog[0].dismiss();
                    }
                });
                return v;
            }
        };

        builder.setSingleChoiceItems(adapter, -1, (d, pos) -> {
            d.dismiss();
            onSelectListener.call(items.get(pos));
        });

        dialog[0] = builder.create();
        dialog[0].show();

    }


    /**
     * Move the cursor to the end of the input field.
     */
    public static void moveCursorToEnd(final EditText input) {
        input.setSelection(input.getText().length(), input.getText().length());
    }

    public static void dismiss(@Nullable final ProgressDialog dialog) {
        if (dialog == null) {
            return;
        }
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private static BottomSheetDialog bottomSheetDialog(final Context context, final View contentView) {
        final BottomSheetDialog dialog = new BottomSheetDialog(newContextThemeWrapper(context));
        dialog.setContentView(contentView);
        return dialog;
    }

    private static void updateActionbarAfterStateChange(final BottomSheetDialog dialog, final BottomsheetDialogWithActionbarBinding dialogView) {
        if (dialog.getBehavior().getState() == BottomSheetBehavior.STATE_EXPANDED) {
            dialogView.toolbar.setNavigationIcon(R.drawable.ic_expand_more_white);
        } else {
            dialogView.toolbar.setNavigationIcon(R.drawable.ic_expand_less_white);
        }
    }

    /**
     * create a bottom sheet dialog with action bar
     */
    public static BottomSheetDialog bottomSheetDialogWithActionbar(final Context context, final View contentView, final @StringRes int titleResId) {
        final BottomsheetDialogWithActionbarBinding dialogView = BottomsheetDialogWithActionbarBinding.inflate(LayoutInflater.from(newContextThemeWrapper(context)));
        final BottomSheetDialog dialog = bottomSheetDialog(context, dialogView.getRoot());

        dialogView.toolbar.setTitle(titleResId);
        dialogView.contentWrapper.addView(contentView);

        dialogView.toolbar.setNavigationOnClickListener(v -> {
            if (dialog.getBehavior().getState() == BottomSheetBehavior.STATE_EXPANDED) {
                dialog.dismiss();
            } else {
                dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        dialog.setOnShowListener(dialog1 -> {
            // provide rather much information directly without need to scroll up
            // this will expand the dialog even when being in landscape mode
            // and will set to expanded the state automatically if the dialog can't be expanded even further
            dialog.getBehavior().setHalfExpandedRatio(0.8f);
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
            dialog.getBehavior().setSkipCollapsed(true);

            updateActionbarAfterStateChange(dialog, dialogView);
        });

        dialog.getBehavior().addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            private boolean closeOnRelease = false;

            @Override
            public void onStateChanged(final @NonNull View bottomSheet, final int newState) {
                if (newState != BottomSheetBehavior.STATE_DRAGGING && closeOnRelease) {
                    dialog.dismiss();
                } else {
                    updateActionbarAfterStateChange(dialog, dialogView);
                }
            }

            @Override
            public void onSlide(final @NonNull View bottomSheet, final float slideOffset) {
                // make closing the dialog easier
                closeOnRelease = slideOffset < -0.2;
            }
        });

        return dialog;
    }

    /**
     * displays an input dialog (one or multiple lines)
     *
     * short form of input(), with default parameters for {@link InputType}, minLines and maxLines
     */
    public static void input(final Activity activity, final String title, final String currentValue, final String label, final Consumer<String> callback) {
        input(activity, title, currentValue, label, -1, 1, 1, callback);
    }

    /**
     * displays an input dialog (one or multiple lines)
     *
     * @param activity     calling activity
     * @param title        dialog title
     * @param currentValue if non-null, this will be the prefilled value of the input field
     * @param label        label / hint for the input field
     * @param inputType    input type flag mask, use constants defined in class {@link InputType}. If a value below 0 is given then standard input type settings (text) are assumed
     * @param minLines     minimum amount of input lines to display
     * @param maxLines     maximum amount of input lines to display (make dialog scrollable, if > 1)
     * @param callback     method to call on positive confirmation, gets current text as parameter
     */
    public static void input(final Activity activity, final String title, final String currentValue, final String label, final int inputType, final int minLines, final int maxLines, final Consumer<String> callback) {
        final Pair<View, EditText> textField = ViewUtils.createTextField(activity, currentValue, TextParam.text(label), null, inputType, minLines, maxLines);
        newBuilder(activity)
                .setView(textField.first)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> callback.accept(textField.second.getText().toString().trim()))
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
                })
                .show();
    }

    public static AlertDialog.Builder newBuilder(final Context context) {
        return new MaterialAlertDialogBuilder(newContextThemeWrapper(context));
    }

    public static AlertDialog.Builder newBuilder(final Context context, final @StyleRes int resId) {
        return new MaterialAlertDialogBuilder(newContextThemeWrapper(context), resId);
    }

    public static ContextThemeWrapper newContextThemeWrapper(final Context context) {
        return new ContextThemeWrapper(context, R.style.cgeo);
    }
}
