package cgeo.geocaching.files;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Track-specific helper for {@link UnifiedGPXParser}.
 * <p>
 * Parses a single {@code <trk>} element into a non-routable {@link Route}.
 * Each {@code <trkseg>} child becomes its own {@link RouteSegment} with
 * {@code linkToPreviousSegment=false} so distinct track segments are rendered
 * as separate polylines.
 * <p>
 * Per-point fields handled today:
 * <ul>
 *   <li>{@code lat} / {@code lon} attributes of {@code <trkpt>} (mandatory — a
 *       trkpt without parseable coordinates is dropped);</li>
 *   <li>{@code <ele>} text content as a float.</li>
 * </ul>
 * Elevation handling follows the convention used by
 * {@code RouteTrackUtils.addMissingElevationData}: a {@link RouteSegment}'s
 * elevation list is only handed back when it is fully aligned with the points
 * list. If any trkpt in the segment is missing an {@code <ele>} (or its value
 * is unparseable), the segment is created without an elevation list so the
 * elevation service can fill it in later.
 */
final class UnifiedGPXTrackParser {

    private UnifiedGPXTrackParser() {
        // utility class
    }

    /**
     * Parse the {@code <trk>} element the supplied parser is currently positioned on
     * and return the resulting {@link Route}. On return the parser is positioned on
     * the matching {@code </trk>} end tag.
     */
    @NonNull
    static Route parseTrack(@NonNull final XmlPullParser parser) throws XmlPullParserException, IOException {
        final int startDepth = parser.getDepth();
        final Route track = new Route(false);

        while (true) {
            final int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) {
                break;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "name":
                    final String trackName = UnifiedGPXParser.readText(parser);
                    if (trackName != null) {
                        track.setName(trackName);
                    }
                    break;
                case "trkseg":
                    final RouteSegment segment = parseTrackSegment(parser);
                    if (segment != null) {
                        track.add(segment);
                    }
                    break;
                default:
                    UnifiedGPXParser.skipSubtree(parser);
                    break;
            }
        }
        return track;
    }

    @Nullable
    private static RouteSegment parseTrackSegment(final XmlPullParser parser)
            throws XmlPullParserException, IOException {
        final int startDepth = parser.getDepth();
        final ArrayList<Geopoint> points = new ArrayList<>();
        final ArrayList<Float> elevations = new ArrayList<>();

        while (true) {
            final int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) {
                break;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            if ("trkpt".equals(parser.getName())) {
                parseTrackPoint(parser, points, elevations);
            } else {
                UnifiedGPXParser.skipSubtree(parser);
            }
        }

        if (points.isEmpty()) {
            return null;
        }

        // Only hand back the elevation list if it lines up with the points list;
        // otherwise leave it null so RouteTrackUtils.addMissingElevationData can
        // re-fetch the missing values from the elevation service.
        final ArrayList<Float> alignedElevations = elevations.size() == points.size() ? elevations : null;
        return new RouteSegment(new RouteItem(points.get(points.size() - 1)), points, alignedElevations, false);
    }

    private static void parseTrackPoint(final XmlPullParser parser, final ArrayList<Geopoint> points,
                                        final ArrayList<Float> elevations)
            throws XmlPullParserException, IOException {
        final Geopoint coords = UnifiedGPXParser.readLatLon(parser);
        final int startDepth = parser.getDepth();
        Float ele = null;

        while (true) {
            final int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
            if (event == XmlPullParser.END_TAG && parser.getDepth() == startDepth) {
                break;
            }
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            if ("ele".equals(parser.getName())) {
                final String eleText = UnifiedGPXParser.readText(parser);
                if (eleText != null) {
                    try {
                        ele = Float.parseFloat(eleText.trim());
                    } catch (NumberFormatException e) {
                        Log.w("UnifiedGPXTrackParser: invalid ele value '" + eleText + "'");
                    }
                }
            } else {
                UnifiedGPXParser.skipSubtree(parser);
            }
        }

        if (coords == null) {
            return;
        }
        points.add(coords);
        if (ele != null) {
            elevations.add(ele);
        }
    }
}
