package cgeo.geocaching.connector.ec;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogCacheActivity;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.net.Uri;

import java.util.Calendar;
import java.util.List;

class ECLoggingManager extends AbstractLoggingManager {

    @NonNull
    private final ECConnector connector;

    @NonNull
    private final Geocache cache;

    @NonNull
    private final LogCacheActivity activity;

    ECLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final ECConnector connector, @NonNull final Geocache cache) {
        this.connector = connector;
        this.cache = cache;
        this.activity = activity;
    }

    @Override
    public final void init() {
        activity.onLoadFinished();
    }

    @Override
    @NonNull
    public final LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs) {
        return ECApi.postLog(cache, logType, date, log);
    }

    @Override
    @NonNull
    public final ImageResult postLogImage(final String logId, final String imageCaption, final String imageDescription, final Uri imageUri) {
        return new ImageResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes() {
        return connector.getPossibleLogTypes(cache);
    }

}
