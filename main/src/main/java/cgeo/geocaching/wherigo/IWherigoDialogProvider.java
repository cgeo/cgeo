package cgeo.geocaching.wherigo;

import android.app.Activity;
import android.app.Dialog;

public interface IWherigoDialogProvider {

    Dialog createDialog(Activity activity);

    default void onGameNotification(final WherigoGame.NotifyType notifyType) {
        //default: do nothing
    }

    default void onDialogDismiss() {
        //default: do nothing
    }


}
