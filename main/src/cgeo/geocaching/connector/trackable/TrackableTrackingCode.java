package cgeo.geocaching.connector.trackable;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

/**
 * Represent a Trackable Tracking code along with the brand.
 *
 * This is useful in some cases, as not all Trackable connectors are activated to handle tracking code lookup.
 * However, coupled with the brand, such lookup are possible.
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
     * Check if the TrackableTrackingCode correspond the the EMPTY reference.
     *
     * @return True if TrackableTrackingCode is empty
     */
    public boolean isEmpty() {
        return trackingCode.equals(EMPTY.trackingCode) && brand.equals(EMPTY.brand);
    }
}
