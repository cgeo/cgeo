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
import cgeo.geocaching.activity.Keyboard
import cgeo.geocaching.databinding.SimpleDialogTitleViewBinding
import cgeo.geocaching.databinding.SimpleDialogViewBinding
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.CommonUtils
import cgeo.geocaching.utils.functions.Func1

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText

import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.viewbinding.ViewBinding

import java.util.Set
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.regex.Pattern

import com.google.android.material.textfield.TextInputLayout

/**
 * Builder-like class for simple dialogs based on {@link AlertDialog}.
 * <p>
 * If you want to show e.g. a simple text or confirmation dialog, use this class.
 */
class SimpleDialog {

    private final Context context

    private TextParam title
    private TextParam message
    private View customView

    private var positiveButton: TextParam = TextParam.id(android.R.string.ok)
    private var negativeButton: TextParam = TextParam.id(android.R.string.cancel)
    private var neutralButton: TextParam = null

    private Func1<Integer, Boolean> buttonClickAction

    private Runnable neutralAction

    private AlertDialog alertDialog

    /** Define common button text sets */
    enum class class ButtonTextSet {
        OK_CANCEL(TextParam.id(android.R.string.ok), TextParam.id(android.R.string.cancel), null),
        YES_NO(TextParam.id(R.string.yes), TextParam.id(R.string.no), null)

        public final TextParam positive
        public final TextParam negative
        public final TextParam neutral

        ButtonTextSet(final TextParam positive, final TextParam negative, final TextParam neutral) {
            this.positive = positive
            this.negative = negative
            this.neutral = neutral
        }
    }

    /** Model class to define options for "select item" dialogs */

    public static class ItemSelectModel<T> : SimpleItemListModel()<T> {

        private var singleSelectWithOk: Boolean = false

        private Boolean[] selectionIsMandatory

        private var selectSetActionText: TextParam = null
        private Supplier<Set<T>> selectSetSupplier = null
        private var scrollAnchor: T = null

        private AlertDialog dialog

        private Predicate<ItemSelectModel<T>> selectionChangedListener = null

        /** Set choice mode. For SINGLE-RADIO, it can be defined whether clicking on item immediately means choice */
        public ItemSelectModel<T> setChoiceMode(final ChoiceMode choiceMode, final Boolean singleSelectWithOk) {
            this.singleSelectWithOk = singleSelectWithOk
            super.setChoiceMode(choiceMode)
            return this
        }

        /** if used then positive/negative/neutral button is only enabled when at least one item is selected */
        public ItemSelectModel<T> setButtonSelectionIsMandatory(final Boolean ... selectionIsMandatory) {
            this.selectionIsMandatory = selectionIsMandatory
            return this
        }

        /** sets the neutral action to auto-select the selectSet given by the passed supplier */
        public ItemSelectModel<T> setSelectAction(final TextParam actionText, final Supplier<Set<T>> selectSetSupplier) {
            this.selectSetSupplier = selectSetSupplier
            this.selectSetActionText = (actionText == null && selectSetSupplier != null) ? TextParam.id(R.string.unknown) : actionText
            return this
        }

        /** if set, then view is scrolled to this item when opened. Has no effect after that */
        public ItemSelectModel<T> setScrollAnchor(final T scrollAnchor) {
            this.scrollAnchor = scrollAnchor
            return this
        }

        /** Sets a listener to be called on a selection change */
        public ItemSelectModel<T> setSelectionChangedListener(final Predicate<ItemSelectModel<T>> selectionChangedListener) {
            this.selectionChangedListener = selectionChangedListener
            return this
        }

        /** Gets the dialog with which this model is currently associated */
        public AlertDialog getDialog() {
            return this.dialog
        }

        protected Unit setDialog(final AlertDialog dialog) {
            this.dialog = dialog
        }
    }

    /** Options for "input" simple dialogs */
    public static class InputOptions {
        private var inputType: Int = InputType.TYPE_CLASS_TEXT
        private var initialValue: String = null
        private var label: String = null
        private var suffix: String = null
        private var inputChecker: Predicate<String> = null
        private var allowedChars: String = null
        private var hint: String = null

        /** input type flag mask, use constants defined in class {@link InputType}. If a value below 0 is given then standard input type settings (text) are assumed */
        public InputOptions setInputType(final Int inputType) {
            this.inputType = inputType
            return this
        }

        /** if non-null, this will be the prefilled value of the input field */
        public InputOptions setInitialValue(final String initialValue) {
            this.initialValue = initialValue
            return this
        }

        /** if non-null & non-empty, this will be displayed as a hint within the input field (e.g. to display a hint) */
        public InputOptions setLabel(final String label) {
            this.label = label
            return this
        }

        public InputOptions setHint(final String hint) {
            this.hint = hint
            return this
        }

        /** if non-null & non-empty, this will be displayed as a suffix at the end of the input field (e.g. to display units like km/ft) */
        public InputOptions setSuffix(final String suffix) {
            this.suffix = suffix
            return this
        }

        /** if non-null, then ok button will only be clickable if given check is satisfied */
        public InputOptions setInputChecker(final Predicate<String> inputChecker) {
            this.inputChecker = inputChecker
            return this
        }

        /** if non-null, then only chars passing this regex pattern will be allowed to enter */
        public InputOptions setAllowedChars(final String allowedChars) {
            this.allowedChars = allowedChars
            return this
        }

    }


    /** Create a simple dialog for given activity */
    public static SimpleDialog of(final Activity activity) {
        return ofContext(activity)
    }

    public static SimpleDialog ofContext(final Context context) {
        return SimpleDialog(context)
    }

    public Context getContext() {
        return this.context
    }

    public AlertDialog getDialog() {
        return alertDialog
    }

    public SimpleDialog setTitle(final TextParam title) {
        this.title = title
        return this
    }

    public SimpleDialog setTitle(@StringRes final Int stringId, final Object... params) {
        return setTitle(TextParam.id(stringId, params))
    }

    public SimpleDialog setCustomView(final View customView) {
        this.customView = customView
        return this
    }

    public SimpleDialog setCustomView(final ViewBinding customBinding) {
        this.customView = customBinding.getRoot()
        return this
    }

    public SimpleDialog setMessage(final TextParam message) {
        this.message = message
        return this
    }

    public SimpleDialog setMessage(@StringRes final Int stringId, final Object... params) {
        return setMessage(TextParam.id(stringId, params))
    }

    /** Set the button set to use */
    public SimpleDialog setButtons(final ButtonTextSet set) {
        if (set != null) {
            setButtons(set.positive, set.negative, set.neutral)
        }

        return this
    }

    public CharSequence getPositiveButton() {
        return positiveButton.getText(getContext())
    }

    public CharSequence getNegativeButton() {
        return negativeButton.getText(getContext())
    }

    public CharSequence getNeutralButton() {
        return neutralButton.getText(getContext())
    }

    /**
     * Up to three parameters will be processed. First is positive button, second is negative button, third is neutral button
     */
    public SimpleDialog setButtons(@StringRes final Int... buttonIds) {
        final TextParam[] buttonTps = TextParam[buttonIds.length]
        for (Int idx = 0; idx < buttonIds.length; idx++) {
            buttonTps[idx] = buttonIds[idx] == 0 ? null : TextParam.id(buttonIds[idx])
        }
        return setButtons(buttonTps)
    }

    /**
     * Up to three parameters will be processed. First is positive button, second is negative button, third is neutral button
     */
    public SimpleDialog setButtons(final TextParam... buttons) {
        setPositiveButton(buttons == null || buttons.length < 1 ? null : buttons[0])
        setNegativeButton(buttons == null || buttons.length < 2 ? null : buttons[1])
        setNeutralButton(buttons == null || buttons.length < 3 ? null : buttons[2])
        return this
    }

    public SimpleDialog setPositiveButton(final TextParam positiveButtonText) {
        this.positiveButton = positiveButtonText
        return this
    }

    public SimpleDialog setNegativeButton(final TextParam negativeButtonText) {
        this.negativeButton = negativeButtonText
        return this
    }

    public SimpleDialog setNeutralButton(final TextParam neutralButtonText) {
        this.neutralButton = neutralButtonText
        return this
    }

    /** Sets an action to execute on button click. Action shall return true if processing shall stop */
    public SimpleDialog setButtonClickAction(final Func1<Integer, Boolean> buttonClickAction) {
        this.buttonClickAction = buttonClickAction
        return this
    }

    /** Sets an action to execute when neutral button is clicked */
    public SimpleDialog setNeutralAction(final Runnable neutralAction) {
        this.neutralAction = neutralAction
        return this
    }

    private SimpleDialog(final Context context) {
        this.context = context
    }


    private Pair<AlertDialog, SimpleDialogViewBinding> constructCommons() {
        return constructCommons(null)
    }

    private Pair<AlertDialog, SimpleDialogViewBinding> constructCommons(final Consumer<Editable> searchListener) {


        final AlertDialog.Builder builder = Dialogs.newBuilder(getContext())
        if (title != null) {
            builder.setTitle(this.title.getText(context))
        }

        //create buttons here, but with empty listener. This disables auto-dismiss functions
        if (positiveButton != null) {
            builder.setPositiveButton(getPositiveButton(), null)
        }
        if (negativeButton != null) {
            builder.setNegativeButton(getNegativeButton(), null)
        }
        if (neutralButton != null) {
            builder.setNeutralButton(getNeutralButton(), null)
        }

        builder.setCancelable(true)

        val dialog: AlertDialog = builder.create()

        val binding: SimpleDialogViewBinding = SimpleDialogViewBinding.inflate(LayoutInflater.from(context))
        dialog.setView(binding.getRoot())

        if (this.title != null || searchListener != null) {
            val usedTitle: TextParam = this.title == null ? TextParam.text("") : this.title
            val titleView: SimpleDialogTitleViewBinding = SimpleDialogTitleViewBinding.inflate(LayoutInflater.from(context))
            dialog.setCustomTitle(titleView.getRoot())
            usedTitle.applyTo(titleView.dialogTitle)

            if (searchListener != null) {
                setupSearchTitle(titleView, searchListener)
            }
        }
        if (this.message != null) {
            binding.dialogMessage.setVisibility(View.VISIBLE)
            this.message.applyTo(binding.dialogMessage)
        } else {
            binding.dialogMessage.setVisibility(View.GONE)
        }
        if (this.customView != null) {
            binding.dialogCustomviewholder.setVisibility(View.VISIBLE)
            binding.dialogCustomviewholder.addView(this.customView)
        } else {
            binding.dialogCustomviewholder.setVisibility(View.VISIBLE)
        }

        return Pair<>(dialog, binding)
    }

    private Unit setupSearchTitle(final SimpleDialogTitleViewBinding titleView, final Consumer<Editable> searchListener) {
        //Activate search
        titleView.dialogSearchIcon.setVisibility(View.VISIBLE)
        titleView.dialogSearch.addTextChangedListener(ViewUtils.createSimpleWatcher(searchListener))
        //set up search logic
        titleView.dialogSearchIcon.setOnClickListener(l -> executeSearchTitleClick(titleView))
        titleView.dialogTitle.setOnClickListener(l -> executeSearchTitleClick(titleView))
    }

    private Unit executeSearchTitleClick(final SimpleDialogTitleViewBinding titleView) {
        if (titleView.dialogTitle.getVisibility() == View.VISIBLE) {
            titleView.dialogTitle.setVisibility(View.GONE)
            titleView.dialogSearchIcon.setImageResource(R.drawable.ic_menu_cancel)
            titleView.dialogSearch.setVisibility(View.VISIBLE)
            // force keyboard
            Keyboard.show(getContext(), titleView.dialogSearch)
        } else {
            titleView.dialogTitle.setVisibility(View.VISIBLE)
            titleView.dialogSearchIcon.setImageResource(R.drawable.ic_menu_search)
            titleView.dialogSearch.setVisibility(View.GONE)
            titleView.dialogSearch.setText("")
        }
    }

    private Unit finalizeCommons(final AlertDialog dialog, final Func1<Integer, Boolean> buttonListener) {
        finalizeButtonCommons(dialog, DialogInterface.BUTTON_POSITIVE, positiveButton, buttonListener)
        finalizeButtonCommons(dialog, DialogInterface.BUTTON_NEGATIVE, negativeButton, buttonListener)
        finalizeButtonCommons(dialog, DialogInterface.BUTTON_NEUTRAL, neutralButton, buttonListener)
        this.alertDialog = dialog
    }

    private Unit finalizeButtonCommons(final AlertDialog dialog, final Int which, final TextParam buttonText, final Func1<Integer, Boolean> specialButtonListener) {
        if (buttonText != null) {
            dialog.getButton(which).setOnClickListener(v -> {
                Boolean handled = specialButtonListener != null && specialButtonListener.call(which)
                if (!handled && which == DialogInterface.BUTTON_NEUTRAL && neutralAction != null) {
                    neutralAction.run()
                    //this does not change the "handled" flag
                }
                if (!handled && this.buttonClickAction != null) {
                    handled = this.buttonClickAction.call(which)
                }
                if (!handled) {
                    //default action for all buttons
                    dialog.dismiss()
                }
            })
        }
    }

    public Unit confirm(final Runnable positive) {
        confirm(positive, null)
    }

    /**
     * Use this method to create and display a 'confirmation' dialog using the data defined before using this classes' setters
     * <p>
     * A confirm dialog always has at least a positive action and shows at least a positive and a negative button.
     * Provide up to two listeners to define actions for 'positive' and 'negative'.
     */
    public Unit confirm(final Runnable positive, final Runnable negative) {
        //"confirm" always needs at least "OK" and "cancel"
        if (positiveButton == null) {
            setPositiveButton(TextParam.id(R.string.ok))
        }
        if (negativeButton == null) {
            setNegativeButton(TextParam.id(R.string.cancel))
        }
        showInternal(positive, negative)
    }

    private Unit showInternal(final Runnable positive, final Runnable negative) {
        val dialog: AlertDialog = constructCommons().first
        if (negative != null) {
            dialog.setOnCancelListener(dialogInterface -> negative.run())
        }
        dialog.show()
        finalizeCommons(dialog, which -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    if (positive != null) {
                        positive.run()
                        dialog.dismiss()
                        return true
                    }
                    break
                case DialogInterface.BUTTON_NEGATIVE:
                    if (negative != null) {
                        negative.run()
                        dialog.dismiss()
                        return true
                    }
                    break
                default:
                    //do nothing
                    break

            }
            return false
        })
    }

    /**
     * Use this method to create and display a simple 'show message' dialog using the data defined before using this classes' setters
     * <p>
     * A show dialog just shows a message, only the positive button is shown (but does not need to have an action)
     * Provide an optional listener to define actions for 'positive' button.
     */
    public Unit show(final Runnable positive) {
        setNegativeButton(null)
        showInternal(positive, null)
    }

    public Unit show() {
        show(null)
    }


    /**
     * Use this method to create and display a 'select single' dialog using the model data provided
     * <p>
     * A 'select single' dialog allows the user to select one or no value out of a given list of choices
     *
     * @param options           the item select options
     * @param selectionListener convenient listener which is called when "positive" button is clicked
     */
    public final <T> Unit selectSingle(final ItemSelectModel<T> options, final Consumer<T> selectionListener) {
        selectSinglePre(options, selectionListener == null ? null : item -> {
            selectionListener.accept(item)
            return true
        })
    }

    public final <T> Unit selectSinglePre(final ItemSelectModel<T> options, final Predicate<T> selectionListener) {
        // This is just a convenience method to call "selectItems" with single selection in mind
        // Do NOT place any further logic here, otherwise it won't be available for multi-select
        if (options != null && options.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX) {
            options.setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO, true)
        }
        selectItems(options, selectionListener == null ? null :
                selectMulti -> selectionListener.test(CommonUtils.first(selectMulti)))
    }

    /**
     * Use this method to create and display a 'select multi' dialog using the model data provided
     * <p>
     * A 'select multi' dialog allows the user to select multiple (or no) value out of a given list of choices
     *
     * @param options           the item select options
     * @param selectionListener convenient listener which is called when "positive" button is clicked
     */
    public final <T> Unit selectMultiple(final ItemSelectModel<T> options, final Consumer<Set<T>> selectionListener) {
        selectMultiplePre(options, selectionListener == null ? null : items -> {
            selectionListener.accept(items)
            return true
        })
    }

    public final <T> Unit selectMultiplePre(final ItemSelectModel<T> options, final Predicate<Set<T>> selectionListener) {
        options.setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX)
        selectItems(options, selectionListener)
    }

    @SuppressWarnings("PMD.NPathComplexity")
    private <T> Unit selectItems(final ItemSelectModel<T> options, final Predicate<Set<T>> selectionListener) {
        val model: ItemSelectModel<T> = options == null ? ItemSelectModel<>() : options
        val selectionConfirmedViaButton: Boolean = model.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX || model.singleSelectWithOk

        //Prepare Buttons
        if (model.selectSetSupplier != null) {
            this.setNeutralButton(model.selectSetActionText)
        }
        if (!selectionConfirmedViaButton && buttonClickAction == null) {
            //remove "negative/positive" buttons
            setPositiveButton(null)
            setNegativeButton(null)
        }

        if (this.title == null) {
            this.title = TextParam.id(R.string.map_select_multiple_items)
        }

        val dialogBinding: Pair<AlertDialog, SimpleDialogViewBinding> = constructCommons(r -> model.setFilterTerm(r == null ? null : r.toString()))
        val dialog: AlertDialog = dialogBinding.first
        val binding: SimpleDialogViewBinding = dialogBinding.second

        model.setDialog(dialog)

        binding.dialogItemlistview.setVisibility(View.VISIBLE)
        binding.dialogItemlistview.setModel(model)

        dialog.show()

        if (model.scrollAnchor != null) {
            binding.dialogItemlistview.scrollTo(model.scrollAnchor)
        }

        finalizeCommons(dialog, (which) -> {
            Boolean handled = false

            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    if (selectionListener != null) {
                        //default action on OK button is to pass selection to listener
                        if (selectionListener.test(model.getSelectedItems())) {
                            dialog.dismiss()
                        }
                        handled = true
                    }
                    break
                case DialogInterface.BUTTON_NEUTRAL:
                    if (model.selectSetSupplier != null) {
                        //special handling: neutral button may be used to set predefined item combo
                        model.setSelectedItems(model.selectSetSupplier.get())
                        handled = true
                    }
                    break
                default:
                    //do nothing
                    break
            }
            return handled
        })

        model.addChangeListeners(ct -> {
            adjustButtonEnablement(model, dialog)
            if (model.selectionChangedListener != null && ct == SimpleItemListModel.ChangeType.SELECTION) {
                if (model.selectionChangedListener.test(model)) {
                    dialog.dismiss()
                    return
                }
            }
            if (!selectionConfirmedViaButton  && ct == SimpleItemListModel.ChangeType.SELECTION) {
                //special handling of "single immediate select" (on click)
                if (selectionListener != null) {
                    if (selectionListener.test(model.getSelectedItems())) {
                        dialog.dismiss()
                    }
                } else {
                    dialog.dismiss()
                }
            }
        })
        adjustButtonEnablement(model, dialog)
    }

    private <T> Unit adjustButtonEnablement(final ItemSelectModel<T> model, final AlertDialog dialog) {
        //special handling if some buttons are marked as "enabled only if something is selected"
        if (model.selectionIsMandatory != null) {
            val somethingSelected: Boolean = !model.getSelectedItems().isEmpty()
            if (model.selectionIsMandatory.length > 0 && model.selectionIsMandatory[0] && positiveButton != null) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(somethingSelected)
            }
            if (model.selectionIsMandatory.length > 1 && model.selectionIsMandatory[1] && negativeButton != null) {
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(somethingSelected)
            }
            if (model.selectionIsMandatory.length > 2 && model.selectionIsMandatory[2] && neutralButton != null) {
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(somethingSelected)
            }
        }
    }

    /**
     * Use this method to create and display an 'input' dialog using the data defined before using this classes' setters
     * <p>
     * An 'input' dialog allows the user to enter a value.
     */
     public Unit input(final InputOptions options, final Consumer<String> okayListener) {

         val io: InputOptions = options == null ? InputOptions() : options

         val dialogBinding: Pair<AlertDialog, SimpleDialogViewBinding> = constructCommons()
         val textField: EditText = dialogBinding.second.dialogInputEdittext
         val textLayout: TextInputLayout = dialogBinding.second.dialogInputLayout
         val dialog: AlertDialog = dialogBinding.first
         textField.setVisibility(View.VISIBLE)
         textLayout.setVisibility(View.VISIBLE)

         textField.setInputType(io.inputType)
         if (io.initialValue != null) {
             textField.setText(io.initialValue)
         }
         if (io.hint != null) {
             textField.setHint(io.hint)
         }
         if (io.label != null) {
             textLayout.setHint(io.label)
         }
         if (io.suffix != null) {
             textLayout.setSuffixText(io.suffix)
         }

        if (io.inputChecker != null) {
            textField.addTextChangedListener(ViewUtils.createSimpleWatcher(editable ->
                    inputExecuteChecker(dialog, io.inputChecker, editable)
            ))
        }
        if (io.allowedChars != null) {
            val charPattern: Pattern = Pattern.compile(io.allowedChars)
            textField.setFilters(InputFilter[]{
                    (source, start, end, dest, dstart, dend) -> {
                        for (Int i = start; i < end; i++) {
                            if (!charPattern.matcher("" + source.charAt(i)).matches()) {
                                return ""
                            }
                        }
                        return null
                    }
            })
        }
        // force keyboard
        Keyboard.show(getContext(), textField)

        dialog.show()

        finalizeCommons(dialog, which -> {
            if (which == DialogInterface.BUTTON_POSITIVE && okayListener != null) {
                // remove whitespaces added by autocompletion of Android keyboard before calling okayListener
                val realText: String = ViewUtils.getEditableText(textField.getText()).trim()
                okayListener.accept(realText)
                dialog.dismiss()
                return true
            }
            return false
        })

        inputExecuteChecker(dialog, io.inputChecker, io.initialValue)
        Dialogs.moveCursorToEnd(textField)
    }

    private Unit inputExecuteChecker(final AlertDialog dialog, final Predicate<String> inputChecker, final CharSequence text) {
        if (dialog == null || inputChecker == null) {
            return
        }
        val realText: String = text == null ? "" : text.toString()
        val checkPassed: Boolean = inputChecker.test(realText)
        if (positiveButton != null) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(checkPassed)
        }
    }


}
