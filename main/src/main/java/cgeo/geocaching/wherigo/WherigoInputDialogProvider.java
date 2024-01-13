package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingDetailsBinding;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.utils.CommonUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import static android.view.View.VISIBLE;

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

        binding.actions.buttonNegative.setVisibility(View.GONE);
        binding.actions.buttonNeutral.setVisibility(View.GONE);

        binding.actions.buttonPositive.setVisibility(VISIBLE);
        binding.actions.buttonPositive.setText("ok");

        final String type = (String) input.rawget("InputType");

        if ("Text".equals(type)) {
            binding.dialogInputLayout.setVisibility(VISIBLE);
            binding.actions.buttonPositive.setOnClickListener(v -> {
                Engine.callEvent(input, "OnGetInput", binding.dialogInputEdittext.getText().toString());
                dialog.dismiss();
            });
        } else if ("MultipleChoice".equals(type)) {
            final LuaTable choicesTable = (LuaTable) input.table.rawget("Choices");
            final String[] choices = new String[choicesTable.len()];
            for (int i = 0; i < choicesTable.len(); i++) {
                choices[i] = (String) choicesTable.rawget((double) (i + 1));
                if (choices[i] == null) {
                    choices[i] = "-";
                }
            }

            binding.dialogItemlistview.setVisibility(VISIBLE);

            final SimpleItemListModel<String> choiceModel = new SimpleItemListModel<>();
            choiceModel.setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO);
            binding.dialogItemlistview.setModel(choiceModel);
            binding.actions.buttonPositive.setOnClickListener(v -> {
                final String item = CommonUtils.first(choiceModel.getSelectedItems());
                Engine.callEvent(input, "OnGetInput", item);
                dialog.dismiss();
            });
        } else {
            binding.actions.buttonPositive.setOnClickListener(v -> {
                Engine.callEvent(input, "OnGetInput", null);
                dialog.dismiss();
            });

        }
        return dialog;

    }

}
