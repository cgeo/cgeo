package cgeo.geocaching.models;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.MatcherWrapper;
import static cgeo.geocaching.utils.Formatter.generateShortGeocode;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class RouteItem implements Parcelable {
    // groups: 1=geocode, 3=wp prefix, 4=additional text
    private static final Pattern GEOINFO_PATTERN = Pattern.compile("^([A-Za-z]{1,2}[0-9A-Za-z]{1,6})(-([A-Za-z0-9]{1,10}))?( .*)?$");
    private static final Pattern AL_GEOINFO_PATTERN = Pattern.compile("^AL([A-Za-z0-9]+)(-[A-Za-z0-9]+)+( .*)?$");
    private static final Pattern AL_WAYPOINT_PATTERN = Pattern.compile("(-([0-9]+))?( .*)?$");

    // only identifier and point are needed to identify a RouteItem
    private String identifier;
    private Geopoint point;
    // other values are for convenience and speed
    private RouteItemType type;
    private String cacheGeocode;
    private int waypointId;
    private String sortFilterString;

    public RouteItem(final INamedGeoCoordinate item) {
        setDetails(item);
    }

    public RouteItem(final Geopoint point) {
        setDetails(buildIdentifier(point), point, RouteItemType.COORDS, "", 0, null);
    }

    // parse name info of GPX route point entry into RouteItem
    public RouteItem(final String sortFilterString, final Geopoint p) {

        // init with default values
        setDetails(buildIdentifier(p), p, RouteItemType.COORDS, "", 0, sortFilterString);

        // try to parse name string
        if (StringUtils.isNotBlank(sortFilterString)) {
            String geocode = null;
            String prefix = null;
            final MatcherWrapper matches = new MatcherWrapper(GEOINFO_PATTERN, sortFilterString);
            if (matches.find()) {
                geocode = matches.group(1);
                if (matches.groupCount() >= 3) {
                    prefix = matches.group(3);
                }
            } else {
                final MatcherWrapper matchesAL = new MatcherWrapper(AL_GEOINFO_PATTERN, sortFilterString);
                if (matchesAL.find()) {
                    final MatcherWrapper matchesALwpt = new MatcherWrapper(AL_WAYPOINT_PATTERN, sortFilterString);
                    if (matchesALwpt.find()) {
                        final int l1 = matchesALwpt.group(1) == null ? 0 : matchesALwpt.group(1).length();
                        final int l3 = matchesALwpt.group(3) == null ? 0 : matchesALwpt.group(3).length();
                        geocode = StringUtils.left(sortFilterString, sortFilterString.length() - l1 - l3);
                        prefix = matchesALwpt.group(2);
                    } else {
                        // remove comment at end (if given)
                        final int l2 = matchesAL.group(3) == null ? 0 : matchesAL.group(3).length();
                        geocode = StringUtils.left(sortFilterString, sortFilterString.length() - l2);
                        prefix = null;
                    }
                }
            }
            if (StringUtils.isNotBlank(geocode) && ConnectorFactory.canHandle(geocode)) {
                // do we have a valid geocode, and a cache available for it?

                final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                if (null != cache) {
                    cacheGeocode = geocode;

                    // if no waypoint data is available: stay with geocache
                    if (StringUtils.isBlank(prefix)) {
                        setDetails(cache);
                        return;
                    }

                    // try to extract waypoint data
                    final List<Waypoint> waypoints = DataStore.loadWaypoints(cacheGeocode);
                    if (null != waypoints && !waypoints.isEmpty()) {
                        int counter = 0;
                        Waypoint tempWaypoint = null;
                        for (Waypoint waypoint : waypoints) {
                            if (prefix.equals(waypoint.getPrefix())) {
                                tempWaypoint = waypoint;
                                counter++;
                            }
                        }
                        if (counter == 1) {
                            // unique prefix => use waypoint
                            setDetails(tempWaypoint);
                            return;
                        } else if (counter > 1) {
                            // for non-unique prefixes try to find a user-defined waypoint with the same coordinates
                            for (Waypoint waypoint : waypoints) {
                                if (point.equalsDecMinute(waypoint.getCoords())) {
                                    setDetails(waypoint);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
        // else stay with defaults (=coords)
    }

    public RouteItem(final GeoitemRef item) {
        switch (item.getType()) {
            case CACHE:
                final Geocache geocache = DataStore.loadCache(item.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                if (null != geocache) {
                    setDetails(buildIdentifier(geocache), geocache.getCoords(), RouteItemType.GEOCACHE, item.getGeocode(), 0, geocache.getName());
                } else {
                    setDetails(item.getGeocode(), null, RouteItemType.GEOCACHE, item.getGeocode(), 0, item.getGeocode());
                }
                break;
            case WAYPOINT:
                final Waypoint waypoint = DataStore.loadWaypoint(item.getId());
                if (null != waypoint) {
                    setDetails(buildIdentifier(waypoint), waypoint.getCoords(), RouteItemType.WAYPOINT, item.getGeocode(), item.getId(), waypoint.getName());
                }
                break;
            default:
                throw new IllegalStateException("RouteItem: unknown item type");
        }
    }

    public RouteItemType getType() {
        return type;
    }

    public int getWaypointId() {
        return waypointId;
    }

    public String getGeocode() {
        return cacheGeocode;
    }

    @Nullable
    public Geocache getGeocache() {
        if (cacheGeocode == null || cacheGeocode.isEmpty()) {
            return null;
        }
        return DataStore.loadCache(cacheGeocode, LoadFlags.LOAD_CACHE_OR_DB);
    }

    @Nullable
    public Waypoint getWaypoint() {
        if (waypointId == 0) {
            return null;
        }
        return DataStore.loadWaypoint(waypointId);
    }

    @NonNull
    public String getShortGeocode() {
        return generateShortGeocode(cacheGeocode);
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getSortFilterString() {
        return sortFilterString;
    }

    public Geopoint getPoint() {
        return point;
    }

    private void setDetails(final INamedGeoCoordinate item) {
        if (item instanceof Waypoint) {
            setDetails(buildIdentifier(item), item.getCoords(), RouteItemType.WAYPOINT, item.getGeocode(), ((Waypoint) item).getId(), item.getName());
        } else {
            setDetails(buildIdentifier(item), item.getCoords(), RouteItemType.GEOCACHE, item.getGeocode(), 0, item.getName());
        }
    }

    private void setDetails(final String identifier, final Geopoint point, final RouteItemType type, final String geocode, final int waypointId, final String name) {
        this.identifier = identifier;
        this.point = point;
        this.type = type;
        this.cacheGeocode = geocode;
        this.waypointId = waypointId;
        this.sortFilterString =
            (name == null ? "" : name)  + ":" +
            (geocode == null ? "" : geocode) + ":" +
            (point == null ? "" : point) + ":" +
            (identifier == null ? "" : identifier);
        checkForCoordinates();
    }

    private void checkForCoordinates() {
        if (null == point) {
            // try to load geocache/waypoint to get coords
            if (type == RouteItemType.GEOCACHE) {
                final Geocache cache = DataStore.loadCache(cacheGeocode, LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    point = cache.getCoords();
                }
            } else if (type == RouteItemType.WAYPOINT && waypointId > 0) {
                final Waypoint waypoint = DataStore.loadWaypoint(waypointId);
                if (waypoint != null) {
                    point = waypoint.getCoords();
                }
            }
        }
    }

    private String buildIdentifier(final INamedGeoCoordinate item) {
        String name = item.getGeocode();
        if (item instanceof Waypoint) {
            name += "-" + ((Waypoint) item).getPrefix();
        }
        return name;
    }

    private String buildIdentifier(final Geopoint point) {
        return "COORDS!" + (null == point ? "" : " " + GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_SHORT, point));
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
        identifier = parcel.readString();
        point = parcel.readParcelable(Geopoint.class.getClassLoader());
        type = RouteItemType.values()[parcel.readInt()];
        cacheGeocode = parcel.readString();
        waypointId = parcel.readInt();
        sortFilterString = parcel.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(identifier);
        dest.writeParcelable(point, flags);
        dest.writeInt(type.ordinal());
        dest.writeString(cacheGeocode);
        dest.writeInt(waypointId);
        dest.writeString(sortFilterString);
    }

}
