package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.brouter.BRouter;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.settings.Settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class DirectionDrawer {
    private Geopoint currentCoords;
    private final Geopoint destinationCoords;
    private final MapItemFactory mapItemFactory;
    private final float width;

    private Paint linePaint = null;

    public DirectionDrawer(final Geopoint coords) {
        this.destinationCoords = coords;
        this.mapItemFactory = Settings.getMapProvider().getMapItemFactory();

        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        width = 5f * metrics.density;

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
            linePaint.setColor(0x80EB391E);
        }


        final Geopoint[] routingPoints = BRouter.getTrack(currentCoords, destinationCoords);

        if (routingPoints != null && routingPoints.length > 1) {

            final Point pixelPoint = new Point();

            // start a new path figure
            final Path path = new Path();
            projection.toPixels(mapItemFactory.getGeoPointBase(routingPoints[0]), pixelPoint);
            path.moveTo(pixelPoint.x, pixelPoint.y);

            for (int i = 1; i < routingPoints.length; i++) {
                final Geopoint currentRoutingPoint = routingPoints[i];
                projection.toPixels(mapItemFactory.getGeoPointBase(currentRoutingPoint), pixelPoint);
                path.lineTo(pixelPoint.x, pixelPoint.y);
            }

            canvas.drawPath(path, linePaint);
        } else {
            final Point pos = new Point();
            final Point dest = new Point();

            projection.toPixels(mapItemFactory.getGeoPointBase(currentCoords), pos);
            projection.toPixels(mapItemFactory.getGeoPointBase(destinationCoords), dest);

            canvas.drawLine(pos.x, pos.y, dest.x, dest.y, linePaint);
        }

    }
}
