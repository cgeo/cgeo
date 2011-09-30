package cgeo.geocaching.compatibility;

import cgeo.geocaching.cgSettings;

import android.app.Activity;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import java.lang.reflect.Method;

public final class Compatibility {

    private final static int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
    private final static boolean isLevel8 = sdkVersion >= 8;

    private static Method dataChangedMethod = null;
    private static Method getRotationMethod = null;

    static {
        if (isLevel8) {
            try {
                final Class<?> cl = Class.forName("cgeo.geocaching.compatibility.AndroidLevel8");
                dataChangedMethod = cl.getDeclaredMethod("dataChanged", String.class);
                getRotationMethod = cl.getDeclaredMethod("getRotation", Activity.class);
            } catch (final Exception e) {
                // Exception can be ClassNotFoundException, SecurityException or NoSuchMethodException
                Log.e(cgSettings.tag, "Cannot load AndroidLevel8 class", e);
            }
        }
    }

    public static Float getDirectionNow(final Float directionNowPre,
            final Activity activity) {
        if (isLevel8) {
            try {
                final int rotation = (Integer) getRotationMethod.invoke(null, activity);
                if (rotation == Surface.ROTATION_90) {
                    return directionNowPre + 90;
                } else if (rotation == Surface.ROTATION_180) {
                    return directionNowPre + 180;
                } else if (rotation == Surface.ROTATION_270) {
                    return directionNowPre + 270;
                }
            } catch (final Exception e) {
                // This should never happen: IllegalArgumentException, IllegalAccessException or InvocationTargetException
                Log.e(cgSettings.tag, "Cannot call getRotation()", e);
            }
        } else {
            final Display display = activity.getWindowManager()
                    .getDefaultDisplay();
            final int rotation = display.getOrientation();
            if (rotation == Configuration.ORIENTATION_LANDSCAPE) {
                return directionNowPre + 90;
            }
        }
        return directionNowPre;
    }

    public static Uri getCalendarProviderURI() {
        return Uri.parse(isLevel8 ? "content://com.android.calendar/calendars" : "content://calendar/calendars");
    }

    public static Uri getCalenderEventsProviderURI() {
        return Uri.parse(isLevel8 ? "content://com.android.calendar/events" : "content://calendar/events");
    }

    public static void dataChanged(final String name) {
        if (isLevel8) {
            try {
                dataChangedMethod.invoke(null, name);
            } catch (final Exception e) {
                // This should never happen: IllegalArgumentException, IllegalAccessException or InvocationTargetException
                Log.e(cgSettings.tag, "Cannot call dataChanged()", e);
            }
        }
    }

}
