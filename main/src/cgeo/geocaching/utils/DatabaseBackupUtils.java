package cgeo.geocaching.utils;

import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;

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
            Dialogs.message(activity, R.string.init_backup_restore, R.string.init_backup_no_backup_available);
            return;
        }
        final int caches = DataStore.getAllCachesCount();
        if (caches == 0) {
            restoreDatabaseInternal(activity);
        } else {
            Dialogs.confirm(activity, R.string.init_backup_restore, activity.getString(R.string.restore_confirm_overwrite, activity.getResources().getQuantityString(R.plurals.cache_counts, caches, caches)), (dialog, which) -> restoreDatabaseInternal(activity));

        }
    }

    private static void restoreDatabaseInternal(final Activity activity) {
        final Resources res = activity.getResources();
        final ProgressDialog dialog = ProgressDialog.show(activity, res.getString(R.string.init_backup_restore), res.getString(R.string.init_restore_running), true, false);
        final AtomicInteger restoreSuccessful = new AtomicInteger(DataStore.RESTORE_FAILED_GENERAL);
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> restoreSuccessful.set(DataStore.restoreDatabaseInternal()), () -> {
            dialog.dismiss();
            final int restored = restoreSuccessful.get();
            final String message =
                    restored == DataStore.RESTORE_SUCCESSFUL ? res.getString(R.string.init_restore_success) :
                    restored == DataStore.RESTORE_FAILED_DBRECREATED ? res.getString(R.string.init_restore_failed_dbrecreated) :
                    res.getString(R.string.init_restore_failed);
            Dialogs.message(activity, R.string.init_backup_restore, message);
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).updateCacheCounter();
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
            Dialogs.confirm(activity, R.string.init_backup, activity.getString(R.string.backup_confirm_overwrite, getBackupDateTime()), (dialog, which) -> createBackupInternal(activity, runAfterwards));
        } else {
            createBackupInternal(activity, runAfterwards);
        }
    }

    private static void createBackupInternal(final Activity activity, final Runnable runAfterwards) {
        final ProgressDialog dialog = ProgressDialog.show(activity,
                activity.getString(R.string.init_backup),
                activity.getString(R.string.init_backup_running), true, false);
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.backupDatabaseInternal(), backupFileName -> {
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
        });
    }

    @Nullable
    public static File getRestoreFile() {
        final File fileSourceFile = DataStore.getBackupFileInternal(true);
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
