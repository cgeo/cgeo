package cgeo.geocaching.connector.oc;

import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class OkapiLoggingManager extends AbstractLoggingManager {

    @NonNull
    private final OCApiLiveConnector connector;
    @NonNull
    private final Geocache cache;
    @NonNull
    private final LogCacheActivity activity;
    private boolean hasLoaderError = true;

    public OkapiLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final OCApiLiveConnector connector, @NonNull final Geocache cache) {
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
    @NonNull
    public final LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs) {
        final LogResult result = OkapiClient.postLog(cache, logType, date, log, logPassword, connector);
        connector.login(null, null);
        return result;
    }

    @Override
    @NonNull
    public final ImageResult postLogImage(final String logId, final Image image) {
        return new ImageResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    @NonNull
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
