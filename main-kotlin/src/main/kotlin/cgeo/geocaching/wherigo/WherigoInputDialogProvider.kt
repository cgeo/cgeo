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

package cgeo.geocaching.wherigo

import cgeo.geocaching.R
import cgeo.geocaching.activity.Keyboard
import cgeo.geocaching.databinding.WherigoThingDetailsBinding
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.utils.CommonUtils
import cgeo.geocaching.utils.EditUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.TranslationUtils
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.openwig.Engine
import cgeo.geocaching.wherigo.openwig.EventTable
import cgeo.geocaching.wherigo.openwig.Media

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.List

/** Handles Wherigo/OpenWIG input dialogs */
class WherigoInputDialogProvider : IWherigoDialogProvider {

    private final EventTable input

    /**
     * Handles Wherigo/OpenWIG Input Dialogs. The following is copied from OpenWIG code for reference
     * <p>
     * Request an input from the user.
     * <p>
     * If another dialog or input is open, it should be closed before displaying this input.
     * <p>
     * The <code>input</code> table must contain a "Type" field,
     * which can be either "Text" (then the UI should offer an one-line text input),
     * or "MultipleChoice". In that case, "Choices" field holds
     * another Lua table with list of strings representing the individual choices.
     * UI can then offer either a button for each choice, or some other
     * method of choosing one answer (such as combo box, radio buttons).
     * <p>
     * "Text" field holds a text of this query - this should be displayed above the
     * input field or the choices. "Media" field holds the associated <code>Media</code>.
     * <p>
     * This EventTable has an event "OnGetInput". When the input is processed, this
     * event should be called with a string parameter - either text of the selected choice,
     * or text from the input line. If the input is closed by another API call, the event
     * should be called with null parameter.
     * @param input Lua table describing the input parameters
     */
    WherigoInputDialogProvider(final EventTable input) {
        this.input = input
    }

    override     public Dialog createAndShowDialog(final Activity activity, final IWherigoDialogControl control) {
        control.setPauseOnDismiss(true)

        val game: WherigoGame = WherigoGame.get()

        val binding: WherigoThingDetailsBinding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity))
        val dialog: AlertDialog = WherigoViewUtils.createFullscreenDialog(activity, LocalizationUtils.getString(R.string.wherigo_player), binding.getRoot())

        //external translator
        TranslationUtils.registerTranslation(activity, binding.translationExternal, () ->
            TranslationUtils.prepareForTranslation(binding.description.getText()))

        binding.media.setMedia((Media) input.table.rawget("Media"))
        val descr: Object = input.table.rawget("Text")
        binding.description.setText(WherigoGame.get().toDisplayText(descr == null ? "" : descr.toString()))

        binding.debugBox.setVisibility(game.isDebugModeForCartridge() ? VISIBLE : GONE)
        if (game.isDebugModeForCartridge()) {
            //noinspection SetTextI18n (debug info only)
            binding.debugInfo.setText("Wherigo Input Dialog")
        }

        val type: String = (String) input.rawget("InputType")
        Boolean handled = false
        val choiceModel: SimpleItemListModel<String> = SimpleItemListModel<>()

        if ("Text" == (type)) {
            binding.dialogInputLayout.setVisibility(VISIBLE)
            WherigoViewUtils.setViewActions(Arrays.asList(Boolean.FALSE, Boolean.TRUE),
                    binding.dialogActionlist, 2, item -> item ? WherigoUtils.TP_OK_BUTTON : WherigoUtils.TP_CANCEL_BUTTON, item -> {
                if (item) {
                    control.setPauseOnDismiss(false)
                    control.dismiss()
                    Engine.callEvent(input, "OnGetInput", String.valueOf(binding.dialogInputEdittext.getText()))
                    WherigoSaveFileHandler.get().markSafeWorthyAction()
                } else {
                    control.dismiss()
                }
            })
            EditUtils.setActionListener(binding.dialogInputEdittext, () -> {
                control.setPauseOnDismiss(false)
                control.dismiss()
                Engine.callEvent(input, "OnGetInput", String.valueOf(binding.dialogInputEdittext.getText()))
                WherigoSaveFileHandler.get().markSafeWorthyAction()
            })
            Keyboard.show(activity, binding.dialogInputEdittext)

            handled = true
        }
        if ("MultipleChoice" == (type)) {
            val choicesTable: LuaTable = (LuaTable) input.table.rawget("Choices")
            val choices: List<String> = ArrayList<>(choicesTable.len())
            for (Int i = 0; i < choicesTable.len(); i++) {
                val choiceRaw: String = (String) choicesTable.rawget((Double) (i + 1))
                val choice: String = choiceRaw == null ? "-" : choiceRaw
                choices.add(choice)
            }

            if (!choices.isEmpty()) {

                binding.dialogItemlistview.setVisibility(VISIBLE)
                choiceModel
                    .setItems(choices)
                    .setDisplayMapper(s -> TextParam.text(game.toDisplayText(s)))
                    .setSelectedItems(Collections.singleton(choices.get(0)))
                    .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO)
                binding.dialogItemlistview.setModel(choiceModel)

                WherigoViewUtils.setViewActions(Arrays.asList(Boolean.FALSE, Boolean.TRUE), binding.dialogActionlist, 2,
                    item -> item ? WherigoUtils.TP_OK_BUTTON : WherigoUtils.TP_CANCEL_BUTTON, item -> {
                        if (item) {
                            control.setPauseOnDismiss(false)
                            control.dismiss()
                            WherigoSaveFileHandler.get().markSafeWorthyAction()
                            Engine.callEvent(input, "OnGetInput", CommonUtils.first(choiceModel.getSelectedItems()))
                        } else {
                            control.dismiss()
                        }
                })
                handled = true
            }

        }

        if (!handled) {
            WherigoViewUtils.setViewActions(Collections.singleton("ok"), binding.dialogActionlist, 1, item -> WherigoUtils.TP_OK_BUTTON, item -> {
                control.setPauseOnDismiss(false)
                control.dismiss()
                WherigoSaveFileHandler.get().markSafeWorthyAction()
                Engine.callEvent(input, "OnGetInput", null)
            })
        }

        //retrigger choice paint on trasnlation events
        control.setOnGameNotificationListener((d, nt) -> choiceModel.triggerRepaint())

        dialog.show()
        return dialog
    }
}
