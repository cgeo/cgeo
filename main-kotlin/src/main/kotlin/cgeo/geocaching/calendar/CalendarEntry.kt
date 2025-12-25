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

package cgeo.geocaching.calendar

import cgeo.geocaching.R
import cgeo.geocaching.location.GeopointFormatter
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.text.Spanned
import android.text.style.ImageSpan

import androidx.annotation.NonNull
import androidx.core.text.HtmlCompat

import java.util.Calendar
import java.util.Date

import org.apache.commons.lang3.StringUtils

class CalendarEntry {

    private final String shortDesc
    private final String longDesc
    private final Date hiddenDate
    private final String url
    private final String personalNote
    private final String name
    private final String coords
    private final Int startTimeMinutes
    private final Int endTimeMinutes

    CalendarEntry(final Geocache cache, final Date hiddenDate) {
        this(TextUtils.stripHtml(StringUtils.defaultString(cache.getShortDescription())),
                TextUtils.stripHtml(StringUtils.defaultString(cache.getDescription())),
                hiddenDate,
                StringUtils.defaultString(cache.getUrl()),
                StringUtils.defaultString(cache.getPersonalNote()),
                cache.getName(),
                cache.getCoords() == null ? "" : cache.getCoords().format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW),
                cache.getEventStartTimeInMinutes(),
                cache.getEventEndTimeInMinutes()
                )
    }

    private CalendarEntry(final String shortDesc, final String longDesc, final Date hiddenDate, final String url,
                          final String personalNote, final String name, final String coords,
                          final Int startTimeMinutes, final Int endTimeMinutes) {
        this.shortDesc = shortDesc
        this.longDesc = longDesc
        this.hiddenDate = hiddenDate
        this.url = url
        this.personalNote = personalNote
        this.name = name
        this.coords = coords
        this.startTimeMinutes = startTimeMinutes
        this.endTimeMinutes = endTimeMinutes
    }

    /**
     * @return {@code Date} based on hidden date. Time is set to 00:00:00.
     */
    private Date parseDate() {
        try {
            val cal: Calendar = Calendar.getInstance()
            cal.setTimeInMillis(hiddenDate.getTime())
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)

            return cal.getTime()
        } catch (final NumberFormatException e) {
            // cannot happen normally, but static code analysis does not know
            throw IllegalStateException("hidden date must be a valid date for cache calendar entries")
        }
    }

    /**
     * @return description string with images removed and personal note included
     */
    private String parseDescription() {
        val description: StringBuilder = StringBuilder()
        description.append(url)

        // if shortdesc is very Short (seems to have no info) use description instead
        val eventDesc: String = shortDesc.length() > 100 ? shortDesc : longDesc

        if (StringUtils.isNotBlank(eventDesc)) {
            // remove images in Short description
            val spanned: Spanned = HtmlCompat.fromHtml(eventDesc.replaceAll("\n", "<br/>"), HtmlCompat.FROM_HTML_MODE_LEGACY)
            String text = spanned.toString()
            final ImageSpan[] spans = spanned.getSpans(0, spanned.length(), ImageSpan.class)
            for (Int i = spans.length - 1; i >= 0; i--) {
                text = text.substring(0, spanned.getSpanStart(spans[i])) + text.substring(spanned.getSpanEnd(spans[i])) + "\n"
            }
            if (StringUtils.isNotBlank(text)) {
                description.append("\n\n")
                description.append(text)
            }
        }

        if (StringUtils.isNotBlank(personalNote)) {
            description.append("\n\n").append(TextUtils.stripHtml(personalNote))
        }

        return description.toString()
    }

    private Unit addEntryToCalendarInternal(final Context context) {
        val eventDate: Date = parseDate()
        val description: String = parseDescription()

        val intent: Intent = Intent(Intent.ACTION_INSERT)
                .setData(Uri.parse("content://com.android.calendar/events"))
                .putExtra(CalendarContract.Events.TITLE, TextUtils.stripHtml(name))
                .putExtra(CalendarContract.Events.DESCRIPTION, description)
                .putExtra(CalendarContract.Events.HAS_ALARM, false)
                .putExtra(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
        val eventTime: Long = eventDate.getTime()
        val entryStartTimeMinutes: Int = startTimeMinutes
        if (entryStartTimeMinutes >= 0) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventTime + entryStartTimeMinutes * 60000L)
            if (endTimeMinutes >= 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventTime + endTimeMinutes * 60000L)
            }
        } else {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventTime)
            intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        }
        if (StringUtils.isNotEmpty(coords)) {
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, coords)
        }
        context.startActivity(intent)
    }

    Unit addEntryToCalendar(final Context context) {
        try {
            addEntryToCalendarInternal(context)
        } catch (final Exception e) {
            ViewUtils.showToast(context, R.string.event_fail)
            Log.e("addEntryToCalendar", e)
        }
    }
}
