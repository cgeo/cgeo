package cgeo.geocaching.utils;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

final public class Log {

    private static final String TAG = "cgeo";

    /**
     * the debug flag is cached here so that we don't need to access the settings every time we have to evaluate it
     */
    private static boolean isDebug = true;
    private static boolean first = true;

    public static boolean isDebug() {
        return isDebug;
    }

    /**
     * make a non persisted copy of the debug flag from the settings for performance reasons
     *
     * @param isDebug
     */
    public static void setDebugUnsaved(boolean isDebug) {
        Log.isDebug = isDebug;
    }

    public static void v(final String msg) {
        if (isDebug) {
            android.util.Log.v(TAG, msg);
        }
    }

    public static void v(final String msg, final Throwable t) {
        if (isDebug) {
            android.util.Log.v(TAG, msg, t);
        }
    }

    public static void d(final String msg) {
        if (isDebug) {
            android.util.Log.d(TAG, msg);
        }
    }

    public static void d(final String msg, final Throwable t) {
        if (isDebug) {
            android.util.Log.d(TAG, msg, t);
        }
    }

    public static void i(final String msg) {
        if (isDebug) {
            android.util.Log.i(TAG, msg);
        }
    }

    public static void i(final String msg, final Throwable t) {
        if (isDebug) {
            android.util.Log.i(TAG, msg, t);
        }
    }

    public static void w(final String msg) {
        android.util.Log.w(TAG, msg);
    }

    public static void w(final String msg, final Throwable t) {
        android.util.Log.w(TAG, msg, t);
    }

    public static void e(final String msg) {
        android.util.Log.e(TAG, msg);
    }

    public static void e(final String msg, final Throwable t) {
        android.util.Log.e(TAG, msg, t);
    }

    /**
     * Log the whole content of a string into "/sdcard/cgeo-debug.log".
     * <br/>
     * Sometimes, the string we want to work on while debugging or developing a new feature is too long to
     * be fully stored in Android logcat. This method will log the content of the string in a file named
     * "/sdcard/cgeo-debug.log". The file will be reset at every run, and if called several times during a run,
     * the contents will be appended to one another.
     * <br/>
     * <strong>This method should never be called in production.</strong>
     *
     * @param msg the message to log, or to add to the log if other messages have been stored in the same run
     */
    public synchronized static void logToFile(final String msg) {
        final File file = new File(Environment.getExternalStorageDirectory(), "cgeo-debug.log");
        if (first) {
            first = false;
            file.delete();
        }
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(msg);
        } catch (final IOException e) {
            Log.e("logToFile: cannot write to " + file, e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }
}
