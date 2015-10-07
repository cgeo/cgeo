package cgeo.geocaching.connector.tc;

import cgeo.geocaching.enumerations.LogType;

/**
 * adapter for terracaching log types
 */
public final class TerraCachingLogType {

    public static LogType getLogType(final String logtype) {
        switch (logtype) {
            case "Found it!":
                return LogType.FOUND_IT;
            case "Missing?":
                return LogType.DIDNT_FIND_IT;
            case "Note":
                return LogType.NOTE;
            case "Repair Required":
                return LogType.NEEDS_MAINTENANCE;
        }
        return LogType.UNKNOWN;
    }
}
