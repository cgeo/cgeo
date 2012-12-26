package cgeo.calendar;

import android.content.ContentValues;
import android.net.Uri;
import android.text.Html;

import java.util.Date;

class AddEntry extends AbstractAddEntry {

    private int calendarId;

    /**
     * @param entry
     * @param calendarId
     *            The selected calendar
     */
    public AddEntry(CalendarEntry entry, CalendarActivity activity, int calendarId) {
        super(entry, activity);
        this.calendarId = calendarId;
    }

    @Override
    protected void addEntryToCalendarInternal() {
        final Uri calendarProvider = Compatibility.getCalendarEventsProviderURI();

        final Date eventDate = entry.parseDate();
        final String description = entry.parseDescription();
        final String eventLocation = entry.parseLocation();

        // values
        final ContentValues event = new ContentValues();
        event.put("calendar_id", calendarId);
        if (entry.getStartTimeMinutes() >= 0) {
            event.put("dtstart", eventDate.getTime() + entry.getStartTimeMinutes() * 60000L);
        }
        else {
            event.put("dtstart", eventDate.getTime() + 43200000); // noon
            event.put("dtend", eventDate.getTime() + 43200000 + 3600000); // + one hour
            event.put("allDay", 1);
        }
        event.put("eventTimezone", "UTC");
        event.put("title", Html.fromHtml(entry.getName()).toString());
        event.put("description", description);

        if (eventLocation.length() > 0) {
            event.put("eventLocation", eventLocation);
        }
        event.put("hasAlarm", 0);

        activity.getContentResolver().insert(calendarProvider, event);
    }

}
