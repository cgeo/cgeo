package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoThingDetailsBinding;
import cgeo.geocaching.ui.TextParam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;

import java.io.IOException;
import java.util.Collections;

import cz.matejcik.openwig.formats.CartridgeFile;

public class WherigoCartridgeDialogProvider implements IWherigoDialogProvider {

    private final CartridgeFile cartridgeFile;
    private final Runnable startAction;
    private WherigoThingDetailsBinding binding;

    public WherigoCartridgeDialogProvider(final CartridgeFile cartridgeFile, final Runnable dismissAction) {
        this.cartridgeFile = cartridgeFile;
        this.startAction = dismissAction;
    }

    @Override
    public Dialog createDialog(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.cgeo_fullScreenDialog);
        binding = WherigoThingDetailsBinding.inflate(LayoutInflater.from(activity));
        final AlertDialog dialog = builder.create();
        dialog.setView(binding.getRoot());

        final String description = cartridgeFile.name + "\n" + cartridgeFile.description +
            "\nAuthor:" + cartridgeFile.author +
            "\nlat:" + cartridgeFile.latitude +
            ", lon:" + cartridgeFile.longitude;

        binding.layoutDetailsTextViewDescription.setText(description);

        try {
            final byte[] data = cartridgeFile.getFile(cartridgeFile.splashId);
            binding.media.setMediaData("jpg", data, null);
        } catch (IOException ioe) {
            //do nothing
        }
        WherigoUtils.setViewActions(Collections.singleton(startAction == null ? "Close" : "Start"), binding.dialogActionlist, TextParam::text, item -> {
            WherigoDialogManager.get().clear();
            if (startAction != null) {
                startAction.run();
            }
        });

        return dialog;
    }

    @Override
    public void onGameNotification(final WherigoGame.NotifyType notifyType) {
        //binding.layoutDetailsTextViewDescription.setText(WherigoUtils.eventTableToString(eventTable, true));
    }

}
