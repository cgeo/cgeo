package cgeo.geocaching.maps.mapsforge.v6;

import org.mapsforge.core.model.LatLong;

import android.os.Parcel;
import android.os.Parcelable;

public class MapState implements Parcelable {

    private final LatLong center;
    private final int zoomLevel;
    private final boolean followMyLocation;
    private final boolean showCircles;

    public MapState(final LatLong center, final int zoomLevel, final boolean followMyLocation, final boolean showCircles) {
        this.center = center;
        this.zoomLevel = zoomLevel;
        this.followMyLocation = followMyLocation;
        this.showCircles = showCircles;
    }

    public MapState(final Parcel in) {
        center = new LatLong(in.readDouble(), in.readDouble());
        zoomLevel = in.readInt();
        followMyLocation = in.readInt() > 0 ? true : false;
        showCircles = in.readInt() > 0 ? true : false;
    }

    public LatLong getCenter() {
        return center;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public boolean followsMyLocation() {
        return followMyLocation;
    }

    public boolean showsCircles() {
        return showCircles;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeDouble(center.latitude);
        dest.writeDouble(center.longitude);
        dest.writeInt(zoomLevel);
        dest.writeInt(followMyLocation ? 1 : 0);
        dest.writeInt(showCircles ? 1 : 0);
    }

    public static final Parcelable.Creator<MapState> CREATOR = new Parcelable.Creator<MapState>() {
        @Override
        public MapState createFromParcel(final Parcel in) {
            return new MapState(in);
        }

        @Override
        public MapState[] newArray(final int size) {
            return new MapState[size];
        }
    };
}
