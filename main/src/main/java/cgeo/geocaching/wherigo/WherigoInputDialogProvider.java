package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.databinding.WherigoThingDetailsBinding;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.EditUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;
import se.krka.kahlua.vm.LuaTable;

/** Handles Wherigo/OpenWIG input dialogs */
public class WherigoInputDialogProvider implements IWherigoDialogProvider {

    private final EventTable input;
    private WherigoThingDetailsBinding binding;


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
        this.input = input;
    }

    @Override
    public Dialog createDialog(final Activity activity, final Consumer<Boolean> resultSetter) {
        resultSetter.accept(false);

        final WherigoGame game = WherigoGame.get();

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreenDialog);
        binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = builder.create();
        dialog.setView(binding.getRoot());
        binding.description.setText(game.toDisplayText((String) input.table.rawget("Text")));

        binding.media.setMedia((Media) input.table.rawget("Media"));
        binding.debugBox.setVisibility(game.isDebugModeForCartridge() ? VISIBLE : GONE);
        if (game.isDebugModeForCartridge()) {
            binding.debugInfo.setText("Wherigo Input Dialog");
        }

        final String type = (String) input.rawget("InputType");
        boolean handled = false;

        if ("Text".equals(type)) {
            binding.dialogInputLayout.setVisibility(VISIBLE);
            WherigoUtils.setViewActions(Arrays.asList(Boolean.TRUE, Boolean.FALSE),
                    binding.dialogActionlist, item -> item ? WherigoUtils.TP_OK_BUTTON : WherigoUtils.TP_PAUSE_BUTTON, item -> {
                if (item) {
                    resultSetter.accept(true);
                    Engine.callEvent(input, "OnGetInput", String.valueOf(binding.dialogInputEdittext.getText()));
                }
                WherigoDialogManager.dismissDialog(dialog);
            });
            EditUtils.setActionListener(binding.dialogInputEdittext, () -> {
                resultSetter.accept(true);
                WherigoDialogManager.dismissDialog(dialog);
                Engine.callEvent(input, "OnGetInput", String.valueOf(binding.dialogInputEdittext.getText()));
            });
            Keyboard.show(activity, binding.dialogInputEdittext);

            handled = true;
        }
        if ("MultipleChoice".equals(type)) {
            final LuaTable choicesTable = (LuaTable) input.table.rawget("Choices");
            final List<String> choices = new ArrayList<>(choicesTable.len());
            for (int i = 0; i < choicesTable.len(); i++) {
                String choice = (String) choicesTable.rawget((double) (i + 1));
                if (choice == null) {
                    choice = "-";
                }
                choices.add(choice);
            }

            if (!choices.isEmpty()) {

                binding.dialogItemlistview.setVisibility(VISIBLE);

                final SimpleItemListModel<String> choiceModel = new SimpleItemListModel<>();
                choiceModel
                    .setItems(choices)
                    .setDisplayMapper(s -> TextParam.text(game.toDisplayText(s)))
                    .setSelectedItems(Collections.singleton(choices.get(0)))
                    .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO);
                binding.dialogItemlistview.setModel(choiceModel);

                WherigoUtils.setViewActions(Arrays.asList(Boolean.TRUE, Boolean.FALSE), binding.dialogActionlist,
                    item -> item ? WherigoUtils.TP_OK_BUTTON : WherigoUtils.TP_PAUSE_BUTTON, item -> {
                        if (item) {
                            resultSetter.accept(true);
                            Engine.callEvent(input, "OnGetInput", CommonUtils.first(choiceModel.getSelectedItems()));
                        }
                        WherigoDialogManager.dismissDialog(dialog);
                });
                handled = true;
            }

        }

        if (!handled) {
            WherigoUtils.setViewActions(Collections.singleton("ok"), binding.dialogActionlist, item -> WherigoUtils.TP_OK_BUTTON, item -> {
                resultSetter.accept(true);
                WherigoDialogManager.dismissDialog(dialog);
                Engine.callEvent(input, "OnGetInput", null);
            });
        }
        return dialog;

    }

}
