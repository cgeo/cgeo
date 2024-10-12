package cgeo.geocaching.wherigo;

import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.DebugUtils;

import android.app.Activity;
import android.app.Dialog;

import org.apache.commons.lang3.StringUtils;

public class WherigoErrorDialogProvider implements IWherigoDialogProvider {

    @Override
    public Dialog createAndShowDialog(final Activity activity, final Runnable onDismiss) {

        final String lastError = WherigoGame.get().getLastError();
        final String errorMessage = lastError == null ? "No error reported" : "An error was reported: " + lastError;
        final String emailMessage = "Problem playing a Wherigo" +
            "\n\nError: " + (lastError == null ? "no error" : lastError) +
            "\n\nWherigo Info: " + WherigoGame.get();

        final SimpleDialog dialog = SimpleDialog.of(activity)
                .setTitle(TextParam.text("Wherigo Error"))
                .setMessage(TextParam.text(errorMessage))
                .setPositiveButton(TextParam.text("Send by Email"))
                .setDismissAction(onDismiss);

        dialog.input(null, text -> {
            WherigoDialogManager.get().clear();
            DebugUtils.createLogcatHelper(activity, false, true, emailMessage + (StringUtils.isBlank(text) ? "" : "\n\nAdditional Info: " + text));
        });

        return dialog.getDialog();

    }

}
