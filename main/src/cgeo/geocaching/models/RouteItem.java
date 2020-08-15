package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.storage.DataStore;

import android.os.Parcel;
import android.os.Parcelable;

public class RouteItem implements Parcelable {
    private RouteItemType type;
    private int id;
    private String geocode;
    private Geopoint point;

    public RouteItem(final IWaypoint item) {
        setDetails(item);
    }

    public RouteItem(final Geopoint point) {
        setDetails(RouteItemType.COORDS, "COORD", 0, point);
    }

    // @todo: Can handle GEOCACHE and WAYPOINT only yet
    public RouteItem(final RouteItemType type, final String geocode, final int id) {
        setDetails(type, geocode, id, null);
    }

    // @todo: Can handle GEOCACHE and WAYPOINT only yet
    public RouteItem(final RouteItemType type, final String geocode) {
        setDetails(type, geocode, type == RouteItemType.GEOCACHE ? 0 : Integer.parseInt(geocode.substring(2)), null);
    }

    // @todo: Can handle GEOCACHE and WAYPOINT only yet
    public RouteItem(final GeoitemRef item) {
        switch (item.getType()) {
            case CACHE:
                setDetails(RouteItemType.GEOCACHE, item.getGeocode(), 0, null);
                break;
            case WAYPOINT:
                setDetails(RouteItemType.WAYPOINT, "WP" + item.getGeocode(), item.getId(), null);
                break;
            default:
                throw new IllegalStateException("RouteItem: unknown item type");
        }
    }

    public RouteItemType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public String getGeocode() {
        return geocode;
    }

    public Geopoint getPoint() {
        return point;
    }

    private void setDetails(final IWaypoint item) {
        if (item instanceof Geocache) {
            setDetails(RouteItemType.GEOCACHE, item.getGeocode(), 0, item.getCoords());
        } else {
            setDetails(RouteItemType.WAYPOINT, "WP" + item.getId(), item.getId(), item.getCoords());
        }
    }

    private void setDetails(final RouteItemType type, final String geocode, final int id, final Geopoint point) {
        this.type = type;
        this.geocode = geocode;
        this.id = id;
        this.point = point;
        checkForCoordinates();
    }

    private void checkForCoordinates() {
        if (null == point) {
            // try to load geocache/waypoint to get coords
            if (type == RouteItemType.GEOCACHE) {
                final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    point = cache.getCoords();
                }
            } else if (type == RouteItemType.WAYPOINT && id > 0) {
                final Waypoint waypoint = DataStore.loadWaypoint(id);
                if (waypoint != null) {
                    point = waypoint.getCoords();
                }
            }
        }
    }

    public enum RouteItemType {
        WAYPOINT,
        GEOCACHE,
        COORDS
    }

    // Parcelable methods

    public static final Parcelable.Creator<RouteItem> CREATOR = new Parcelable.Creator<RouteItem>() {

        @Override
        public RouteItem createFromParcel(final Parcel source) {
            return new RouteItem(source);
        }

        @Override
        public RouteItem[] newArray(final int size) {
            return new RouteItem[size];
        }

    };

    private RouteItem(final Parcel parcel) {
        type = RouteItemType.values()[parcel.readInt()];
        id = parcel.readInt();
        geocode = parcel.readString();
        point = parcel.readParcelable(Geopoint.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(type.ordinal());
        dest.writeInt(id);
        dest.writeString(geocode);
        dest.writeParcelable(point, flags);
    }

}
