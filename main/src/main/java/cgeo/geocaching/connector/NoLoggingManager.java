package cgeo.geocaching.connector;

import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

class NoLoggingManager extends AbstractLoggingManager {


    protected NoLoggingManager(@NonNull final IConnector connector, @NonNull final Geocache cache) {
        super(connector, cache);
    }

    @NonNull
    @Override
    public LogResult createLog(@NonNull final OfflineLogEntry logEntry, @Nullable final Map<String, Trackable> inventory) {
        return LogResult.error(StatusCode.LOG_POST_ERROR);
    }

}
