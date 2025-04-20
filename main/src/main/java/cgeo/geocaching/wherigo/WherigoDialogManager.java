package cgeo.geocaching.wherigo;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.ProcessUtils;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Starts and handles Wherigo-related dialogs
 * <br>
 * - Makes sure that only one Wherigo-dialog is active at any time
 * - If dialogs can't or shouldn't be displayed then makes sure it is displayed later
 * - If last displayed dialog is not closed properly, make sure it can be opened again
 */
public class WherigoDialogManager {

    private static final WherigoDialogManager INSTANCE = new WherigoDialogManager();

    public static WherigoDialogManager get() {
        return INSTANCE;
    }

    private Predicate<Activity> displayAllowedChecker = activity -> true;

    private State state = State.NO_DIALOG;
    private boolean currentlyDisplayingNotification;
    private IWherigoDialogProvider currentDialogProvider; //filled in states DIALOG_DISPLAYED, DIALOG_WAITING, DIALOG_PAUSED
    private IWherigoDialogControl currentDialogControl; // filled in state DIALOG_DISPLAYED
    private final AtomicInteger currentDialogId = new AtomicInteger(0); // if DIALOG_DISPLAYED, then holds id of this dialog


    public enum State {
        NO_DIALOG, // no dialog is displayed and none is waiting to be displayed
        DIALOG_DISPLAYED, //a dialog is currently being displayed
        DIALOG_WAITING, // a dialog is waiting for being displayed
        DIALOG_PAUSED // current dialog was already displayed, then paused and is not waiting to be "unpaused" (=displayed again)
    }

    private static class WherigoDialogControl implements IWherigoDialogControl {

        private Dialog dialog;
        private int gameListenerId = 0;
        private boolean pauseOnDismiss;
        private BiConsumer<Dialog, WherigoGame.NotifyType> gameListener;
        private Consumer<Dialog> dismissListener;
        private boolean isDismissed = false;
        private boolean flaggedForNoUserResult = false;

        public static WherigoDialogControl createAndShowDialog(final Activity activity, final IWherigoDialogProvider dialogProvider, final Consumer<Boolean> onDismissAction) {
            return new WherigoDialogControl(activity, dialogProvider, onDismissAction);
        }

        private WherigoDialogControl(final Activity activity, final IWherigoDialogProvider dialogProvider, final Consumer<Boolean> onUserResultAction) {
            dialog = dialogProvider.createAndShowDialog(activity, this);
            if (gameListener != null) {
                gameListenerId = WherigoGame.get().addListener(nt -> WherigoViewUtils.ensureRunOnUi(() -> {
                    if (!isDismissed) {
                        gameListener.accept(dialog, nt);
                    }
                }));
            }

            dialog.setOnDismissListener(d -> WherigoViewUtils.ensureRunOnUi(() -> {
                WherigoGame.get().removeListener(gameListenerId);
                isDismissed = true;
                if (dismissListener != null) {
                    dismissListener.accept(dialog);
                }
                if (onUserResultAction != null && !flaggedForNoUserResult) {
                    onUserResultAction.accept(pauseOnDismiss);
                }
                WherigoGame.get().notifyListeners(WherigoGame.NotifyType.DIALOG_CLOSE);
                dialog = null;
            }));
            WherigoGame.get().notifyListeners(WherigoGame.NotifyType.DIALOG_OPEN);
        }

        @Override
        public void setPauseOnDismiss(final boolean pauseOnDismiss) {
            this.pauseOnDismiss = pauseOnDismiss;
        }

        @Override
        public void setOnGameNotificationListener(final BiConsumer<Dialog, WherigoGame.NotifyType> listener) {
            this.gameListener = listener;
        }

        @Override
        public void setOnDismissListener(final Consumer<Dialog> listener) {
            this.dismissListener = listener;
        }

        @Override
        public void dismiss() {
            WherigoViewUtils.safeDismissDialog(dialog);
        }

        @Override
        public void dismissWithoutUserResult() {
            flaggedForNoUserResult = true;
            dismiss();
        }
    }

    private WherigoDialogManager() {
        CgeoApplication.getInstance().addLifecycleListener(() -> executeAction(this::checkDialogDisplay));
    }

    /** displays a dialog directly on top of an activity. This will NOT close an existing displayed dialog */
    public static void displayDirect(final Activity activity, final IWherigoDialogProvider dialogProvider) {
        WherigoViewUtils.ensureRunOnUi(() -> WherigoDialogControl.createAndShowDialog(activity, dialogProvider, null));
    }

    public State getState() {
        return state;
    }

    public void unpause() {
        executeAction(() -> {
            if (state == State.DIALOG_PAUSED) {
                this.state = State.DIALOG_WAITING;
                checkDialogDisplay();
            }
        });
    }

    /** sets a new Wherigo Dialog to display. Removes/closes any other waiting/opened dialog */
    public void display(final IWherigoDialogProvider dialogProvider) {
        executeAction(() -> {
            clearInternal();
            this.currentDialogProvider = dialogProvider;
            state = State.DIALOG_WAITING;
            checkDialogDisplay();
        });
    }

    public void clear() {
        executeAction(this::clearInternal);
    }

    /** called after a change in activities. Checks if a waiting dialog can now be displayed */
    private void checkDialogDisplay() {
        final Activity currentActivity = CgeoApplication.getInstance().getCurrentForegroundActivity();
        if (state == State.DIALOG_WAITING && currentActivity != null && displayAllowedChecker.test(currentActivity)) {
            closeCurrentDialog();
            displayDialogForCurrentProvider(currentActivity);
        }
        if (state == State.DIALOG_DISPLAYED && currentActivity == null) {
            //close dialog, but prepare to re-open when activity comes back
            currentDialogControl.dismissWithoutUserResult();
            state = State.DIALOG_WAITING;
        }
    }

    private void closeCurrentDialog() {
        if (currentDialogControl == null) {
            return;
        }
        currentDialogControl.dismiss();
        currentDialogControl = null;
    }

    /** opens currently waiting dialog in given activity */
    private void displayDialogForCurrentProvider(final Activity activity) {

        if (currentDialogControl != null || currentDialogProvider == null) {
            return;
        }

        //create a unique dialog id to check in callbacks if this is still the right dialog
        final int dialogId = currentDialogId.addAndGet(1);

        currentDialogControl = WherigoDialogControl.createAndShowDialog(activity, currentDialogProvider, isPause -> executeAction(() -> {
            if (currentDialogId.get() == dialogId) {
                closeCurrentDialog();
                if (isPause) {
                    state = State.DIALOG_PAUSED;
                    final Activity currentActivity = CgeoApplication.getInstance().getCurrentForegroundActivity();
                    if (currentActivity != null) {
                        ViewUtils.showToast(null, R.string.wherigo_dialog_pause_info);
                    }
                } else {
                    this.currentDialogProvider = null;
                    state = State.NO_DIALOG;
                }
            }
        }));
        state = State.DIALOG_DISPLAYED;
    }

    private void clearInternal() {
        closeCurrentDialog();
        currentDialogProvider = null;
        state = State.NO_DIALOG;

    }

    private void checkNotificationDisplay() {
        final boolean currentHasNotificaton = this.state == State.DIALOG_WAITING || this.state == State.DIALOG_PAUSED;
        if (this.currentlyDisplayingNotification && !currentHasNotificaton) {
            Notifications.cancel(CgeoApplication.getInstance(), Notifications.ID_WHERIGO_NEW_DIALOG_ID);
        } else if (!this.currentlyDisplayingNotification && currentHasNotificaton) {
            createNotification();
        }
        this.currentlyDisplayingNotification = currentHasNotificaton;
    }

    private void createNotification() {
        final String content = LocalizationUtils.getString(R.string.wherigo_notification_waiting, WherigoGame.get().getCartridgeName());
        final Context context = CgeoApplication.getInstance();
        Notifications.send(context, Notifications.ID_WHERIGO_NEW_DIALOG_ID, NotificationChannels.WHERIGO_NOTIFICATION, builder -> builder
            .setSmallIcon(R.drawable.ic_menu_wherigo)
            // deliberately set notification info to both title and content, as some devices
            // show title first (and content is cut off)
            .setContentTitle(content)
            .setContentText(content)
            .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, WherigoActivity.class), ProcessUtils.getFlagImmutable()))
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        );
    }

    private void executeAction(final Runnable r) {
        WherigoViewUtils.ensureRunOnUi(() -> {
            r.run();
            checkNotificationDisplay();
        });
    }

}
