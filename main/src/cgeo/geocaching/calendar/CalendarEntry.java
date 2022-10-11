package cgeo.geocaching.calendar;

import cgeo.geocaching.R;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

class CalendarEntry {

    @NonNull
    private final String shortDesc;
    @NonNull
    private final String longDesc;
    @NonNull
    private final Date hiddenDate;
    @NonNull
    private final String url;
    @NonNull
    private final String personalNote;
    @NonNull
    private final String name;
    @NonNull
    private final String coords;
    private final int startTimeMinutes;
    private final int endTimeMinutes;

    CalendarEntry(@NonNull final Geocache cache, @NonNull final Date hiddenDate) {
        this(TextUtils.stripHtml(StringUtils.defaultString(cache.getShortDescription())),
                TextUtils.stripHtml(StringUtils.defaultString(cache.getDescription())),
                hiddenDate,
                StringUtils.defaultString(cache.getUrl()),
                StringUtils.defaultString(cache.getPersonalNote()),
                cache.getName(),
                cache.getCoords() == null ? "" : cache.getCoords().format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW),
                cache.getEventStartTimeInMinutes(),
                cache.getEventEndTimeInMinutes()
                );
    }

    private CalendarEntry(@NonNull final String shortDesc, @NonNull final String longDesc, @NonNull final Date hiddenDate, @NonNull final String url,
                          @NonNull final String personalNote, @NonNull final String name, @NonNull final String coords,
                          final int startTimeMinutes, final int endTimeMinutes) {
        this.shortDesc = shortDesc;
        this.longDesc = longDesc;
        this.hiddenDate = hiddenDate;
        this.url = url;
        this.personalNote = personalNote;
        this.name = name;
        this.coords = coords;
        this.startTimeMinutes = startTimeMinutes;
        this.endTimeMinutes = endTimeMinutes;
    }

    /**
     * @return {@code Date} based on hidden date. Time is set to 00:00:00.
     */
    @NonNull
    private Date parseDate() {
        try {
            final Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(hiddenDate.getTime());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);

            return cal.getTime();
        } catch (final NumberFormatException e) {
            // cannot happen normally, but static code analysis does not know
            throw new IllegalStateException("hidden date must be a valid date for cache calendar entries");
        }
    }

    /**
     * @return description string with images removed and personal note included
     */
    @NonNull
    private String parseDescription() {
        final StringBuilder description = new StringBuilder();
        description.append(url);

        // if shortdesc is very short (seems to have no info) use description instead
        final String eventDesc = shortDesc.length() > 100 ? shortDesc : longDesc;

        if (StringUtils.isNotBlank(eventDesc)) {
            // remove images in short description
            final Spanned spanned = HtmlCompat.fromHtml(eventDesc.replaceAll("\n", "<br/>"), HtmlCompat.FROM_HTML_MODE_LEGACY);
            String text = spanned.toString();
            final ImageSpan[] spans = spanned.getSpans(0, spanned.length(), ImageSpan.class);
            for (int i = spans.length - 1; i >= 0; i--) {
                text = text.substring(0, spanned.getSpanStart(spans[i])) + text.substring(spanned.getSpanEnd(spans[i])) + "\n";
            }
            if (StringUtils.isNotBlank(text)) {
                description.append("\n\n");
                description.append(text);
            }
        }

        if (StringUtils.isNotBlank(personalNote)) {
            description.append("\n\n").append(TextUtils.stripHtml(personalNote));
        }

        return description.toString();
    }

    private void addEntryToCalendarInternal(final Context context) {
        final Date eventDate = parseDate();
        final String description = parseDescription();

        final Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Uri.parse("content://com.android.calendar/events"))
                .putExtra(CalendarContract.Events.TITLE, TextUtils.stripHtml(name))
                .putExtra(CalendarContract.Events.DESCRIPTION, description)
                .putExtra(CalendarContract.Events.HAS_ALARM, false)
                .putExtra(CalendarContract.Events.EVENT_TIMEZONE, "UTC");
        final long eventTime = eventDate.getTime();
        final int entryStartTimeMinutes = startTimeMinutes;
        if (entryStartTimeMinutes >= 0) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventTime + entryStartTimeMinutes * 60000L);
            if (endTimeMinutes >= 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventTime + endTimeMinutes * 60000L);
            }
        } else {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventTime);
            intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
        }
        if (StringUtils.isNotEmpty(coords)) {
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, coords);
        }
        context.startActivity(intent);
    }

    void addEntryToCalendar(final Context context) {
        try {
            addEntryToCalendarInternal(context);
        } catch (final Exception e) {
            showToast(context, R.string.event_fail);
            Log.e("addEntryToCalendar", e);
        }
    }

    public void showToast(final Context context, final int res) {
        final String text = context.getResources().getString(res);
        final Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        if (Build.VERSION.SDK_INT < 30) {
            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
        }
        toast.show();
    }
}
