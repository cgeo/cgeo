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
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.CalendarUtils
import cgeo.geocaching.utils.Log

import android.app.Activity

import androidx.annotation.NonNull

import java.util.Date

class CalendarAdder {

    private CalendarAdder() {
        // utility class
    }

    public static Unit addToCalendar(final Activity activity, final Geocache cache) {
        val hiddenDate: Date = cache.getHiddenDate()
        if (hiddenDate == null) {
            // This should not happen, because menu entries to add caches to the calendar are enabled
            // only if the cache can indeed be added to the calendar.
            Log.e("addToCalendar: attempt to add a cache without a hiddenDate (" + cache.getGeocode() + ")")
            return
        }
        val entry: CalendarEntry = CalendarEntry(cache, hiddenDate)
        if (CalendarUtils.isPastEvent(cache)) {
            // Event is in the past, only add to calendar after confirmation
            SimpleDialog.of(activity).setTitle(R.string.helper_calendar_pastevent_title).setMessage(R.string.helper_calendar_pastevent_question).setButtons(SimpleDialog.ButtonTextSet.YES_NO).confirm(
                    () -> entry.addEntryToCalendar(activity))
        } else {
            entry.addEntryToCalendar(activity)
        }
    }

}
