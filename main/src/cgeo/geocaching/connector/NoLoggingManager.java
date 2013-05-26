package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;

import android.net.Uri;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class NoLoggingManager implements ILoggingManager {

    @Override
    public void init() {
        // nothing to do
    }

    @Override
    public LogResult postLog(Geocache cache, LogType logType, Calendar date, String log, List<TrackableLog> trackableLogs) {
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
    public List<TrackableLog> getTrackables() {
        return Collections.emptyList();
    }

    @Override
    public List<LogType> getPossibleLogTypes() {
        return Collections.emptyList();
    }

}
