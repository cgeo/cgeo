package cgeo.geocaching.wherigo;

import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.DebugUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

public class WherigoErrorDialogProvider implements IWherigoDialogProvider {

    private final String error;

    public WherigoErrorDialogProvider(final String error) {
        this.error = error;
    }

    @Override
    public Dialog createDialog(final Activity activity) {
        final androidx.appcompat.app.AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setTitle("Wherigo Error");
        dialog.setMessage("An error occured: " + error);
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Send by email", (d, w) -> {
            WherigoDialogManager.get().clear();
            DebugUtils.createLogcatHelper(activity, false, true, "Problem playing Wherigo: " + error +
                    "\n\nWherigo information: " + WherigoGame.get());
        });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (d, w) -> {
            WherigoDialogManager.get().clear();
        });

        return dialog;
    }

}
