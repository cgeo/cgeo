package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.utils.ImageUtils;
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
import org.mapsforge.core.util.MercatorProjection;
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
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {

        if (coordinates == null) {
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
            circle.setDisplayModel(this.getDisplayModel());
            circle.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
        }

        // prepare heading indicator

        final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        final double pixelX = MercatorProjection.longitudeToPixelX(this.location.longitude, mapSize);
        final double pixelY = MercatorProjection.latitudeToPixelY(this.location.latitude, mapSize);
        final int centerX = (int) (pixelX - topLeftPoint.x);
        final int centerY = (int) (pixelY - topLeftPoint.y);

        if (arrow == null) {
            arrowNative = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron, null));
            rotateArrow();
        }

        final int left = centerX - widthArrowHalf;
        final int top = centerY - heightArrowHalf;
        final int right = left + this.arrow.getWidth();
        final int bottom = top + this.arrow.getHeight();
        final Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
        final Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return;
        }
        canvas.drawBitmap(arrow, left, top);
    }

    private void rotateArrow() {
        if (arrowNative == null) {
            return;
        }

        final Matrix matrix = new Matrix();
        matrix.setRotate(heading, widthArrowHalf, heightArrowHalf);
        final android.graphics.Bitmap arrowRotNative = android.graphics.Bitmap.createBitmap(arrowNative, 0, 0, arrowNative.getWidth(), arrowNative.getHeight(), matrix, true);

        final Drawable tmpArrow = new BitmapDrawable(CgeoApplication.getInstance().getResources(), arrowRotNative);
        arrow = AndroidGraphicFactory.convertToBitmap(tmpArrow);

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
