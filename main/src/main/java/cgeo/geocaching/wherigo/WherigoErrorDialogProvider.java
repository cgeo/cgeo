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
    public Dialog createAndShowDialog(final Activity activity) {

        final String lastError = WherigoGame.get().getLastError();
        final String errorMessage = lastError == null ? LocalizationUtils.getString(R.string.wherigo_error_game_noerror) : LocalizationUtils.getString(R.string.wherigo_error_game_error, lastError);

        final SimpleDialog dialog = SimpleDialog.of(activity)
                .setTitle(TextParam.id(R.string.wherigo_error_title))
                .setMessage(TextParam.text(errorMessage))
                .setPositiveButton(TextParam.id(R.string.about_system_info_send_button));

        dialog.input(null, text -> {
            final String emailMessage = LocalizationUtils.getString(R.string.wherigo_error_email, errorMessage, String.valueOf(WherigoGame.get()), text);
            WherigoDialogManager.get().clear();
            DebugUtils.createLogcatHelper(activity, false, true, emailMessage);
        });

        return dialog.getDialog();

    }

}
