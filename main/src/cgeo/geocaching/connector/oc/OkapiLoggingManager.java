package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;

import android.net.Uri;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class OkapiLoggingManager extends AbstractLoggingManager {

    private final OCApiLiveConnector connector;
    private final Geocache cache;
    private LogCacheActivity activity;
    private boolean hasLoaderError = true;

    public OkapiLoggingManager(final LogCacheActivity activity, final OCApiLiveConnector connector, final Geocache cache) {
        this.connector = connector;
        this.cache = cache;
        this.activity = activity;
    }

    @Override
    public final void init() {
        if (connector.isLoggedIn()) {
            hasLoaderError = false;
        }
        activity.onLoadFinished();
    }

    @Override
    public final LogResult postLog(final LogType logType, final Calendar date, final String log, final String logPassword, final List<TrackableLog> trackableLogs) {
        final LogResult result = OkapiClient.postLog(cache, logType, date, log, logPassword, connector);
        connector.login(null, null);
        return result;
    }

    @Override
    public final ImageResult postLogImage(final String logId, final String imageCaption, final String imageDescription, final Uri imageUri) {
        return new ImageResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    public List<LogType> getPossibleLogTypes() {
        if (hasLoaderError) {
            return Collections.emptyList();
        }
        return connector.getPossibleLogTypes(cache);
    }

    @Override
    public boolean hasLoaderError() {
        return hasLoaderError;
    }

}
