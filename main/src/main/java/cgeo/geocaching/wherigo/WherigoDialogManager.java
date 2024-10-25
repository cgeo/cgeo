package cgeo.geocaching.wherigo;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ProcessUtils;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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

    private final Object mutex = new Object();

    private IWherigoDialogProvider dialogProvider;
    private Dialog currentDialog;
    private boolean currentDialogResult;

    private boolean isPaused = false;

    private final AtomicInteger currentDialogId = new AtomicInteger(0);


    private WherigoDialogManager() {
        CgeoApplication.getInstance().addLifecycleListener(() -> ensureRunOnUi(() -> this.checkDialogDisplay(false)));
    }

    /** displays a dialog directly on top of an activity. This will NOT close an existing displayed dialog */
    public static void displayDirect(final Activity activity, final IWherigoDialogProvider dialogProvider) {
        displayDialog(activity, dialogProvider, result -> { }, () -> { });
    }

    public static void dismissDialog(final Dialog dialog) {
        if (dialog == null) {
            return;
        }
        try {
            dialog.dismiss();
        } catch (Exception ex) {
            Log.w("Exception when dismissing dialog", ex);
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void unpause() {
        synchronized (mutex) {
            this.isPaused = false;
            checkDialogDisplay(true);
        }
    }

    /** sets a new Wherigo Dialog to display */
    public void display(final IWherigoDialogProvider dialogProvider) {
        ensureRunOnUi(() -> {
            synchronized (mutex) {
                clear();
                this.dialogProvider = dialogProvider;
                final boolean success = checkDialogDisplay(true);
                if (!success) {
                    createNotification();
                }
            }
        });
    }

    private void createNotification() {
        final String content = LocalizationUtils.getString(R.string.wherigo_notification_waiting, WherigoGame.get().getCartridgeName());
        final Context context = CgeoApplication.getInstance();
        Notifications.send(context, Notifications.ID_WHERIGO_NEW_DIALOG_ID, NotificationChannels.WHERIGO_NOTIFICATION, builder -> builder
            .setSmallIcon(R.drawable.type_marker_wherigo)
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

    private boolean checkDialogDisplay(final boolean force) {
        synchronized (mutex) {
            if (!force && isPaused) {
                return false;
            }
            if (currentDialog != null && currentDialog.getOwnerActivity() != null && currentDialog.getOwnerActivity().isFinishing()) {
                closeCurrentDialog();
            }
            final Activity currentActivity = CgeoApplication.getInstance().getCurrentForegroundActivity();
            if (currentActivity != null && (!WherigoGame.get().openOnlyInWherigo() || currentActivity instanceof WherigoActivity)) {
                return openCurrentDialog(currentActivity);
            }
            return false;
        }
    }

    private void closeCurrentDialog() {
        synchronized (mutex) {
            if (currentDialog == null) {
                return;
            }

            dismissDialog(currentDialog);

            currentDialog = null;
            isPaused = !currentDialogResult;
            if (currentDialogResult) {
                dialogProvider = null;
            } else {
                createNotification();
            }
        }
    }

    private boolean openCurrentDialog(final Activity activity) {
        synchronized (mutex) {
            if (currentDialog != null || dialogProvider == null) {
                return false;
            }

            //create a unique dialog id to check in callbacks if this is still the right dialog
            final int dialogId = currentDialogId.addAndGet(1);

            isPaused = false;
            currentDialogResult = true;
            currentDialog = displayDialog(activity, dialogProvider, result -> {
                synchronized (mutex) {
                    if (currentDialogId.get() == dialogId) {
                        currentDialogResult = result;
                    }
                }
            }, () -> {
                synchronized (mutex) {
                    if (currentDialog != null && dialogId == currentDialogId.get()) {
                        closeCurrentDialog();
                    }
                }
            });

            //dismiss any wherigo dialog notification since the dialog would now be displayed
            Notifications.cancel(activity, Notifications.ID_WHERIGO_NEW_DIALOG_ID);
            return true;
        }
    }

//    private boolean openCurrentDialog(final Activity activity) {
//        synchronized (mutex) {
//            if (currentDialog != null || dialogProvider == null) {
//                return false;
//            }
//            final int dialogId = currentDialogId.addAndGet(1);
//            currentDialog = dialogProvider.createAndShowDialog(activity);
//            currentDialog.setOnDismissListener(d -> ensureRunOnUi(() -> {
//                synchronized (mutex) {
//                    //check whether the dialog for this dismiss-event still exists
//                    if (currentDialog != null && dialogId == currentDialogId.get()) {
//                        closeCurrentDialog();
//                    }
//                }
//            }));
//            gameListenerId[0] = WherigoGame.get().addListener(nt -> ensureRunOnUi(() -> {
//                synchronized (mutex) {
//                    //check whether the dialog for this wherigo-event still exists
//                    if (currentDialog != null && dialogId == currentDialogId.get()) {
//                        dialogProvider.onGameNotification(nt);
//                    }
//                }
//            }));
//            //dismiss any wherigo dialog notification since the dialog would now be displayed
//            Notifications.cancel(activity, Notifications.ID_WHERIGO_NEW_DIALOG_ID);
//            return true;
//        }
//    }


    private static Dialog displayDialog(final Activity activity, final IWherigoDialogProvider dialogProvider, final Consumer<Boolean> onResultSet, final Runnable onCloseAction) {

        final Dialog[] dialog = new Dialog[]{ null };
        final int[] gameListenerId = new int[]{ -1 };

        dialog[0] = dialogProvider.createAndShowDialog(activity, onResultSet);
        dialog[0].setOnDismissListener(d -> ensureRunOnUi(() -> {
            WherigoGame.get().removeListener(gameListenerId[0]);
            dialogProvider.onDialogDismiss();
            WherigoGame.get().notifyListeners(WherigoGame.NotifyType.DIALOG_CLOSE);
            onCloseAction.run();
        }));

        gameListenerId[0] = WherigoGame.get().addListener(nt -> ensureRunOnUi(() -> {
            dialogProvider.onGameNotification(nt);
        }));

        WherigoGame.get().notifyListeners(WherigoGame.NotifyType.DIALOG_OPEN);

        return dialog[0];
    }


    public void clear() {
        ensureRunOnUi(() -> {
            synchronized (mutex) {
                closeCurrentDialog();
                currentDialog = null;
            }
        });
    }

    private static void ensureRunOnUi(final Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            AndroidRxUtils.runOnUi(r);
        }
    }

}
