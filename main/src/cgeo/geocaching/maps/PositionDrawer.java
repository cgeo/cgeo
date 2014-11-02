package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
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

    private Location coordinates = null;
    private GeoPointImpl location = null;
    private float heading = 0f;
    private Paint accuracyCircle = null;
    private Paint historyLine = null;
    private Paint historyLineShadow = null;
    private Point center = new Point();
    private Point left = new Point();
    private Bitmap arrow = null;
    private int widthArrowHalf = 0;
    private int heightArrowHalf = 0;
    private PaintFlagsDrawFilter setfil = null;
    private PaintFlagsDrawFilter remfil = null;
    private PositionHistory positionHistory = new PositionHistory();
    private MapItemFactory mapItemFactory;

    public PositionDrawer() {
        this.mapItemFactory = Settings.getMapProvider().getMapItemFactory();
    }

    void drawPosition(Canvas canvas, MapProjectionImpl projection) {
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

        double latitude = coordinates.getLatitude();
        double longitude = coordinates.getLongitude();
        float accuracy = coordinates.getAccuracy();

        float[] result = new float[1];

        Location.distanceBetween(latitude, longitude, latitude, longitude + 1, result);
        float longitudeLineDistance = result[0];

        final Geopoint leftCoords = new Geopoint(latitude, longitude - accuracy / longitudeLineDistance);
        GeoPointImpl leftGeo = mapItemFactory.getGeoPointBase(leftCoords);
        projection.toPixels(leftGeo, left);
        projection.toPixels(location, center);
        int radius = center.x - left.x;

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

            int size = paintHistory.size();
            if (size > 1) {
                int alphaCnt = size - 201;
                if (alphaCnt < 1) {
                    alphaCnt = 1;
                }

                Point pointNow = new Point();
                Point pointPrevious = new Point();
                Location prev = paintHistory.get(0);
                projection.toPixels(mapItemFactory.getGeoPointBase(new Geopoint(prev)), pointPrevious);

                for (int cnt = 1; cnt < size; cnt++) {
                    Location now = paintHistory.get(cnt);
                    projection.toPixels(mapItemFactory.getGeoPointBase(new Geopoint(now)), pointNow);

                    int alpha;
                    if ((alphaCnt - cnt) > 0) {
                        alpha = 255 / (alphaCnt - cnt);
                    }
                    else {
                        alpha = 255;
                    }

                    historyLineShadow.setAlpha(alpha);
                    historyLine.setAlpha(alpha);

                    canvas.drawLine(pointPrevious.x, pointPrevious.y, pointNow.x, pointNow.y, historyLineShadow);
                    canvas.drawLine(pointPrevious.x, pointPrevious.y, pointNow.x, pointNow.y, historyLine);

                    pointPrevious.set(pointNow.x, pointNow.y);
                }
            }
        }

        if (arrow == null) {
            arrow = BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron);
            widthArrowHalf = arrow.getWidth() / 2;
            heightArrowHalf = arrow.getHeight() / 2;
        }

        int marginLeft = center.x - widthArrowHalf;
        int marginTop = center.y - heightArrowHalf;

        Matrix matrix = new Matrix();
        matrix.setRotate(heading, widthArrowHalf, heightArrowHalf);
        matrix.postTranslate(marginLeft, marginTop);

        canvas.drawBitmap(arrow, matrix, null);

        canvas.setDrawFilter(remfil);
    }

    public ArrayList<Location> getHistory() {
        return positionHistory.getHistory();
    }

    public void setHistory(ArrayList<Location> history) {
        positionHistory.setHistory(history);
    }

    public void setHeading(float bearingNow) {
        heading = bearingNow;
    }

    public float getHeading() {
        return heading;
    }

    public void setCoordinates(Location coordinatesIn) {
        coordinates = coordinatesIn;
        location = mapItemFactory.getGeoPointBase(new Geopoint(coordinates));
    }

    public Location getCoordinates() {
        return coordinates;
    }

}
