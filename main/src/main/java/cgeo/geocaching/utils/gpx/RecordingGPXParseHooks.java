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

    /** kind of a single, top-level (or route-/track-embedded) recorded item; see {@link #getItems()} */
    public enum ItemKind { GEOCACHE, WAYPOINT, NAMED_COORDINATE, COORDINATE }

    /** a single recorded geocache/waypoint/(named) coordinate, in the order it was encountered */
    public static final class RecordedItem {
        private final ItemKind kind;
        private final ICoordinate coordinate;
        private final String parentGeocode;
        private final List<LogEntry> logs;

        RecordedItem(final ItemKind kind, final ICoordinate coordinate, final String parentGeocode) {
            this(kind, coordinate, parentGeocode, null);
        }

        RecordedItem(final ItemKind kind, final ICoordinate coordinate, final String parentGeocode, final List<LogEntry> logs) {
            this.kind = kind;
            this.coordinate = coordinate;
            this.parentGeocode = parentGeocode;
            this.logs = logs;
        }

        public ItemKind getKind() {
            return kind;
        }

        public ICoordinate getCoordinate() {
            return coordinate;
        }

        /** only set for {@link ItemKind#WAYPOINT}; the best-effort resolved parent geocode, may be {@code null} */
        @Nullable
        public String getParentGeocode() {
            return parentGeocode;
        }

        /** only set for {@link ItemKind#GEOCACHE}; the geocache's logs, {@code null} if none were found */
        @Nullable
        public List<LogEntry> getLogs() {
            return logs;
        }
    }

    /** a single recorded route ({@code <rte>}) */
    public static final class RecordedRoute {
        private final String name;
        private final int pointCount;
        private final List<ICoordinate> points;

        RecordedRoute(final String name, final int pointCount, final List<ICoordinate> points) {
            this.name = name;
            this.pointCount = pointCount;
            this.points = points;
        }

        @Nullable
        public String getName() {
            return name;
        }

        /** authoritative count of valid points, see {@link IGPXParseHooks#onRouteEnd}; may exceed {@link #getPoints()}'s size under {@link GPXParser.WptParseMode#SKIP} */
        public int getPointCount() {
            return pointCount;
        }

        /** points actually reported via hooks while this route was being parsed; empty under {@link GPXParser.WptParseMode#SKIP} */
        public List<ICoordinate> getPoints() {
            return points;
        }
    }

    /** a single recorded track segment ({@code <trkseg>}) */
    public static final class RecordedTrackSegment {
        private final String name;
        private final int pointCount;
        private final List<ICoordinate> points;

        RecordedTrackSegment(final String name, final int pointCount, final List<ICoordinate> points) {
            this.name = name;
            this.pointCount = pointCount;
            this.points = points;
        }

        @Nullable
        public String getName() {
            return name;
        }

        public int getPointCount() {
            return pointCount;
        }

        public List<ICoordinate> getPoints() {
            return points;
        }
    }

    /** a single recorded track ({@code <trk>}), made up of its recorded segments */
    public static final class RecordedTrack {
        private final String name;
        private final int segmentCount;
        private final int totalPointCount;
        private final List<RecordedTrackSegment> segments;

        RecordedTrack(final String name, final int segmentCount, final int totalPointCount, final List<RecordedTrackSegment> segments) {
            this.name = name;
            this.segmentCount = segmentCount;
            this.totalPointCount = totalPointCount;
            this.segments = segments;
        }

        @Nullable
        public String getName() {
            return name;
        }

        public List<RecordedTrackSegment> getSegments() {
            return segments;
        }

        public int getSegmentCount() {
            return segmentCount;
        }

        public int getTotalPointCount() {
            return totalPointCount;
        }
    }

    private String gpxCreatorOrName;
    private final List<RecordedItem> items = new ArrayList<>();
    private final List<RecordedRoute> routes = new ArrayList<>();
    private final List<RecordedTrack> tracks = new ArrayList<>();

    /** points recorded so far for the currently-open route; non-null only between onRouteStart and onRouteEnd */
    private List<ICoordinate> currentRoutePoints;
    /** points recorded so far for the currently-open track segment; non-null only between onTrackSegmentStart and onTrackSegmentEnd */
    private List<ICoordinate> currentSegmentPoints;
    /** segments recorded so far for the currently-open track; non-null only between onTrackStart and onTrackEnd */
    private List<RecordedTrackSegment> currentTrackSegments;

    @Nullable
    public String getGpxCreatorOrName() {
        return gpxCreatorOrName;
    }

    /** all top-level (and route-/track-embedded) geocaches/waypoints/coordinates, in document order */
    public List<RecordedItem> getItems() {
        return items;
    }

    public List<RecordedRoute> getRoutes() {
        return routes;
    }

    public List<RecordedTrack> getTracks() {
        return tracks;
    }

    public List<Geocache> getGeocaches() {
        final List<Geocache> result = new ArrayList<>();
        for (final RecordedItem item : items) {
            if (item.getKind() == ItemKind.GEOCACHE && item.getCoordinate() instanceof Geocache) {
                result.add((Geocache) item.getCoordinate());
            }
        }
        return result;
    }

    public List<Waypoint> getWaypoints() {
        final List<Waypoint> result = new ArrayList<>();
        for (final RecordedItem item : items) {
            if (item.getKind() == ItemKind.WAYPOINT && item.getCoordinate() instanceof Waypoint) {
                result.add((Waypoint) item.getCoordinate());
            }
        }
        return result;
    }

    /** records {@code item} globally, and additionally into the currently-open route's/segment's point list, if any */
    private void record(final RecordedItem item) {
        items.add(item);
        if (currentRoutePoints != null) {
            currentRoutePoints.add(item.getCoordinate());
        }
        if (currentSegmentPoints != null) {
            currentSegmentPoints.add(item.getCoordinate());
        }
    }

    @Override
    public void onInit(@Nullable final String gpxCreatorOrName) {
        this.gpxCreatorOrName = gpxCreatorOrName;
    }

    @Override
    public void onGeocache(@NonNull final Geocache geocache, @Nullable final List<LogEntry> logs) {
        record(new RecordedItem(ItemKind.GEOCACHE, geocache, null, logs));
    }

    @Override
    public void onWaypoint(@NonNull final Waypoint waypoint, @Nullable final String parentGeocode) {
        record(new RecordedItem(ItemKind.WAYPOINT, waypoint, parentGeocode));
    }

    @Override
    public void onCoordinate(final ICoordinate coordinate) {
        record(new RecordedItem(ItemKind.COORDINATE, coordinate, null));
    }

    @Override
    public void onNamedCoordinate(final NamedGeoCoordinate coordinate) {
        record(new RecordedItem(ItemKind.NAMED_COORDINATE, coordinate, null));
    }

    @Nullable
    @Override
    public GPXParser.WptParseMode onRouteStart() {
        currentRoutePoints = new ArrayList<>();
        return null; // defer to GPXParser's configured default route mode
    }

    @Override
    public void onRouteEnd(@Nullable final String name, final int pointCount) {
        routes.add(new RecordedRoute(name, pointCount, currentRoutePoints));
        currentRoutePoints = null;
    }

    @Nullable
    @Override
    public GPXParser.WptParseMode onTrackStart() {
        currentTrackSegments = new ArrayList<>();
        return null; // defer to GPXParser's configured default track mode
    }

    @Nullable
    @Override
    public GPXParser.WptParseMode onTrackSegmentStart() {
        currentSegmentPoints = new ArrayList<>();
        return null; // defer to the mode in effect for the enclosing track
    }

    @Override
    public void onTrackSegmentEnd(@Nullable final String name, final int pointCount) {
        currentTrackSegments.add(new RecordedTrackSegment(name, pointCount, currentSegmentPoints));
        currentSegmentPoints = null;
    }

    @Override
    public void onTrackEnd(@Nullable final String name, final int segmentCount, final int totalPointCount) {
        tracks.add(new RecordedTrack(name, segmentCount, totalPointCount, currentTrackSegments));
        currentTrackSegments = null;
    }
}



