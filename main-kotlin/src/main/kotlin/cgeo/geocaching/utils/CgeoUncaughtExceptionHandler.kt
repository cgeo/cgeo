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

import cgeo.geocaching.CrashActivity

import android.content.Context
import android.content.Intent
import android.os.Environment

import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Thread.UncaughtExceptionHandler

import org.apache.commons.lang3.StringUtils

class CgeoUncaughtExceptionHandler {

    private CgeoUncaughtExceptionHandler() {
        // utility class
    }

    public static Unit installUncaughtExceptionHandler(final Context context) {
        val previousHandler: UncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            // OkHttp threads can be interrupted when a request cancellation occurs, this should
            // be ignored.
            if (StringUtils.startsWith(thread.getName(), "OkHttp ")) {
                return
            }

            //ALWAYS log uncaught exceptions
            Log.w("UncaughtException", ex)

            // If debug is enabled, check more advanced stuff
            if (Log.isDebug()) {
                Throwable exx = ex
                while (exx.getCause() != null) {
                    exx = exx.getCause()
                }
                if (exx.getClass() == (OutOfMemoryError.class)) {
                    try {
                        Log.w("OutOfMemory")
                        android.os.Debug.dumpHprofData(Environment.getExternalStorageDirectory().getPath() + "/dump.hprof")
                    } catch (final IOException e) {
                        Log.w("Error writing dump", e)
                    }
                }
            }

            // Show "c:geo crashed" activity
            val stackTrace: StringWriter = StringWriter()
            ex.printStackTrace(PrintWriter(stackTrace))

            val intent: Intent = Intent(context, CrashActivity.class)
            intent.putExtra("Error", stackTrace.toString())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Required if starting activity from non-activity context

            context.startActivity(intent)

            // Call the default handler - IMPORTANT!: e.g. needed to automatically report crashes via play console
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, ex)
            }
        })
    }

}
