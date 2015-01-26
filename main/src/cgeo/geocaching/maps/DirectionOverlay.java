package cgeo.geocaching.maps;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;
import cgeo.geocaching.settings.Settings;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;

public class DirectionOverlay implements GeneralOverlay {
    private Geopoint currentCoords;
    private final Geopoint destinationCoords;
    private final MapItemFactory mapItemFactory;

    private OverlayImpl ovlImpl = null;

    private Paint line = null;

    public DirectionOverlay(final OverlayImpl ovlImpl, final MapViewImpl mapView, final Geopoint coords, final String geocode) {
        this.ovlImpl = ovlImpl;

        if (coords == null) {
            final Viewport bounds = DataStore.getBounds(geocode);
            if (bounds == null) {
                this.destinationCoords = new Geopoint(0, 0);
            } else {
                this.destinationCoords = bounds.center;
            }
        } else {
            this.destinationCoords = coords;
        }

        this.mapItemFactory = Settings.getMapProvider().getMapItemFactory();
    }

    public void setCoordinates(final Location coordinatesIn) {
        currentCoords = new Geopoint(coordinatesIn);
    }

    @Override
    public void draw(final Canvas canvas, final MapViewImpl mapView, final boolean shadow) {
        drawInternal(canvas, mapView.getMapProjection());
    }

    @Override
    public void drawOverlayBitmap(final Canvas canvas, final Point drawPosition, final MapProjectionImpl projection, final byte drawZoomLevel) {
        drawInternal(canvas, projection);
    }

    private void drawInternal(final Canvas canvas, final MapProjectionImpl projection) {
        if (currentCoords == null) {
            return;
        }

        if (line == null) {
            line = new Paint();
            line.setAntiAlias(true);
            line.setStrokeWidth(2f);
            line.setColor(0xFFEB391E);
        }

        final Point pos = new Point();
        final Point dest = new Point();
        projection.toPixels(mapItemFactory.getGeoPointBase(currentCoords), pos);
        projection.toPixels(mapItemFactory.getGeoPointBase(destinationCoords), dest);

        canvas.drawLine(pos.x, pos.y, dest.x, dest.y, line);
    }

    @Override
    public OverlayImpl getOverlayImpl() {
        return this.ovlImpl;
    }

}
