package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.v6.TapHandler;

import androidx.core.content.res.ResourcesCompat;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Marker;

public class IndividualRoutePointLayer extends Marker {

    private static final Bitmap marker = AndroidGraphicFactory.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.marker_routepoint, null));

    private final TapHandler tapHandler;

    public IndividualRoutePointLayer(final LatLong latLong, final TapHandler tapHandler) {
        super(latLong, marker, 0, 0);
        this.tapHandler = tapHandler;
    }

    @Override
    public boolean onLongPress(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {
        if (Math.abs(layerXY.x - tapXY.x) < marker.getWidth() && Math.abs(layerXY.y - tapXY.y) < marker.getHeight()) {
            tapHandler.setHit(getLatLong());
        }
        return super.onLongPress(tapLatLong, layerXY, tapXY);
    }
}
