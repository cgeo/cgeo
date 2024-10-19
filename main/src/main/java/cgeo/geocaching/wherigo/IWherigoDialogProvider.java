package cgeo.geocaching.wherigo;

import android.app.Activity;
import android.app.Dialog;

import java.util.function.Consumer;

public interface IWherigoDialogProvider {

    default Dialog createDialog(Activity activity, final Consumer<Boolean> resultSetter) {
        throw new IllegalStateException("Either 'createDialog' or 'createAndShowDialog' must be overriddeen");
    }

    default Dialog createAndShowDialog(Activity activity, final Consumer<Boolean> resultSetter) {
        final Dialog dialog = createDialog(activity, resultSetter);
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
