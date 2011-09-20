package cgeo.geocaching.compatibility;

import android.app.Activity;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.view.Display;
import android.view.Surface;

public final class Compatibility {

    private final static int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
    private final static boolean isLevel8 = sdkVersion >= 8;
    private final static AndroidLevel8 level8 = isLevel8 ? new AndroidLevel8() : null;

    public static Float getDirectionNow(final Float directionNowPre,
            final Activity activity) {
        if (isLevel8) {
            final int rotation = level8.getRotation(activity);
            if (rotation == Surface.ROTATION_90) {
                return directionNowPre + 90;
            } else if (rotation == Surface.ROTATION_180) {
                return directionNowPre + 180;
            } else if (rotation == Surface.ROTATION_270) {
                return directionNowPre + 270;
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

}
