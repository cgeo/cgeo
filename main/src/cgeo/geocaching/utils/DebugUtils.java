package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.schedulers.Schedulers;


public class DebugUtils {

    private DebugUtils() {
        // utility class
    }

    public static void createMemoryDump(@NonNull final Activity context) {
        Toast.makeText(context, R.string.init_please_wait, Toast.LENGTH_LONG).show();
        final File file = ContentStorage.get().createTempFile();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                android.os.Debug.dumpHprofData(file.getPath());
            } catch (IOException e) {
                Log.e("createMemoryDump", e);
            }
            final Uri dumpFileUri = ContentStorage.get().writeFileToFolder(PersistableFolder.LOGFILES, FileNameCreator.MEMORY_DUMP, file, true);

            ShareUtils.shareOrDismissDialog(context, dumpFileUri, "*/*", R.string.init_memory_dump, context.getString(R.string.init_memory_dumped, UriUtils.toUserDisplayableString(dumpFileUri)));
        }, 1000);
    }

    public static void askUserToReportProblem(@NonNull final Activity context, @Nullable final String errorMsg) {
        final StringBuilder message = new StringBuilder();
        if (errorMsg != null) {
            message.append(context.getString(R.string.debug_user_error_errortext)).append("\n[")
                    .append(errorMsg).append("]\n\n");
            Log.w("User was asked to report problem: " + errorMsg);
        }
        message.append(context.getString(R.string.debug_user_error_explain_options));

        SimpleDialog.of(context)
                .setTitle(R.string.debug_user_error_report_title)
                .setMessage(TextParam.text(message.toString()))
                .setPositiveButton(TextParam.id(R.string.about_system_info_send_button))
                .confirm(
                        (dialog, which) -> createLogcatHelper(context, true, true, errorMsg == null ? null : context.getString(R.string.debug_user_error_report_title) + ": " + errorMsg),
                        SimpleDialog.DO_NOTHING
                );
    }

    public static void createLogcat(@NonNull final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // no differentiation possible on older systems, so no need to ask
            createLogcatHelper(activity, true, false, null);
        } else {
            SimpleDialog.of(activity)
                    .setTitle(R.string.about_system_write_logcat)
                    .setMessage(R.string.about_system_write_logcat_type)
                    .setButtons(R.string.about_system_write_logcat_type_standard, 0, R.string.about_system_write_logcat_type_extended)
                    .confirm(
                            (dialog, which) -> createLogcatHelper(activity, false, false, null),
                            SimpleDialog.DO_NOTHING,
                            (dialog, which) -> createLogcatHelper(activity, true, false, null)
                    );
        }
    }


    private static void createLogcatHelper(@NonNull final Activity activity, final boolean fullInfo, final boolean forceEmail, final String additionalMessage) {
        final AtomicReference<Uri> result = new AtomicReference(null);

        final File file = ContentStorage.get().createTempFile();

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
                    final Uri logfileUri = ContentStorage.get().writeFileToFolder(PersistableFolder.LOGFILES, FileNameCreator.LOGFILE, file, true);
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
                    SimpleDialog.of(activity)
                            .setTitle(R.string.about_system_write_logcat)
                            .setMessage(R.string.about_system_write_logcat_success, UriUtils.getLastPathSegment(result.get()), PersistableFolder.LOGFILES.getFolder().toUserDisplayableString())
                            .setButtons(0, 0, R.string.about_system_info_send_button)
                            .confirm(
                                    SimpleDialog.DO_NOTHING,
                                    null,
                                    (dialog, which) -> shareLogfileAsEmail(activity, additionalMessage, result.get())
                            );

                }
            } else {
                ActivityMixin.showToast(activity, R.string.about_system_write_logcat_error);
            }
        });
    }

    private static void shareLogfileAsEmail(@NonNull final Activity activity, final String additionalMessage, final Uri logfileUri) {
        final String systemInfo = SystemInformation.getSystemInformation(activity);
        final String emailText = additionalMessage == null ? systemInfo : additionalMessage + "\n\n" + systemInfo;
        ShareUtils.shareAsEmail(activity, String.format(activity.getString(R.string.mailsubject_problem_report), Version.getVersionName(activity)), emailText, logfileUri, R.string.about_system_info_send_chooser);
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
                sb.append(con.getName()).append("(").append(CollectionStream.of(con.getParameterTypes()).map(Class::getName).toJoinedString(",")).append(");");
            }
            sb.append("]");

        } catch (ClassNotFoundException cnfe) {
            sb.append(" NOT EXISTING");
        } catch (Exception ex) {
            //something unexpected happened
            Log.w("Unexpected exception while trying to get class information for '" + className + "' (gathered so far: " + sb + ")", ex);
        }
        return sb.toString();

    }
}
