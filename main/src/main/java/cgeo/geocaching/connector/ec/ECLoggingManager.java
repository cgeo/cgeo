package cgeo.geocaching.connector.ec;

import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

class ECLoggingManager extends AbstractLoggingManager {

    ECLoggingManager(@NonNull final ECConnector connector, @NonNull final Geocache cache) {
        super(connector, cache);
    }

    @NonNull
    @Override
    public LogResult createLog(@NonNull final LogEntry logEntry, @Nullable final String logPassword, @NonNull final List<TrackableLog> trackableLogs, final boolean addToFavorites, final float rating) {
        return ECApi.postLog(getCache(), logEntry.logType, logEntry.getDate(), logEntry.log);
    }

}
