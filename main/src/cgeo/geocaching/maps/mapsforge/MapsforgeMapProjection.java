package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.GeoPoint;

import android.graphics.Point;

public class MapsforgeMapProjection implements MapProjectionImpl {

    private Projection projection;

    public MapsforgeMapProjection(Projection projectionIn) {
        projection = projectionIn;
    }

    @Override
    public void toPixels(GeoPointImpl leftGeo, Point left) {
        projection.toPixels((GeoPoint) leftGeo, left);
    }

    @Override
    public Object getImpl() {
        return projection;
    }

}
