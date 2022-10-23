package cgeo.geocaching.models;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.TextUtils;
import static cgeo.geocaching.utils.Formatter.generateShortGeocode;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class RouteItem implements Parcelable {
    public static final Comparator<? super RouteItem> NAME_COMPARATOR = (Comparator<RouteItem>) (left, right) -> TextUtils.COLLATOR.compare(left.identifier, right.identifier);


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

    public RouteItem(final IWaypoint item) {
        setDetails(item);
    }

    public RouteItem(final Geopoint point) {
        setDetails(buildIdentifier(point), point, RouteItemType.COORDS, "", 0);
    }

    // parse name info of GPX route point entry into RouteItem
    public RouteItem(final String name, final Geopoint p) {

        // init with default values
        setDetails(buildIdentifier(p), p, RouteItemType.COORDS, "", 0);

        // try to parse name string
        if (StringUtils.isNotBlank(name)) {
            String geocode = null;
            String prefix = null;
            final MatcherWrapper matches = new MatcherWrapper(GEOINFO_PATTERN, name);
            if (matches.find()) {
                geocode = matches.group(1);
                if (matches.groupCount() >= 3) {
                    prefix = matches.group(3);
                }
            } else {
                final MatcherWrapper matchesAL = new MatcherWrapper(AL_GEOINFO_PATTERN, name);
                if (matchesAL.find()) {
                    final MatcherWrapper matchesALwpt = new MatcherWrapper(AL_WAYPOINT_PATTERN, name);
                    if (matchesALwpt.find()) {
                        final int l1 = matchesALwpt.group(1) == null ? 0 : matchesALwpt.group(1).length();
                        final int l3 = matchesALwpt.group(3) == null ? 0 : matchesALwpt.group(3).length();
                        geocode = StringUtils.left(name, name.length() - l1 - l3);
                        prefix = matchesALwpt.group(2);
                    } else {
                        // remove comment at end (if given)
                        final int l2 = matchesAL.group(3) == null ? 0 : matchesAL.group(3).length();
                        geocode = StringUtils.left(name, name.length() - l2);
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
                    setDetails(buildIdentifier(geocache), geocache.getCoords(), RouteItemType.GEOCACHE, item.getGeocode(), 0);
                } else {
                    setDetails(item.getGeocode(), null, RouteItemType.GEOCACHE, item.getGeocode(), 0);
                }
                break;
            case WAYPOINT:
                final Waypoint waypoint = DataStore.loadWaypoint(item.getId());
                if (null != waypoint) {
                    setDetails(buildIdentifier(waypoint), waypoint.getCoords(), RouteItemType.WAYPOINT, item.getGeocode(), item.getId());
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

    @NonNull
    public String getShortGeocode() {
        return generateShortGeocode(cacheGeocode);
    }

    public String getIdentifier() {
        return identifier;
    }

    public Geopoint getPoint() {
        return point;
    }

    private void setDetails(final IWaypoint item) {
        if (item instanceof Geocache) {
            setDetails(buildIdentifier(item), item.getCoords(), RouteItemType.GEOCACHE, item.getGeocode(), 0);
        } else {
            setDetails(buildIdentifier(item), item.getCoords(), RouteItemType.WAYPOINT, item.getGeocode(), item.getId());
        }
    }

    private void setDetails(final String identifier, final Geopoint point, final RouteItemType type, final String geocode, final int waypointId) {
        this.identifier = identifier;
        this.point = point;
        this.type = type;
        this.cacheGeocode = geocode;
        this.waypointId = waypointId;
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

    private String buildIdentifier(final IWaypoint item) {
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
    }

}
