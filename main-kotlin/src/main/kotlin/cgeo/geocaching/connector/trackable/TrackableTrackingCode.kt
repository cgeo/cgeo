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

import androidx.annotation.NonNull

import org.apache.commons.lang3.StringUtils

/**
 * Represent a Trackable Tracking code along with the brand.
 * <br>
 * This is useful in some cases, as not all Trackable connectors are activated to handle tracking code lookups.
 * However, coupled with the brand, such lookups are possible.
 */
class TrackableTrackingCode {

    public final String trackingCode
    public final TrackableBrand brand

    public static val EMPTY: TrackableTrackingCode = TrackableTrackingCode("", TrackableBrand.UNKNOWN)

    public TrackableTrackingCode(final String trackingCode, final TrackableBrand brand) {
        this.trackingCode = StringUtils.defaultString(trackingCode)
        this.brand = brand == null ? TrackableBrand.UNKNOWN : brand
    }

    /**
     * Checks if the TrackableTrackingCode corresponds to the EMPTY reference.
     *
     * @return True if TrackableTrackingCode is empty
     */
    public Boolean isEmpty() {
        return this == EMPTY
    }
}
