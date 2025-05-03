package cgeo.geocaching.files;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.utils.xml.XmlNode;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

public class GPXMultiParserRTE extends GPXMultiParserBase {

    /*
        @todo: IndividualRoute parser is slightly different:
        - remember name in separate variable
        - set linkToPreviousSegment to true
        - does not use setNameAndLatLonParsers() (and does not recognize elevation)
     */

    private final ArrayList<Route> result = new ArrayList<>();

    @Override
    public String getNodeName() {
        return "rte";
    }

    @Override
    void addNode(@NonNull final XmlNode node) {
        final Route route = new Route(true);
        route.setName(node.getValueAsString("name"));
        final ArrayList<Geopoint> points = new ArrayList<>();
        final ArrayList<Float> elevation = new ArrayList<>();
        for (XmlNode routePoint : node.getAsList("rtept")) {
            final Double lat = routePoint.getAttributeAsDouble("lat");
            final Double lon = routePoint.getAttributeAsDouble("lon");
            if (lat != null && lon != null) {
                points.add(new Geopoint(lat, lon));
                elevation.add(routePoint.getValueAsFloat("ele"));
            }
        }
        if (!points.isEmpty()) {
            route.add(new RouteSegment(new RouteItem(points.get(points.size() - 1)), points, elevation, false));
        }
        result.add(route);
    }

    @Override
    void onParsingDone(@NonNull final Collection<Object> result) {
        result.addAll(this.result);
    }
}
