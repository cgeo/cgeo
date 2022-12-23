package cgeo.geocaching.files;

import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;

import android.sax.Element;
import android.sax.RootElement;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

public class GPXTrackParser extends AbstractTrackOrRouteParser implements AbstractTrackOrRouteParser.RouteParse {

    protected GPXTrackParser(final String namespaceIn, final String versionIn) {
        super(namespaceIn, versionIn, false);
    }

    @NonNull
    public Route parse(@NonNull final InputStream stream) throws IOException, ParserException {
        final RootElement root = new RootElement(namespace, "gpx");
        points = root.getChild(namespace, "trk");
        final Element trackSegment = points.getChild(namespace, "trkseg");
        point = trackSegment.getChild(namespace, "trkpt");

        points.setEndElementListener(() -> {
            if (temp.size() > 0) {
                result.add(new RouteSegment(new RouteItem(temp.get(temp.size() - 1)), temp, false));
                temp = null;
            }
        });

        return super.parse(stream, root);
    }
}
