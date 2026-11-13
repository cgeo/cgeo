package cgeo.geocaching.sorting;

import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.Nullable;

/**
 * cache with given visited date and optional offline log, which does not need database access
 */
class VisitedCache extends Geocache {

    // middle of month at noon UTC, so that the month is the same in every time zone
    static final long NOV_2023 = 1_700_049_600_000L; // 2023-11-15
    static final long JAN_2024 = 1_705_320_000_000L; // 2024-01-15
    static final long MAR_2024 = 1_710_504_000_000L; // 2024-03-15

    @Nullable
    private final OfflineLogEntry offlineLog;

    /**
     * @param offlineLogDate date of offline log, 0 for no offline log
     */
    VisitedCache(final String geocode, final long visitedDate, final long offlineLogDate) {
        setGeocode(geocode);
        setVisitedDate(visitedDate);
        setHasLogOffline(offlineLogDate > 0);
        offlineLog = offlineLogDate > 0 ? new OfflineLogEntry.Builder().setDate(offlineLogDate).build() : null;
    }

    @Override
    @Nullable
    public OfflineLogEntry getOfflineLog() {
        return offlineLog;
    }
}
