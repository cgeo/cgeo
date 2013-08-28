package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;

import android.net.Uri;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class OkapiLoggingManager implements ILoggingManager {

    private final OCApiLiveConnector connector;
    private final Geocache cache;
    private LogCacheActivity activity;

    private final static List<LogType> standardLogTypes = Arrays.asList(LogType.FOUND_IT, LogType.DIDNT_FIND_IT, LogType.NOTE);
    private final static List<LogType> eventLogTypes = Arrays.asList(LogType.WILL_ATTEND, LogType.ATTENDED, LogType.NOTE);

    public OkapiLoggingManager(final LogCacheActivity activity, OCApiLiveConnector connector, Geocache cache) {
        this.connector = connector;
        this.cache = cache;
        this.activity = activity;
    }

    @Override
    public void init() {
        activity.onLoadFinished();
    }

    @Override
    public LogResult postLog(Geocache cache, LogType logType, Calendar date, String log, String logPassword, List<TrackableLog> trackableLogs) {
        final LogResult result = OkapiClient.postLog(cache, logType, date, log, logPassword, connector);
        connector.login(null, null);
        return result;
    }

    @Override
    public ImageResult postLogImage(String logId, String imageCaption, String imageDescription, Uri imageUri) {
        return new ImageResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    public boolean hasLoaderError() {
        return false;
    }

    @Override
    public List<TrackableLog> getTrackables() {
        return Collections.emptyList();
    }

    @Override
    public List<LogType> getPossibleLogTypes() {
        if (cache.isEventCache()) {
            return eventLogTypes;
        }

        return standardLogTypes;
    }
}
