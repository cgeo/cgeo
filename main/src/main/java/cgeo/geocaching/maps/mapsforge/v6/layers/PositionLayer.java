package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.GeoHeightUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapLineUtils;

import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;

import androidx.core.content.res.ResourcesCompat;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Circle;

public class PositionLayer extends Layer {

    private Location coordinates = null;
    private LatLong location = null;
    private float heading = 0f;
    private android.graphics.Bitmap arrowNative = null;
    private Bitmap arrow = null;
    private Paint accuracyCircle = null;
    private Paint accuracyCircleFill = null;
    private int widthArrowHalf = 0;
    private int heightArrowHalf = 0;

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {

        if (coordinates == null || location == null) {
            return;
        }

        // prepare accuracy circle

        final float accuracy = coordinates.getAccuracy();
        if (accuracyCircle == null) {
            accuracyCircle = AndroidGraphicFactory.INSTANCE.createPaint();
            accuracyCircle.setStyle(Style.STROKE);
            accuracyCircle.setStrokeWidth(1.0f);
            accuracyCircle.setColor(MapLineUtils.getAccuracyCircleColor());

            accuracyCircleFill = AndroidGraphicFactory.INSTANCE.createPaint();
            accuracyCircleFill.setStyle(Style.FILL);
            accuracyCircleFill.setColor(MapLineUtils.getAccuracyCircleFillColor());
        }

        if (accuracy >= 0) {
            final Circle circle = new Circle(location, accuracy, accuracyCircleFill, accuracyCircle);
            circle.setDisplayModel(getDisplayModel());
            circle.draw(boundingBox, zoomLevel, canvas, topLeftPoint, rotation);
        }

        // prepare heading indicator

        final long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
        final double pixelX = MercatorProjection.longitudeToPixelX(location.longitude, mapSize);
        final double pixelY = MercatorProjection.latitudeToPixelY(location.latitude, mapSize);
        final int centerX = (int) (pixelX - topLeftPoint.x);
        final int centerY = (int) (pixelY - topLeftPoint.y);

        if (arrow == null) {
            // temporarily call local copy of convertToBitmap instead of ImageUtils.convertToBitmap
            // trying to catch the cause for #14295
            arrowNative = convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron, null));
            rotateArrow();
        }
        final Bitmap localArrow = arrow;
        if (localArrow != null && !localArrow.isDestroyed()) {
            final int left = centerX - widthArrowHalf;
            final int top = centerY - heightArrowHalf;
            final int right = left + localArrow.getWidth();
            final int bottom = top + localArrow.getHeight();
            final Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
            final Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
            if (!canvasRectangle.intersects(bitmapRectangle)) {
                return;
            }
            canvas.drawBitmap(localArrow, left, top);

            if (coordinates.hasAltitude() && Settings.showElevation()) {
                final Bitmap elevationInfo = new AndroidBitmap(MapUtils.getElevationBitmap(CgeoApplication.getInstance().getResources(), localArrow.getHeight(), GeoHeightUtils.getAltitude(coordinates)));
                canvas.drawBitmap(elevationInfo, centerX - elevationInfo.getWidth() / 2, centerY - elevationInfo.getHeight() / 2);
            }
        } else {
            Log.e("PositionLayer.draw: localArrow=null or destroyed, arrowNative=" + arrowNative);
        }
    }

    // temporary copy of ImageUtils.convertToBitmap for documentation purposes
    // trying to catch the cause for #14295
    private static android.graphics.Bitmap convertToBitmap(final Drawable drawable) {
        if (drawable == null) {
            Log.e("PositionLayer.convertToBitmap: got null drawable");
        }
        if (drawable instanceof BitmapDrawable) {
            if (((BitmapDrawable) drawable).getBitmap() == null) {
                Log.e("PositionLayer.convertToBitmap: drawable.getBitmap() returned null");
            }
            return ((BitmapDrawable) drawable).getBitmap();
        }

        // handle solid colors, which have no width
        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        final android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
        if (bitmap == null) {
            Log.e("PositionLayer.convertToBitmap: createBitmap returned null");
        }
        final android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private void rotateArrow() {
        if (arrowNative == null || arrowNative.getWidth() == 0 || arrowNative.getHeight() == 0) {
            return;
        }

        final Matrix matrix = new Matrix();
        matrix.setRotate(heading, widthArrowHalf, heightArrowHalf);
        final android.graphics.Bitmap arrowRotNative = android.graphics.Bitmap.createBitmap(arrowNative, 0, 0, arrowNative.getWidth(), arrowNative.getHeight(), matrix, true);
        if (arrowRotNative == null) {
            Log.e("PositionLayer.rotateArrow: arrowRotNative is null");
        }

        final Drawable tmpArrow = new BitmapDrawable(CgeoApplication.getInstance().getResources(), arrowRotNative);
        arrow = AndroidGraphicFactory.convertToBitmap(tmpArrow);
        if (arrow.isDestroyed()) {
            Log.e("PositionLayer.rotateArrow: arrow.isDestroyed is true");
        }

        widthArrowHalf = arrow.getWidth() / 2;
        heightArrowHalf = arrow.getHeight() / 2;
    }

    public void setHeading(final float bearingNow) {
        if (heading != bearingNow) {
            heading = bearingNow;
            rotateArrow();
        }
    }

    public float getHeading() {
        return heading;
    }

    public void setCoordinates(final Location coordinatesIn) {
        coordinates = coordinatesIn;
        location = new LatLong(coordinatesIn.getLatitude(), coordinatesIn.getLongitude());
    }

    public Location getCoordinates() {
        return coordinates;
    }

}
