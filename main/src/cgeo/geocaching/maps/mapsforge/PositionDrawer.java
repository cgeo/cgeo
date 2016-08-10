package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.settings.Settings;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.location.Location;

import java.util.ArrayList;

public class PositionDrawer {

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    private static final float LINE_MAXIMUM_DISTANCE_METERS = 10000;

    private Location coordinates = null;
    private GeoPointImpl location = null;
    private float heading = 0f;
    private Paint accuracyCircle = null;
    private Paint historyLine = null;
    private Paint historyLineShadow = null;
    private final Point center = new Point();
    private final Point left = new Point();
    private Bitmap arrow = null;
    private int widthArrowHalf = 0;
    private int heightArrowHalf = 0;
    private PaintFlagsDrawFilter setfil = null;
    private PaintFlagsDrawFilter remfil = null;
    private final PositionHistory positionHistory = new PositionHistory();
    private final MapItemFactory mapItemFactory;

    public PositionDrawer() {
        this.mapItemFactory = Settings.getMapProvider().getMapItemFactory();
    }

    void drawPosition(final Canvas canvas, final MapProjectionImpl projection) {
        if (coordinates == null || location == null) {
            return;
        }

        if (accuracyCircle == null) {
            accuracyCircle = new Paint();
            accuracyCircle.setAntiAlias(true);
            accuracyCircle.setStrokeWidth(1.0f);
        }

        if (historyLine == null) {
            historyLine = new Paint();
            historyLine.setAntiAlias(true);
            historyLine.setStrokeWidth(3.0f);
            historyLine.setColor(0xFFFFFFFF);
        }

        if (historyLineShadow == null) {
            historyLineShadow = new Paint();
            historyLineShadow.setAntiAlias(true);
            historyLineShadow.setStrokeWidth(7.0f);
            historyLineShadow.setColor(0x66000000);
        }

        if (setfil == null) {
            setfil = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
        }
        if (remfil == null) {
            remfil = new PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0);
        }

        canvas.setDrawFilter(setfil);

        final double latitude = coordinates.getLatitude();
        final double longitude = coordinates.getLongitude();
        final float accuracy = coordinates.getAccuracy();

        final float[] result = new float[1];

        Location.distanceBetween(latitude, longitude, latitude, longitude + 1, result);
        final float longitudeLineDistance = result[0];

        final Geopoint leftCoords = new Geopoint(latitude, longitude - accuracy / longitudeLineDistance);
        final GeoPointImpl leftGeo = mapItemFactory.getGeoPointBase(leftCoords);
        projection.toPixels(leftGeo, left);
        projection.toPixels(location, center);
        final int radius = center.x - left.x;

        accuracyCircle.setColor(0x66000000);
        accuracyCircle.setStyle(Style.STROKE);
        canvas.drawCircle(center.x, center.y, radius, accuracyCircle);

        accuracyCircle.setColor(0x08000000);
        accuracyCircle.setStyle(Style.FILL);
        canvas.drawCircle(center.x, center.y, radius, accuracyCircle);

        positionHistory.rememberTrailPosition(coordinates);

        if (Settings.isMapTrail()) {
            // always add current position to drawn history to have a closed connection
            final ArrayList<Location> paintHistory = new ArrayList<>(positionHistory.getHistory());
            paintHistory.add(coordinates);

            final int size = paintHistory.size();
            if (size > 1) {
                int alphaCnt = size - 201;
                if (alphaCnt < 1) {
                    alphaCnt = 1;
                }

                final Point pointNow = new Point();
                final Point pointPrevious = new Point();
                final Location prev = paintHistory.get(0);
                projection.toPixels(mapItemFactory.getGeoPointBase(new Geopoint(prev)), pointPrevious);

                for (int cnt = 1; cnt < size; cnt++) {
                    final Location now = paintHistory.get(cnt);
                    projection.toPixels(mapItemFactory.getGeoPointBase(new Geopoint(now)), pointNow);

                    final int alpha;
                    if ((alphaCnt - cnt) > 0) {
                        alpha = 255 / (alphaCnt - cnt);
                    } else {
                        alpha = 255;
                    }

                    historyLineShadow.setAlpha(alpha);
                    historyLine.setAlpha(alpha);

                    // connect points by line, but only if distance between previous and current point is less than defined max
                    if (now.distanceTo(prev) < LINE_MAXIMUM_DISTANCE_METERS) {
                        canvas.drawLine(pointPrevious.x, pointPrevious.y, pointNow.x, pointNow.y, historyLineShadow);
                        canvas.drawLine(pointPrevious.x, pointPrevious.y, pointNow.x, pointNow.y, historyLine);
                    }

                    pointPrevious.set(pointNow.x, pointNow.y);
                }
            }
        }

        if (arrow == null) {
            arrow = BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron);
            widthArrowHalf = arrow.getWidth() / 2;
            heightArrowHalf = arrow.getHeight() / 2;
        }

        final int marginLeft = center.x - widthArrowHalf;
        final int marginTop = center.y - heightArrowHalf;

        final Matrix matrix = new Matrix();
        matrix.setRotate(heading, widthArrowHalf, heightArrowHalf);
        matrix.postTranslate(marginLeft, marginTop);

        canvas.drawBitmap(arrow, matrix, null);

        canvas.setDrawFilter(remfil);
    }

    public ArrayList<Location> getHistory() {
        return positionHistory.getHistory();
    }

    public void setHistory(final ArrayList<Location> history) {
        positionHistory.setHistory(history);
    }

    public void setHeading(final float bearingNow) {
        heading = bearingNow;
    }

    public float getHeading() {
        return heading;
    }

    public void setCoordinates(final Location coordinatesIn) {
        coordinates = coordinatesIn;
        location = mapItemFactory.getGeoPointBase(new Geopoint(coordinates));
    }

    public Location getCoordinates() {
        return coordinates;
    }

}
