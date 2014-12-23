package cgeo.geocaching.connector;

import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;

import android.net.Uri;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

class NoLoggingManager extends AbstractLoggingManager {

    @Override
    public void init() {
        // nothing to do
    }

    @Override
    public LogResult postLog(final LogType logType, final Calendar date, final String log, final String logPassword, final List<TrackableLog> trackableLogs) {
        return new LogResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    public ImageResult postLogImage(final String logId, final String imageCaption, final String imageDescription, final Uri imageUri) {
        return new ImageResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    public boolean hasLoaderError() {
        return true;
    }

    @Override
    public List<LogType> getPossibleLogTypes() {
        return Collections.emptyList();
    }

}
