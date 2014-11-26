package cgeo.geocaching.utils;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.ui.dialog.Dialogs;

import org.apache.commons.lang3.StringUtils;

import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseBackupUtils {

    private DatabaseBackupUtils() {
        // utility class
    }

    /**
     * restore the database in a new thread, showing a progress window
     *
     * @param activity
     *            calling activity
     */
    public static void restoreDatabase(final Activity activity) {
        final Resources res = activity.getResources();
        final ProgressDialog dialog = ProgressDialog.show(activity, res.getString(R.string.init_backup_restore), res.getString(R.string.init_restore_running), true, false);
        final AtomicBoolean restoreSuccessful = new AtomicBoolean(false);
        RxUtils.andThenOnUi(Schedulers.io(), new Action0() {
            @Override
            public void call() {
                restoreSuccessful.set(DataStore.restoreDatabaseInternal());
            }
        }, new Action0() {
            @Override
            public void call() {
                dialog.dismiss();
                boolean restored = restoreSuccessful.get();
                String message = restored ? res.getString(R.string.init_restore_success) : res.getString(R.string.init_restore_failed);
                Dialogs.message(activity, R.string.init_backup_restore, message);
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).updateCacheCounter();
                }
            }
        });
    }

    public static boolean createBackup(final Activity activity, final Runnable runAfterwards) {
        // avoid overwriting an existing backup with an empty database
        // (can happen directly after reinstalling the app)
        if (DataStore.getAllCachesCount() == 0) {
            Dialogs.message(activity, R.string.init_backup, R.string.init_backup_unnecessary);
            return false;
        }

        final ProgressDialog dialog = ProgressDialog.show(activity,
                activity.getString(R.string.init_backup),
                activity.getString(R.string.init_backup_running), true, false);
        RxUtils.andThenOnUi(Schedulers.io(), new Func0<String>() {
            @Override
            public String call() {
                return DataStore.backupDatabaseInternal();
            }
        }, new Action1<String>() {
            @Override
            public void call(final String backupFileName) {
                dialog.dismiss();
                Dialogs.message(activity,
                        R.string.init_backup_backup,
                        backupFileName != null
                                ? activity.getString(R.string.init_backup_success)
                                + "\n" + backupFileName
                                : activity.getString(R.string.init_backup_failed));
                if (runAfterwards != null) {
                    runAfterwards.run();
                }
            }
        });
        return true;
    }

    public static File getRestoreFile() {
        final File fileSourceFile = DataStore.getBackupFileInternal();
        return fileSourceFile.exists() && fileSourceFile.length() > 0 ? fileSourceFile : null;
    }

    public static boolean hasBackup() {
        return getRestoreFile() != null;
    }

    public static String getBackupDateTime() {
        final File restoreFile = getRestoreFile();
        if (restoreFile == null) {
            return StringUtils.EMPTY;
        }
        return Formatter.formatShortDateTime(restoreFile.lastModified());
    }

}
