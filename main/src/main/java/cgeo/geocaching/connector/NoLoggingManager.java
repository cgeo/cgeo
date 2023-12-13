package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

class NoLoggingManager extends AbstractLoggingManager {


    protected NoLoggingManager(@NonNull final IConnector connector, @NonNull final Geocache cache) {
        super(connector, cache);
    }

    @NonNull
    @Override
    public LogResult createLog(@NonNull final LogEntry logEntry, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, final boolean addToFavorites, final float rating) {
        return new LogResult(StatusCode.LOG_POST_ERROR);
    }

}
