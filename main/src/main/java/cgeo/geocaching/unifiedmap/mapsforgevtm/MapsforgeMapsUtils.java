package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.location.GeoObject;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.IGeoDataProvider;

import java.util.ArrayList;
import java.util.Collection;

import org.oscim.core.GeoPoint;

public class MapsforgeMapsUtils {

    private MapsforgeMapsUtils() {
        //no instance
    }

    public static ArrayList<ArrayList<GeoPoint>> toGeoPoint(final IGeoDataProvider gg) {
        final ArrayList<ArrayList<GeoPoint>> list = new ArrayList<>();
        for (GeoObject go : gg.getGeoData()) {
            list.add(toGeoPoint(go.getPoints()));
        }
        return list;
    }

    public static ArrayList<GeoPoint> toGeoPoint(final Collection<Geopoint> gps) {
        final ArrayList<GeoPoint> list = new ArrayList<>();
        for (Geopoint gp : gps) {
            list.add(toGeoPoint(gp));
        }
        return list;
    }

    public static GeoPoint toGeoPoint(final Geopoint gp) {
        return new GeoPoint(gp.getLatitude(), gp.getLongitude());
    }
}
