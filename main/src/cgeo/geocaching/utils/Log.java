package cgeo.geocaching.utils;

import cgeo.geocaching.Settings;

final public class Log {

    public static void v(final String tag, final String msg) {
        if (Settings.isDebug()) {
            android.util.Log.v(tag, msg);
        }
    }

    public static void v(final String tag, final String msg, final Throwable t) {
        if (Settings.isDebug()) {
            android.util.Log.v(tag, msg, t);
        }
    }

    public static void d(final String tag, final String msg) {
        if (Settings.isDebug()) {
            android.util.Log.d(tag, msg);
        }
    }

    public static void d(final String tag, final String msg, final Throwable t) {
        if (Settings.isDebug()) {
            android.util.Log.d(tag, msg, t);
        }
    }

    public static void i(final String tag, final String msg) {
        if (Settings.isDebug()) {
            android.util.Log.i(tag, msg);
        }
    }

    public static void i(final String tag, final String msg, final Throwable t) {
        if (Settings.isDebug()) {
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
