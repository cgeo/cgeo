package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;

import org.mapsforge.v3.android.maps.Projection;
import org.mapsforge.v3.core.GeoPoint;

import android.graphics.Point;

public class MapsforgeMapProjection implements MapProjectionImpl {

    private final Projection projection;

    public MapsforgeMapProjection(final Projection projectionIn) {
        projection = projectionIn;
    }

    @Override
    public void toPixels(final GeoPointImpl leftGeo, final Point left) {
        projection.toPixels((GeoPoint) leftGeo, left);
    }

    @Override
    public Object getImpl() {
        return projection;
    }

}
