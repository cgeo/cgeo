package cgeo.geocaching.connector.ec;

import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogContextInfo;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.List;

class ECLoggingManager extends AbstractLoggingManager {

    ECLoggingManager(@NonNull final ECConnector connector, @NonNull final Geocache cache) {
        super(connector, cache);
    }

    @Override
    @NonNull
    public final LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem, final float rating) {
        return ECApi.postLog(getCache(), logType, date, log);
    }

    @Override
    @NonNull
    public final ImageResult postLogImage(final String logId, final Image image) {
        return new ImageResult(StatusCode.LOG_POST_ERROR);
    }

    @NonNull
    @Override
    public LogContextInfo getLogContextInfo(@Nullable final String serviceLogId) {
        return new LogContextInfo(this, serviceLogId);
    }

}
