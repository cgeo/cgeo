package cgeo.geocaching.files;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.utils.xml.XmlNode;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

public class GPXMultiParserTRK extends GPXMultiParserBase {

    private final ArrayList<Route> result = new ArrayList<>();

    @Override
    public String getNodeName() {
        return "trk";
    }

    @Override
    void addNode(@NonNull final XmlNode node) {
        final Route route = new Route(false);
        route.setName(node.getValueAsString("name"));
        for (XmlNode trackSegment : node.getAsList("trkseg")) {
            final ArrayList<Geopoint> points = new ArrayList<>();
            final ArrayList<Float> elevation = new ArrayList<>();
            for (XmlNode trackPoint : trackSegment.getAsList("trkpt")) {
                final Double lat = trackPoint.getAttributeAsDouble("lat");
                final Double lon = trackPoint.getAttributeAsDouble("lon");
                if (lat != null && lon != null) {
                    points.add(new Geopoint(lat, lon));
                    elevation.add(trackPoint.getValueAsFloat("ele"));
                }
            }
            if (!points.isEmpty()) {
                route.add(new RouteSegment(new RouteItem(points.get(points.size() - 1)), points, elevation, false));
            }
        }
        result.add(route);
    }

    @Override
    void onParsingDone(@NonNull final Collection<Object> result) {
        result.addAll(this.result);
    }

}
