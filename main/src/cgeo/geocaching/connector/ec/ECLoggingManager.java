package cgeo.geocaching.connector.ec;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LogType;

import android.net.Uri;

import java.util.Calendar;
import java.util.List;

class ECLoggingManager extends AbstractLoggingManager {

    private final ECConnector connector;
    private final Geocache cache;
    private final LogCacheActivity activity;

    ECLoggingManager(final LogCacheActivity activity, final ECConnector connector, final Geocache cache) {
        this.connector = connector;
        this.cache = cache;
        this.activity = activity;
    }

    @Override
    public final void init() {
        activity.onLoadFinished();
    }

    @Override
    public final LogResult postLog(final LogType logType, final Calendar date, final String log, final String logPassword, final List<TrackableLog> trackableLogs) {
        return ECApi.postLog(cache, logType, date, log);
    }

    @Override
    public final ImageResult postLogImage(final String logId, final String imageCaption, final String imageDescription, final Uri imageUri) {
        return null;
    }

    @Override
    public List<LogType> getPossibleLogTypes() {
        return connector.getPossibleLogTypes(cache);
    }

}
