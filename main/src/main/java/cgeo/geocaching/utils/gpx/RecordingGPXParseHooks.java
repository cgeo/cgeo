package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.ICoordinate;
import cgeo.geocaching.models.NamedGeoCoordinate;
import cgeo.geocaching.models.Waypoint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link IGPXParseHooks} which simply records everything encountered during a
 * parse run, in document order.
 */
public class RecordingGPXParseHooks implements IGPXParseHooks {

    /** kind of a single, top-level (or route-/track-embedded) recorded item; see {@link #getGlobalItems()} */
    public enum ItemKind { GEOCACHE, WAYPOINT, NAMED_COORDINATE, COORDINATE }

    /** a single recorded geocache/waypoint/(named) coordinate, in the order it was encountered */
    public static final class Item {
        public final ItemKind kind;
        public final ICoordinate coordinate;
        public final String parentGeocode;
        public final List<LogEntry> logs;

        Item(final ICoordinate coordinate, final String parentGeocode, final List<LogEntry> logs) {
            this.kind = calculateKind(coordinate);
            this.coordinate = coordinate;
            this.parentGeocode = parentGeocode;
            this.logs = logs;
        }

        private ItemKind calculateKind(final ICoordinate coordinate) {
            if (coordinate instanceof Geocache) {
                return ItemKind.GEOCACHE;
            } else if (coordinate instanceof Waypoint) {
                return ItemKind.WAYPOINT;
            } else if (coordinate instanceof NamedGeoCoordinate) {
                return ItemKind.NAMED_COORDINATE;
            } else {
                return ItemKind.COORDINATE;
            }
        }

    }

    /** a single recorded route ({@code <rte>}) */
    public static final class NamedCoordinateList {
        public final String name;
        public final int pointCount;
        public final List<ICoordinate> points;

        NamedCoordinateList(final String name, final int pointCount, final List<ICoordinate> points) {
            this.name = name;
            this.pointCount = pointCount;
            this.points = points;
        }
    }

    /** a single recorded track ({@code <trk>}), made up of its recorded segments */
    public static final class Track {
        public final String name;
        public final int segmentCount;
        public final int totalPointCount;
        public final List<NamedCoordinateList> segments;

        Track(final String name, final int segmentCount, final int totalPointCount, final List<NamedCoordinateList> segments) {
            this.name = name;
            this.segmentCount = segmentCount;
            this.totalPointCount = totalPointCount;
            this.segments = segments;
        }

    }

    private String gpxCreatorOrName;
    private final List<Item> globalItems = new ArrayList<>();
    private final List<NamedCoordinateList> routes = new ArrayList<>();
    private final List<Track> tracks = new ArrayList<>();

    /** points recorded so far for the currently-open route; non-null only between onRouteStart and onRouteEnd */
    private List<ICoordinate> currentRoutePoints;
    /** points recorded so far for the currently-open track segment; non-null only between onTrackSegmentStart and onTrackSegmentEnd */
    private List<ICoordinate> currentSegmentPoints;
    /** segments recorded so far for the currently-open track; non-null only between onTrackStart and onTrackEnd */
    private List<NamedCoordinateList> currentTrackSegments;

    private GPXParser.ParseMode parseModeGlobal;
    private GPXParser.ParseMode parseModeRoutes;
    private GPXParser.ParseMode parseModeTracks;

    public void setModes(final GPXParser.ParseMode global, final GPXParser.ParseMode routes, final GPXParser.ParseMode tracks) {
        this.parseModeGlobal = global;
        this.parseModeRoutes = routes;
        this.parseModeTracks = tracks;
    }

    @Nullable
    public String getGpxCreatorOrName() {
        return gpxCreatorOrName;
    }

    /** all top-level (and route-/track-embedded) geocaches/waypoints/coordinates, in document order */
    public List<Item> getGlobalItems() {
        return globalItems;
    }

    public List<NamedCoordinateList> getRoutes() {
        return routes;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public List<Geocache> getGeocaches() {
        final List<Geocache> result = new ArrayList<>();
        for (final Item item : globalItems) {
            if (item.coordinate instanceof Geocache) {
                result.add((Geocache) item.coordinate);
            }
        }
        return result;
    }

    public List<Waypoint> getWaypoints() {
        final List<Waypoint> result = new ArrayList<>();
        for (final Item item : globalItems) {
            if (item.coordinate instanceof Waypoint) {
                result.add((Waypoint) item.coordinate);
            }
        }
        return result;
    }

    /** records {@code item} globally, and additionally into the currently-open route's/segment's point list, if any */
    private void record(final Item item) {
        globalItems.add(item);
        if (currentRoutePoints != null) {
            currentRoutePoints.add(item.coordinate);
        }
        if (currentSegmentPoints != null) {
            currentSegmentPoints.add(item.coordinate);
        }
    }

    @Override
    public GPXParser.ParseMode onInit(@Nullable final String gpxCreatorOrName) {
        this.gpxCreatorOrName = gpxCreatorOrName;
        return parseModeGlobal;
    }

    @Override
    public GPXParser.ParseMode onGeocache(@NonNull final Geocache geocache, @Nullable final List<LogEntry> logs) {
        record(new Item(geocache, null, logs));
        return null;
    }

    @Override
    public GPXParser.ParseMode onWaypoint(@NonNull final Waypoint waypoint, @Nullable final String parentGeocode) {
        record(new Item(waypoint, parentGeocode, null));
        return null;
    }

    @Override
    public GPXParser.ParseMode onCoordinate(final ICoordinate coordinate) {
        record(new Item(coordinate, null, null));
        return null;
    }

    @Override
    public GPXParser.ParseMode onNamedCoordinate(final NamedGeoCoordinate coordinate) {
        record(new Item(coordinate, null, null));
        return null;
    }

    @Nullable
    @Override
    public GPXParser.ParseMode onRouteStart() {
        currentRoutePoints = new ArrayList<>();
        return parseModeRoutes; // defer to GPXParser's configured default route mode
    }

    @Override
    public GPXParser.ParseMode onRouteEnd(@Nullable final String name, final int pointCount) {
        routes.add(new NamedCoordinateList(name, pointCount, currentRoutePoints));
        currentRoutePoints = null;
        return parseModeGlobal;
    }

    @Nullable
    @Override
    public GPXParser.ParseMode onTrackStart() {
        currentTrackSegments = new ArrayList<>();
        return parseModeTracks; // defer to GPXParser's configured default track mode
    }

    @Nullable
    @Override
    public GPXParser.ParseMode onTrackSegmentStart() {
        currentSegmentPoints = new ArrayList<>();
        return null; // defer to the mode in effect for the enclosing track
    }

    @Override
    public GPXParser.ParseMode onTrackSegmentEnd(@Nullable final String name, final int pointCount) {
        currentTrackSegments.add(new NamedCoordinateList(name, pointCount, currentSegmentPoints));
        currentSegmentPoints = null;
        return null;
    }

    @Override
    public GPXParser.ParseMode onTrackEnd(@Nullable final String name, final int segmentCount, final int totalPointCount) {
        tracks.add(new Track(name, segmentCount, totalPointCount, currentTrackSegments));
        currentTrackSegments = null;
        return parseModeGlobal;
    }
}



