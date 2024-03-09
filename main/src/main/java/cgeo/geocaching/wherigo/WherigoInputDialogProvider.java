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
import static android.view.View.VISIBLE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;
import se.krka.kahlua.vm.LuaTable;

public class WherigoInputDialogProvider implements IWherigoDialogProvider {

    private final EventTable input;
    private WherigoThingDetailsBinding binding;


    WherigoInputDialogProvider(final EventTable input) {
        this.input = input;
    }

    @Override
    public Dialog createDialog(final Activity activity) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreenDialog);
        binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = builder.create();
        dialog.setView(binding.getRoot());
        binding.layoutDetailsTextViewName.setText("Title");
        binding.layoutDetailsTextViewDescription.setText((String) input.table.rawget("Text"));

        binding.media.setMedia((Media) input.table.rawget("Media"));

        final String type = (String) input.rawget("InputType");
        boolean handled = false;

        if ("Text".equals(type)) {
            binding.dialogInputLayout.setVisibility(VISIBLE);
            WherigoUtils.setViewActions(Collections.singleton("ok"), binding.dialogActionlist, item -> WherigoUtils.TP_OK_BUTTON, item -> {
                WherigoDialogManager.get().clear();
                Engine.callEvent(input, "OnGetInput", binding.dialogInputEdittext.getText().toString());
            });
            EditUtils.setActionListener(binding.dialogInputEdittext, () -> {
                WherigoDialogManager.get().clear();
                Engine.callEvent(input, "OnGetInput", binding.dialogInputEdittext.getText().toString());
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
                    .setDisplayMapper(TextParam::text)
                    .setSelectedItems(Collections.singleton(choices.get(0)))
                    .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO);
                binding.dialogItemlistview.setModel(choiceModel);

                WherigoUtils.setViewActions(Collections.singleton("ok"), binding.dialogActionlist, item -> WherigoUtils.TP_OK_BUTTON, item -> {
                    WherigoDialogManager.get().clear();
                    Engine.callEvent(input, "OnGetInput", CommonUtils.first(choiceModel.getSelectedItems()));
                });
                handled = true;
            }

        }

        if (!handled) {
            WherigoUtils.setViewActions(Collections.singleton("ok"), binding.dialogActionlist, item -> WherigoUtils.TP_OK_BUTTON, item -> {
                WherigoDialogManager.get().clear();
                Engine.callEvent(input, "OnGetInput", null);
            });
        }
        return dialog;

    }

}
