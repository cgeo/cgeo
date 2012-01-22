package cgeo.calendar;

import android.net.Uri;
import android.os.Build;

public final class Compatibility {

    private final static int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
    private final static boolean isLevel8 = sdkVersion >= 8;

    public static Uri getCalendarProviderURI() {
        return Uri.parse(isLevel8 ? "content://com.android.calendar/calendars" : "content://calendar/calendars");
    }

    public static Uri getCalenderEventsProviderURI() {
        return Uri.parse(isLevel8 ? "content://com.android.calendar/events" : "content://calendar/events");
    }
}
