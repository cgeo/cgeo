package cgeo.calendar;

import android.util.Log;

abstract class AbstractAddEntry {

    protected final CalendarEntry entry;
    protected final CalendarActivity activity;

    public AbstractAddEntry(final CalendarEntry entry, final CalendarActivity activity) {
        this.entry = entry;
        this.activity = activity;
    }

    void addEntryToCalendar() {
        try {
            addEntryToCalendarInternal();
            activity.showToast(R.string.event_success);
        } catch (final Exception e) {
            activity.showToast(R.string.event_fail);

            Log.e(CalendarActivity.LOG_TAG, "addToCalendar", e);
        }
    }

    protected abstract void addEntryToCalendarInternal();

}
