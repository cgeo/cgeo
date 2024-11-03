package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.DebugUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.app.Activity;
import android.app.Dialog;

public class WherigoErrorDialogProvider implements IWherigoDialogProvider {

    @Override
    public Dialog createAndShowDialog(final Activity activity, final IWherigoDialogControl dialogControl) {

        final String lastError = WherigoGame.get().getLastError();
        final String errorMessage = lastError == null ? LocalizationUtils.getString(R.string.wherigo_error_game_noerror) :
                (LocalizationUtils.getString(R.string.wherigo_error_game_error, lastError) +
                        "\n\n" + LocalizationUtils.getString(R.string.wherigo_error_game_error_addinfo));
        final String errorMessageEmail = lastError == null ? LocalizationUtils.getString(R.string.wherigo_error_game_noerror) : LocalizationUtils.getString(R.string.wherigo_error_game_error, lastError);


        final SimpleDialog dialog = SimpleDialog.of(activity)
                .setTitle(TextParam.id(R.string.wherigo_error_title))
                .setMessage(TextParam.text(errorMessage))
                .setPositiveButton(TextParam.id(R.string.about_system_info_send_button))
                .setNegativeButton(TextParam.id(R.string.close));

        dialog.confirm(() -> {
            final String emailMessage = LocalizationUtils.getString(R.string.wherigo_error_email,
                    errorMessageEmail,
                    WherigoGame.get().getCartridgeName() + " (" + WherigoGame.get().getCGuid() + ")´");
            DebugUtils.createLogcatHelper(activity, false, true, emailMessage);
        });

        return dialog.getDialog();

    }

}
