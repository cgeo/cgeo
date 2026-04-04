package cgeo.geocaching.wherigo;

import android.app.Dialog;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.reactivex.rxjava3.disposables.Disposable;

/** a dialog control interface passed to dialogs to control certain behaviour */
public interface IWherigoDialogControl {

    void setTitle(CharSequence title);

    void setPauseOnDismiss(boolean pauseOnDismiss);

    void setOnGameNotificationListener(BiConsumer<Dialog, WherigoGame.NotifyType> listener);

    void setOnDismissListener(Consumer<Dialog> listener);

    void dismiss();

    void dismissWithoutUserResult();

    <T extends Disposable> T disposeOnDismiss(T disposable);


}
