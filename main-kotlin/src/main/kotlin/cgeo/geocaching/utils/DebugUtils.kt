// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.SimpleDialog

import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.io.File
import java.io.IOException
import java.lang.reflect.Constructor
import java.util.concurrent.atomic.AtomicReference

import io.reactivex.rxjava3.schedulers.Schedulers


class DebugUtils {

    private DebugUtils() {
        // utility class
    }

    public static Unit createMemoryDump(final Activity context) {
        ViewUtils.showToast(context, R.string.init_please_wait)
        val file: File = ContentStorage.get().createTempFile()

        Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                android.os.Debug.dumpHprofData(file.getPath())
            } catch (IOException e) {
                Log.e("createMemoryDump", e)
            }
            val dumpFileUri: Uri = ContentStorage.get().writeFileToFolder(PersistableFolder.LOGFILES, FileNameCreator.MEMORY_DUMP, file, true)

            ShareUtils.shareOrDismissDialog(context, dumpFileUri, "*/*", R.string.init_memory_dump, context.getString(R.string.init_memory_dumped, UriUtils.toUserDisplayableString(dumpFileUri)))
        }, 1000)
    }

    public static Unit askUserToReportProblem(final Activity context, final String errorMsg) {
        askUserToReportProblem(context, errorMsg, true)
    }

    public static Unit askUserToReportProblem(final Activity context, final String errorMsg, final Boolean showErrorTextIfAvailable) {
        val message: StringBuilder = StringBuilder()
        if (showErrorTextIfAvailable && errorMsg != null) {
            message.append(context.getString(R.string.debug_user_error_errortext)).append("\n[")
                    .append(errorMsg).append("]\n\n")
            Log.w("User was asked to report problem: " + errorMsg)
        }
        message.append(context.getString(R.string.debug_user_error_explain_options))

        SimpleDialog.of(context)
                .setTitle(R.string.debug_user_error_report_title)
                .setMessage(TextParam.text(message.toString()))
                .setPositiveButton(TextParam.id(R.string.about_system_info_send_button))
                .confirm(
                        () -> createLogcatHelper(context, true, true, errorMsg == null ? null : context.getString(R.string.debug_user_error_report_title) + ": " + errorMsg)
                )
    }

    public static Unit createLogcat(final Activity activity) {
        SimpleDialog.of(activity)
                .setTitle(R.string.about_system_write_logcat)
                .setMessage(R.string.about_system_write_logcat_type)
                .setButtons(R.string.about_system_write_logcat_type_standard, R.string.cancel, R.string.about_system_write_logcat_type_extended)
                .setNeutralAction(() -> createLogcatHelper(activity, true, false, null))
                .confirm(() -> createLogcatHelper(activity, false, false, null))
    }


    public static Unit createLogcatHelper(final Activity activity, final Boolean fullInfo, final Boolean forceEmail, final String additionalMessage) {
        val result: AtomicReference<Uri> = AtomicReference<>(null)

        val file: File = ContentStorage.get().createTempFile()

        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
            try {
                val builder: ProcessBuilder = ProcessBuilder()
                if (fullInfo) {
                    builder.command("logcat", "-d", "*:V", "-f", file.getAbsolutePath())
                } else {
                    builder.command("logcat", "-d", "AndroidRuntime:E", "cgeo:D", "System.err:I", "System.out:I", "*:S", "-f", file.getAbsolutePath())
                }
                Log.iForce("[LogCat]Issuing command: " + builder.command())
                val returnCode: Int = builder.start().waitFor()
                if (returnCode == 0 && file.isFile()) {
                    val logfileUri: Uri = ContentStorage.get().writeFileToFolder(PersistableFolder.LOGFILES, FileNameCreator.LOGFILE, file, true)
                    result.set(logfileUri)
                } else {
                    Log.w("Problem creating logfile " + file + " (returnCode=" + returnCode + ", isFile=" + file.isFile() + ")")
                }

            } catch (IOException | InterruptedException e) {
                Log.e("error calling logcat: " + e.getMessage())
            }
        }, () -> {
            if (result.get() != null) {
                if (forceEmail) {
                    shareLogfileAsEmail(activity, additionalMessage, result.get())
                } else {
                    SimpleDialog.of(activity)
                            .setTitle(R.string.about_system_write_logcat)
                            .setMessage(R.string.about_system_write_logcat_success, UriUtils.getLastPathSegment(result.get()), PersistableFolder.LOGFILES.getFolder().toUserDisplayableString())
                            .setNeutralButton(TextParam.id(R.string.about_system_info_send_button))
                            .setNeutralAction(() -> shareLogfileAsEmail(activity, additionalMessage, result.get()))
                            .show()

                }
            } else {
                ActivityMixin.showToast(activity, R.string.about_system_write_logcat_error)
            }
        })
    }

    private static Unit shareLogfileAsEmail(final Activity activity, final String additionalMessage, final Uri logfileUri) {
        val systemInfo: String = SystemInformation.getSystemInformation(activity)
        val emailText: String = additionalMessage == null ? systemInfo : additionalMessage + "\n\n" + systemInfo
        ShareUtils.shareAsEmail(activity, String.format(activity.getString(R.string.mailsubject_problem_report), Version.getVersionName(activity)), emailText, logfileUri, R.string.about_system_info_send_chooser)
    }


    public static Unit logClassInformation(final String className) {
        if (Log.isEnabled(Log.LogLevel.VERBOSE)) {
            Log.v(String.format("Class info: %s", getClassInformation(className)))
        }
    }

    public static String getClassInformation(final String className) {
        val sb: StringBuilder = StringBuilder("Class[" + className + "]:")
        try {
            val c: Class<?> = Class.forName(className)
            sb.append("Cons:[")
            for (Constructor<?> con : c.getDeclaredConstructors()) {
                sb.append(con.getName()).append("(").append(CollectionStream.of(con.getParameterTypes()).map(Class::getName).toJoinedString(",")).append(");")
            }
            sb.append("]")

        } catch (ClassNotFoundException cnfe) {
            sb.append(" NOT EXISTING")
        } catch (Exception ex) {
            //something unexpected happened
            Log.w("Unexpected exception while trying to get class information for '" + className + "' (gathered so far: " + sb + ")", ex)
        }
        return sb.toString()

    }
}
