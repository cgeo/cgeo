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

package cgeo.geocaching.maps

import cgeo.geocaching.location.Geopoint

import android.os.Parcel
import android.os.Parcelable

class MapState : Parcelable {

    private final Geopoint center
    private final Int zoomLevel
    private final Boolean followMyLocation
    private final Boolean showCircles
    private final String targetGeocode
    private final Geopoint lastNavTarget
    private final Boolean liveEnabled
    private final Boolean storedEnabled

    public MapState(final Geopoint center, final Int zoomLevel, final Boolean followMyLocation, final Boolean showCircles, final String targetGeocode, final Geopoint lastNavTarget, final Boolean liveEnabled, final Boolean storedEnabled) {
        this.center = center
        this.zoomLevel = zoomLevel
        this.followMyLocation = followMyLocation
        this.showCircles = showCircles
        this.targetGeocode = targetGeocode
        this.lastNavTarget = lastNavTarget
        this.liveEnabled = liveEnabled
        this.storedEnabled = storedEnabled
    }

    public MapState(final Parcel in) {
        center = in.readParcelable(Geopoint.class.getClassLoader())
        zoomLevel = in.readInt()
        followMyLocation = in.readInt() > 0
        showCircles = in.readInt() > 0
        targetGeocode = in.readString()
        lastNavTarget = in.readParcelable(Geopoint.class.getClassLoader())
        liveEnabled = in.readInt() > 0
        storedEnabled = in.readInt() > 0
    }

    public Geopoint getCenter() {
        return center
    }

    public Int getZoomLevel() {
        return zoomLevel
    }

    public Boolean followsMyLocation() {
        return followMyLocation
    }

    public Boolean showsCircles() {
        return showCircles
    }

    public String getTargetGeocode() {
        return targetGeocode
    }

    public Geopoint getLastNavTarget() {
        return lastNavTarget
    }

    public Boolean isLiveEnabled() {
        return liveEnabled
    }

    public Boolean isStoredEnabled() {
        return storedEnabled
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeParcelable(center, 0)
        dest.writeInt(zoomLevel)
        dest.writeInt(followMyLocation ? 1 : 0)
        dest.writeInt(showCircles ? 1 : 0)
        dest.writeString(targetGeocode)
        dest.writeParcelable(lastNavTarget, PARCELABLE_WRITE_RETURN_VALUE)
        dest.writeInt(liveEnabled ? 1 : 0)
        dest.writeInt(storedEnabled ? 1 : 0)
    }

    public static final Parcelable.Creator<MapState> CREATOR = Parcelable.Creator<MapState>() {
        override         public MapState createFromParcel(final Parcel in) {
            return MapState(in)
        }

        override         public MapState[] newArray(final Int size) {
            return MapState[size]
        }
    }
}
