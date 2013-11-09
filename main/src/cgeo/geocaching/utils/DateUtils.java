package cgeo.geocaching.utils;

import cgeo.geocaching.Geocache;

import java.util.Calendar;
import java.util.Date;

public final class DateUtils {

    private DateUtils() {
        // utility class
    }

    public static int daysSince(long date) {
        final Calendar logDate = Calendar.getInstance();
        logDate.setTimeInMillis(date);
        logDate.set(Calendar.SECOND, 0);
        logDate.set(Calendar.MINUTE, 0);
        logDate.set(Calendar.HOUR_OF_DAY, 0);
        final Calendar today = Calendar.getInstance();
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.HOUR_OF_DAY, 0);
        return (int) Math.round((today.getTimeInMillis() - logDate.getTimeInMillis()) / 86400000d);
    }

    public static boolean isPastEvent(final Geocache cache) {
        if (!cache.isEventCache()) {
            return false;
        }
        final Date hiddenDate = cache.getHiddenDate();
        return hiddenDate != null && DateUtils.daysSince(hiddenDate.getTime()) > 0;
    }

}
