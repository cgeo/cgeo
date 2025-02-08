package cgeo.geocaching.files;

import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;

import android.sax.Element;

import androidx.annotation.NonNull;

public class GPXMultiParserTracks extends GPXMultiParser {

    GPXMultiParserTracks(@NonNull final Element root, @NonNull final String namespace) {
/*
        result = new Route(false);
        this.namespace = namespace;

        points = root.getChild(namespace, "trk");
        points.setStartElementListener(attributes -> {
            final Element trackSegment = points.getChild(namespace, "trkseg");
            point = trackSegment.getChild(namespace, "trkpt");
            resetTempData();
            setNameAndLatLonParsers();
            points.setEndElementListener(() -> {
                if (!temp.isEmpty()) {
                    result.add(new RouteSegment(new RouteItem(temp.get(temp.size() - 1)), temp, tempElevation, false));
                    resetTempData();
                }
            });
        });
*/
    }

}
