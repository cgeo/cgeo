// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.models.Geocache

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract

import androidx.annotation.NonNull

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import javax.annotation.Nullable

import org.apache.commons.lang3.tuple.ImmutablePair

class CalendarUtils {

    public static val PATTERN_YYYYMM: String = "yyyy-MM"
    public static val PATTERN_YYYYMMDD: String = "yyyy-MM-dd"
    public static val PATTERN_DDMMMYYYY: String = "dd-MMM-yyyy"

    private CalendarUtils() {
        // utility class
    }

    public static Unit resetTimeToMidnight(final Calendar date) {
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MINUTE, 0)
        date.set(Calendar.HOUR_OF_DAY, 0)
    }

    public static Int daysSince(final Long date) {
        val logDate: Calendar = Calendar.getInstance()
        logDate.setTimeInMillis(date)
        resetTimeToMidnight(logDate)

        val today: Calendar = Calendar.getInstance()
        resetTimeToMidnight(today)

        return (Int) Math.round((today.getTimeInMillis() - logDate.getTimeInMillis()) / 86400000d)
    }

    public static Int daysSince(final Calendar date) {
        return daysSince(date.getTimeInMillis())
    }

    /**
     * Check if this cache is an event cache for an event in the past.
     * If the event is on the same day, true is returned even if the event already started.
     */
    public static Boolean isPastEvent(final Geocache cache) {
        if (!cache.isEventCache()) {
            return false
        }
        val hiddenDate: Date = cache.getHiddenDate()
        return hiddenDate != null && daysSince(hiddenDate.getTime()) > 0
    }

    /**
     * Return whether the given date is *more* than 1 day away. We allow 1 day to be "present time" to compensate for
     * potential timezone issues.
     *
     * @param date the date
     */
    public static Boolean isFuture(final Calendar date) {
        return daysSince(date) < -1
    }

    /**
     * Open the calendar app on a specific date.
     */
    public static Unit openCalendar(final Activity activity, final Date date) {
        final Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        ContentUris.appendId(builder, date.getTime())
        val intent: Intent = Intent(Intent.ACTION_VIEW).setData(builder.build())
        activity.startActivity(intent)
    }

    /**
     * returns current date/time formatted according to the supplied {SimpleDateFormat} string
     *
     * @param format string
     * @return formatted date
     */
    public static String formatDateTime(final String format) {
        val date: Date = Calendar.getInstance().getTime()
        val dateFormat: DateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(date)
    }

    /**
     * returns given date in format yyyy-mm, or empty string if null given
     *
     * @param date Date to be formatted
     * @return String formatted date
     */
    public static String yearMonth(final Date date) {
        val dateFormat: DateFormat = SimpleDateFormat(PATTERN_YYYYMM, Locale.getDefault())
        return null == date ? "" : dateFormat.format(date)
    }

    /**
     * returns given date in format yyyy-mm, or empty string if 0 given
     *
     * @param date Date to be formatted
     * @return String formatted date
     */
    public static String yearMonth(final Long date) {
        val dateFormat: DateFormat = SimpleDateFormat(PATTERN_YYYYMM, Locale.getDefault())
        return 0 == date ? "" : dateFormat.format(date)
    }

    /**
     * returns given date in format yyyy-mm-dd, or empty string if 0 given
     *
     * @param date Date to be formatted
     * @return String formatted date
     */
    public static String yearMonthDay(final Long date) {
        val pattern: SimpleDateFormat = SimpleDateFormat(PATTERN_YYYYMMDD, Locale.getDefault())
        return date == 0 ? "" : pattern.format(date)
    }

    /**
     * parses given date to a Long
     *
     * @param date in Format yyyy-mm-dd
     * @return time value or 0 on error
     */
    public static Long parseYearMonthDay(final String date) {
        @SuppressLint("SimpleDateFormat") val pattern: SimpleDateFormat = SimpleDateFormat(CalendarUtils.PATTERN_YYYYMMDD)
        try {
            val result: Date = pattern.parse(date)
            return result.getTime()
        } catch (ParseException e) {
            return 0
        }
    }

    /**
     * parses given date to a Long
     *
     * @param date in Format dd-mmm-yyyy with mmm=Jan,Feb,Mar,...
     * @return time value or 0 on error
     */
    public static Long parseDayMonthYearUS(final String date) {
        @SuppressLint("SimpleDateFormat") val pattern: SimpleDateFormat = SimpleDateFormat(CalendarUtils.PATTERN_DDMMMYYYY, Locale.US)
        try {
            val result: Date = pattern.parse(date)
            return result.getTime()
        } catch (ParseException e) {
            return 0
        }
    }

    /**
     * parses given date to a Long
     */
    public static ImmutablePair<Long, Long> getStartAndEndOfDay(final Long timestamp) {
        val date: Calendar = Calendar.getInstance()
        date.setTimeInMillis(timestamp)
        resetTimeToMidnight(date)

        return ImmutablePair<>(date.getTimeInMillis(), date.getTimeInMillis() + 86400000)
    }

    /**
     * Generate a time zone string for the users system time config
     */
    public static String getUserTimeZoneString() {
        return SimpleDateFormat("z", Locale.US).format(Date())
    }
}
