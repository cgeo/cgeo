// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.wherigo

import android.app.Dialog

import java.util.function.BiConsumer
import java.util.function.Consumer

import io.reactivex.rxjava3.disposables.Disposable

/** a dialog control interface passed to dialogs to control certain behaviour */
interface IWherigoDialogControl {

    Unit setTitle(CharSequence title)

    Unit setPauseOnDismiss(Boolean pauseOnDismiss)

    Unit setOnGameNotificationListener(BiConsumer<Dialog, WherigoGame.NotifyType> listener)

    Unit setOnDismissListener(Consumer<Dialog> listener)

    Unit dismiss()

    Unit dismissWithoutUserResult()

    <T : Disposable()> T disposeOnDismiss(T disposable)


}
