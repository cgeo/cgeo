package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class DebugUtils {

    private enum LogcatResults {
        LOGCAT_OK,
        LOGCAT_EMPTY,
        LOGCAT_ERROR
    }

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
        final AtomicInteger result = new AtomicInteger(LogcatResults.LOGCAT_ERROR.ordinal());
        final File file = FileUtils.getUniqueNamedLogfile("logcat", "txt");
        final String filename = file.getName();
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
            try {
                final ProcessBuilder builder = new ProcessBuilder();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    builder.command("logcat", "-d", "-f", file.getAbsolutePath());
                } else {
                    builder.command("logcat", "-d", "AndroidRuntime:E", "cgeo:D", "cgeo.geocachin:E", "*:S", "-f", file.getAbsolutePath());
                }
                final int returnCode = builder.start().waitFor();
                if (returnCode == 0) {
                    result.set(file.exists() ? LogcatResults.LOGCAT_OK.ordinal() : LogcatResults.LOGCAT_EMPTY.ordinal());
                }

            } catch (IOException | InterruptedException e) {
                Log.e("error calling logcat: " + e.getMessage());
            }
        }, () -> {
            if (result.get() == LogcatResults.LOGCAT_OK.ordinal()) {
                Dialogs.confirmPositiveNegativeNeutral(activity, activity.getString(R.string.about_system_write_logcat),
                        String.format(activity.getString(R.string.about_system_write_logcat_success), filename, LocalStorage.LOGFILES_DIR_NAME),
                        activity.getString(android.R.string.ok), null, activity.getString(R.string.about_system_info_send_button),
                        null, null, (dialog, which) -> {
                    final String systemInfo = SystemInformation.getSystemInformation(activity);
                    ShareUtils.shareAsEMail(activity, activity.getString(R.string.about_system_info), systemInfo, file, R.string.about_system_info_send_chooser);
                });
            } else {
                ActivityMixin.showToast(activity, result.get() == LogcatResults.LOGCAT_EMPTY.ordinal() ? R.string.about_system_write_logcat_empty : R.string.about_system_write_logcat_error);
            }
        });

    }
}
