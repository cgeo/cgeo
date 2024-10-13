package cgeo.geocaching.wherigo;

import android.app.Activity;
import android.app.Dialog;

public interface IWherigoDialogProvider {

    default Dialog createDialog(Activity activity) {
        throw new IllegalStateException("Either 'createDialog' or 'createAndShowDialog' must be overriddeen");
    }

    default Dialog createAndShowDialog(Activity activity) {
        final Dialog dialog = createDialog(activity);
        dialog.show();
        return dialog;
    }

    default void onGameNotification(final WherigoGame.NotifyType notifyType) {
        //default: do nothing
    }

    default void onDialogDismiss() {
        //default: do nothing
    }



}
