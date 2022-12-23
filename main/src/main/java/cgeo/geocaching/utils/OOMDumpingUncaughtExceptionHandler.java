package cgeo.geocaching.utils;

import android.os.Environment;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.lang3.StringUtils;

public final class OOMDumpingUncaughtExceptionHandler {

    private OOMDumpingUncaughtExceptionHandler() {
        // utility class
    }

    public static void installUncaughtExceptionHandler() {
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

            // Call the default handler
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, ex);
            }
        });
    }

}
