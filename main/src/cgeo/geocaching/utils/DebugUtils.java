package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class DebugUtils {

    private DebugUtils() {
        // utility class
    }

    public static void createMemoryDump(@NonNull final Context context) {
        try {
            final File file = FileUtils.getUniqueNamedLogfile("cgeo_dump", "hprof");
            android.os.Debug.dumpHprofData(file.getPath());
            Toast.makeText(context, context.getString(R.string.init_memory_dumped, file.getAbsolutePath()),
                    Toast.LENGTH_LONG).show();
            ShareUtils.share(context, file, R.string.init_memory_dump);
        } catch (final IOException e) {
            Log.e("createMemoryDump", e);
        }
    }

    public static void createLogcat(@NonNull final Activity activity) {
        final AtomicBoolean success = new AtomicBoolean(false);
        final File file = FileUtils.getUniqueNamedLogfile("logcat", "log");
        final String filename = file.getName();
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
            try {
                final Process process = new ProcessBuilder()
                    .command("logcat", "-d", "cgeo.geocachin:D", "*:S", "-f", file.getAbsolutePath())
                    .start();
                final int result = process.waitFor();
                success.set(result == 0);
            } catch (IOException | InterruptedException e) {
                Log.e("error calling logcat: " + e.getMessage());
                success.set(false);
            }
        }, () -> {
            if (success.get()) {
                Dialogs.confirm(activity, activity.getString(R.string.about_system_write_logcat), String.format(activity.getString(R.string.about_system_write_logcat_success), filename, LocalStorage.LOGFILES_DIR_NAME), activity.getString(R.string.about_system_info_send_button), (dialog, which) -> {
                    final String systemInfo = SystemInformation.getSystemInformation(activity);
                    ShareUtils.shareAsEMail(activity, activity.getString(R.string.about_system_info), systemInfo, file, R.string.about_system_info_send_chooser);
                });
            } else {
                ActivityMixin.showToast(activity, R.string.about_system_write_logcat_error);
            }
        });

    }
}
