package cgeo.geocaching.utils;

import cgeo.geocaching.CrashActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.lang3.StringUtils;

public final class CgeoUncaughtExceptionHandler {

    private CgeoUncaughtExceptionHandler() {
        // utility class
    }

    public static void installUncaughtExceptionHandler(final Context context) {
        final UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            // OkHttp threads can be interrupted when a request cancellation occurs, this should
            // be ignored.
            if (StringUtils.startsWith(thread.getName(), "OkHttp ")) {
                return;
            }

            //ALWAYS log uncaught exceptions
            Log.w("UncaughtException", ex);

            // If debug is enabled, check more advanced stuff
            if (Log.isDebug()) {
                Throwable exx = ex;
                while (exx.getCause() != null) {
                    exx = exx.getCause();
                }
                if (exx.getClass().equals(OutOfMemoryError.class)) {
                    try {
                        Log.w("OutOfMemory");
                        android.os.Debug.dumpHprofData(Environment.getExternalStorageDirectory().getPath() + "/dump.hprof");
                    } catch (final IOException e) {
                        Log.w("Error writing dump", e);
                    }
                }
            }

            // Show "c:geo crashed" activity
            final StringWriter stackTrace = new StringWriter();
            ex.printStackTrace(new PrintWriter(stackTrace));

            final Intent intent = new Intent(context, CrashActivity.class);
            intent.putExtra("Error", stackTrace.toString());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Required if starting activity from non-activity context

            context.startActivity(intent);

            // Call the default handler - IMPORTANT!: e.g. needed to automatically report crashes via play console
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, ex);
            }
        });
    }

}
