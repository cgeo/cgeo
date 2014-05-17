package cgeo.geocaching.maps;

import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;

import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Location;

import java.util.ArrayList;

public class PositionAndScaleOverlay implements GeneralOverlay {
    private OverlayImpl ovlImpl = null;

    PositionDrawer positionDrawer = null;
    ScaleDrawer scaleDrawer = null;

    public PositionAndScaleOverlay(OverlayImpl ovlImpl) {
        this.ovlImpl = ovlImpl;
        positionDrawer = new PositionDrawer();
        scaleDrawer = new ScaleDrawer();
    }

    public void setCoordinates(Location coordinatesIn) {
        positionDrawer.setCoordinates(coordinatesIn);
    }

    public Location getCoordinates() {
        return positionDrawer.getCoordinates();
    }

    public void setHeading(float bearingNow) {
        positionDrawer.setHeading(bearingNow);
    }

    public float getHeading() {
        return positionDrawer.getHeading();
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
        positionDrawer.drawPosition(canvas, projection);
        scaleDrawer.drawScale(canvas, mapView);
    }

    @Override
    public OverlayImpl getOverlayImpl() {
        return this.ovlImpl;
    }

    public ArrayList<Location> getHistory() {
        return positionDrawer.getHistory();
    }

    public void setHistory(ArrayList<Location> history) {
        positionDrawer.setHistory(history);
    }
}
