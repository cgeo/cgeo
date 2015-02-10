package cgeo.geocaching.utils;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.ui.dialog.Dialogs;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseBackupUtils {

    private DatabaseBackupUtils() {
        // utility class
    }

    /**
     * After confirming to overwrite the existing caches on the devices, restore the database in a new thread, showing a
     * progress window
     *
     * @param activity
     *            calling activity
     */
    public static void restoreDatabase(final Activity activity) {
        if (!hasBackup()) {
            return;
        }
        final int caches = DataStore.getAllCachesCount();
        if (caches == 0) {
            restoreDatabaseInternal(activity);
        }
        else {
            Dialogs.confirm(activity, R.string.init_backup_restore, activity.getString(R.string.restore_confirm_overwrite, activity.getResources().getQuantityString(R.plurals.cache_counts, caches, caches)), new OnClickListener() {

                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    restoreDatabaseInternal(activity);
                }
            });

        }
    }

    private static void restoreDatabaseInternal(final Activity activity) {
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
                final boolean restored = restoreSuccessful.get();
                final String message = restored ? res.getString(R.string.init_restore_success) : res.getString(R.string.init_restore_failed);
                Dialogs.message(activity, R.string.init_backup_restore, message);
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).updateCacheCounter();
                }
            }
        });
    }

    /**
     * Create a backup after confirming to overwrite the existing backup.
     *
     */
    public static void createBackup(final Activity activity, final Runnable runAfterwards) {
        // avoid overwriting an existing backup with an empty database
        // (can happen directly after reinstalling the app)
        if (DataStore.getAllCachesCount() == 0) {
            Dialogs.message(activity, R.string.init_backup, R.string.init_backup_unnecessary);
            return;
        }
        if (hasBackup()) {
            Dialogs.confirm(activity, R.string.init_backup, activity.getString(R.string.backup_confirm_overwrite, getBackupDateTime()), new OnClickListener() {

                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    createBackupInternal(activity, runAfterwards);
                }
            });
        }
        else {
            createBackupInternal(activity, runAfterwards);
        }
    }

    private static void createBackupInternal(final Activity activity, final Runnable runAfterwards) {
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
    }

    @Nullable
    public static File getRestoreFile() {
        final File fileSourceFile = DataStore.getBackupFileInternal();
        return fileSourceFile.exists() && fileSourceFile.length() > 0 ? fileSourceFile : null;
    }

    public static boolean hasBackup() {
        return getRestoreFile() != null;
    }

    @NonNull
    public static String getBackupDateTime() {
        final File restoreFile = getRestoreFile();
        if (restoreFile == null) {
            return StringUtils.EMPTY;
        }
        return Formatter.formatShortDateTime(restoreFile.lastModified());
    }

}
