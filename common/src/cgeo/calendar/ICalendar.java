package cgeo.calendar;

import android.support.annotation.NonNull;

public interface ICalendar {
    @Deprecated
    @NonNull String CALENDAR_ADDON_URI = "market://details?id=cgeo.calendar";

    @NonNull String INTENT = "cgeo.calendar.RESERVE";

    @NonNull String URI_SCHEME = "add";
    @NonNull String URI_HOST = "cgeo.org";

    @NonNull String PARAM_SHORT_DESC = "shortDesc"; // cache short description
    @NonNull String PARAM_HIDDEN_DATE = "hiddenDate"; // cache hidden date in milliseconds
    @NonNull String PARAM_URL = "url"; // cache URL
    @NonNull String PARAM_NOTE = "note"; // personal note
    @NonNull String PARAM_NAME = "name"; // cache name
    @NonNull String PARAM_LOCATION = "location"; // cache location, or empty string
    @NonNull String PARAM_COORDS = "coords"; // cache coordinates, or empty string
    @NonNull String PARAM_START_TIME_MINUTES = "time"; // time of start
}
