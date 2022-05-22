package cgeo.geocaching.unifiedmap;

import android.os.Parcel;
import android.os.Parcelable;

class UnifiedMapType implements Parcelable {

    public enum UnifiedMapTypeType {
        UMTT_Undefined,         // invalid state
        UMTT_PlainMap,          // open map (from bottom navigation)
        UMTT_SetTarget          // set cache or waypoint as target
        // to be extended
    }

    public UnifiedMapTypeType type;
    public String target;

    /**
     * initializes a UnifiedMapType object with defaults
     * (called internally only, use public static methods to create a UnifiedMapType)
     */
    private UnifiedMapType() {
        type = UnifiedMapTypeType.UMTT_Undefined;
        target = null;
    }

    public static UnifiedMapType getPlainMap() {
        UnifiedMapType umt = new UnifiedMapType();
        umt.type = UnifiedMapTypeType.UMTT_PlainMap;
        return umt;
    }

    public static UnifiedMapType getTarget(final String geocode) {
        UnifiedMapType umt = new UnifiedMapType();
        umt.type = UnifiedMapTypeType.UMTT_SetTarget;
        umt.target = geocode;
        return umt;
    }

    // parcelable methods

    UnifiedMapType(final Parcel in) {
        type = UnifiedMapTypeType.values()[in.readInt()];
        target = in.readString();
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
