package cgeo.geocaching.utils;

import cgeo.geocaching.models.Geocache;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nullable;

public final class CalendarUtils {

    public static final String PATTERN_YYYYMM = "yyyy-MM";
    public static final String PATTERN_YYYYMMDD = "yyyy-MM-dd";

    private CalendarUtils() {
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
        return hiddenDate != null && daysSince(hiddenDate.getTime()) > 0;
    }

    /**
     * Return whether the given date is *more* than 1 day away. We allow 1 day to be "present time" to compensate for
     * potential timezone issues.
     *
     * @param date
     *            the date
     */
    public static boolean isFuture(final Calendar date) {
        return daysSince(date) < -1;
    }

    /**
     * Open the calendar app on a specific date.
     */
    public static void openCalendar(final Activity activity, final Date date) {
        final Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
        builder.appendPath("time");
        ContentUris.appendId(builder, date.getTime());
        final Intent intent = new Intent(Intent.ACTION_VIEW).setData(builder.build());
        activity.startActivity(intent);
    }

    /**
     * returns current date/time formatted according to the supplied {SimpleDateFormat} string
     * @param format string
     * @return formatted date
     */
    public static String formatDateTime(final String format) {
        final Date date = Calendar.getInstance().getTime();
        final DateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        return dateFormat.format(date);
    }

    /**
     * returns given date in format yyyy-mm, or empty string if null given
     * @param date Date to be formatted
     * @return String formatted date
     */
    public static String yearMonth(@Nullable final Date date) {
        final DateFormat dateFormat = new SimpleDateFormat(PATTERN_YYYYMM, Locale.getDefault());
        return null == date ? "" : dateFormat.format(date);
    }

    /**
     * returns given date in format yyyy-mm, or empty string if 0 given
     * @param date Date to be formatted
     * @return String formatted date
     */
    public static String yearMonth(final long date) {
        final DateFormat dateFormat = new SimpleDateFormat(PATTERN_YYYYMM, Locale.getDefault());
        return 0 == date ? "" : dateFormat.format(date);
    }

    /**
     * returns given date in format yyyy-mm-dd, or empty string if 0 given
     * @param date Date to be formatted
     * @return String formatted date
     */
    public static String yearMonthDay(final long date) {
        final SimpleDateFormat pattern = new SimpleDateFormat(PATTERN_YYYYMMDD, Locale.getDefault());
        return date == 0 ? "" : pattern.format(date);
    }
}
