package cgeo.geocaching.utils;

import cgeo.geocaching.Geocache;

import java.util.Calendar;
import java.util.Date;

public final class DateUtils {

    private DateUtils() {
        // utility class
    }

    public static int daysSince(final long date) {
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

    public static int daysSince(final Calendar date) {
        return daysSince(date.getTimeInMillis());
    }

    public static boolean isPastEvent(final Geocache cache) {
        if (!cache.isEventCache()) {
            return false;
        }
        final Date hiddenDate = cache.getHiddenDate();
        return hiddenDate != null && DateUtils.daysSince(hiddenDate.getTime()) > 0;
    }

    /**
     * Return whether the given date is *more* than 1 day away. We allow 1 day to be "present time" to compensate for
     * potential timezone issues.
     * 
     * @param date
     * @return
     */
    public static boolean isFuture(final Calendar date) {
        return daysSince(date) < -1;
    }

}
