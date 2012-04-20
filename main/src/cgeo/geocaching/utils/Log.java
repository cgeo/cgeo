package cgeo.geocaching.utils;


final public class Log {

    private static final String TAG = "cgeo";

    private static boolean isDebug = true;

    public static boolean isDebug() {
        return isDebug;
    }

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
}
