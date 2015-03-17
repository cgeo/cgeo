package cgeo.geocaching.maps.google.v1;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;

import android.graphics.Point;

public class GoogleMapProjection implements MapProjectionImpl {

    private final Projection projection;

    public GoogleMapProjection(final Projection projectionIn) {
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
