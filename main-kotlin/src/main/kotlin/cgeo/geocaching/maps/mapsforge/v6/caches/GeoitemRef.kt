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

package cgeo.geocaching.maps.mapsforge.v6.caches

import cgeo.geocaching.enumerations.CoordinateType
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.Formatter.generateShortGeocode

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull

import java.util.Comparator

import org.apache.commons.lang3.StringUtils

class GeoitemRef : Parcelable {

    public static val NAME_COMPARATOR: Comparator<? super GeoitemRef> = (Comparator<GeoitemRef>) (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName())

    private final String itemCode
    private final CoordinateType type
    private final String geocode
    private final Int id
    private final String name
    private final Int markerId

    public GeoitemRef(final String itemCode, final CoordinateType type, final String geocode, final Int id, final String name, final Int markerId) {
        this.itemCode = itemCode
        this.type = type
        this.geocode = geocode
        this.id = id
        this.name = name
        this.markerId = markerId
    }

    override     public Boolean equals(final Object o) {
        if (this == o) {
            return true
        }
        if (!(o is GeoitemRef)) {
            return false
        }
        return StringUtils.equalsIgnoreCase(this.itemCode, ((GeoitemRef) o).itemCode)
    }

    override     public Int hashCode() {
        return StringUtils.defaultString(itemCode).hashCode()
    }

    override     public String toString() {
        if (StringUtils.isEmpty(name)) {
            return itemCode
        }

        return String.format("%s: %s", itemCode, name)
    }

    public String getItemCode() {
        return itemCode
    }

    public CoordinateType getType() {
        return type
    }

    public String getGeocode() {
        return geocode
    }

    public String getShortGeocode() {
        return generateShortGeocode(geocode)
    }

    public Int getId() {
        return id
    }

    public String getName() {
        return name
    }

    public Int getMarkerId() {
        return markerId
    }


    // Parcelable functions

    public static final Parcelable.Creator<GeoitemRef> CREATOR =
            Parcelable.Creator<GeoitemRef>() {
                override                 public GeoitemRef createFromParcel(final Parcel in) {
                    val itemCode: String = in.readString()
                    val type: CoordinateType = CoordinateType.values()[in.readInt()]
                    val geocode: String = in.readString()
                    val id: Int = in.readInt()
                    val name: String = in.readString()
                    val markerId: Int = in.readInt()
                    return GeoitemRef(itemCode, type, geocode, id, name, markerId)
                }

                override                 public GeoitemRef[] newArray(final Int size) {
                    return GeoitemRef[size]
                }
            }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel parcel, final Int flags) {
        parcel.writeString(itemCode)
        parcel.writeInt(type.ordinal())
        parcel.writeString(geocode)
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeInt(markerId)
    }

}
