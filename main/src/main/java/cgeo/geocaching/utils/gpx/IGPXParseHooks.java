package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.ICoordinate;
import cgeo.geocaching.models.NamedGeoCoordinate;
import cgeo.geocaching.models.Waypoint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/** Callback interface for {@link GPXParser}. */
public interface IGPXParseHooks {

    /** Called once, right after the root {@code <gpx>} element (and its {@code creator} attribute, if any) was read. */
    void onInit(@Nullable String gpxCreatorOrName);

    /**
     * Called only when {@code geocacheParseMode} is {@link GPXParser.WptParseMode#FULL}
     * @param logs the geocache's logs, in document order; {@code null} if no log tag was found
     */
    void onGeocache(@NonNull Geocache geocache, @Nullable List<LogEntry> logs);

    /**
     * Called only when {@code waypointParseMode} is {@link GPXParser.WptParseMode#FULL}
     * @param parentGeocode best-effort resolved parent geocode (see D1/D6); may be {@code null}
     */
    void onWaypoint(@NonNull Waypoint waypoint, @Nullable String parentGeocode);

    /** Bare coordinate: only lat/lon, nothing else usable. */
    void onCoordinate(ICoordinate coordinate);

    /** Has a name and/or elevation and/or geocode, but wasn't classified as geocache/waypoint. */
    void onNamedCoordinate(NamedGeoCoordinate coordinate);

    /** @return the {@link GPXParser.WptParseMode} to use for this specific route (null for default) */
    @Nullable
    GPXParser.WptParseMode onRouteStart();

    /**
     * @param name       the route's {@code <name>}, if any; may be {@code null}
     * @param pointCount number of {@code <rtept>} children with valid lat/lon
     */
    void onRouteEnd(@Nullable String name, int pointCount);

    /** @return the {@link GPXParser.WptParseMode} to use for this specific track; (null for default) */
    @Nullable
    GPXParser.WptParseMode onTrackStart();

    /** @return the {@link GPXParser.WptParseMode} to use for this specific track segment; (null for default) */
    @Nullable
    GPXParser.WptParseMode onTrackSegmentStart();

    /**
     * @param name       the track segment's {@code <name>}, if any; may be {@code null}
     * @param pointCount number of {@code <trkpt>} children with valid lat/lon
     */
    void onTrackSegmentEnd(@Nullable String name, int pointCount);

    /**@param name the track's {@code <name>}, if any; may be {@code null} */
    void onTrackEnd(@Nullable String name, int segmentCount, int totalPointCount);
}



