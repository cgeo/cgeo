package cgeo.calendar;

import android.net.Uri;
import android.os.Build;

public final class Compatibility {

    private final static int SDK_VERSION = Build.VERSION.SDK_INT;
    private final static boolean IS_LEVEL_8 = SDK_VERSION >= 8;
    private final static boolean IS_LEVEL_14 = SDK_VERSION >= 14;

    public static Uri getCalendarProviderURI() {
        return Uri.parse(IS_LEVEL_8 ? "content://com.android.calendar/calendars" : "content://calendar/calendars");
    }

    public static Uri getCalendarEventsProviderURI() {
        return Uri.parse(IS_LEVEL_8 ? "content://com.android.calendar/events" : "content://calendar/events");
    }

    public static boolean isLevel14() {
        return IS_LEVEL_14;
    }
}
