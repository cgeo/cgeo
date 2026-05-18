package cgeo.geocaching.files.unifiedgpxparser;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Parse result for a single {@code <wpt>} element. Exactly one of {@link #cache}
 * or {@link #childWaypoint} is non-null. Logs are kept separately so the integration
 * layer can persist them with {@code DataStore.saveLogs}.
 */
final class Parsed {

    @Nullable final Geocache cache;
    @Nullable final ChildWaypoint childWaypoint;
    @NonNull final List<LogEntry> logs;

    Parsed(@Nullable final Geocache cache,
           @Nullable final ChildWaypoint childWaypoint,
           @NonNull final List<LogEntry> logs) {
        this.cache = cache;
        this.childWaypoint = childWaypoint;
        this.logs = logs;
    }

    static Parsed empty() {
        return new Parsed(null, null, Collections.emptyList());
    }
}
