package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.MapLineUtils;

import android.location.Location;

import androidx.core.util.Pair;

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
    private PostRealDistance postRealDistance = null;

    public interface PostRealDistance {
        void postRealDistance (float realDistance);
    }

    public NavigationLayer(final Geopoint coords, final PostRealDistance postRealDistance) {

        this.destinationCoords = coords;
        this.postRealDistance = postRealDistance;
        width = MapLineUtils.getDirectionLineWidth();
    }

    public void setDestination(final Geopoint coords) {
        destinationCoords = coords;
    }

    public void setCoordinates(final Location coordinatesIn) {
        currentCoords = new Geopoint(coordinatesIn);
    }

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        if (destinationCoords == null || currentCoords == null || !Settings.isMapDirection()) {
            return;
        }

        if (line == null) {
            line = AndroidGraphicFactory.INSTANCE.createPaint();
            line.setStrokeWidth(width);
            line.setStyle(Style.STROKE);
            line.setColor(MapLineUtils.getDirectionColor());
            line.setTextSize(20);
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

        // calculate distance
        if (null != postRealDistance && routingPoints.length > 1) {
            float distance = 0.0f;
            for (int i = 1; i < routingPoints.length; i++) {
                distance += routingPoints[i - 1].distanceTo(routingPoints[i]);
            }
            postRealDistance.postRealDistance(distance);
        }
    }

    private static Pair<Integer, Integer> translateToPixels(final long mapSize, final Point topLeftPoint, final Geopoint coords) {
        final int posX = (int) (MercatorProjection.longitudeToPixelX(coords.getLongitude(), mapSize) - topLeftPoint.x);
        final int posY = (int) (MercatorProjection.latitudeToPixelY(coords.getLatitude(), mapSize) - topLeftPoint.y);
        return new Pair<>(posX, posY);
    }
}
