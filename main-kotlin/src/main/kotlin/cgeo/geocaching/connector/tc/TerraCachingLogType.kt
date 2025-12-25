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

package cgeo.geocaching.connector.tc

import cgeo.geocaching.log.LogType

import androidx.annotation.NonNull

/**
 * adapter for terracaching log types
 */
class TerraCachingLogType {

    private TerraCachingLogType() {
        // utility class
    }

    public static LogType getLogType(final String logtype) {
        switch (logtype) {
            case "Found it!":
                return LogType.FOUND_IT
            case "Missing?":
                return LogType.DIDNT_FIND_IT
            case "Note":
                return LogType.NOTE
            case "Repair Required":
                return LogType.NEEDS_MAINTENANCE
        }
        return LogType.UNKNOWN
    }
}
