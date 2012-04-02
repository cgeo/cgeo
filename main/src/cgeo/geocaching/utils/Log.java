package cgeo.geocaching.utils;

import cgeo.geocaching.BuildConfig;
import cgeo.geocaching.Settings;

final public class Log {

    // A warning may be issued when DEBUG is false
    @SuppressWarnings("unused")
    private static boolean debugEnabled() {
        return BuildConfig.DEBUG && Settings.isDebug();
    }

    public static void v(final String tag, final String msg) {
        if (debugEnabled()) {
            android.util.Log.v(tag, msg);
        }
    }

    public static void v(final String tag, final String msg, final Throwable t) {
        if (debugEnabled()) {
            android.util.Log.v(tag, msg, t);
        }
    }

    public static void d(final String tag, final String msg) {
        if (debugEnabled()) {
            android.util.Log.d(tag, msg);
        }
    }

    public static void d(final String tag, final String msg, final Throwable t) {
        if (debugEnabled()) {
            android.util.Log.d(tag, msg, t);
        }
    }

    public static void i(final String tag, final String msg) {
        if (debugEnabled()) {
            android.util.Log.i(tag, msg);
        }
    }

    public static void i(final String tag, final String msg, final Throwable t) {
        if (debugEnabled()) {
            android.util.Log.i(tag, msg, t);
        }
    }

    public static void w(final String tag, final String msg) {
        android.util.Log.w(tag, msg);
    }

    public static void w(final String tag, final String msg, final Throwable t) {
        android.util.Log.w(tag, msg, t);
    }

    public static void e(final String tag, final String msg) {
        android.util.Log.e(tag, msg);
    }

    public static void e(final String tag, final String msg, final Throwable t) {
        android.util.Log.e(tag, msg, t);
    }

    public static void wtf(final String tag, final String msg) {
        android.util.Log.wtf(tag, msg);
    }

    public static void wtf(final String tag, final String msg, final Throwable t) {
        android.util.Log.wtf(tag, msg, t);
    }

}
