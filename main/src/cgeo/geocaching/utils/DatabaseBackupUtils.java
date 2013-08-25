package cgeo.geocaching.utils;

import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.cgData;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.ui.Formatter;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;

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
        Thread restoreThread = new Thread() {
            final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    dialog.dismiss();
                    boolean restored = restoreSuccessful.get();
                    String message = restored ? res.getString(R.string.init_restore_success) : res.getString(R.string.init_restore_failed);
                    ActivityMixin.helpDialog(activity, res.getString(R.string.init_backup_restore), message);
                    if (activity instanceof MainActivity) {
                        ((MainActivity) activity).updateCacheCounter();
                    }
                }
            };

            @Override
            public void run() {
                restoreSuccessful.set(cgData.restoreDatabaseInternal());
                handler.sendMessage(handler.obtainMessage());
            }
        };
        restoreThread.start();
    }

    public static boolean createBackup(final Activity activity, final Runnable runAfterwards) {
        final Context context = activity;
        // avoid overwriting an existing backup with an empty database
        // (can happen directly after reinstalling the app)
        if (cgData.getAllCachesCount() == 0) {
            ActivityMixin.helpDialog(activity,
                    context.getString(R.string.init_backup),
                    context.getString(R.string.init_backup_unnecessary));
            return false;
        }

        final ProgressDialog dialog = ProgressDialog.show(context,
                context.getString(R.string.init_backup),
                context.getString(R.string.init_backup_running), true, false);
        new Thread() {
            @Override
            public void run() {
                final String backupFileName = cgData.backupDatabaseInternal();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        ActivityMixin.helpDialog(activity,
                                context.getString(R.string.init_backup_backup),
                                backupFileName != null
                                        ? context.getString(R.string.init_backup_success)
                                                + "\n" + backupFileName
                                        : context.getString(R.string.init_backup_failed));
                        if (runAfterwards != null) {
                            runAfterwards.run();
                        }
                    }
                });
            }
        }.start();
        return true;
    }

    public static File getRestoreFile() {
        final File fileSourceFile = cgData.getBackupFileInternal();
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
