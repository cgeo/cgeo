package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.v6.TapHandler;

import androidx.core.content.res.ResourcesCompat;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Marker;

public class TapHandlerLayer extends Layer {

    private final TapHandler tapHandler;
    private LatLong longTapLatLong;
    private Bitmap markerBitmap;

    public TapHandlerLayer(final TapHandler tapHandler) {
        this.tapHandler = tapHandler;
    }

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        // display a pin marker if a long click was performed, otherwise nothing visible here
        if (longTapLatLong != null) {
            if (markerBitmap == null) {
                markerBitmap = AndroidGraphicFactory.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.map_pin, null));
            }
            final Marker marker = new Marker(longTapLatLong, markerBitmap, 0, -markerBitmap.getHeight() / 2);
            marker.setDisplayModel(this.getDisplayModel());
            marker.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
        }
    }

    @Override
    public boolean onTap(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {

        tapHandler.finished();

        return true;
    }

    @Override
    public boolean onLongPress(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {
        longTapLatLong = tapLatLong;
        if (!tapHandler.onLongPress(tapXY)) {
            longTapLatLong = null;
        }
        requestRedraw();
        tapHandler.finished();

        return true;
    }

    public LatLong getLongTapLatLong() {
        return longTapLatLong;
    }

    public void resetLongTapLatLong() {
        longTapLatLong = null;
        requestRedraw();
    }
}
