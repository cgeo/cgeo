package cgeo.geocaching.wherigo;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.Dialog;
import android.os.Looper;

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

    private final boolean onlyInWherigo = false;

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
                checkDialogDisplay();
            }
        });
    }

    private void checkDialogDisplay() {
        synchronized (mutex) {
            if (currentDialog != null && currentDialog.getOwnerActivity() != null && currentDialog.getOwnerActivity().isFinishing()) {
                closeCurrentDialog();
                return;
            }
            final Activity currentActivity = CgeoApplication.getInstance().getCurrentForegroundActivity();
            if (currentActivity != null && (!onlyInWherigo || currentActivity instanceof WherigoActivity)) {
                openCurrentDialog(currentActivity);
            }
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

    private void openCurrentDialog(final Activity activity) {
        synchronized (mutex) {
            if (currentDialog != null || dialogProvider == null) {
                return;
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
