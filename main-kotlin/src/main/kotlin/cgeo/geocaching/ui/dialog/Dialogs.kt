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

package cgeo.geocaching.ui.dialog

import cgeo.geocaching.R
import cgeo.geocaching.databinding.BottomsheetDialogWithActionbarBinding
import cgeo.geocaching.databinding.DialogTextCheckboxBinding
import cgeo.geocaching.storage.extension.OneTimeDialogs
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.functions.Action1

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.InputType
import android.util.Pair
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.Consumer

import java.util.Objects

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import org.apache.commons.lang3.StringUtils

/**
 * Helper class providing methods when constructing custom Dialogs.
 * <br>
 * To create simple dialogs, consider using {@link SimpleDialog} instead.
 */
class Dialogs {


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
    private static Unit internalOneTimeMessage(final Context context, final String title, final String message, final String moreInfoURL, final OneTimeDialogs.DialogType dialogType, final Observable<Drawable> iconObservable, final Boolean cancellable, final Runnable runAfterwards, final Boolean disableCancelOnDAMA) {
        val content: DialogTextCheckboxBinding = DialogTextCheckboxBinding.inflate(LayoutInflater.from(context))

        content.message.setText(message)

        /* @todo Check if any adaptions to Material theme necessary
        if (Settings.isLightSkin()) {
            final Int[][] states = {{android.R.attr.state_checked}, {}}
            final Int[] colors = {context.getResources().getColor(R.color.colorAccent), context.getResources().getColor(R.color.steel)}
            content.checkBox.setButtonTintList(ColorStateList(states, colors))
        }
        */

        final AlertDialog.Builder builder = newBuilder(context)
                .setView(content.getRoot())
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (content.checkBox.isChecked()) {
                        OneTimeDialogs.setStatus(dialogType, OneTimeDialogs.DialogStatus.DIALOG_HIDE)
                    }
                    if (runAfterwards != null) {
                        runAfterwards.run()
                    }
                })

        if (title != null) {
            builder.setTitle(title)
        }

        if (StringUtils.isNotBlank(moreInfoURL)) {
            builder.setNeutralButton(R.string.more_information, (dialog, which) -> ShareUtils.openUrl(context, moreInfoURL))
        }

        if (cancellable) {
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                // reachable only when disableCancelOnDAMA is set to false
                if (content.checkBox.isChecked()) {
                    OneTimeDialogs.setStatus(dialogType, OneTimeDialogs.DialogStatus.DIALOG_HIDE)
                }
            })
        }

        builder.setIcon(ImageUtils.getTransparent1x1Drawable(context.getResources()))

        val dialog: AlertDialog = builder.create()

        if (iconObservable != null) {
            iconObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(dialog::setIcon)
        }
        dialog.show()

        // disable cancel on "don't ask me again"?
        if (cancellable && disableCancelOnDAMA) {
            content.checkBox.setOnClickListener(result -> {
                val button: Button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                button.setEnabled(!content.checkBox.isChecked())
            })
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
    public static Unit basicOneTimeMessage(final Context context, final OneTimeDialogs.DialogType dialogType) {

        if (OneTimeDialogs.showDialog(dialogType)) {
            OneTimeDialogs.setStatus(dialogType, OneTimeDialogs.DialogStatus.DIALOG_HIDE, OneTimeDialogs.DialogStatus.DIALOG_SHOW)
            internalOneTimeMessage(context, LocalizationUtils.getString(dialogType.messageTitle), LocalizationUtils.getString(dialogType.messageText), dialogType.moreInfoURLResId > 0 ? LocalizationUtils.getString(dialogType.moreInfoURLResId) : null, dialogType,
                    Observable.just(Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_info_blue, context.getTheme()))), false, null, true)
        }
    }

    /**
     * Message dialog which is shown max one time each c:geo session, until "don't shown again" is checked.
     * Please define your dialog name/message strings at OneTimeDialogs.DialogType.
     * @param runOnOk gets started on closing the dialog with "ok".
     * Dialog can be cancelled even when "don't ask me again" checkbox is set
     */
    public static Unit basicOneTimeMessage(final Context context, final OneTimeDialogs.DialogType dialogType, final Runnable runOnOk) {

        if (OneTimeDialogs.showDialog(dialogType)) {
            OneTimeDialogs.setStatus(dialogType, OneTimeDialogs.DialogStatus.DIALOG_HIDE, OneTimeDialogs.DialogStatus.DIALOG_SHOW)
            internalOneTimeMessage(context, LocalizationUtils.getString(dialogType.messageTitle), LocalizationUtils.getString(dialogType.messageText), dialogType.moreInfoURLResId > 0 ? LocalizationUtils.getString(dialogType.moreInfoURLResId) : null, dialogType,
                    Observable.just(Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_info_blue, context.getTheme()))), true, runOnOk, false)
        }
    }


    /**
     * OK (+ cancel) dialog which is shown, until "don't shown again" is checked. Title, text, icon and runAfterwards can be set.
     * If "don't shown again" is selected for this dialog, runAfterwards will be executed directly.
     *
     * @param dialogType used for storing the dialog status in the DB, title and message defined in the dialogType are ignored
     */
    public static Unit advancedOneTimeMessage(final Context context, final OneTimeDialogs.DialogType dialogType, final String title, final String message, final String moreInfoURL, final Boolean cancellable, final Observable<Drawable> iconObservable, final Runnable runAfterwards) {
        if (OneTimeDialogs.showDialog(dialogType)) {
            internalOneTimeMessage(context, title, message, moreInfoURL, dialogType, iconObservable, cancellable, runAfterwards, true)
        } else if (runAfterwards != null) {
            runAfterwards.run()
        }
    }

    /**
     * Show dialog with title, message and checkbox
     * Checkbox value is returned to the listeners
     */
    public static Unit confirmWithCheckbox(final Context context, final String title, final String message, final CheckboxDialogConfig checkboxConfig, final Action1<Boolean> onConfirm, final Action1<Boolean> onCancel) {
        val content: View = LayoutInflater.from(context).inflate(R.layout.dialog_text_checkbox, null)
        val checkbox: CheckBox = content.findViewById(R.id.check_box)
        val textView: TextView = content.findViewById(R.id.message)
        textView.setText(message)
        checkbox.setText(checkboxConfig.getTextRes())
        checkbox.setChecked(checkboxConfig.isCheckedOnInit())
        checkbox.setVisibility(checkboxConfig.isVisible() ? View.VISIBLE : View.GONE)

        val yesNoLabel: Boolean = checkboxConfig.getActionButtonLabel() == CheckboxDialogConfig.ActionButtonLabel.YES_NO

        val alertDialog: AlertDialog = Dialogs.newBuilder(context)
                .setView(content)
                .setTitle(title)
                .setCancelable(true)
                .setPositiveButton(yesNoLabel ? R.string.yes : android.R.string.ok, (dialog, which) -> onConfirm.call(checkbox.isChecked()))
                .setNegativeButton(yesNoLabel ? R.string.no : android.R.string.cancel, (dialog2, which2) -> {
                    if (onCancel != null) {
                        onCancel.call(checkbox.isChecked())
                    } else {
                        dialog2.dismiss()
                    }
                })
                .create()
        alertDialog.show()

        val buttonPositive: Button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val buttonNegative: Button = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        buttonPositive.setEnabled(checkboxConfig.getPositiveCheckCondition().eval(checkbox.isChecked()))
        buttonNegative.setEnabled(checkboxConfig.getNegativeCheckCondition().eval(checkbox.isChecked()))

        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonPositive.setEnabled(checkboxConfig.getPositiveCheckCondition().eval(isChecked))
            buttonNegative.setEnabled(checkboxConfig.getNegativeCheckCondition().eval(isChecked))
        })

        if (context is Activity) {
            alertDialog.setOwnerActivity((Activity) context)
        }
    }


    /**
     * Move the cursor to the end of the input field.
     */
    public static Unit moveCursorToEnd(final EditText input) {
        input.setSelection(input.getText().length(), input.getText().length())
    }

    public static Unit dismiss(final ProgressDialog dialog) {
        if (dialog == null) {
            return
        }
        if (dialog.isShowing()) {
            dialog.dismiss()
        }
    }

    private static BottomSheetDialog bottomSheetDialog(final Context context, final View contentView) {
        val dialog: BottomSheetDialog = BottomSheetDialog(newContextThemeWrapper(context))
        dialog.setContentView(contentView)
        return dialog
    }

    private static Unit updateActionbarAfterStateChange(final BottomSheetDialog dialog, final BottomsheetDialogWithActionbarBinding dialogView) {
        if (dialog.getBehavior().getState() == BottomSheetBehavior.STATE_EXPANDED) {
            dialogView.toolbar.setNavigationIcon(R.drawable.ic_expand_more_white)
            dialogView.toolbar.setNavigationContentDescription(dialog.getContext().getString(R.string.close))
        } else {
            dialogView.toolbar.setNavigationIcon(R.drawable.ic_expand_less_white)
            dialogView.toolbar.setNavigationContentDescription(dialog.getContext().getString(R.string.expand))
        }
    }

    /**
     * create a bottom sheet dialog with action bar
     */
    public static BottomSheetDialog bottomSheetDialogWithActionbar(final Context context, final View contentView, final @StringRes Int titleResId) {
        return bottomSheetDialogWithActionbar(context, contentView, LocalizationUtils.getString(titleResId))
    }

    public static BottomSheetDialog bottomSheetDialogWithActionbar(final Context context, final View contentView, final CharSequence title) {

        val dialogView: BottomsheetDialogWithActionbarBinding = BottomsheetDialogWithActionbarBinding.inflate(LayoutInflater.from(newContextThemeWrapper(context)))
        val dialog: BottomSheetDialog = bottomSheetDialog(context, dialogView.getRoot())

        dialogView.toolbar.setTitle(title)
        dialogView.contentWrapper.addView(contentView)

        dialogView.toolbar.setNavigationOnClickListener(v -> {
            if (dialog.getBehavior().getState() == BottomSheetBehavior.STATE_EXPANDED) {
                dialog.dismiss()
            } else {
                dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED)
            }
        })

        dialog.setOnShowListener(dialog1 -> {
            // provide rather much information directly without need to scroll up
            // this will expand the dialog even when being in landscape mode
            // and will set to expanded the state automatically if the dialog can't be expanded even further
            dialog.getBehavior().setHalfExpandedRatio(0.8f)
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_HALF_EXPANDED)
            dialog.getBehavior().setSkipCollapsed(true)

            updateActionbarAfterStateChange(dialog, dialogView)
        })

        dialog.getBehavior().addBottomSheetCallback(BottomSheetBehavior.BottomSheetCallback() {
            private var closeOnRelease: Boolean = false

            override             public Unit onStateChanged(final View bottomSheet, final Int newState) {
                if (newState != BottomSheetBehavior.STATE_DRAGGING && closeOnRelease) {
                    dialog.dismiss()
                } else {
                    updateActionbarAfterStateChange(dialog, dialogView)
                }
            }

            override             public Unit onSlide(final View bottomSheet, final Float slideOffset) {
                // make closing the dialog easier
                closeOnRelease = slideOffset < -0.2
            }
        })

        return dialog
    }

    /**
     * displays an input dialog (one or multiple lines)
     * <br>
     * Short form of input(), with default parameters for {@link InputType}, minLines and maxLines
     */
    public static Unit input(final Activity activity, final String title, final String currentValue, final String label, final Consumer<String> callback) {
        input(activity, title, currentValue, label, -1, 1, 1, callback)
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
    public static Unit input(final Activity activity, final String title, final String currentValue, final String label, final Int inputType, final Int minLines, final Int maxLines, final Consumer<String> callback) {
        val textField: Pair<View, EditText> = ViewUtils.createTextField(activity, currentValue, TextParam.text(label), null, inputType, minLines, maxLines)
        newBuilder(activity)
                .setView(textField.first)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> callback.accept(textField.second.getText().toString().trim()))
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> {
                })
                .show()
    }

    public static AlertDialog.Builder newBuilder(final Context context) {
        return MaterialAlertDialogBuilder(newContextThemeWrapper(context))
    }

    public static AlertDialog.Builder newBuilder(final Context context, final @StyleRes Int resId) {
        return MaterialAlertDialogBuilder(newContextThemeWrapper(context), resId)
    }

    public static ContextThemeWrapper newContextThemeWrapper(final Context context) {
        return ContextThemeWrapper(context, R.style.cgeo)
    }

    /** checks a Float value and restricts it to given bounds, emitting a Short warning message if necessary */
    public static Float checkInputRange(final Context context, final Float currentValue, final Float minValue, final Float maxValue) {
        Float newValue = currentValue
        if (newValue > maxValue) {
            newValue = maxValue
            ViewUtils.showShortToast(context, R.string.number_input_err_boundarymax)
        }
        if (newValue < minValue) {
            newValue = minValue
            ViewUtils.showShortToast(context, R.string.number_input_err_boundarymin)
        }
        return newValue
    }
}
