package cgeo.geocaching.files;

import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;

import android.sax.Element;

import androidx.annotation.NonNull;

public class GPXMultiParserRoutes extends GPXMultiParserAbstractTracksRoutes implements IGPXMultiParser {

    /*
        @todo: IndividualRoute parser is slightly different:
        - remember name in separate variable
        - set linkToPreviousSegment to true
        - does not use setNameAndLatLonParsers() (and does not recognize elevation)
     */

    GPXMultiParserRoutes(@NonNull final Element root, @NonNull final String namespace) {
        result = new Route(true);
        this.namespace = namespace;

        points = root.getChild(namespace, "rte");
        points.setStartElementListener(attributes -> {
            point = points.getChild(namespace, "rtept");
            resetTempData();
            setNameAndLatLonParsers();
            point.setEndElementListener(() -> {
                if (!temp.isEmpty()) {
                    result.add(new RouteSegment(new RouteItem(temp.get(temp.size() - 1)), temp, tempElevation, false));
                    resetTempData();
                }
            });
        });
    }

}
