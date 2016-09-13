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
import android.graphics.Point;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import org.xml.sax.SAXException;

public class DirectionDrawer {
    private Geopoint currentCoords;
    private final Geopoint destinationCoords;
    private final MapItemFactory mapItemFactory;
    private final float width;

    private Paint line = null;

    public DirectionDrawer(final Geopoint coords) {
        this.destinationCoords = coords;
        this.mapItemFactory = Settings.getMapProvider().getMapItemFactory();

        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        width = 4f * metrics.density;

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

        if (line == null) {
            line = new Paint();
            line.setAntiAlias(true);
            line.setStrokeWidth(width);
            line.setColor(0x80EB391E);
        }

        final Point pos = new Point();
        final Point dest = new Point();

        Geopoint[] routingPoints =  null;
        try {
            routingPoints = BRouter.getTrack(currentCoords, destinationCoords);
        } catch (SAXException e) {
            e.printStackTrace();
        }

        if(routingPoints != null && routingPoints.length > 0){
            Point lastPoint = null;
            Point currentPoint = new Point();
            for (Geopoint currentGeoPoint: routingPoints) {
                projection.toPixels(mapItemFactory.getGeoPointBase(currentGeoPoint), currentPoint);

                if(lastPoint != null){
                    canvas.drawLine(lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y, line);
                }
                else {
                    lastPoint = new Point();
                }
                lastPoint.x = currentPoint.x;
                lastPoint.y = currentPoint.y;
            }
        }
        else
        {
            projection.toPixels(mapItemFactory.getGeoPointBase(currentCoords), pos);
            projection.toPixels(mapItemFactory.getGeoPointBase(destinationCoords), dest);

            canvas.drawLine(pos.x, pos.y, dest.x, dest.y, line);
        }

    }
}
