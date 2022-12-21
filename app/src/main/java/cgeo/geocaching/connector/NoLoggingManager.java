package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

class NoLoggingManager extends AbstractLoggingManager {

    @Override
    public void init() {
        // nothing to do
    }

    @Override
    @NonNull
    public LogResult postLog(@NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, @NonNull final ReportProblemType reportProblem) {
        return new LogResult(StatusCode.LOG_POST_ERROR, "", "");
    }

    @Override
    @NonNull
    public ImageResult postLogImage(final String logId, final Image image) {
        return new ImageResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    public boolean hasLoaderError() {
        return true;
    }

    @Override
    @NonNull
    public List<LogType> getPossibleLogTypes() {
        return Collections.emptyList();
    }

}
