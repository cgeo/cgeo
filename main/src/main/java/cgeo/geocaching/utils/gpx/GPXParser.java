package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.ICoordinate;
import cgeo.geocaching.models.NamedGeoCoordinate;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.xml.XmlNode;
import cgeo.geocaching.utils.xml.XmlUtils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Unified, namespace-tolerant "business element" pull-parser for GPX files relevant to Geocaching. Reacts on
 * Geocaches, Waypoints, (Named) Coordinates, Routes and Tracks instead of raw XML elements, via
 * {@link IGPXParseHooks}.
 * <br>
 * <ul>
 *     <li>Handles GPX 1.0 and GPX 1.1 (and mixtures thereof) in one unified code path (no version-specific subclasses).</li>
 *     <li>Does not rely on namespaces being correctly declared; elements/attributes are identified by local
 *     name only (namespace prefixes, if any, are simply stripped).</li>
 *     <li>Self-contained: will only use information from GPX file(s) in created objects.</li>
 *     <li>Multi-file-capability: multiple calls to "parse" may be used to scan related GPX files (eg from a ZIP file)</li>
 *     <li>Handling of enhancements such as GSAP, Groundspeak and c:geo's own tags (via dedicated methods).</li>
 * </ul>
 * <br>
 * {@link #parse} can be called repeatedly, in any order, on the same instance to handle multi-GPX-file-situations (eg from ZIP file).
 * Session-scoped state (esp the cross-file name→geocode index) is <em>not</em> cleared automatically between calls,
 * so resolution works cross-file. Call {@link #reset()} to explicitly start a new, unrelated session on a reused instance.
 */
public class GPXParser {

    private GPXFullWptParser fullWptParser;

    private ParseMode mode = ParseMode.FULL;

    // Retain document metadata even before the first FULL point is encountered.
    private String scriptUrl;

    private IGPXParseHooks hooks;

    /** Controls how wptTypes are parsed. */
    public enum ParseMode {
        /** Parse/collect complete information. */
        FULL,
        /** Parse only basic data per wptType (name + coordinate) */
        COORDINATES_ONLY,
        /** Do not collect wptType data.*/
        SKIP,
        /** Flag to use to abort parsing completely */
        ABORT
    }

    private static class AbortException extends RuntimeException {

    }

    public GPXParser setParseMode(final ParseMode mode) {
        this.mode = mode == null ? ParseMode.FULL : mode;
        return this;
    }

    /** Clears this instance's session-scoped state (esp the cross-file name→geocode index). */
    public GPXParser reset() {
        if (fullWptParser != null) {
            fullWptParser.reset();
        }
        scriptUrl = null;
        return this;
    }

    /** Parses the given stream as GPX, notifying {@code hooks} about business elements found. */
    public boolean parse(@NonNull final InputStream stream, @NonNull final IGPXParseHooks hooks) throws IOException, XmlPullParserException {
        final XmlPullParser parser = XmlUtils.createParser(stream, true);
        return parse(parser, hooks);
    }

    /**
     * Parses the given, already-positioned-or-fresh pull parser as GPX, notifying {@code hooks} about business
     * elements found. The parser is driven forward (single, forward-only pass). May be called
     * repeatedly on the same instance to parse several related GPX documents as one logical unit
     * @return if true, then parsing completed. If false then parsing was aborted by hooks.
     */
    public boolean parse(@NonNull final XmlPullParser parser, @NonNull final IGPXParseHooks hooks) throws IOException, XmlPullParserException {
        this.hooks = hooks;
        if (fullWptParser != null) {
            fullWptParser.resetDocument();
        }
        this.scriptUrl = null;

        try {
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "gpx".equals(GPXUtils.localName(parser.getName()))) {
                    handleGpx(parser);
                    return true;
                }
                event = parser.next();
            }
            return true;
        } catch (AbortException ae) {
            return false;
        }
    }

    private void handleGpx(final XmlPullParser parser) throws IOException, XmlPullParserException {
        final String creator = GPXUtils.attr(parser, "creator");
        scriptUrl = creator;
        adjustParsemode(hooks.onInit(creator));

        int event = parser.next();
        while (!(event == XmlPullParser.END_TAG && "gpx".equals(GPXUtils.localName(parser.getName())))) {
            if (event == XmlPullParser.START_TAG) {
                final String name = GPXUtils.localName(parser.getName());
                if ("wpt".equals(name)) {
                    dispatchWptType(parser);
                } else if ("rte".equals(name)) {
                    handleRoute(parser);
                } else if ("trk".equals(name)) {
                    handleTrack(parser);
                } else if ("url".equals(name) || "creator".equals(name)) {
                    scriptUrl = XmlUtils.parseText(parser);
                } else if ("metadata".equals(name)) {
                    final XmlNode metadata = XmlNode.scanNode(parser);
                    final String url = GPXUtils.readLinkUrl(metadata.getChild("link"));
                    if (StringUtils.isNotBlank(url)) {
                        scriptUrl = url;
                    }
                } else {
                    XmlUtils.skipSubtree(parser);
                }
            }
            event = parser.next();
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // routes
    // ---------------------------------------------------------------------------------------------------

    private void handleRoute(final XmlPullParser parser) throws IOException, XmlPullParserException {
        adjustParsemode(hooks.onRouteStart());
        String routeName = null;
        int pointCount = 0;

        int event = parser.next();
        while (!(event == XmlPullParser.END_TAG && "rte".equals(GPXUtils.localName(parser.getName())))) {
            if (event == XmlPullParser.START_TAG) {
                final String name = GPXUtils.localName(parser.getName());
                if ("name".equals(name)) {
                    routeName = XmlUtils.parseText(parser);
                } else if ("rtept".equals(name)) {
                    dispatchWptType(parser);
                    pointCount++;
                } else {
                    XmlUtils.skipSubtree(parser);
                }
            }
            event = parser.next();
        }
        adjustParsemode(hooks.onRouteEnd(routeName, pointCount));
    }

    // ---------------------------------------------------------------------------------------------------
    // tracks
    // ---------------------------------------------------------------------------------------------------

    private void handleTrack(final XmlPullParser parser) throws IOException, XmlPullParserException {
        adjustParsemode(hooks.onTrackStart());
        String trackName = null;
        int segmentCount = 0;
        int totalPointCount = 0;

        int event = parser.next();
        while (!(event == XmlPullParser.END_TAG && "trk".equals(GPXUtils.localName(parser.getName())))) {
            if (event == XmlPullParser.START_TAG) {
                final String name = GPXUtils.localName(parser.getName());
                if ("name".equals(name)) {
                    trackName = XmlUtils.parseText(parser);
                } else if ("trkseg".equals(name)) {
                    totalPointCount += handleTrackSegment(parser);
                    segmentCount++;
                } else {
                    XmlUtils.skipSubtree(parser);
                }
            }
            event = parser.next();
        }
        adjustParsemode(hooks.onTrackEnd(trackName, segmentCount, totalPointCount));
    }

    /** @return number of point entries found in this segment, including entries without coordinates */
    private int handleTrackSegment(final XmlPullParser parser) throws IOException, XmlPullParserException {
        adjustParsemode(hooks.onTrackSegmentStart());
        String segmentName = null;
        int pointCount = 0;

        int event = parser.next();
        while (!(event == XmlPullParser.END_TAG && "trkseg".equals(GPXUtils.localName(parser.getName())))) {
            if (event == XmlPullParser.START_TAG) {
                final String name = GPXUtils.localName(parser.getName());
                if ("name".equals(name)) {
                    segmentName = XmlUtils.parseText(parser);
                } else if ("trkpt".equals(name)) {
                    dispatchWptType(parser);
                    pointCount++;
                } else {
                    XmlUtils.skipSubtree(parser);
                }
            }
            event = parser.next();
        }
        adjustParsemode(hooks.onTrackSegmentEnd(segmentName, pointCount));
        return pointCount;
    }

    /**
     * Handles a single {@code wpt}/{@code rtept}/{@code trkpt} according to the given (already-resolved) {@link ParseMode}.
     * Fires the appropriate hook(s) for the point, if any.
     */
    private void dispatchWptType(final XmlPullParser parser) throws IOException, XmlPullParserException {
        if (this.mode == ParseMode.SKIP) {
            XmlUtils.skipSubtree(parser);
        } else if (this.mode == ParseMode.COORDINATES_ONLY) {
            dispatchWptTypeCoordinateOnly(parser);
        } else {
            if (fullWptParser == null) {
                fullWptParser = new GPXFullWptParser();
            }
            final XmlNode wptNode = XmlNode.scanNode(parser);
            final ICoordinate entity = fullWptParser.parse(wptNode, scriptUrl);
            if (entity instanceof Geocache) {
                adjustParsemode(hooks.onGeocache((Geocache) entity, fullWptParser.getParsedLogs()));
            } else if (entity instanceof Waypoint) {
                adjustParsemode(hooks.onWaypoint((Waypoint) entity, fullWptParser.getParsedParentGeocode()));
            } else if (entity instanceof NamedGeoCoordinate) {
                adjustParsemode(hooks.onNamedCoordinate((NamedGeoCoordinate) entity));
            } else {
                adjustParsemode(hooks.onCoordinate(entity));
            }
        }
    }

    /**
     * lightweight, non-buffering read of a wpt/rtept/trkpt when only coordinates are wanted (ParseMode.COORDINATES_ONLY)
     */
    private void dispatchWptTypeCoordinateOnly(final XmlPullParser parser) throws IOException, XmlPullParserException {
        final Geopoint coords = XmlUtils.parseGeopoint(GPXUtils.attr(parser, "lat"), GPXUtils.attr(parser, "lon"), true);

        String name = null;
        Float elevation = null;
        int event = parser.next();
        while (event != XmlPullParser.END_TAG) {
            if (event == XmlPullParser.START_TAG) {
                final String childName = GPXUtils.localName(parser.getName());
                if ("name".equals(childName)) {
                    name = XmlUtils.parseText(parser);
                } else if ("ele".equals(childName)) {
                    elevation = XmlUtils.parseFloat(XmlUtils.parseText(parser));
                } else {
                    XmlUtils.skipSubtree(parser);
                }
            }
            event = parser.next();
        }

        if (StringUtils.isBlank(name) && elevation == null) {
            adjustParsemode(hooks.onCoordinate(coords));
        } else {
            final NamedGeoCoordinate named = new NamedGeoCoordinate();
            named.setCoords(coords);
            if (elevation != null) {
                named.setElevation(elevation);
            }
            if (StringUtils.isNotBlank(name)) {
                named.setName(name.trim());
            }
            adjustParsemode(hooks.onNamedCoordinate(named));
        }
    }

    private ParseMode adjustParsemode(final ParseMode candidate) {
        if (candidate != null) {
            this.mode = candidate;
        }
        if (this.mode == ParseMode.ABORT) {
            throw new AbortException();
        }
        return this.mode;
    }
}
