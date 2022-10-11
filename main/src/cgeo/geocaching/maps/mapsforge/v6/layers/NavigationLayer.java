package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.MapLineUtils;

import android.location.Location;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

public class NavigationLayer extends Layer {

    private Geopoint currentCoords;
    private Geopoint destinationCoords;

    private Paint paint = null;
    private PostRealDistance postRealDistance = null;

    public interface PostRealDistance {
        void postRealDistance(float realDistance);
    }

    public NavigationLayer(final Geopoint coords, final PostRealDistance postRealDistance) {
        this.destinationCoords = coords;
        this.postRealDistance = postRealDistance;
        resetColor();
    }

    public void resetColor() {
        paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setStrokeWidth(MapLineUtils.getDirectionLineWidth(false));
        paint.setStyle(Style.STROKE);
        paint.setColor(MapLineUtils.getDirectionColor());
        paint.setTextSize(20);
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

        final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());

        final Geopoint[] routingPoints = Routing.getTrack(currentCoords, destinationCoords);
        Geopoint point = routingPoints[0];
        final Path path = AndroidGraphicFactory.INSTANCE.createPath();
        path.moveTo((float) (MercatorProjection.longitudeToPixelX(point.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(point.getLatitude(), mapSize) - topLeftPoint.y));

        for (int i = 1; i < routingPoints.length; i++) {
            point = routingPoints[i];
            path.lineTo((float) (MercatorProjection.longitudeToPixelX(point.getLongitude(), mapSize) - topLeftPoint.x), (float) (MercatorProjection.latitudeToPixelY(point.getLatitude(), mapSize) - topLeftPoint.y));
        }
        canvas.drawPath(path, paint);

        // calculate distance
        if (null != postRealDistance && routingPoints.length > 1) {
            float distance = 0.0f;
            for (int i = 1; i < routingPoints.length; i++) {
                distance += routingPoints[i - 1].distanceTo(routingPoints[i]);
            }
            postRealDistance.postRealDistance(distance);
        }
    }

}
