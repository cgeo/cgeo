package cgeo.geocaching.maps;

import cgeo.geocaching.location.Geopoint;

import android.os.Parcel;
import android.os.Parcelable;

public class MapState implements Parcelable {

    private final Geopoint center;
    private final int zoomLevel;
    private final boolean followMyLocation;
    private final boolean showCircles;
    private final String targetGeocode;
    private final Geopoint lastNavTarget;
    private final boolean liveEnabled;
    private final boolean storedEnabled;

    public MapState(final Geopoint center, final int zoomLevel, final boolean followMyLocation, final boolean showCircles, final String targetGeocode, final Geopoint lastNavTarget, final boolean liveEnabled, final boolean storedEnabled) {
        this.center = center;
        this.zoomLevel = zoomLevel;
        this.followMyLocation = followMyLocation;
        this.showCircles = showCircles;
        this.targetGeocode = targetGeocode;
        this.lastNavTarget = lastNavTarget;
        this.liveEnabled = liveEnabled;
        this.storedEnabled = storedEnabled;
    }

    public MapState(final Parcel in) {
        center = in.readParcelable(Geopoint.class.getClassLoader());
        zoomLevel = in.readInt();
        followMyLocation = in.readInt() > 0;
        showCircles = in.readInt() > 0;
        targetGeocode = in.readString();
        lastNavTarget = in.readParcelable(Geopoint.class.getClassLoader());
        liveEnabled = in.readInt() > 0;
        storedEnabled = in.readInt() > 0;
    }

    public Geopoint getCenter() {
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

    public String getTargetGeocode() {
        return targetGeocode;
    }

    public Geopoint getLastNavTarget() {
        return lastNavTarget;
    }

    public boolean isLiveEnabled() {
        return liveEnabled;
    }

    public boolean isStoredEnabled() {
        return storedEnabled;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(center, 0);
        dest.writeInt(zoomLevel);
        dest.writeInt(followMyLocation ? 1 : 0);
        dest.writeInt(showCircles ? 1 : 0);
        dest.writeString(targetGeocode);
        dest.writeParcelable(lastNavTarget, PARCELABLE_WRITE_RETURN_VALUE);
        dest.writeInt(liveEnabled ? 1 : 0);
        dest.writeInt(storedEnabled ? 1 : 0);
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
