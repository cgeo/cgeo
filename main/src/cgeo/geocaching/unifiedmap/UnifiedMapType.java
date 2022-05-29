package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.location.Geopoint;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

public class UnifiedMapType implements Parcelable {

    public static final String BUNDLE_MAPTYPE = "maptype";

    public enum UnifiedMapTypeType {
        UMTT_Undefined,         // invalid state
        UMTT_PlainMap,          // open map (from bottom navigation)
        UMTT_TargetGeocode,     // set cache or waypoint as target
        UMTT_TargetCoords       // set coords as target
        // to be extended
    }

    public UnifiedMapTypeType type = UnifiedMapTypeType.UMTT_Undefined;
    public String target = null;
    public Geopoint coords = null;
    // reminder: add additional fields to parcelable methods below

    /** default UnifiedMapType is PlainMap with no further data */
    public UnifiedMapType() {
        type = UnifiedMapTypeType.UMTT_PlainMap;
    }

    /** set geocode as target */
    public UnifiedMapType(final String geocode) {
        type = UnifiedMapTypeType.UMTT_TargetGeocode;
        target = geocode;
    }

    /** set coords as target */
    public UnifiedMapType(final Geopoint coords) {
        type = UnifiedMapTypeType.UMTT_TargetCoords;
        this.coords = coords;
    }

    /** launch fresh map with current settings */
    public void launchMap(final Context fromActivity) {
        final Intent intent = new Intent(fromActivity, UnifiedMapActivity.class);
        intent.putExtra(BUNDLE_MAPTYPE, this);
        fromActivity.startActivity(intent);
    }

    // ========================================================================
    // parcelable methods

    UnifiedMapType(final Parcel in) {
        type = UnifiedMapTypeType.values()[in.readInt()];
        target = in.readString();
        coords = in.readParcelable(Geopoint.class.getClassLoader());
        // ...
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(type.ordinal());
        dest.writeString(target);
        dest.writeParcelable(coords, 0);
        // ...
    }

    public static final Parcelable.Creator<UnifiedMapType> CREATOR = new Parcelable.Creator<UnifiedMapType>() {
        @Override
        public UnifiedMapType createFromParcel(final Parcel in) {
            return new UnifiedMapType(in);
        }

        @Override
        public UnifiedMapType[] newArray(final int size) {
            return new UnifiedMapType[size];
        }
    };

}
