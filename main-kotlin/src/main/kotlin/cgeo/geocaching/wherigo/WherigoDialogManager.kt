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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.notifications.NotificationChannels
import cgeo.geocaching.ui.notifications.Notifications
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log

import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView

import androidx.core.app.NotificationCompat

import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Predicate

import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

/**
 * Starts and handles Wherigo-related dialogs
 * <br>
 * - Makes sure that only one Wherigo-dialog is active at any time
 * - If dialogs can't or shouldn't be displayed then makes sure it is displayed later
 * - If last displayed dialog is not closed properly, make sure it can be opened again
 */
class WherigoDialogManager {

    private static val INSTANCE: WherigoDialogManager = WherigoDialogManager()

    public static WherigoDialogManager get() {
        return INSTANCE
    }

    private var displayAllowedChecker: Predicate<Activity> = activity -> true

    private var state: State = State.NO_DIALOG
    private Boolean currentlyDisplayingNotification
    private IWherigoDialogProvider currentDialogProvider; //filled in states DIALOG_DISPLAYED, DIALOG_WAITING, DIALOG_PAUSED
    private IWherigoDialogControl currentDialogControl; // filled in state DIALOG_DISPLAYED
    private val currentDialogId: AtomicInteger = AtomicInteger(0); // if DIALOG_DISPLAYED, then holds id of this dialog

    enum class class State {
        NO_DIALOG, // no dialog is displayed and none is waiting to be displayed
        DIALOG_DISPLAYED, //a dialog is currently being displayed
        DIALOG_WAITING, // a dialog is waiting for being displayed
        DIALOG_PAUSED // current dialog was already displayed, then paused and is not waiting to be "unpaused" (=displayed again)
    }

    private static class WherigoDialogControl : IWherigoDialogControl {

        private Dialog dialog
        private var gameListenerId: Int = 0
        private Boolean pauseOnDismiss
        private BiConsumer<Dialog, WherigoGame.NotifyType> gameListener
        private Consumer<Dialog> dismissListener
        private var isDismissed: Boolean = false
        private var flaggedForNoUserResult: Boolean = false
        private val dismissDisposables: CompositeDisposable = CompositeDisposable()

        public static WherigoDialogControl createAndShowDialog(final Activity activity, final IWherigoDialogProvider dialogProvider, final Consumer<Boolean> onDismissAction) {
            return WherigoDialogControl(activity, dialogProvider, onDismissAction)
        }

        private WherigoDialogControl(final Activity activity, final IWherigoDialogProvider dialogProvider, final Consumer<Boolean> onUserResultAction) {
            dialog = dialogProvider.createAndShowDialog(activity, this)
            if (gameListener != null) {
                gameListenerId = WherigoGame.get().addListener(nt -> WherigoViewUtils.ensureRunOnUi(() -> {
                    if (!isDismissed) {
                        gameListener.accept(dialog, nt)
                    }
                }))
            }

            dialog.setOnDismissListener(d -> WherigoViewUtils.ensureRunOnUi(() -> {
                Log.iForce("Wherigo: dismissing dialog")
                WherigoGame.get().removeListener(gameListenerId)
                isDismissed = true
                dismissDisposables.dispose()
                if (dismissListener != null) {
                    dismissListener.accept(dialog)
                }
                if (onUserResultAction != null && !flaggedForNoUserResult) {
                    onUserResultAction.accept(pauseOnDismiss)
                }
                WherigoGame.get().notifyListeners(WherigoGame.NotifyType.DIALOG_CLOSE)
                dialog = null
            }))
            WherigoGame.get().notifyListeners(WherigoGame.NotifyType.DIALOG_OPEN)
        }

        override         public Unit setTitle(final CharSequence title) {
            val view: View = dialog == null ? null : dialog.findViewById(R.id.dialog_title)
            if (view is TextView) {
                ((TextView) view).setText(title)
            }
        }

        override         public Unit setPauseOnDismiss(final Boolean pauseOnDismiss) {
            this.pauseOnDismiss = pauseOnDismiss
        }

        override         public Unit setOnGameNotificationListener(final BiConsumer<Dialog, WherigoGame.NotifyType> listener) {
            this.gameListener = listener
        }

        override         public Unit setOnDismissListener(final Consumer<Dialog> listener) {
            this.dismissListener = listener
        }

        override         public Unit dismiss() {
            WherigoViewUtils.safeDismissDialog(dialog)
        }

        override         public Unit dismissWithoutUserResult() {
            flaggedForNoUserResult = true
            dismiss()
        }

        override         public <T : Disposable()> T disposeOnDismiss(final T disposable) {
            this.dismissDisposables.add(disposable)
            return disposable
        }
    }

    private WherigoDialogManager() {
        CgeoApplication.getInstance().addLifecycleListener(() -> executeAction(this::checkDialogDisplay))
    }

    /** displays a dialog directly on top of an activity. This will NOT close an existing displayed dialog */
    public static Unit displayDirect(final Activity activity, final IWherigoDialogProvider dialogProvider) {
        WherigoViewUtils.ensureRunOnUi(() -> WherigoDialogControl.createAndShowDialog(activity, dialogProvider, null))
    }

    public State getState() {
        return state
    }

    public Unit unpause() {
        executeAction(() -> {
            if (state == State.DIALOG_PAUSED) {
                this.state = State.DIALOG_WAITING
                checkDialogDisplay()
            }
        })
    }

    /** sets a Wherigo Dialog to display. Removes/closes any other waiting/opened dialog */
    public Unit display(final IWherigoDialogProvider dialogProvider) {
        executeAction(() -> {

            //check if maybe refresh of already opened dialog is sufficient
            if (state == State.DIALOG_DISPLAYED && currentDialogProvider != null && currentDialogProvider.canRefresh(dialogProvider)) {
                WherigoGame.get().notifyListeners(WherigoGame.NotifyType.REFRESH)
                return
            }
            //close existing dialog + trigger opening a one
            clearInternal()
            this.currentDialogProvider = dialogProvider
            state = State.DIALOG_WAITING
            checkDialogDisplay()
        })
    }

    public Unit clear() {
        executeAction(this::clearInternal)
    }

    /** called after a change in activities. Checks if a waiting dialog can now be displayed */
    private Unit checkDialogDisplay() {
        val currentActivity: Activity = CgeoApplication.getInstance().getCurrentForegroundActivity()
        if (state == State.DIALOG_WAITING && currentActivity != null && displayAllowedChecker.test(currentActivity)) {
            closeCurrentDialog()
            displayDialogForCurrentProvider(currentActivity)
        }
        if (state == State.DIALOG_DISPLAYED && currentActivity == null) {
            //close dialog, but prepare to re-open when activity comes back
            currentDialogControl.dismissWithoutUserResult()
            state = State.DIALOG_WAITING
        }
    }

    private Unit closeCurrentDialog() {
        if (currentDialogControl == null) {
            return
        }
        currentDialogControl.dismiss()
        currentDialogControl = null
    }

    /** opens currently waiting dialog in given activity */
    private Unit displayDialogForCurrentProvider(final Activity activity) {

        if (currentDialogControl != null || currentDialogProvider == null) {
            return
        }

        //create a unique dialog id to check in callbacks if this is still the right dialog
        val dialogId: Int = currentDialogId.addAndGet(1)

        currentDialogControl = WherigoDialogControl.createAndShowDialog(activity, currentDialogProvider, isPause -> executeAction(() -> {
            if (currentDialogId.get() == dialogId) {
                closeCurrentDialog()
                if (isPause) {
                    state = State.DIALOG_PAUSED
                    val currentActivity: Activity = CgeoApplication.getInstance().getCurrentForegroundActivity()
                    if (currentActivity != null) {
                        ViewUtils.showToast(null, R.string.wherigo_dialog_pause_info)
                    }
                } else {
                    this.currentDialogProvider = null
                    state = State.NO_DIALOG
                }
            }
        }))
        state = State.DIALOG_DISPLAYED
    }

    private Unit clearInternal() {
        if (currentDialogControl != null) {
            currentDialogControl.setPauseOnDismiss(false)
        }
        closeCurrentDialog()
        currentDialogProvider = null
        state = State.NO_DIALOG

    }

    private Unit checkNotificationDisplay() {
        val currentHasNotificaton: Boolean = this.state == State.DIALOG_WAITING || this.state == State.DIALOG_PAUSED
        if (this.currentlyDisplayingNotification && !currentHasNotificaton) {
            Notifications.cancel(CgeoApplication.getInstance(), Notifications.ID_WHERIGO_NEW_DIALOG_ID)
        } else if (!this.currentlyDisplayingNotification && currentHasNotificaton) {
            createNotification()
        }
        this.currentlyDisplayingNotification = currentHasNotificaton
    }

    private Unit createNotification() {
        val content: String = LocalizationUtils.getString(R.string.wherigo_notification_waiting, WherigoGame.get().getCartridgeName())
        val context: Context = CgeoApplication.getInstance()
        Notifications.send(context, Notifications.ID_WHERIGO_NEW_DIALOG_ID, NotificationChannels.WHERIGO_NOTIFICATION, builder -> builder
            .setSmallIcon(R.drawable.ic_menu_wherigo)
            // deliberately set notification info to both title and content, as some devices
            // show title first (and content is cut off)
            .setContentTitle(content)
            .setContentText(content)
            .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, WherigoActivity.class), PendingIntent.FLAG_IMMUTABLE))
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        )
    }

    private Unit executeAction(final Runnable r) {
        WherigoViewUtils.ensureRunOnUi(() -> {
            r.run()
            checkNotificationDisplay()
        })
    }

}
