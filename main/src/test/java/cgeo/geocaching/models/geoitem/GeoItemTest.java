package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.functions.Action1;
import static cgeo.geocaching.models.geoitem.GeoItem.GeoType.CIRCLE;
import static cgeo.geocaching.models.geoitem.GeoItem.GeoType.POLYGON;
import static cgeo.geocaching.models.geoitem.GeoItem.GeoType.POLYLINE;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Parcel;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeoItemTest {

    private static final Geopoint GP_1 = new Geopoint(48, 11);

    private static final ToScreenProjector TO_LATLON_E6 = gp -> new int[]{gp.getLongitudeE6(), gp.getLatitudeE6()};

    /** A fake bitmap provider used for testing touching routines */
    public static class FakeBitmap implements GeoIcon.BitmapProvider {
        public final int width;
        public final int height;

        public FakeBitmap(final int width, final int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public Bitmap getBitmap() {
            return null;
        }

        @Override
        public Bitmap getRotatedBitmap(final float angleInDegree) {
            return null;
        }

        @Override
        public Pair<Integer, Integer> getRotatedBitmapDimensions(final float angleInDegree) {
            return new Pair<>(width, height);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull final Parcel parcel, final int i) {
            //empty on purpose
        }
    }

    @Test
    public void simpleTouchMarker() {
        final GeoPrimitive marker = marker(GP_1, 20, 30, null);
        assertThat(marker.touches(GP_1, TO_LATLON_E6)).isTrue();
    }

    @Test
    public void touchMarkerHotspots() {
        //CENTER
        assertThat(marker(GP_1, 20, 30, null).touches(addE6(GP_1, 15, 10), TO_LATLON_E6)).isTrue();
        assertThat(marker(GP_1, 20, 30, null).touches(addE6(GP_1, 16, 11), TO_LATLON_E6)).isFalse();

        //UPPER_RIGHT
        assertThat(marker(GP_1, 20, 30, gi -> gi.setHotspot(GeoIcon.Hotspot.UPPER_RIGHT_CORNER)).touches(addE6(GP_1, 0, 0), TO_LATLON_E6)).isTrue();
        assertThat(marker(GP_1, 20, 30, gi -> gi.setHotspot(GeoIcon.Hotspot.UPPER_RIGHT_CORNER)).touches(addE6(GP_1, -1, 1), TO_LATLON_E6)).isFalse();
    }

    @Test
    public void touchPolyline() {

       //directly on line
        assertThat(polyline(GP_1, 0, 0, 10).touches(GP_1, TO_LATLON_E6)).isTrue();
        assertThat(polyline(GP_1, 0, 0, 10).touches(addE6(GP_1, 0, 10), TO_LATLON_E6)).isTrue();

        //within "half linewidth" distance of line
        final int minLineWidth  = GeoItemUtils.getMinPixelTouchWidth();
        assertThat(polyline(GP_1, 0, 0, 10).touches(addE6(GP_1, minLineWidth / 2, 10), TO_LATLON_E6)).isTrue();
        assertThat(polyline(GP_1, 0, 0, 10).touches(addE6(GP_1, minLineWidth / 2 + 10, 10), TO_LATLON_E6)).isFalse();
    }

    @Test
    public void touchPolygon() {
        //directly on line
        assertThat(polygon(GP_1, 0, false, 0, 1000, 1000, 0).touches(GP_1, TO_LATLON_E6)).isTrue();
        assertThat(polygon(GP_1, 0, false, 0, 1000, 1000, 0).touches(addE6(GP_1, 0, 10), TO_LATLON_E6)).isTrue();

        //within "half linewidth" distance of line
        final int minLineWidth  = GeoItemUtils.getMinPixelTouchWidth();
        assertThat(polygon(GP_1, 0, false, 0, 1000, 1000, 0).touches(addE6(GP_1, minLineWidth / 2, 10), TO_LATLON_E6)).isTrue();
        assertThat(polygon(GP_1, 0, false, 0, 1000, 1000, 0).touches(addE6(GP_1, minLineWidth / 2 + 10, 10), TO_LATLON_E6)).isFalse();

        //inside
        assertThat(polygon(GP_1, 0, false, 0, 1000, 1000, 0).touches(addE6(GP_1, 750, 750), TO_LATLON_E6)).isFalse();
        assertThat(polygon(GP_1, 0, true, 0, 1000, 1000, 0).touches(addE6(GP_1, 750, 750), TO_LATLON_E6)).isTrue();
        assertThat(polygon(GP_1, 0, true, 0, 1000, 1000, 0).touches(addE6(GP_1, 1000, 0), TO_LATLON_E6)).isFalse();

    }

    @Test
    public void touchesCircle() {
        //directly on line
        assertThat(circle(GP_1, 10, 0, false).touches(GP_1.project(180, 10), TO_LATLON_E6)).isTrue();

        //within "half linewidth" distance of line
        final int minLineWidth  = GeoItemUtils.getMinPixelTouchWidth();
        assertThat(circle(GP_1, 10, 0, false).touches(addE6(GP_1.project(180, 10), minLineWidth / 2, 0), TO_LATLON_E6)).isTrue();
        assertThat(circle(GP_1, 10, 0, false).touches(addE6(GP_1.project(180, 10), minLineWidth / 2 + 10, 0), TO_LATLON_E6)).isFalse();

        //inside
        assertThat(circle(GP_1, 10, 0, false).touches(GP_1, TO_LATLON_E6)).isFalse();
        assertThat(circle(GP_1, 10, 0, true).touches(GP_1, TO_LATLON_E6)).isTrue();
        assertThat(circle(GP_1, 10, 0, true).touches(addE6(GP_1.project(180, 10), - minLineWidth / 2 - 10, 0), TO_LATLON_E6)).isFalse();

    }

    private static GeoPrimitive polyline(final Geopoint start, final int lineWidth, final int ... points) {
        return polylineGon(false, start, lineWidth, false, points);
    }

    private static GeoPrimitive polygon(final Geopoint start, final int lineWidth, final boolean filled, final int ... points) {
        return polylineGon(true, start, lineWidth, filled, points);
    }


    private static GeoPrimitive polylineGon(final boolean isPolygon, final Geopoint start, final int lineWidth, final boolean filled, final int ... points) {

        final GeoPrimitive.Builder gbb = GeoPrimitive.builder().setType(isPolygon ? POLYGON : POLYLINE)
                .setStyle(GeoStyle.builder().setStrokeWidth((float) lineWidth).setFillColor(filled ? Color.BLACK : Color.TRANSPARENT).build()).addPoints(start);
        Geopoint current = start;
        for (int i = 0; i < points.length; i += 2) {
            current = addE6(current, points[i], points[i + 1]);
            gbb.addPoints(current);
        }
        return gbb.build();
    }

    private static GeoPrimitive circle(final Geopoint gp, final float radius, final int lineWidth, final boolean filled) {
        return GeoPrimitive.builder().setType(CIRCLE).setRadius(radius).addPoints(gp)
                .setStyle(GeoStyle.builder().setStrokeWidth((float) lineWidth).setFillColor(filled ? Color.BLACK : Color.TRANSPARENT).build()).build();

    }

    private static GeoPrimitive marker(final Geopoint gp, final int width, final int height, final Action1<GeoIcon.Builder> addActions) {
        final GeoIcon.Builder gib = GeoIcon.builder().setBitmapProvider(new FakeBitmap(width, height));
        if (addActions != null) {
            addActions.call(gib);
        }
        return GeoPrimitive.createMarker(gp, gib.build());
    }

    private static Geopoint addE6(final Geopoint gp, final int addLatE6, final int addLonE6) {
        return Geopoint.forE6(gp.getLatitudeE6() + addLatE6, gp.getLongitudeE6() + addLonE6);
    }

}
