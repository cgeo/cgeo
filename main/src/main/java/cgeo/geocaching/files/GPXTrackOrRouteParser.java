package cgeo.geocaching.files;

import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;

import android.sax.Element;
import android.sax.RootElement;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

public class GPXTrackOrRouteParser extends AbstractTrackOrRouteParser implements AbstractTrackOrRouteParser.RouteParse {

    enum PARSINGMODE {
        MODE_NONE, MODE_TRACK, MODE_ROUTE
    }
    protected PARSINGMODE parsingMode = PARSINGMODE.MODE_NONE;

    GPXTrackOrRouteParser(final String namespaceIn, final String versionIn) {
        super(namespaceIn, versionIn);
    }

    @Override
    @NonNull
    public Route parse(@NonNull final InputStream stream) throws IOException, ParserException {
        final RootElement root = new RootElement(namespace, "gpx");

        // check for tracks
        final Element pointsTrack = root.getChild(namespace, "trk");
        pointsTrack.setStartElementListener(attributes -> {
            if (parsingMode == PARSINGMODE.MODE_NONE) {
                points = pointsTrack;
                final Element trackSegment = points.getChild(namespace, "trkseg");
                point = trackSegment.getChild(namespace, "trkpt");
                configure(PARSINGMODE.MODE_TRACK, false, points);
            }
        });

        // check for GPX routes (tracks take precedence)
        final Element pointsRoute = root.getChild(namespace, "rte");
        pointsRoute.setStartElementListener(attributes -> {
            if (parsingMode == PARSINGMODE.MODE_NONE) {
                points = pointsRoute;
                point = points.getChild(namespace, "rtept");
                configure(PARSINGMODE.MODE_ROUTE, true, point);
            }
        });

        return doParsing(stream, root);
    }

    private void configure(final PARSINGMODE parsingMode, final boolean routeable, final Element endElementForListener) {
        this.parsingMode = parsingMode;
        result.setRouteable(routeable);

        resetTempData();
        setNameAndLatLonParsers();
        endElementForListener.setEndElementListener(() -> {
            if (!temp.isEmpty()) {
                result.add(new RouteSegment(new RouteItem(temp.get(temp.size() - 1)), temp, tempElevation, false));
                resetTempData();
            }
        });
    }

}
