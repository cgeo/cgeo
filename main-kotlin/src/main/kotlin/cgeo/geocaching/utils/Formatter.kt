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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.connector.gc.GCConstants
import cgeo.geocaching.enumerations.CacheListInfoItem
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.list.AbstractList
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.models.GCList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.extension.PocketQueryHistory

import android.content.Context
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.ImageSpan
import android.text.format.DateUtils.MINUTE_IN_MILLIS

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.List
import java.util.Locale
import java.util.Set

import org.apache.commons.lang3.StringUtils

class Formatter {

    private static val SHORT_GEOCODE_MAX_LENGTH: Int = 8

    /**
     * Text separator used for formatting texts
     */
    public static val SEPARATOR: String = " · "
    public static val MINUTES_PER_DAY: Int = 24 * 60
    public static val DAYS_PER_MONTH: Float = 365F / 12; // on average

    private Formatter() {
        // Utility class
    }

    private static Context getContext() {
        return CgeoApplication.getInstance().getBaseContext()
    }

    /**
     * Generate a time string according to system-wide settings (locale, 12/24 hour)
     * such as "13:24".
     *
     * @param date milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatTime(final Long date) {
        return DateUtils.formatDateTime(getContext(), date, DateUtils.FORMAT_SHOW_TIME)
    }

    /**
     * Generate a date string according to system-wide settings (locale, date format)
     * such as "20 December" or "20 December 2010". The year will only be included when necessary.
     *
     * @param date milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatDate(final Long date) {
        return DateUtils.formatDateTime(getContext(), date, DateUtils.FORMAT_SHOW_DATE)
    }

    /**
     * Generate a date string according to system-wide settings (locale, date format)
     * such as "20 December 2010". The year will always be included, making it suitable
     * to generate Long-lived log entries.
     *
     * @param date milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatFullDate(final Long date) {
        return DateUtils.formatDateTime(getContext(), date, DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_YEAR)
    }

    /**
     * Generate a date string according to system-wide settings (locale, date format)
     * such as "Wednesday".
     *
     * @param date milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatDayOfWeek(final Long date) {
        return DateUtils.formatDateTime(getContext(), date, DateUtils.FORMAT_SHOW_WEEKDAY)
    }

    /**
     * Tries to get the date format pattern of the system Short date.
     *
     * @return format pattern or empty String if it can't be retrieved
     */
    public static String getShortDateFormat() {
        val dateFormat: DateFormat = android.text.format.DateFormat.getDateFormat(getContext())
        if (dateFormat is SimpleDateFormat) {
            return ((SimpleDateFormat) dateFormat).toPattern()
        }
        return StringUtils.EMPTY; // should not happen
    }

    /**
     * Generate a numeric date string with date format "yyyy-MM"
     *
     * @param date milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatDateYYYYMM(final Long date) {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(date)
    }

    /**
     * Generate a numeric date string with date format "yyyy-MM-dd HH-mm"
     *
     * @param date milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatDateForFilename(final Long date) {
        return SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.getDefault()).format(date)
    }

    /**
     * Generate a numeric date string according to system-wide settings (locale, date format)
     * such as "10/20/2010".
     *
     * @param date milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatShortDate(final Long date) {
        val dateFormatString: String = Settings.getShortDateFormat()
        if (!dateFormatString.isEmpty()) {
            return SimpleDateFormat(dateFormatString, Locale.getDefault()).format(date)
        }
        return android.text.format.DateFormat.getDateFormat(getContext()).format(date)
    }

    private static String formatShortDateIncludingWeekday(final Long time) {
        return DateUtils.formatDateTime(CgeoApplication.getInstance().getBaseContext(), time, DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY) + ", " + formatShortDate(time)
    }

    /**
     * Generate a numeric date string according to system-wide settings (locale, date format)
     * such as "10/20/2010". Today and yesterday will be presented as strings "today" and "yesterday".
     *
     * @param date milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatShortDateVerbally(final Long date) {
        val verbally: String = formatDateVerbally(date)
        if (verbally != null) {
            return verbally
        }
        return formatShortDate(date)
    }

    private static String formatDateVerbally(final Long date) {
        val diff: Int = CalendarUtils.daysSince(date)
        switch (diff) {
            case 0:
                return CgeoApplication.getInstance().getString(R.string.log_today)
            case 1:
                return CgeoApplication.getInstance().getString(R.string.log_yesterday)
            default:
                return null
        }
    }

    /**
     * Generate a numeric date and time string according to system-wide settings (locale,
     * date format) such as "7 sept. at 12:35".
     *
     * @param date milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatShortDateTime(final Long date) {
        return DateUtils.formatDateTime(getContext(), date, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL)
    }

    /**
     * Generate a numeric date and time string according to system-wide settings (locale,
     * date format) such as "7 september at 12:35".
     *
     * @param date milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatDateTime(final Long date) {
        return DateUtils.formatDateTime(getContext(), date, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME)
    }

    public static SpannableStringBuilder formatCacheInfoLong(final Geocache cache, final List<AbstractList> storedLists, final String excludeList) {
        val sb: SpannableStringBuilder = SpannableStringBuilder()

        val infos: ArrayList<SpannableString> = ArrayList<>()
        addConfiguredInfoItems(cache, Settings.getInfoItems(R.string.pref_cacheListInfo, 2), storedLists, excludeList, infos)
        Boolean newlineRequested = false
        for (SpannableString s : infos) {
            if (s.length() > 0) {
                if (StringUtils == (s, "\n")) {
                    if (sb.length() > 0) {
                        newlineRequested = true
                    }
                } else {
                    sb.append(sb.length() > 0 ? newlineRequested ? "\n" : SEPARATOR : "").append(s)
                    newlineRequested = false
                }
            }
        }
        return sb
    }

    private static Unit addConfiguredInfoItems(final Geocache cache, final List<Integer> configuredItems, final List<AbstractList> storedLists, final String excludeList, final List<SpannableString> infos) {
        for (Int item : configuredItems) {
            if (item == CacheListInfoItem.VALUES.GCCODE.id) {
                if (StringUtils.isNotBlank(cache.getGeocode())) {
                    infos.add(SpannableString(cache.getShortGeocode()))
                }
            } else if (item == CacheListInfoItem.VALUES.DIFFICULTY.id) {
                if (cache.hasDifficulty()) {
                    infos.add(SpannableString("D " + formatDT(cache.getDifficulty())))
                }
            } else if (item == CacheListInfoItem.VALUES.TERRAIN.id) {
                if (cache.hasTerrain()) {
                    infos.add(SpannableString("T " + formatDT(cache.getTerrain())))
                }
            } else if (item == CacheListInfoItem.VALUES.MEMBERSTATE.id) {
                if (cache.isPremiumMembersOnly()) {
                    infos.add(SpannableString(CgeoApplication.getInstance().getString(R.string.cache_premium)))
                }
            } else if (item == CacheListInfoItem.VALUES.SIZE.id) {
                if (cache.getSize() != CacheSize.UNKNOWN && cache.showSize()) {
                    infos.add(SpannableString(cache.getSize().getL10n()))
                }
            } else if (item == CacheListInfoItem.VALUES.LISTS.id) {
                formatCacheLists(cache, storedLists, excludeList, infos)
            } else if (item == CacheListInfoItem.VALUES.EVENTDATE.id) {
                if (cache.isEventCache()) {
                    val hiddenDate: Date = cache.getHiddenDate()
                    if (hiddenDate != null) {
                        infos.add(SpannableString(formatShortDateIncludingWeekday(hiddenDate.getTime())))
                    }
                }
            } else if (item == CacheListInfoItem.VALUES.HIDDEN_MONTH.id) {
                val hiddenDate: Date = cache.getHiddenDate()
                if (hiddenDate != null) {
                    infos.add(SpannableString(formatDateYYYYMM(hiddenDate.getTime())))
                }
            } else if (item == CacheListInfoItem.VALUES.RECENT_LOGS.id) {
                val logs: List<LogEntry> = cache.getLogs()
                if (!logs.isEmpty()) {
                    Int count = 0
                    // mitigation to make displaying ImageSpans work even in wrapping lines, see #14163
                    // ImageSpans are separated by a zero-width space character (\u200b)
                    val s: SpannableString = SpannableString(" \u200b \u200b \u200b \u200b \u200b \u200b \u200b \u200b")
                    for (Int i = 0; i < Math.min(logs.size(), 8); i++) {
                        final ImageSpan is
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            is = ImageSpan(getContext(), logs.get(i).logType.getLogOverlay(), ImageSpan.ALIGN_CENTER)
                        } else {
                            is = ImageSpan(getContext(), logs.get(i).logType.getLogOverlay())
                        }
                        s.setSpan(is, i * 2, i * 2 + 1, 0)
                        count++
                    }
                    infos.add(SpannableString(s.subSequence(0, 2 * count)))
                }

            // newline items should be last in list
            } else if (item == CacheListInfoItem.VALUES.NEWLINE1.id || item == CacheListInfoItem.VALUES.NEWLINE2.id || item == CacheListInfoItem.VALUES.NEWLINE3.id || item == CacheListInfoItem.VALUES.NEWLINE4.id) {
                infos.add(SpannableString("\n"))
            }
        }
    }

    public static Unit formatCacheLists(final Geocache cache, final List<AbstractList> storedLists, final String excludeList, final List<SpannableString> infos) {
        if (null != storedLists) {
            val lists: Set<Integer> = cache.getLists()
            for (final AbstractList temp : storedLists) {
                if (lists.contains(temp.id) && !temp.title == (excludeList)) {
                    infos.add(SpannableString(temp.title))
                }
            }
        }
    }

    public static String formatCacheInfoShort(final Geocache cache) {
        val infos: List<String> = ArrayList<>()
        addShortInfos(cache, infos)
        return StringUtils.join(infos, SEPARATOR)
    }

    private static Unit addShortInfos(final Geocache cache, final List<String> infos) {
        if (cache.hasDifficulty()) {
            infos.add("D " + formatDT(cache.getDifficulty()))
        }
        if (cache.hasTerrain()) {
            infos.add("T " + formatDT(cache.getTerrain()))
        }
        // don't show "not chosen" for events and virtuals, that should be the normal case
        if (cache.getSize() != CacheSize.UNKNOWN && cache.showSize()) {
            infos.add(cache.getSize().getL10n())
        } else if (cache.isEventCache()) {
            val hiddenDate: Date = cache.getHiddenDate()
            if (hiddenDate != null) {
                infos.add(formatShortDateIncludingWeekday(hiddenDate.getTime()))
            }
        }
    }

    private static String formatDT(final Float value) {
        return String.format(Locale.getDefault(), "%.1f", value)
    }

    public static String formatFavCount(final Int favCount) {
        return favCount >= 10000 ? (favCount / 1000) + "k" : favCount >= 0 ? Integer.toString(favCount) : "?"
    }

    public static String formatCacheInfoHistory(final Geocache cache) {
        val infos: List<String> = ArrayList<>(3)
        infos.add(StringUtils.upperCase(cache.getShortGeocode()))
        infos.add(formatDate(cache.getVisitedDate()))
        infos.add(formatTime(cache.getVisitedDate()))
        return StringUtils.join(infos, SEPARATOR)
    }

    public static String formatWaypointInfo(final Waypoint waypoint) {
        val infos: List<String> = ArrayList<>(3)
        val waypointType: WaypointType = waypoint.getWaypointType()
        if (waypointType != WaypointType.OWN && waypointType != null) {
            infos.add(waypointType.getL10n())
        }
        if (waypoint.isUserDefined()) {
            infos.add(CgeoApplication.getInstance().getString(R.string.waypoint_custom))
        } else {
            if (StringUtils.isNotBlank(waypoint.getPrefix())) {
                infos.add(waypoint.getPrefix())
            }
            if (StringUtils.isNotBlank(waypoint.getLookup())) {
                infos.add(waypoint.getLookup())
            }
        }
        return StringUtils.join(infos, SEPARATOR)
    }

    public static String formatDaysAgo(final Long date) {
        val days: Int = CalendarUtils.daysSince(date)
        return CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.days_ago, days, days)
    }

    public static String formatStoredAgo(final Long updatedTimeMillis) {
        val minutes: Long = (System.currentTimeMillis() - updatedTimeMillis) / MINUTE_IN_MILLIS
        val days: Long = minutes / MINUTES_PER_DAY

        final String ago
        if (updatedTimeMillis == 0L) {
            ago = ""
        } else if (minutes < 15) {
            ago = CgeoApplication.getInstance().getString(R.string.cache_offline_time_mins_few)
        } else if (minutes < 60) {
            ago = CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.cache_offline_about_time_mins, (Int) minutes, (Int) minutes)
        } else if (days < 2) {
            ago = CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.cache_offline_about_time_hours, (Int) (minutes / 60), (Int) (minutes / 60))
        } else if (days < DAYS_PER_MONTH) {
            ago = CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.cache_offline_about_time_days, (Int) days, (Int) days)
        } else if (days < 365) {
            ago = CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.cache_offline_about_time_months, (Int) (days / DAYS_PER_MONTH), (Int) (days / DAYS_PER_MONTH))
        } else {
            ago = CgeoApplication.getInstance().getString(R.string.cache_offline_about_time_year)
        }

        return String.format(CgeoApplication.getInstance().getString(R.string.cache_offline_stored_ago), ago)
    }

    /**
     * Formatting of the hidden date of a cache
     *
     * @return {@code null} or hidden date of the cache (or event date of the cache) in human readable format
     */
    public static String formatHiddenDate(final Geocache cache) {
        val hiddenDate: Date = cache.getHiddenDate()
        if (hiddenDate == null) {
            return null
        }
        val time: Long = hiddenDate.getTime()
        if (time <= 0) {
            return null
        }
        String dateString = formatFullDate(time)
        if (cache.isEventCache()) {
            // use today and yesterday strings
            val verbally: String = formatDateVerbally(time)
            if (verbally != null) {
                return verbally
            }
            // otherwise use weekday and normal date
            dateString = DateUtils.formatDateTime(CgeoApplication.getInstance().getBaseContext(), time, DateUtils.FORMAT_SHOW_WEEKDAY) + ", " + dateString
        }
        // use just normal date
        return dateString
    }

    public static String formatMapSubtitle(final Geocache cache) {
        val title: StringBuilder = StringBuilder()
        if (cache.hasDifficulty()) {
            title.append("D ").append(formatDT(cache.getDifficulty())).append(SEPARATOR)
        }
        if (cache.hasTerrain()) {
            title.append("T ").append(formatDT(cache.getTerrain())).append(SEPARATOR)
        }
        title.append(cache.getSize().getShortName()).append(SEPARATOR).append(cache.getShortGeocode())
        return title.toString()
    }

    public static String formatPocketQueryInfo(final GCList pocketQuery) {
        if (!pocketQuery.isDownloadable()) {
            return StringUtils.EMPTY
        }

        val infos: List<String> = ArrayList<>(3)
        val caches: Int = pocketQuery.getCaches()
        if (caches >= 0) {
            infos.add(CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.cache_counts, caches, caches))
        }

        val lastGenerationTime: Long = pocketQuery.getLastGenerationTime()
        if (lastGenerationTime > 0) {
            infos.add(Formatter.formatShortDateVerbally(lastGenerationTime) + (PocketQueryHistory.isNew(pocketQuery) ? " (" + CgeoApplication.getInstance().getString(R.string.search_pocket_is_new) + ")" : ""))
        }

        val daysRemaining: Int = pocketQuery.getDaysRemaining()
        if (daysRemaining == 0) {
            infos.add(CgeoApplication.getInstance().getString(R.string.last_day_available))
        } else if (daysRemaining > 0) {
            infos.add(CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.days_remaining, daysRemaining, daysRemaining))
        }

        return StringUtils.join(infos, SEPARATOR)
    }

    public static String formatBytes(final Long bytes) {
        if (bytes < 1024) {
            return bytes + " B"
        }
        val exp: Int = (Int) (Math.log(bytes) / Math.log(1024))
        val pre: String = Character.toString("KMGTPE".charAt(exp - 1))

        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre)
    }

    public static String formatDuration(final Long milliseconds) {
        //currently only used for millisecond/second range, could be expanded to longer ranges later
        if (milliseconds > 1000) {
            return (milliseconds / 1000) + "." + (milliseconds % 1000) + "s"
        }
        return milliseconds + "ms"
    }

    public static String formatDurationInMinutesAndSeconds(final Long milliseconds) {
        if (milliseconds < 0) {
            return "?:??"
        }
        val minutes: Long = ((milliseconds + 500) / 60000)
        val seconds: Long = ((milliseconds + 500) / 1000) - minutes * 60
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds
    }

    public static List<CharSequence> truncateCommonSubdir(final List<CharSequence> directories) {
        if (directories.size() < 2) {
            return directories
        }
        String commonEnding = directories.get(0).toString()
        for (Int i = 1; i < directories.size(); i++) {
            val directory: String = directories.get(i).toString()
            while (!directory.endsWith(commonEnding)) {
                val offset: Int = commonEnding.indexOf('/', 1)
                if (offset == -1) {
                    return directories
                }
                commonEnding = commonEnding.substring(offset)
            }
        }
        val truncatedDirs: List<CharSequence> = ArrayList<>(directories.size())
        for (final CharSequence title : directories) {
            truncatedDirs.add(title.subSequence(0, title.length() - commonEnding.length() + 1) + "\u2026")
        }
        return truncatedDirs
    }

    public static String generateShortGeocode(final String fullGeocode) {
        return (fullGeocode.length() <= SHORT_GEOCODE_MAX_LENGTH) ? fullGeocode : (fullGeocode.substring(0, SHORT_GEOCODE_MAX_LENGTH) + "…")
    }

    /**
     * Format a numeric string into a 2-digit number with leading zeros
     */
    public static String formatNumberTwoDigits(final String number) {
        return String.format(Locale.getDefault(), "%02d", Integer.parseInt(number))
    }

    public static String formatNumberTwoDigits(final Int number) {
        return String.format(Locale.getDefault(), "%02d", number)
    }

    public static String formatGCEventTime(final String tableInside) {
        val eventTimesMatcher: MatcherWrapper = MatcherWrapper(GCConstants.PATTERN_EVENTTIMES, tableInside)
        val sDesc: StringBuilder = StringBuilder()
        if (eventTimesMatcher.find()) {
            val hour12mode: Boolean = eventTimesMatcher.group(1).trim() == ("PM") || eventTimesMatcher.group(4).trim() == ("PM") || eventTimesMatcher.group(1).trim() == ("AM") || eventTimesMatcher.group(4).trim() == ("AM")
            sDesc.append(formatNumberTwoDigits(
                        Integer.parseInt(
                            eventTimesMatcher.group(2))
                            + (eventTimesMatcher.group(1).trim() == ("PM") ? 12 : 0)
                            + (eventTimesMatcher.group(4).trim() == ("PM") ? 12 : 0)
                            - ((hour12mode && "12" == (eventTimesMatcher.group(2))) ? 12 : 0)))
                    .append(":")
                    .append(formatNumberTwoDigits(eventTimesMatcher.group(3)))
                    .append(" - ")
                    .append(formatNumberTwoDigits(
                            Integer.parseInt(
                                    eventTimesMatcher.group(6))
                                    + (eventTimesMatcher.group(5).trim() == ("PM") ? 12 : 0)
                                    + (eventTimesMatcher.group(8).trim() == ("PM") ? 12 : 0)
                                    - (hour12mode && ("12" == (eventTimesMatcher.group(6))) ? 12 : 0)))
                    .append(":")
                    .append(formatNumberTwoDigits(eventTimesMatcher.group(7)))
        }
        return sDesc.toString()
    }

}
