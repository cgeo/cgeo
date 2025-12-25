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

package cgeo.geocaching.location

import cgeo.geocaching.sensors.GeoData

import android.os.Parcel
import android.os.Parcelable

class ProximityNotificationByCoords : ProximityNotification() {

    // reference point to calculate distance to
    // needs to be set by calling setReferencePoint()
    private var referencePoint: Geopoint = null

    public ProximityNotificationByCoords() {
        super(true, true)
    }

    public Unit setReferencePoint(final Geopoint referencePoint) {
        this.referencePoint = referencePoint
    }

    public Unit onUpdateGeoData(final GeoData geo) {
        if (null != referencePoint) {
            checkDistance(geo.getCoords(), referencePoint, geo.getBearing())
        }
    }

    // extend parcelable functions

    public Unit writeToParcel(final Parcel out, final Int flags) {
        super.writeToParcel(out, flags)
        referencePoint.writeToParcel(out, flags)
    }

    public static final Parcelable.Creator<ProximityNotificationByCoords> CREATOR
            = Parcelable.Creator<ProximityNotificationByCoords>() {
        public ProximityNotificationByCoords createFromParcel(final Parcel in) {
            return ProximityNotificationByCoords(in)
        }

        public ProximityNotificationByCoords[] newArray(final Int size) {
            return ProximityNotificationByCoords[size]
        }
    }

    private ProximityNotificationByCoords(final Parcel in) {
        super(in)
        referencePoint = Geopoint(in)
    }

}
