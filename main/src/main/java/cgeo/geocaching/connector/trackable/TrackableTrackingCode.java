package cgeo.geocaching.connector.trackable;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

/**
 * Represent a Trackable Tracking code along with the brand.
 *
 * This is useful in some cases, as not all Trackable connectors are activated to handle tracking code lookups.
 * However, coupled with the brand, such lookups are possible.
 */
public final class TrackableTrackingCode {

    @NonNull public final String trackingCode;
    @NonNull public final TrackableBrand brand;

    public static final TrackableTrackingCode EMPTY = new TrackableTrackingCode("", TrackableBrand.UNKNOWN);

    public TrackableTrackingCode(final String trackingCode, final TrackableBrand brand) {
        this.trackingCode = StringUtils.defaultString(trackingCode);
        this.brand = brand == null ? TrackableBrand.UNKNOWN : brand;
    }

    /**
     * Checks if the TrackableTrackingCode corresponds to the EMPTY reference.
     *
     * @return True if TrackableTrackingCode is empty
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }
}
