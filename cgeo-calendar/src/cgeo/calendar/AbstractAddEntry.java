package cgeo.calendar;

import android.util.Log;


abstract class AbstractAddEntry {

    protected CalendarEntry entry;
    protected CalendarActivity activity;

    public AbstractAddEntry(final CalendarEntry entry, CalendarActivity activity) {
        this.entry = entry;
        this.activity = activity;
    }

    void addEntryToCalendar() {
        try {
            addEntryToCalendarInternal();
        } catch (Exception e) {
            activity.showToast(R.string.event_fail);

            Log.e(CalendarActivity.LOG_TAG, "addToCalendar: " + e.toString());
        }
    }

    protected abstract void addEntryToCalendarInternal();

}
