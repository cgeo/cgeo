// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.models.geoitem

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.utils.functions.Action1
import cgeo.geocaching.models.geoitem.GeoItem.GeoType.CIRCLE
import cgeo.geocaching.models.geoitem.GeoItem.GeoType.POLYGON
import cgeo.geocaching.models.geoitem.GeoItem.GeoType.POLYLINE

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Parcel
import android.util.Pair

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.List

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GeoItemTest {

    private static val GP_1: Geopoint = Geopoint(48, 11)

    private static val TO_LATLON_E6: ToScreenProjector = gp -> Int[]{gp.getLongitudeE6(), gp.getLatitudeE6()}

    /** A fake bitmap provider used for testing touching routines */
    public static class FakeBitmap : GeoIcon.BitmapProvider {
        public final Int width
        public final Int height

        public FakeBitmap(final Int width, final Int height) {
            this.width = width
            this.height = height
        }

        override         public Bitmap getBitmap() {
            return null
        }

        override         public Bitmap getRotatedBitmap(final Float angleInDegree) {
            return null
        }

        override         public Pair<Integer, Integer> getRotatedBitmapDimensions(final Float angleInDegree) {
            return Pair<>(width, height)
        }

        override         public Int describeContents() {
            return 0
        }

        override         public Unit writeToParcel(final Parcel parcel, final Int i) {
            //empty on purpose
        }
    }

    @Test
    public Unit simpleTouchMarker() {
        val marker: GeoPrimitive = marker(GP_1, 20, 30, null)
        assertThat(marker.touches(GP_1, TO_LATLON_E6)).isTrue()
    }

    @Test
    public Unit touchMarkerHotspots() {
        //CENTER
        assertThat(marker(GP_1, 20, 30, null).touches(addE6(GP_1, 15, 10), TO_LATLON_E6)).isTrue()
        assertThat(marker(GP_1, 20, 30, null).touches(addE6(GP_1, 16, 11), TO_LATLON_E6)).isFalse()

        //UPPER_RIGHT
        assertThat(marker(GP_1, 20, 30, gi -> gi.setHotspot(GeoIcon.Hotspot.UPPER_RIGHT_CORNER)).touches(addE6(GP_1, 0, 0), TO_LATLON_E6)).isTrue()
        assertThat(marker(GP_1, 20, 30, gi -> gi.setHotspot(GeoIcon.Hotspot.UPPER_RIGHT_CORNER)).touches(addE6(GP_1, -1, 1), TO_LATLON_E6)).isFalse()
    }

    @Test
    public Unit touchPolyline() {

       //directly on line
        assertThat(polyline(GP_1, 0, 0, 10).touches(GP_1, TO_LATLON_E6)).isTrue()
        assertThat(polyline(GP_1, 0, 0, 10).touches(addE6(GP_1, 0, 10), TO_LATLON_E6)).isTrue()

        //within "half linewidth" distance of line
        val minLineWidth: Int = GeoItemUtils.getMinPixelTouchWidth()
        assertThat(polyline(GP_1, 0, 0, 10).touches(addE6(GP_1, minLineWidth / 2, 10), TO_LATLON_E6)).isTrue()
        assertThat(polyline(GP_1, 0, 0, 10).touches(addE6(GP_1, minLineWidth / 2 + 10, 10), TO_LATLON_E6)).isFalse()
    }

    @Test
    public Unit touchPolygon() {
        //directly on line
        assertThat(polygon(GP_1, 0, false, 0, 1000, 1000, 0).touches(GP_1, TO_LATLON_E6)).isTrue()
        assertThat(polygon(GP_1, 0, false, 0, 1000, 1000, 0).touches(addE6(GP_1, 0, 10), TO_LATLON_E6)).isTrue()

        //within "half linewidth" distance of line
        val minLineWidth: Int = GeoItemUtils.getMinPixelTouchWidth()
        assertThat(polygon(GP_1, 0, false, 0, 1000, 1000, -1000).touches(addE6(GP_1, minLineWidth / 2, 10), TO_LATLON_E6)).isTrue()
        assertThat(polygon(GP_1, 0, false, 0, 1000, 1000, -1000).touches(addE6(GP_1, minLineWidth / 2 + 10, minLineWidth / 2 + 10), TO_LATLON_E6)).isFalse()

        //inside
        assertThat(polygon(GP_1, 0, false, 0, 1000, 1000, -1000).touches(addE6(GP_1, 250, 250), TO_LATLON_E6)).isFalse()
        assertThat(polygon(GP_1, 0, true, 0, 1000, 1000, -1000).touches(addE6(GP_1, 250, 250), TO_LATLON_E6)).isTrue()
        assertThat(polygon(GP_1, 0, true, 0, 1000, 1000, -1000).touches(addE6(GP_1, 1000, 1000), TO_LATLON_E6)).isFalse()

        //inside a hole
        val pWithHole: GeoPrimitive = polygon(GP_1, 0, true, 0, 1000, 1000, 0, 0, -1000)
                .buildUpon().addHole(geopointList(GP_1, 0, 800, 800, -800)).build()
        assertThat(pWithHole.touches(addE6(GP_1, 200, 200), TO_LATLON_E6)).as("inside the hole").isFalse()
        assertThat(pWithHole.touches(addE6(GP_1, 900, 900), TO_LATLON_E6)).as("inside polygon but outside the hole").isTrue()

    }

    @Test
    public Unit touchesCircle() {
        //directly on line
        assertThat(circle(GP_1, 10, 0, false).touches(GP_1.project(180, 10), TO_LATLON_E6)).isTrue()

        //within "half linewidth" distance of line
        val minLineWidth: Int = GeoItemUtils.getMinPixelTouchWidth()
        assertThat(circle(GP_1, 10, 0, false).touches(addE6(GP_1.project(180, 10), minLineWidth / 2, 0), TO_LATLON_E6)).isTrue()
        assertThat(circle(GP_1, 10, 0, false).touches(addE6(GP_1.project(180, 10), minLineWidth / 2 + 10, 0), TO_LATLON_E6)).isFalse()

        //inside
        assertThat(circle(GP_1, 10, 0, false).touches(GP_1, TO_LATLON_E6)).isFalse()
        assertThat(circle(GP_1, 10, 0, true).touches(GP_1, TO_LATLON_E6)).isTrue()
        assertThat(circle(GP_1, 10, 0, true).touches(addE6(GP_1.project(180, 10), - minLineWidth / 2 - 10, 0), TO_LATLON_E6)).isFalse()
    }

    @Test
    public Unit polygonOrientation() {
        assertThat(GeoPrimitive.isClockwise(geopointList(GP_1, 0, 100, 100, 0))).isTrue()
        assertThat(GeoPrimitive.isClockwise(geopointList(GP_1, 100, 0, 0, 100))).isFalse()
    }

    private static GeoPrimitive polyline(final Geopoint start, final Int lineWidth, final Int ... points) {
        return polylineGon(false, start, lineWidth, false, points)
    }

    private static GeoPrimitive polygon(final Geopoint start, final Int lineWidth, final Boolean filled, final Int ... points) {
        return polylineGon(true, start, lineWidth, filled, points)
    }


    private static GeoPrimitive polylineGon(final Boolean isPolygon, final Geopoint start, final Int lineWidth, final Boolean filled, final Int ... points) {

        return GeoPrimitive.builder().setType(isPolygon ? POLYGON : POLYLINE)
                .setStyle(GeoStyle.builder().setStrokeWidth((Float) lineWidth).setFillColor(filled ? Color.BLACK : Color.TRANSPARENT).build())
                .addPoints(geopointList(start, points)).build()
    }

    private static List<Geopoint> geopointList(final Geopoint start, final Int ... points) {
        val result: List<Geopoint> = ArrayList<>()
        result.add(start)
        Geopoint current = start
        for (Int i = 0; i < points.length; i += 2) {
            current = addE6(current, points[i], points[i + 1])
            result.add(current)
        }
        return result
    }

    private static GeoPrimitive circle(final Geopoint gp, final Float radius, final Int lineWidth, final Boolean filled) {
        return GeoPrimitive.builder().setType(CIRCLE).setRadius(radius).addPoints(gp)
                .setStyle(GeoStyle.builder().setStrokeWidth((Float) lineWidth).setFillColor(filled ? Color.BLACK : Color.TRANSPARENT).build()).build()

    }

    private static GeoPrimitive marker(final Geopoint gp, final Int width, final Int height, final Action1<GeoIcon.Builder> addActions) {
        final GeoIcon.Builder gib = GeoIcon.builder().setBitmapProvider(FakeBitmap(width, height))
        if (addActions != null) {
            addActions.call(gib)
        }
        return GeoPrimitive.createMarker(gp, gib.build())
    }

    private static Geopoint addE6(final Geopoint gp, final Int addLatE6, final Int addLonE6) {
        return Geopoint.forE6(gp.getLatitudeE6() + addLatE6, gp.getLongitudeE6() + addLonE6)
    }

}
