package cgeo.geocaching.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public final class Log {

    private static final String TAG = "cgeo";

    private static final class StackTraceDebug extends RuntimeException {
        final static private long serialVersionUID = 27058374L;
    }

    /**
     * The debug flag is cached here so that we don't need to access the settings every time we have to evaluate it.
     */
    private static boolean isDebug = true;
    private static boolean first = true;

    private Log() {
        // utility class
    }

    public static boolean isDebug() {
        return isDebug;
    }

    /**
     * Save a copy of the debug flag from the settings for performance reasons.
     *
     */
    public static void setDebug(final boolean isDebug) {
        Log.isDebug = isDebug;
    }

    private static String addThreadInfo(final String msg) {
        return new StringBuilder("[").append(Thread.currentThread().getName()).append("] ").append(msg).toString();
    }

    public static void v(final String msg) {
        if (isDebug) {
            android.util.Log.v(TAG, addThreadInfo(msg));
        }
    }

    public static void v(final String msg, final Throwable t) {
        if (isDebug) {
            android.util.Log.v(TAG, addThreadInfo(msg), t);
        }
    }

    public static void d(final String msg) {
        if (isDebug) {
            android.util.Log.d(TAG, addThreadInfo(msg));
        }
    }

    public static void d(final String msg, final Throwable t) {
        if (isDebug) {
            android.util.Log.d(TAG, addThreadInfo(msg), t);
        }
    }

    public static void i(final String msg) {
        if (isDebug) {
            android.util.Log.i(TAG, addThreadInfo(msg));
        }
    }

    public static void i(final String msg, final Throwable t) {
        if (isDebug) {
            android.util.Log.i(TAG, addThreadInfo(msg), t);
        }
    }

    public static void w(final String msg) {
        android.util.Log.w(TAG, addThreadInfo(msg));
    }

    public static void w(final String msg, final Throwable t) {
        android.util.Log.w(TAG, addThreadInfo(msg), t);
    }

    public static void e(final String msg) {
        android.util.Log.e(TAG, addThreadInfo(msg));
    }

    public static void e(final String msg, final Throwable t) {
        android.util.Log.e(TAG, addThreadInfo(msg), t);
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
    public static synchronized void logToFile(final String msg) {
        final File file = new File(Environment.getExternalStorageDirectory(), "cgeo-debug.log");
        if (first) {
            first = false;
            FileUtils.delete(file);
        }
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), CharEncoding.UTF_8));
            writer.write(addThreadInfo(msg));
        } catch (final IOException e) {
            Log.e("logToFile: cannot write to " + file, e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    /**
     * Log a debug message with the actual stack trace.
     *
     * @param msg the debug message
     */
    public static void logStackTrace(final String msg) {
        try {
            throw new StackTraceDebug();
        } catch (final StackTraceDebug dbg) {
            Log.d(msg, dbg);
        }
    }
}
