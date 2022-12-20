package cgeo.geocaching.calendar;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.Log;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.Date;

public class CalendarAdder {

    private CalendarAdder() {
        // utility class
    }

    public static void addToCalendar(@NonNull final Activity activity, @NonNull final Geocache cache) {
        final Date hiddenDate = cache.getHiddenDate();
        if (hiddenDate == null) {
            // This should not happen, because menu entries to add caches to the calendar are enabled
            // only if the cache can indeed be added to the calendar.
            Log.e("addToCalendar: attempt to add a cache without a hiddenDate (" + cache.getGeocode() + ")");
            return;
        }
        final CalendarEntry entry = new CalendarEntry(cache, hiddenDate);
        if (cache.isPastEvent()) {
            // Event is in the past, only add to calendar after confirmation
            SimpleDialog.of(activity).setTitle(R.string.helper_calendar_pastevent_title).setMessage(R.string.helper_calendar_pastevent_question).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm(
                    (dialog, id) -> entry.addEntryToCalendar(activity));
        } else {
            entry.addEntryToCalendar(activity);
        }
    }

}
