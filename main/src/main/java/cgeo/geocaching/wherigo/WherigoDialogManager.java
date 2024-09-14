package cgeo.geocaching.wherigo;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Starts and handles Wherigo-related dialogs
 * <br>
 * - Makes sure that only one Wherigo-dialog is active at any time
 * - If dialogs can't or shouldn't be displayed then makes sure it is displayed later
 */
public class WherigoDialogManager {

    private static final WherigoDialogManager INSTANCE = new WherigoDialogManager();

    public static WherigoDialogManager get() {
        return INSTANCE;
    }

    private final Object mutex = new Object();

    private IWherigoDialogProvider dialogProvider;
    private final int[] gameListenerId = new int[1];
    private Dialog currentDialog;
    private final AtomicInteger currentDialogId = new AtomicInteger(0);


    private WherigoDialogManager() {
        CgeoApplication.getInstance().addLifecycleListener(() -> ensureRunOnUi(this::checkDialogDisplay));
    }

    public void display(final IWherigoDialogProvider dialogProvider) {
        ensureRunOnUi(() -> {
            synchronized (mutex) {
                clear();
                this.dialogProvider = dialogProvider;
                final boolean success = checkDialogDisplay();
                if (!success) {
                    createNotification(this.dialogProvider);
                }
            }
        });
    }

    private void createNotification(final IWherigoDialogProvider provider) {
        final String content = "Wherigo is waiting: " + provider.getClass().getName();
        final Context context = CgeoApplication.getInstance();
        Notifications.send(context, Notifications.ID_WHERIGO_NEW_DIALOG_ID, NotificationChannels.WHERIGO_NOTIFICATION, builder -> builder
            .setSmallIcon(R.drawable.type_marker_wherigo)
            // deliberately set notification info to both title and content, as some devices
            // show title first (and content is cut off)
            .setContentTitle(content)
            .setContentText(content)
            .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, WherigoActivity.class), PendingIntent.FLAG_IMMUTABLE))
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        );
    }

    private boolean checkDialogDisplay() {
        synchronized (mutex) {
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
            WherigoGame.get().removeListener(gameListenerId[0]);
            dialogProvider.onDialogDismiss();
            dialogProvider = null;
            try {
                currentDialog.dismiss();
            } catch (Exception ex) {
                Log.w("Exception when dismissing dialog", ex);
            }
            currentDialog = null;
        }
    }

    private boolean openCurrentDialog(final Activity activity) {
        synchronized (mutex) {
            if (currentDialog != null || dialogProvider == null) {
                return false;
            }
            final int dialogId = currentDialogId.addAndGet(1);
            currentDialog = dialogProvider.createDialog(activity);
            currentDialog.setOnDismissListener(d -> ensureRunOnUi(() -> {
                synchronized (mutex) {
                    //check whether the dialog for this dismiss-event still exists
                    if (currentDialog != null && dialogId == currentDialogId.get()) {
                        closeCurrentDialog();
                    }
                }
            }));
            gameListenerId[0] = WherigoGame.get().addListener(nt -> ensureRunOnUi(() -> {
                synchronized (mutex) {
                    //check whether the dialog for this wherigo-event still exists
                    if (currentDialog != null && dialogId == currentDialogId.get()) {
                        dialogProvider.onGameNotification(nt);
                    }
                }
            }));
            currentDialog.show();
            //dismiss any wherigo dialog notification since the dialog would now be displayed
            Notifications.cancel(activity, Notifications.ID_WHERIGO_NEW_DIALOG_ID);
            return true;
        }
    }

    public void clear() {
        ensureRunOnUi(() -> {
            synchronized (mutex) {
                closeCurrentDialog();
                currentDialog = null;
            }
        });
    }

    private void ensureRunOnUi(final Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            AndroidRxUtils.runOnUi(r);
        }
    }

}
