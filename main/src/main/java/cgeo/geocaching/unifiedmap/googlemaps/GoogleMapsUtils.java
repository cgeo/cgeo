package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.location.GeoObject;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.IGeoDataProvider;

import java.util.ArrayList;
import java.util.Collection;

import com.google.android.gms.maps.model.LatLng;

public class GoogleMapsUtils {

    private GoogleMapsUtils() {
        //no instance
    }

    public static ArrayList<ArrayList<LatLng>> toLatLng(final IGeoDataProvider gg) {
        final ArrayList<ArrayList<LatLng>> list = new ArrayList<>();
        for (GeoObject go : gg.getGeoData()) {
            list.add(toLatLng(go.getPoints()));
        }
        return list;
    }

    public static ArrayList<LatLng> toLatLng(final Collection<Geopoint> gps) {
        final ArrayList<LatLng> list = new ArrayList<>();
        for (Geopoint gp : gps) {
            list.add(toLatLng(gp));
        }
        return list;
    }

    public static LatLng toLatLng(final Geopoint gp) {
        return new LatLng(gp.getLatitude(), gp.getLongitude());
    }
}
