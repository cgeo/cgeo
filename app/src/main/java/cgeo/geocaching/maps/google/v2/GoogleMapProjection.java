package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;

import android.graphics.Point;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;

public class GoogleMapProjection implements MapProjectionImpl {

    private final Projection projection;

    public GoogleMapProjection(final Projection projectionIn) {
        projection = projectionIn;
    }

    @Override
    public void toPixels(final GeoPointImpl leftGeo, final Point left) {
        final Point p = projection.toScreenLocation(new LatLng(leftGeo.getLatitudeE6() / 1e6, leftGeo.getLongitudeE6() / 1e6));
        if (p == null) {
            return;
        }
        left.x = p.x;
        left.y = p.y;
    }

    @Override
    public Object getImpl() {
        return projection;
    }

}
