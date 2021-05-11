package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.utils.TextUtils;
import static cgeo.geocaching.utils.Formatter.generateShortGeocode;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

public class GeoitemRef implements Parcelable {

    public static final Comparator<? super GeoitemRef> NAME_COMPARATOR = (Comparator<GeoitemRef>) (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName());

    private final String itemCode;
    private final CoordinatesType type;
    private final String geocode;
    private final int id;
    private final String name;
    private final int markerId;

    public GeoitemRef(final String itemCode, final CoordinatesType type, final String geocode, final int id, final String name, final int markerId) {
        this.itemCode = itemCode;
        this.type = type;
        this.geocode = geocode;
        this.id = id;
        this.name = name;
        this.markerId = markerId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeoitemRef)) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(this.itemCode, ((GeoitemRef) o).itemCode);
    }

    @Override
    public int hashCode() {
        return StringUtils.defaultString(itemCode).hashCode();
    }

    @Override
    @NonNull
    public String toString() {
        if (StringUtils.isEmpty(name)) {
            return itemCode;
        }

        return String.format("%s: %s", itemCode, name);
    }

    public String getItemCode() {
        return itemCode;
    }

    @NonNull
    public String getShortItemCode() {
        return generateShortGeocode(itemCode);
    }

    public CoordinatesType getType() {
        return type;
    }

    public String getGeocode() {
        return geocode;
    }

    @NonNull
    public String getShortGeocode() {
        return generateShortGeocode(geocode);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMarkerId() {
        return markerId;
    }


    // Parcelable functions

    public static final Parcelable.Creator<GeoitemRef> CREATOR =
            new Parcelable.Creator<GeoitemRef>() {
                @Override
                public GeoitemRef createFromParcel(final Parcel in) {
                    final String itemCode = in.readString();
                    final CoordinatesType type = CoordinatesType.values()[in.readInt()];
                    final String geocode = in.readString();
                    final int id = in.readInt();
                    final String name = in.readString();
                    final int markerId = in.readInt();
                    return new GeoitemRef(itemCode, type, geocode, id, name, markerId);
                }

                @Override
                public GeoitemRef[] newArray(final int size) {
                    return new GeoitemRef[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        parcel.writeString(itemCode);
        parcel.writeInt(type.ordinal());
        parcel.writeString(geocode);
        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeInt(markerId);
    }

}
