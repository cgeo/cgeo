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
    ParseMode onInit(@Nullable String gpxCreatorOrName);

    /**
     * Called only when {@code geocacheParseMode} is {@link ParseMode#FULL}
     * @param logs the geocache's logs, in document order; {@code null} if no log tag was found
     */
    ParseMode onGeocache(@NonNull Geocache geocache, @Nullable List<LogEntry> logs);

    /**
     * Called only when {@code waypointParseMode} is {@link ParseMode#FULL}
     * Coordinates may be {@code null}, e.g. for an unsolved stage. Extension flags, prefix and
     * listing/user notes are populated before this callback. Attaching the waypoint to its parent
     * (and marking that parent's coordinates as user-modified for an ORIGINAL waypoint) is the caller's responsibility.
     * @param parentGeocode best-effort resolved parent geocode (see D1/D6); may be {@code null}
     */
    ParseMode onWaypoint(@NonNull Waypoint waypoint, @Nullable String parentGeocode);

    /** Bare coordinate; missing or invalid latitude/longitude components are replaced with zero. */
    ParseMode onCoordinate(ICoordinate coordinate);

    /** Named coordinate; missing or invalid latitude/longitude components are replaced with zero. */
    ParseMode onNamedCoordinate(NamedGeoCoordinate coordinate);

    /** @return the {@link ParseMode} to use for this specific route (null for default) */
    @Nullable
    ParseMode onRouteStart();

    /**
     * @param name       the route's {@code <name>}, if any; may be {@code null}
     * @param pointCount number of {@code <rtept>} children, including entries with missing/invalid coordinates and skipped entries
     */
    ParseMode onRouteEnd(@Nullable String name, int pointCount);

    /** @return the {@link ParseMode} to use for this specific track; (null for default) */
    @Nullable
    ParseMode onTrackStart();

    /** @return the {@link ParseMode} to use for this specific track segment; (null for default) */
    @Nullable
    ParseMode onTrackSegmentStart();

    /**
     * @param name       the track segment's {@code <name>}, if any; may be {@code null}
     * @param pointCount number of {@code <trkpt>} children, including entries with missing/invalid coordinates and skipped entries
     */
    ParseMode onTrackSegmentEnd(@Nullable String name, int pointCount);

    /**@param name the track's {@code <name>}, if any; may be {@code null} */
    ParseMode onTrackEnd(@Nullable String name, int segmentCount, int totalPointCount);
}



