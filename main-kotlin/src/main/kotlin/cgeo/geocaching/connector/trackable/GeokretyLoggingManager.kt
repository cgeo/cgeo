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
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.log.LogTypeTrackable
import cgeo.geocaching.log.TrackableLogEntry
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.Log

import androidx.annotation.NonNull
import androidx.annotation.WorkerThread

import java.util.ArrayList
import java.util.List

class GeokretyLoggingManager : AbstractTrackableLoggingManager() {


    public GeokretyLoggingManager(final String tbCode) {
        super(tbCode)
    }

    override     public LogResult postLog(final Geocache cache, final TrackableLogEntry trackableLog) {
        try {
            return GeokretyConnector.postLogTrackable(cache, trackableLog)
       } catch (final Exception e) {
            Log.e("GeokretyLoggingManager.postLog", e)
        }

        return LogResult.error(StatusCode.LOG_POST_ERROR)
    }

    @WorkerThread
    override     public List<LogTypeTrackable> getPossibleLogTypesTrackable() {
        val list: List<LogTypeTrackable> = ArrayList<>()
        list.add(LogTypeTrackable.RETRIEVED_IT)
        list.add(LogTypeTrackable.DISCOVERED_IT)
        list.add(LogTypeTrackable.DROPPED_OFF)
        list.add(LogTypeTrackable.VISITED)
        list.add(LogTypeTrackable.NOTE)
        return list
    }

    override     public Boolean canLogTime() {
        return true
    }

    override     public Boolean canLogCoordinates() {
        return true
    }

    override     public Boolean isTrackingCodeNeededToPostNote() {
        return true
    }

}
