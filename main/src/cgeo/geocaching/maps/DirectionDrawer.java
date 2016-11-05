package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CanvasUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.util.ArrayList;

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

        final Geopoint[] routingPoints = Routing.getTrack(currentCoords, destinationCoords);
        final ArrayList<Point> pixelPoints = new ArrayList<>();

        // add actual routing points if available
        if (routingPoints != null && routingPoints.length > 1) {
            for (final Geopoint geopoint : routingPoints) {
                pixelPoints.add(translateToPixels(projection, geopoint));
            }
        }

        // add artificial line from routing end to target
        pixelPoints.add(translateToPixels(projection, destinationCoords));

        CanvasUtils.drawPath(pixelPoints, canvas, linePaint);

    }

    private Point translateToPixels(final MapProjectionImpl projection, final Geopoint geopoint) {
        final Point pixelPoint = new Point();
        projection.toPixels(mapItemFactory.getGeoPointBase(geopoint), pixelPoint);
        return pixelPoint;
    }
}
