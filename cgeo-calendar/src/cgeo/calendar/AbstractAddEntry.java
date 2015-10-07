package cgeo.calendar;

import org.eclipse.jdt.annotation.NonNull;

import android.util.Log;

abstract class AbstractAddEntry {

    @NonNull
    protected final CalendarEntry entry;
    @NonNull
    protected final CalendarActivity activity;

    public AbstractAddEntry(@NonNull final CalendarEntry entry, @NonNull final CalendarActivity activity) {
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
