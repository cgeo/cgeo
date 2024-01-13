package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingDetailsBinding;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;

import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;

public class WherigoDetailDialogProvider implements IWherigoDialogProvider {

    private final EventTable eventTable;
    private WherigoThingDetailsBinding binding;

    public WherigoDetailDialogProvider(final EventTable et) {
        this.eventTable = et;
    }

    @Override
    public Dialog createDialog(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreenDialog);
        binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = builder.create();
        dialog.setView(binding.getRoot());
        binding.layoutDetailsTextViewDescription.setText(WherigoUtils.eventTableToString(eventTable, true));

        binding.media.setMedia((Media) eventTable.table.rawget("Media"));

        binding.actions.buttonPositive.setText("Close");
        binding.actions.buttonNegative.setVisibility(View.GONE);
        binding.actions.buttonNeutral.setVisibility(View.GONE);
        binding.actions.buttonPositive.setOnClickListener(v -> WherigoDialogManager.get().clear());
        return dialog;
    }

    @Override
    public void onGameNotification(final WherigoGame.NotifyType notifyType) {
        binding.layoutDetailsTextViewDescription.setText(WherigoUtils.eventTableToString(eventTable, true));
    }

}
