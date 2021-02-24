package cgeo.geocaching.connector.lc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.List;

import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;

class LCLoggingManager extends AbstractLoggingManager {

    @NonNull
    private final LCConnector connector;

    @NonNull
    private final Geocache cache;

    @NonNull
    private final LogCacheActivity activity;

    LCLoggingManager(@NonNull final LogCacheActivity activity, @NonNull final LCConnector connector, @NonNull final Geocache cache) {
        this.connector = connector;
        this.cache = cache;
        this.activity = activity;
    }

    @Override
    public final void init() {
        // TODO: This doesn't work because enablePostButton(false) is being called after this call
        activity.onLoadFinished();
    }

    @Override
    @NonNull
    public final LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem) {
        return LCApi.postLog(cache, logType, date, log);
    }

    @Override
    @NonNull
    public final ImageResult postLogImage(final String logId, final Image image) {
        return new ImageResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes() {
        return connector.getPossibleLogTypes(cache);
    }

}
