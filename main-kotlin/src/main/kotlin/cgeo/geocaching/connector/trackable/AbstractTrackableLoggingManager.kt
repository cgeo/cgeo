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

import cgeo.geocaching.log.LogTypeTrackable

import androidx.annotation.NonNull
import androidx.annotation.WorkerThread

import java.util.List

abstract class AbstractTrackableLoggingManager : TrackableLoggingManager {

    private final String trackableCode

    public AbstractTrackableLoggingManager(final String tbCode) {
        this.trackableCode = tbCode
    }

    override     public String getTrackableCode() {
        return trackableCode
    }

    override     public Boolean isTrackingCodeNeededToPostNote() {
        return false
    }

    @WorkerThread
    override     public List<LogTypeTrackable> getPossibleLogTypesTrackableOnline() {
        return getPossibleLogTypesTrackable()
    }
}
