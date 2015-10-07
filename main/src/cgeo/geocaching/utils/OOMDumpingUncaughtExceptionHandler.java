package cgeo.geocaching.utils;

import android.os.Environment;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

public class OOMDumpingUncaughtExceptionHandler implements UncaughtExceptionHandler {

    private UncaughtExceptionHandler defaultHandler = null;
    private boolean defaultReplaced = false;

    public static boolean activateHandler() {
        final OOMDumpingUncaughtExceptionHandler handler = new OOMDumpingUncaughtExceptionHandler();

        return handler.activate();
    }

    private boolean activate() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        // replace default handler if that has not been done already
        if (!(defaultHandler instanceof OOMDumpingUncaughtExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(this);
            defaultReplaced = true;
        } else {
            defaultHandler = null;
            defaultReplaced = false;
        }

        return defaultReplaced;
    }

    public static boolean resetToDefault() {
        final UncaughtExceptionHandler unspecificHandler = Thread.getDefaultUncaughtExceptionHandler();
        boolean defaultResetted = unspecificHandler != null;

        if (unspecificHandler instanceof OOMDumpingUncaughtExceptionHandler) {
            final OOMDumpingUncaughtExceptionHandler handler = (OOMDumpingUncaughtExceptionHandler) unspecificHandler;
            defaultResetted = handler.reset();
        }

        return defaultResetted;
    }

    private boolean reset() {
        final boolean resetted = defaultReplaced;

        if (defaultReplaced) {
            Thread.setDefaultUncaughtExceptionHandler(defaultHandler);
            defaultReplaced = false;
        }

        return resetted;
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        Log.e("UncaughtException", ex);
        Throwable exx = ex;
        while (exx.getCause() != null) {
            exx = exx.getCause();
        }
        if (exx.getClass().equals(OutOfMemoryError.class)) {
            try {
                Log.e("OutOfMemory");
                android.os.Debug.dumpHprofData(Environment.getExternalStorageDirectory().getPath() + "/dump.hprof");
            } catch (final IOException e) {
                Log.e("Error writing dump", e);
            }
        }
        defaultHandler.uncaughtException(thread, ex);
    }
}
