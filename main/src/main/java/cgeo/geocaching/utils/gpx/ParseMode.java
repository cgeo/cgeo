package cgeo.geocaching.utils.gpx;

/**
 * Controls how wptTypes are parsed.
 */
public enum ParseMode {
    /**
     * Parse/collect complete information.
     */
    FULL,
    /**
     * Parse only basic data per wptType (name + coordinate)
     */
    COORDINATES_ONLY,
    /**
     * Do not collect wptType data.
     */
    SKIP,
    /**
     * Flag to use to abort parsing completely
     */
    ABORT
}
