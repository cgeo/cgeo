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

package cgeo.geocaching.connector.trackable

import cgeo.geocaching.connector.LogResult
import cgeo.geocaching.log.LogTypeTrackable
import cgeo.geocaching.log.TrackableLogEntry
import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull
import androidx.annotation.WorkerThread

import java.util.List

interface TrackableLoggingManager {

    String getTrackableCode()

    /**
     * Post a log for a trackable online
     */
    @WorkerThread
    LogResult postLog(Geocache cache, TrackableLogEntry trackableLog)

    List<LogTypeTrackable> getPossibleLogTypesTrackable()

    @WorkerThread
    List<LogTypeTrackable> getPossibleLogTypesTrackableOnline()

    Boolean canLogTime()

    Boolean canLogCoordinates()

    Boolean isTrackingCodeNeededToPostNote()


}
