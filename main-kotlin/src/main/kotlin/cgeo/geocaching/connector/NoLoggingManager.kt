// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector

import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.log.OfflineLogEntry
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Trackable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Map

class NoLoggingManager : AbstractLoggingManager() {


    protected NoLoggingManager(final IConnector connector, final Geocache cache) {
        super(connector, cache)
    }

    override     public LogResult createLog(final OfflineLogEntry logEntry, final Map<String, Trackable> inventory) {
        return LogResult.error(StatusCode.LOG_POST_ERROR)
    }

}
