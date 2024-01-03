package cgeo.geocaching.connector.ec;

import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

class ECLoggingManager extends AbstractLoggingManager {

    ECLoggingManager(@NonNull final ECConnector connector, @NonNull final Geocache cache) {
        super(connector, cache);
    }

    @NonNull
    @Override
    public LogResult createLog(@NonNull final OfflineLogEntry logEntry, @Nullable final Map<String, Trackable> inventory) {
        return ECApi.postLog(getCache(), logEntry.logType, logEntry.getDate(), logEntry.log);
    }

}
