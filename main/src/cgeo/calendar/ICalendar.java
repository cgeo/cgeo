package cgeo.calendar;

import org.eclipse.jdt.annotation.NonNull;

public interface ICalendar {
    @NonNull static final String CALENDAR_ADDON_URI = "market://details?id=cgeo.calendar";

    @NonNull static final String INTENT = "cgeo.calendar.RESERVE";

    @NonNull static final String URI_SCHEME = "add";
    @NonNull static final String URI_HOST = "cgeo.org";

    @NonNull static final String PARAM_SHORT_DESC = "shortDesc"; // cache short description
    @NonNull static final String PARAM_HIDDEN_DATE = "hiddenDate"; // cache hidden date in milliseconds
    @NonNull static final String PARAM_URL = "url"; // cache URL
    @NonNull static final String PARAM_NOTE = "note"; // personal note
    @NonNull static final String PARAM_NAME = "name"; // cache name
    @NonNull static final String PARAM_LOCATION = "location"; // cache location, or empty string
    @NonNull static final String PARAM_COORDS = "coords"; // cache coordinates, or empty string
    @NonNull static final String PARAM_START_TIME_MINUTES = "time"; // time of start
}
