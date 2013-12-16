package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;

import android.net.Uri;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class NoLoggingManager extends AbstractLoggingManager {

    @Override
    public void init() {
        // nothing to do
    }

    @Override
    public LogResult postLog(Geocache cache, LogType logType, Calendar date, String log, String logPassword, List<TrackableLog> trackableLogs) {
        return new LogResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    public ImageResult postLogImage(String logId, String imageCaption, String imageDescription, Uri imageUri) {
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
