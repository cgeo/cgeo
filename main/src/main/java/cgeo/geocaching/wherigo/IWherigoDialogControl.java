package cgeo.geocaching.wherigo;

import android.app.Dialog;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** a dialog control interface passed to dialogs to control certain behaviour */
public interface IWherigoDialogControl {

    void setPauseOnDismiss(boolean pauseOnDismiss);

    void setOnGameNotificationListener(BiConsumer<Dialog, WherigoGame.NotifyType> listener);

    void setOnDismissListener(Consumer<Dialog> listener);

    void dismiss();

    void dismissWithoutUserResult();
}
