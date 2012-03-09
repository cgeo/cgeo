package cgeo.calendar;

import android.net.Uri;
import android.os.Build;

public final class Compatibility {

    private final static int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
    private final static boolean isLevel8 = sdkVersion >= 8;
    private final static boolean isLevel14 = sdkVersion >= 14;

    public static Uri getCalendarProviderURI() {
        return Uri.parse(isLevel8 ? "content://com.android.calendar/calendars" : "content://calendar/calendars");
    }

    public static Uri getCalendarEventsProviderURI() {
        return Uri.parse(isLevel8 ? "content://com.android.calendar/events" : "content://calendar/events");
    }

    public static boolean isLevel14() {
        return isLevel14;
    }
}
