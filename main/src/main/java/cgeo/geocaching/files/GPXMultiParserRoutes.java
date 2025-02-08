package cgeo.geocaching.files;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.xml.XmlNode;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

public class GPXMultiParserRoutes extends GPXMultiParserBase {

    /*
        @todo: IndividualRoute parser is slightly different:
        - remember name in separate variable
        - set linkToPreviousSegment to true
        - does not use setNameAndLatLonParsers() (and does not recognize elevation)
     */

    private final ArrayList<Route> result = new ArrayList<>();

    GPXMultiParserRoutes(@NonNull final String namespace) {
        /*
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
        */
    }

    @Override
    public boolean handlesNode(final String node) {
        return StringUtils.equalsIgnoreCase(node, "rte");
    }

    @Override
    void addNode(@NonNull final XmlNode node) {
        final Route route = new Route(true);
        final ArrayList<Geopoint> points = new ArrayList<>();
        for (XmlNode routePoint : node.getAsList("rtept")) {
            final Double lat = routePoint.getAttributeAsDouble("lat");
            final Double lon = routePoint.getAttributeAsDouble("lon");
            Log.e("rtept (lat=" + lat + ", lon=" + lon + ")");
            if (lat != null && lon != null) {
                points.add(new Geopoint(lat, lon));
            }
        }
        if (!points.isEmpty()) {
            route.add(new RouteSegment(new RouteItem(points.get(0)), points, true));
        }
        result.add(route);
    }

    @Override
    void onParsingDone(@NonNull final Collection<Object> result) {
        result.addAll(this.result);
    }
}
