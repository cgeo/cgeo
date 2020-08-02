package cgeo.geocaching.maps.routing;

import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;

import android.os.Parcel;
import android.os.Parcelable;

public class RouteItem implements Parcelable {
    private CoordinatesType type = CoordinatesType.CACHE;
    private String geocode = "";         // cache code for type=cache
    private int id = 0;                  // numeric id for type=waypoint

    public RouteItem (final GeoitemRef ref) {
        this.type = ref.getType();
        if (this.type == CoordinatesType.CACHE) {
            this.geocode = ref.getGeocode();
            this.id = 0;
        } else {
            this.id = ref.getId();
            this.geocode = "WP" + this.id;
        }
    }

    public RouteItem (final IWaypoint item) {
        if (item instanceof Geocache) {
            this.type = CoordinatesType.CACHE;
            this.geocode = item.getGeocode();
            this.id = 0;
        } else {
            this.type = CoordinatesType.WAYPOINT;
            this.id = item.getId();
            this.geocode = "WP" + this.id;
        }
    }

    public RouteItem (final CoordinatesType type, final String geocode) {
        this.type = type;
        this.geocode = geocode;
        this.id = type == CoordinatesType.CACHE ? 0 : Integer.parseInt(geocode.substring(2));
    }

    public CoordinatesType getType() {
        return this.type;
    }

    public String getGeocode() {
        return geocode;
    }

    public int getId() {
        return id;
    }

    // parcelable methods

    public RouteItem (final CoordinatesType type, final String geocode, final int id) {
        this.type = type;
        this.geocode = geocode;
        this.id = id;
    }

    public static final Parcelable.Creator<RouteItem> CREATOR = new Parcelable.Creator<RouteItem>() {
                @Override
                public RouteItem createFromParcel(final Parcel in) {
                    return new RouteItem(in);
                }

                @Override
                public RouteItem[] newArray(final int size) {
                    return new RouteItem[size];
                }
            };

    private RouteItem (final Parcel parcel) {
        type = CoordinatesType.values()[parcel.readInt()];
        geocode = parcel.readString();
        id = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        parcel.writeInt(type.ordinal());
        parcel.writeString(geocode);
        parcel.writeInt(id);
    }

}
