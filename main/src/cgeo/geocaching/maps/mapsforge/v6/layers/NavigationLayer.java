package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.routing.Routing;

import android.content.Context;
import android.location.Location;
import android.support.v4.util.Pair;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.util.ArrayList;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

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

        width = 8f * metrics.density;
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
            line.setStyle(Style.STROKE);
            line.setColor(0xD0EB391E);
        }
        final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());

        final Geopoint[] routingPoints = Routing.getTrack(currentCoords, destinationCoords);
        final ArrayList<Pair<Integer, Integer>> pixelPoints = new ArrayList<>(routingPoints.length);

        for (final Geopoint geopoint : routingPoints) {
            pixelPoints.add(translateToPixels(mapSize, topLeftPoint, geopoint));
        }

        // paint path segments
        for (int i = 1; i < pixelPoints.size(); i++) {
            final Pair<Integer, Integer> source = pixelPoints.get(i - 1);
            final Pair<Integer, Integer> destination = pixelPoints.get(i);
            canvas.drawLine(source.first, source.second, destination.first, destination.second, line);
        }
    }

    private static Pair<Integer, Integer> translateToPixels(final long mapSize, final Point topLeftPoint, final Geopoint coords) {
        final int posX = (int) (MercatorProjection.longitudeToPixelX(coords.getLongitude(), mapSize) - topLeftPoint.x);
        final int posY = (int) (MercatorProjection.latitudeToPixelY(coords.getLatitude(), mapSize) - topLeftPoint.y);
        return new Pair<>(posX, posY);
    }
}
