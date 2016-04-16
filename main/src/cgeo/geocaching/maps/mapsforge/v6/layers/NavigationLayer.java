package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

import android.content.Context;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class NavigationLayer extends Layer {

    private Geopoint currentCoords;
    private Geopoint destinationCoords;
    private final float width;

    private Paint line = null;

    public NavigationLayer(final Geopoint coords) {

        this.destinationCoords = coords;

        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        width = 4f * metrics.density;
    }

    public void setDestination(final Geopoint coords) {
        destinationCoords = coords;
    }

    public void setCoordinates(final Location coordinatesIn) {
        currentCoords = new Geopoint(coordinatesIn);
    }

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        if (destinationCoords == null || currentCoords == null) {
            return;
        }

        if (line == null) {
            line = AndroidGraphicFactory.INSTANCE.createPaint();
            line.setStrokeWidth(width);
            line.setColor(0x80EB391E);
        }

        final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());

        final int posX = (int) (MercatorProjection.longitudeToPixelX(this.currentCoords.getLongitude(), mapSize) - topLeftPoint.x);
        final int posY = (int) (MercatorProjection.latitudeToPixelY(this.currentCoords.getLatitude(), mapSize) - topLeftPoint.y);

        final int destX = (int) (MercatorProjection.longitudeToPixelX(this.destinationCoords.getLongitude(), mapSize) - topLeftPoint.x);
        final int destY = (int) (MercatorProjection.latitudeToPixelY(this.destinationCoords.getLatitude(), mapSize) - topLeftPoint.y);

        canvas.drawLine(posX, posY, destX, destY, line);
    }
}
