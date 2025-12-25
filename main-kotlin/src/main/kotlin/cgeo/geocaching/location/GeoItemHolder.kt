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

import cgeo.geocaching.models.geoitem.GeoItem
import cgeo.geocaching.models.geoitem.IGeoItemSupplier

import android.os.Parcel
import android.os.Parcelable

/** A list of geoobjects */
class GeoItemHolder : IGeoItemSupplier, Parcelable {

    private final GeoItem item

    private Boolean isHidden

    public GeoItemHolder(final GeoItem item) {
        this.item = item
    }

    public Boolean isHidden() {
        return isHidden
    }

    public Unit setHidden(final Boolean isHidden) {
        this.isHidden = isHidden
    }

    override     public Boolean hasData() {
        return item != null && item.isValid()
    }

    override     public Viewport getViewport() {
        return item == null ? null : item.getViewport()
    }

    public Geopoint getCenter() {
        return item == null ? null : item.getCenter()
    }

    override     public GeoItem getItem() {
        return item
    }

    // Parcelable implementation

    protected GeoItemHolder(final Parcel in) {
        isHidden = in.readByte() != 0
        item = in.readParcelable(GeoItem.class.getClassLoader())
    }

    public static val CREATOR: Creator<GeoItemHolder> = Creator<GeoItemHolder>() {
        override         public GeoItemHolder createFromParcel(final Parcel in) {
            return GeoItemHolder(in)
        }

        override         public GeoItemHolder[] newArray(final Int size) {
            return GeoItemHolder[size]
        }
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeByte((Byte) (isHidden ? 1 : 0))
        dest.writeParcelable(item, flags)
    }

}
