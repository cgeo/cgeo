package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.io.IOUtils;


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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // no differentiation possible on older systems, so no need to ask
            createLogcatHelper(activity, true);
        } else {
            Dialogs.confirmPositiveNegativeNeutral(
                    activity,
                    activity.getString(R.string.about_system_write_logcat),
                    activity.getString(R.string.about_system_write_logcat_type),
                    activity.getString(R.string.about_system_write_logcat_type_standard),
                    null,
                    activity.getString(R.string.about_system_write_logcat_type_extended),
                    (dialog, which) -> createLogcatHelper(activity, false),
                    null,
                    (dialog, which) -> createLogcatHelper(activity, true));
        }
    }

    private static void createLogcatHelper(@NonNull final Activity activity, final boolean fullInfo) {
        final AtomicInteger result = new AtomicInteger(LogcatResults.LOGCAT_ERROR.ordinal());

       // File cgeoDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "cgeo");
        //File file = new File(cgeoDir, "logfile_"+System.currentTimeMillis()+".txt");

        //try: /storage/emulated/0/cgeo
        final File file = FileUtils.getUniqueNamedLogfile("logcat", "txt");
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
                if (returnCode == 0) {
                    result.set(file.exists() ? LogcatResults.LOGCAT_OK.ordinal() : LogcatResults.LOGCAT_EMPTY.ordinal());


                    //the following code COPIES the created log file to the public folder as chosen y user in MainActivity
                    //TODO: this code is a hack and just to try out the concepts of the storage framework

                    Log.w("Available URIs with persisted permissions: " +
                                    CollectionStream.of(activity.getContentResolver().getPersistedUriPermissions()).map(u -> u.getUri().getPath()).toJoinedString(","));

                    //get the uri which was granted access before (NOTE: if there's no such URL then this fails of course!)
                    final Uri cgeoUri = activity.getContentResolver().getPersistedUriPermissions().get(0).getUri();
                    //"take" access rights
                    activity.getContentResolver().takePersistableUriPermission(cgeoUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    // Create a new file in the directory and copy logfile content to it
                    final DocumentFile pickedDir = DocumentFile.fromTreeUri(activity, cgeoUri);
                    final DocumentFile newFile = pickedDir.createFile("text/plain", file.getName());
                    final OutputStream out = activity.getContentResolver().openOutputStream(newFile.getUri());
                    out.write("Copy of logcat file starts...".getBytes()); //add this line in the copied logfile just for debug purposes
                    final InputStream in = new FileInputStream(file);
                    IOUtils.copy(in, out);
                    in.close(); //TODO: closing w/o being in final block would e unacceptable in production!
                    out.close(); //TODO: closing w/o being in final block would e unacceptable in production!
                    //hack end
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
