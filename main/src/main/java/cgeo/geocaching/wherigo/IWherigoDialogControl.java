package cgeo.geocaching.wherigo;

import cgeo.geocaching.utils.offlinetranslate.Translator;
import cgeo.geocaching.utils.offlinetranslate.TranslatorUtils;

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

    Translator getTranslator();

    default void addTranslation(final String original, final BiConsumer<String, Boolean> translationAction) {
        disposeOnDismiss(getTranslator().addTranslation(original, translationAction));
    }

    default TranslatorUtils.ChangeableText createChangeableTranslation() {
        return disposeOnDismiss(new TranslatorUtils.ChangeableText(getTranslator()));
    }


}
