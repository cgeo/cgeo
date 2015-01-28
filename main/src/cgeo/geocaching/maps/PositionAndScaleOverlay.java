package cgeo.geocaching.maps;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
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
    DirectionDrawer directionDrawer = null;
    DistanceDrawer distanceDrawer = null;

    public PositionAndScaleOverlay(final OverlayImpl ovlImpl, final MapViewImpl mapView, final Geopoint coords, final String geocode) {
        this.ovlImpl = ovlImpl;
        positionDrawer = new PositionDrawer();
        scaleDrawer = new ScaleDrawer();

        if (coords != null) {
            directionDrawer = new DirectionDrawer(coords);
            distanceDrawer = new DistanceDrawer(mapView, coords);
        } else if (geocode != null) {
            final Viewport bounds = DataStore.getBounds(geocode);
            if (bounds != null) {
                directionDrawer = new DirectionDrawer(bounds.center);
                distanceDrawer = new DistanceDrawer(mapView, bounds.center);
            }
        }
    }

    public void setCoordinates(final Location coordinatesIn) {
        positionDrawer.setCoordinates(coordinatesIn);
        if (directionDrawer != null) {
            directionDrawer.setCoordinates(coordinatesIn);
            distanceDrawer.setCoordinates(coordinatesIn);
        }

    }

    public Location getCoordinates() {
        return positionDrawer.getCoordinates();
    }

    public void setHeading(final float bearingNow) {
        positionDrawer.setHeading(bearingNow);
    }

    public float getHeading() {
        return positionDrawer.getHeading();
    }

    @Override
    public void drawOverlayBitmap(final Canvas canvas, final Point drawPosition,
            final MapProjectionImpl projection, final byte drawZoomLevel) {

        drawInternal(canvas, projection, getOverlayImpl().getMapViewImpl());
    }

    @Override
    public void draw(final Canvas canvas, final MapViewImpl mapView, final boolean shadow) {

        drawInternal(canvas, mapView.getMapProjection(), mapView);
    }

    private void drawInternal(final Canvas canvas, final MapProjectionImpl projection, final MapViewImpl mapView) {
        if (directionDrawer != null) {
            directionDrawer.drawDirection(canvas, projection);
        }
        positionDrawer.drawPosition(canvas, projection);
        scaleDrawer.drawScale(canvas, mapView);
        if (distanceDrawer != null) {
            distanceDrawer.drawDistance(canvas);
        }
    }

    @Override
    public OverlayImpl getOverlayImpl() {
        return this.ovlImpl;
    }

    public ArrayList<Location> getHistory() {
        return positionDrawer.getHistory();
    }

    public void setHistory(final ArrayList<Location> history) {
        positionDrawer.setHistory(history);
    }
}
