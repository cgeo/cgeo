package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CanvasUtils;
import cgeo.geocaching.utils.MapLineUtils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;

import java.util.ArrayList;

public class DirectionDrawer {

    private Geopoint currentCoords;
    private final Geopoint destinationCoords;
    private final MapItemFactory mapItemFactory;
    private final float width;

    private Paint linePaint = null;
    private PostRealDistance postRealDistance = null;

    public interface PostRealDistance {
        void postRealDistance (float realDistance);
    }

    public DirectionDrawer(final Geopoint coords, final PostRealDistance postRealDistance) {
        this.destinationCoords = coords;
        this.mapItemFactory = Settings.getMapProvider().getMapItemFactory();
        this.postRealDistance = postRealDistance;
        width = MapLineUtils.getDirectionLineWidth();
    }

    public void setCoordinates(final Location coordinatesIn) {
        currentCoords = new Geopoint(coordinatesIn);
    }

    void drawDirection(final Canvas canvas, final MapProjectionImpl projection) {
        if (currentCoords == null) {
            return;
        }

        if (!Settings.isMapDirection()) {
            return;
        }

        if (linePaint == null) {
            linePaint = new Paint();
            linePaint.setAntiAlias(true);
            linePaint.setStrokeWidth(width);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setColor(MapLineUtils.getDirectionColor());
        }

        final Geopoint[] routingPoints = Routing.getTrack(currentCoords, destinationCoords);
        final ArrayList<Point> pixelPoints = new ArrayList<>(routingPoints.length);

        for (final Geopoint geopoint : routingPoints) {
            pixelPoints.add(translateToPixels(projection, geopoint));
        }

        CanvasUtils.drawPath(pixelPoints, canvas, linePaint);

        // calculate distance
        if (null != postRealDistance && routingPoints.length > 1) {
            float distance = 0.0f;
            for (int i = 1; i < routingPoints.length; i++) {
                distance += routingPoints[i - 1].distanceTo(routingPoints[i]);
            }
            postRealDistance.postRealDistance(distance);
        }
    }

    private Point translateToPixels(final MapProjectionImpl projection, final Geopoint geopoint) {
        final Point pixelPoint = new Point();
        projection.toPixels(mapItemFactory.getGeoPointBase(geopoint), pixelPoint);
        return pixelPoint;
    }
}
