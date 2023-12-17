package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.location.Geopoint;

import android.os.Parcel;
import android.os.Parcelable;

public class UnifiedMapState implements Parcelable {
    public static final String BUNDLE_MAPSTATE = "mapstate";

    Geopoint center;
    int zoomLevel;

    public UnifiedMapState(final Geopoint center, final int zoomLevel) {
        this.center = center;
        this.zoomLevel = zoomLevel;
    };

    // ========================================================================
    // parcelable methods

    UnifiedMapState(final Parcel in) {
        center = in.readParcelable(Geopoint.class.getClassLoader());
        zoomLevel = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(center, 0);
        dest.writeInt(zoomLevel);
    }

    public static final Parcelable.Creator<UnifiedMapState> CREATOR = new Parcelable.Creator<UnifiedMapState>() {
        @Override
        public UnifiedMapState createFromParcel(final Parcel in) {
            return new UnifiedMapState(in);
        }

        @Override
        public UnifiedMapState[] newArray(final int size) {
            return new UnifiedMapState[size];
        }
    };

}
