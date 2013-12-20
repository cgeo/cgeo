package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Point;

public class PositionAndScaleOverlay implements GeneralOverlay {
    private OverlayImpl ovlImpl = null;

    //  PositionDrawer positionDrawer = null;

    //    ScaleDrawer scaleDrawer = null;

    public PositionAndScaleOverlay(Activity activity, OverlayImpl ovlImpl) {
        this.ovlImpl = ovlImpl;
        //        positionDrawer = new PositionDrawer(activity);
        //scaleDrawer = new ScaleDrawer(activity);
    }

    @Override
    public void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel) {

        drawInternal(canvas, projection, getOverlayImpl().getMapViewImpl());
    }

    @Override
    public void draw(Canvas canvas, MapViewImpl mapView, boolean shadow) {

        drawInternal(canvas, mapView.getMapProjection(), mapView);
    }

    private void drawInternal(Canvas canvas, MapProjectionImpl projection, MapViewImpl mapView) {
        //    positionDrawer.drawPosition(canvas, projection);
        //        scaleDrawer.drawScale(canvas, mapView);
    }

    @Override
    public OverlayImpl getOverlayImpl() {
        return this.ovlImpl;
    }

}
