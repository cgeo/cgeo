package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.VisitCacheActivity;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;

import android.app.Activity;
import android.net.Uri;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class OkapiLoggingManager implements ILoggingManager {

    private final OCApiConnector connector;
    private final Geocache cache;
    private VisitCacheActivity activity;

    private final static List<LogType> standardLogTypes = Arrays.asList(LogType.FOUND_IT, LogType.DIDNT_FIND_IT, LogType.NOTE, LogType.NEEDS_MAINTENANCE);
    private final static List<LogType> eventLogTypes = Arrays.asList(LogType.WILL_ATTEND, LogType.ATTENDED, LogType.NOTE);

    public OkapiLoggingManager(Activity activity, OCApiConnector connector, Geocache cache) {
        this.connector = connector;
        this.cache = cache;
        this.activity = (VisitCacheActivity) activity;
    }

    @Override
    public void init() {
        activity.onLoadFinished();
    }

    @Override
    public LogResult postLog(Geocache cache, LogType logType, Calendar date, String log, List<TrackableLog> trackableLogs) {
        return OkapiClient.postLog(cache, logType, date, log, connector);
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
