package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.mapsforge.v6.TapHandler;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemTestLayer;

import android.content.Context;

import androidx.core.content.res.ResourcesCompat;

import java.lang.ref.WeakReference;

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

    private final GeoItemTestLayer testItemLayer;
    private final WeakReference<Context> wrContext;

    public TapHandlerLayer(final TapHandler tapHandler, final GeoItemTestLayer testItemLayer, final Context context) {
        this.tapHandler = tapHandler;
        this.testItemLayer = testItemLayer;
        this.wrContext = new WeakReference<>(context);
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

        final Context ctx = wrContext.get();
        if (testItemLayer != null && ctx != null) {
            testItemLayer.handleTap(ctx, new Geopoint(tapLatLong.latitude, tapLatLong.longitude));
        }

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
