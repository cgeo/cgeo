package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.storage.PublicLocalFolder;
import cgeo.geocaching.storage.PublicLocalStorage;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.schedulers.Schedulers;


public class DebugUtils {

    private DebugUtils() {
        // utility class
    }

    public static void createMemoryDump(@NonNull final Context context) {
        Toast.makeText(context, R.string.init_please_wait, Toast.LENGTH_LONG).show();
        final File file = FileUtils.getUniqueNamedLogfile("cgeo_dump", "hprof");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    android.os.Debug.dumpHprofData(file.getPath());
                } catch (IOException e) {
                    Log.e("createMemoryDump", e);
                }
                ShareUtils.shareFileOrDismissDialog(context, file, "*/*", R.string.init_memory_dump, context.getString(R.string.init_memory_dumped, file.getAbsolutePath()));
                }, 1000);
    }

    public static void askUserToReportProblem(@NonNull final Activity context, @Nullable final String errorMsg) {
        final StringBuilder htmlMessage = new StringBuilder();
        if (errorMsg != null) {
            htmlMessage.append("<p>").append(context.getString(R.string.debug_user_error_errortext)).append("</p><p><i>")
                .append(errorMsg).append("</i></p>");
            Log.w("User was asked to report problem: " + errorMsg);
        }
        htmlMessage.append(context.getString(R.string.debug_user_error_explain_options_html));

        Dialogs.confirmPositiveNegativeNeutral(
            context,
            context.getString(R.string.debug_user_error_report_title),
            HtmlCompat.fromHtml(htmlMessage.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY),
            context.getString(R.string.about_system_info_send_button),
            null,
            context.getString(android.R.string.cancel),
            (dialog, which) -> createLogcatHelper(context, true, true,
                errorMsg == null ? null : context.getString(R.string.debug_user_error_report_title) + ": " + errorMsg),
            null,
            null);
    }

    public static void createLogcat(@NonNull final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // no differentiation possible on older systems, so no need to ask
            createLogcatHelper(activity, true, false, null);
        } else {
            Dialogs.confirmPositiveNegativeNeutral(
                    activity,
                    activity.getString(R.string.about_system_write_logcat),
                    activity.getString(R.string.about_system_write_logcat_type),
                    activity.getString(R.string.about_system_write_logcat_type_standard),
                    null,
                    activity.getString(R.string.about_system_write_logcat_type_extended),
                    (dialog, which) -> createLogcatHelper(activity, false, false, null),
                    null,
                    (dialog, which) -> createLogcatHelper(activity, true, false, null));
        }
    }

    private static void createLogcatHelper(@NonNull final Activity activity, final boolean fullInfo, final boolean forceEmail, final String additionalMessage) {
        final AtomicReference<Uri> result = new AtomicReference(null);

        final File file = PublicLocalStorage.get().createTempFile();

        final String filename = file.getName();
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
            try {
                final ProcessBuilder builder = new ProcessBuilder();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    builder.command("logcat", "-d", "-f", file.getAbsolutePath());
                } else {
                    if (fullInfo) {
                        builder.command("logcat", "-d", "*:V", "-f", file.getAbsolutePath());
                    } else {
                        builder.command("logcat", "-d", "AndroidRuntime:E", "cgeo:D", "cgeo.geocachin:I", "*:S", "-f", file.getAbsolutePath());
                    }
                }
                Log.iForce("[LogCat]Issuing command: " + builder.command());
                final int returnCode = builder.start().waitFor();
                if (returnCode == 0 && file.isFile()) {
                    final Uri logfileUri = PublicLocalStorage.get().writeFileToFolder(PublicLocalFolder.LOGFILES, FileNameCreator.LOGFILE, file, true);
                    result.set(logfileUri);
                } else {
                    Log.w("Problem creating logfile " + file + " (returnCode=" + returnCode + ", isFile=" + file.isFile() + ")");
                }

            } catch (IOException | InterruptedException e) {
                Log.e("error calling logcat: " + e.getMessage());
            }
        }, () -> {
            if (result.get() != null) {
                if (forceEmail) {
                    shareLogfileAsEmail(activity, additionalMessage, result.get());
                } else {
                    Dialogs.confirmPositiveNegativeNeutral(activity, activity.getString(R.string.about_system_write_logcat),
                        String.format(activity.getString(R.string.about_system_write_logcat_success), UriUtils.getFileName(result.get()), PublicLocalFolder.LOGFILES.getLocation().getUserDisplayableName()),
                        activity.getString(android.R.string.ok), null, activity.getString(R.string.about_system_info_send_button),
                        null, null, (dialog, which) -> shareLogfileAsEmail(activity, additionalMessage, result.get()));
                }
            } else {
                ActivityMixin.showToast(activity, R.string.about_system_write_logcat_error);
            }
        });
    }

    private static void shareLogfileAsEmail(@NonNull final Activity activity, final String additionalMessage, final Uri logfileUri) {
        final String systemInfo = SystemInformation.getSystemInformation(activity);
        final String emailText = additionalMessage == null ? systemInfo : additionalMessage + "\n\n" + systemInfo;
        ShareUtils.shareAsEmail(activity, activity.getString(R.string.about_system_info), emailText, logfileUri, R.string.about_system_info_send_chooser);
    }


    public static void logClassInformation(final String className) {
        if (Log.isEnabled(Log.LogLevel.VERBOSE)) {
            Log.v(String.format("Class info: %s", getClassInformation(className)));
        }
    }

    public static String getClassInformation(final String className) {
        final StringBuilder sb = new StringBuilder("Class[" + className + "]:");
        try {
            final Class<?> c = Class.forName(className);
            sb.append("Cons:[");
            for (Constructor<?> con : c.getDeclaredConstructors()) {
                sb.append(con.getName()).append("(").append(CollectionStream.of(con.getParameterTypes()).map(p -> p.getName()).toJoinedString(",")).append(");");
            }
            sb.append("]");

        } catch (ClassNotFoundException cnfe) {
            sb.append(" NOT EXISTING");
        } catch (Exception ex) {
            //something unexpected happened
            Log.w("Unexpected exception while trying to get class information for '" + className + "' (gathered so far: " + sb.toString() + ")", ex);
        }
        return sb.toString();

    }
}
