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

package cgeo.geocaching.unifiedmap.geoitemlayer

import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.geoitem.GeoGroup
import cgeo.geocaching.models.geoitem.GeoIcon
import cgeo.geocaching.models.geoitem.GeoItem
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.EmojiUtils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color

import androidx.annotation.DrawableRes

import java.util.ArrayList
import java.util.Arrays
import java.util.List

import io.reactivex.rxjava3.disposables.Disposable

/** This is a test layer to experiment with unified geoitem layers. It is only activated with a specific developer setting */
class GeoItemTestLayer {

    public static val TESTLAYER_KEY_PREFIX: String = "TESTLAYER_"

    private static val CENTER: Geopoint = Geopoint(40, 10)

    private var testLayer: GeoItemLayer<String> = GeoItemLayer<>("test")
    private Disposable stopper

    private static val ICON_SMILEY: GeoIcon = GeoIcon.builder().setBitmap(ImageParam.emoji(EmojiUtils.SMILEY_LIKE).getAsBitmap()).build()
    private static val ICON_SPARKLES: GeoIcon = GeoIcon.builder().setBitmap(ImageParam.emoji(EmojiUtils.SPARKLES).getAsBitmap()).build()

    private static val ICON_CAMERA: GeoIcon = GeoIcon.builder().setBitmap(ImageParam.id(R.drawable.ic_menu_camera).getAsBitmap()).build()
    private static val ICON_TYPE_GIGA: GeoIcon = GeoIcon.builder().setBitmap(ImageParam.id(R.drawable.type_giga).getAsBitmap()).build()
    private static val ICON_CGEO_LOGO: GeoIcon = GeoIcon.builder().setBitmap(ImageParam.id(R.drawable.ic_launcher_rounded_noborder).getAsBitmap()).build()

    private static final GeoIcon[] ICON_ALL = GeoIcon[] { ICON_SMILEY, ICON_SPARKLES, ICON_CAMERA, ICON_TYPE_GIGA, ICON_CGEO_LOGO }

    public Unit initforUnifiedMap(final GeoItemLayer<String> layer) {
        if (!Settings.enableFeatureUnifiedGeoItemLayer()) {
            return
        }
        destroy()
        testLayer = layer
        this.stopper = startData()
    }

    public Unit init(final IProviderGeoItemLayer<?> provider) {
        if (!Settings.enableFeatureUnifiedGeoItemLayer()) {
            return
        }
        destroy()
        testLayer = GeoItemLayer<>("test")
        testLayer.setProvider(provider, 0)
        this.stopper = startData()
    }

    public Unit destroy() {
        if (!Settings.enableFeatureUnifiedGeoItemLayer()) {
            return
        }
        if (stopper != null) {
            stopper.dispose()
            stopper = null
        }
        if (testLayer != null) {
            testLayer.destroy()
            testLayer = null
        }
    }

    public static Unit handleTapTest(final GeoItemLayer<String> testLayer, final Context ctx, final Geopoint tapped, final String key, final Boolean isLongTap) {
        if (!Settings.enableFeatureUnifiedGeoItemLayer() || testLayer == null) {
            return
        }
        SimpleDialog.ofContext(ctx)
            .setTitle(TextParam.text("Touched Test item"))
            .setMessage(TextParam.text("**" + key + "**\n\n*Point: " + tapped + "*\n\n" + testLayer.get(key)).setMarkdown(true))
            .show()
    }

    private Disposable startData() {

        //static test data
        createStaticElements(CENTER.project(90, 50), testLayer)

        final Int[] indexStore = { 0 }
        val indexLength: Int = 360
        return AndroidRxUtils.runPeriodically(AndroidRxUtils.computationScheduler, () -> {
            val index: Int = indexStore[0]
            indexStore[0] = (indexStore[0] + 1) % indexLength

            createDynamicElements(CENTER, testLayer, index)

            }, 0, 1000)
    }

    private static Unit createStaticElements(final Geopoint center, final GeoItemLayer<String> layer) {

        for (Int x = 0; x <= 2; x++) {
            for (Int y = 0; y <= 2; y++) {
                val point: Geopoint = center.project(90, x * 20).project(180, y * 20)
                val gi: GeoItem = testIconWithPoly(point, Color.GREEN, x * 0.5f, y * 0.5f, 0)
                layer.put(TESTLAYER_KEY_PREFIX + "MarkerOffset-" + x + "-" + y, gi)
            }
        }

        layer.put(TESTLAYER_KEY_PREFIX + "MarkerOffsetRightTop", testIconWithPoly(center.project(90, 100), Color.GRAY, GeoIcon.Hotspot.UPPER_RIGHT_CORNER.xAnchor, GeoIcon.Hotspot.UPPER_RIGHT_CORNER.yAnchor, 0))

        val circleCenter: Geopoint = center.project(90, 200)
        for (Int a = 0; a < 360; a += 40) {
            val point: Geopoint = circleCenter.project(a, 40)
            val gi: GeoItem = testIconWithPoly(point, Color.RED, GeoIcon.Hotspot.CENTER.xAnchor, GeoIcon.Hotspot.CENTER.yAnchor, a)
            layer.put(TESTLAYER_KEY_PREFIX + "MarkerAngle-" + a, gi)
        }

        val staticPolylineCenter: Geopoint = center.project(180, 50)
        layer.put(TESTLAYER_KEY_PREFIX + "staticPolyline", polygonAround(staticPolylineCenter, 50, Color.RED))

        val staticCircle: Geopoint = staticPolylineCenter.project(90, 50)
        layer.put(TESTLAYER_KEY_PREFIX + "staticCircle", GeoPrimitive.createCircle(staticCircle, 40, GeoStyle.builder().setStrokeColor(Color.DKGRAY).setFillColor(Color.YELLOW).build()))

        val staticPolygon: Geopoint = staticCircle.project(90, 50)
        layer.put(TESTLAYER_KEY_PREFIX + "staticPolygonWithText", GeoPrimitive.createPolygon(geoGridPoints(
                staticPolygon, 10, 0, 0, 3, 0, 3, 1, 2, 1, 2, 2, 3, 2, 3, 3, 1, 3, 0, 2
        ), GeoStyle.builder().setStrokeColor(Color.YELLOW).setStrokeWidth(5f).setFillColor(Color.GREEN).build())
                .buildUpon().setIcon(GeoIcon.builder().setText("This is my polygon").build()).build())

        val staticPolWithHole: Geopoint = staticPolygon.project(90, 50)
        val pol: GeoPrimitive = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYGON)
                .setStyle(GeoStyle.builder().setStrokeColor(Color.YELLOW).setStrokeWidth(5f).setFillColor(Color.GREEN).build())
                .addPoints(geoGridPoints(staticPolWithHole, 10, 0, 0, 7, 0, 7, 7, 0, 7))
                .addHole(geoGridPoints(staticPolWithHole, 10, 1, 1, 1, 2, 2, 2, 2, 1))
                .addHole(geoGridPoints(staticPolWithHole, 10, 4, 4, 4, 5, 5, 5, 5, 4))
                .build()
        layer.put(TESTLAYER_KEY_PREFIX + "staticPolygonWithTwoHoles", pol)

        val zLevelStuff: Geopoint = staticPolygon.project(180, 50)
        val zGreen: GeoPrimitive = GeoPrimitive.createPolygon(geoGridPoints(zLevelStuff, 10, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0), GeoStyle.builder().setStrokeColor(Color.BLUE).setStrokeWidth(5f).setFillColor(Color.GREEN).build())
        val zYellow: GeoPrimitive = GeoPrimitive.createPolygon(geoGridPoints(zLevelStuff.project(110, 8), 10, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0), GeoStyle.builder().setStrokeColor(Color.BLUE).setStrokeWidth(5f).setFillColor(Color.YELLOW).build())
        val zRed: GeoPrimitive = GeoPrimitive.createPolygon(geoGridPoints(zLevelStuff.project(140, 6), 10, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0), GeoStyle.builder().setStrokeColor(Color.BLUE).setStrokeWidth(5f).setFillColor(Color.RED).build())

        //order visible should be yellow, red, green
        layer.put(TESTLAYER_KEY_PREFIX + "zGreen-z0(most background)(placed-0)", zGreen.buildUpon().setZLevel(0).build())
        layer.put(TESTLAYER_KEY_PREFIX + "zYellow-z2(most foreground)(placed-1)", zYellow.buildUpon().setZLevel(2).build())
        layer.put(TESTLAYER_KEY_PREFIX + "zRed-z1(placed-2)", zRed.buildUpon().setZLevel(1).build())

        //line thicknesses
        val lineThickness: Geopoint = zLevelStuff.project(180, 50)
        for (Int i = 0; i < 10 ; i++) {
            val widthInDp: Int = (i + 1) * 5
            val start: Geopoint = lineThickness.project(90, i * 10)
            val end: Geopoint = start.project(180, 10)
            val style: GeoStyle = GeoStyle.builder().setStrokeColor(Color.BLUE).setStrokeWidth((Float) widthInDp).build()
            layer.put(TESTLAYER_KEY_PREFIX + "lineThickness-" + widthInDp,
                GeoPrimitive.createPolyline(Arrays.asList(start, end), style).buildUpon().setIcon(GeoIcon.builder().setText(widthInDp + "dp").build()).build())
        }

        //button width: 38dp
        layer.put(TESTLAYER_KEY_PREFIX + "lineThickness-oneButton",
            GeoPrimitive.createPolyline(Arrays.asList(lineThickness.project(180, 20), lineThickness.project(180, 20).project(90, 50)),
                GeoStyle.builder().setStrokeColor(Color.GREEN).setStrokeWidth(38f).build()))
        layer.put(TESTLAYER_KEY_PREFIX + "lineThickness-twoButton",
            GeoPrimitive.createPolyline(Arrays.asList(lineThickness.project(180, 25), lineThickness.project(180, 25).project(90, 50)),
                GeoStyle.builder().setStrokeColor(Color.RED).setStrokeWidth(38f * 2).build()))




    }

    private static GeoGroup testIconWithPoly(final Geopoint point, final Int polyColor, final Float xAnchor, final Float yAnchor, final Float angle) {


        val poly: GeoItem = polygonAround(point, 20, polyColor)
        val marker: GeoItem = GeoPrimitive.createMarker(point,
                GeoIcon.builder()
                        .setXAnchor(xAnchor)
                        .setYAnchor(yAnchor)
                        .setRotation(angle)
                        .setBitmap(createBitmap(R.drawable.ic_launcher_rounded_noborder)).build())

        return GeoGroup.builder().addItems(poly, marker).build()

    }

    private static GeoPrimitive polygonAround(final Geopoint center, final Float distance, final Int color) {
        val start: Geopoint = center.project(0, distance / 2).project(270, distance / 2)
        return GeoPrimitive.builder().setType(GeoItem.GeoType.POLYLINE).addPoints(
                start,
                start.project(90, distance),
                start.project(90, distance).project(180, distance),
                start.project(180, distance),
                start).setStyle(GeoStyle.builder().setStrokeColor(color).build()).build()
    }

    private static List<Geopoint> geoGridPoints(final Geopoint start, final Float dist, final Int ... coords) {
        val result: List<Geopoint> = ArrayList<>()
        for (Int i = 0; i < coords.length; i += 2) {
            val x: Int = coords[i]
            val y: Int = coords[i + 1]
            result.add(start.project(90, x * dist).project(180, y * dist))
        }
        return result
    }


    private static Unit createDynamicElements(final Geopoint center, final GeoItemLayer<String> layer, final Int index) {
        //index runs from 0 to 359, then back to 0

        val angle: Int = (index * 20) % 360

        val distance: Double = 20; //km

        //dynamic test data
        val poly: GeoItem = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYLINE).addPoints(
                center.project(angle, distance),
                center.project((angle + 90) % 360, distance),
                center.project((angle + 180) % 360, distance),
                center.project((angle + 270) % 360, distance),
                center.project(angle, distance)).build()
        val point: GeoItem = GeoPrimitive.createPoint(center.project(angle, distance), GeoStyle.builder().setStrokeColor(Color.GREEN).build())
        val group: GeoItem = GeoGroup.builder().addItems(point, poly).build()

        layer.put(TESTLAYER_KEY_PREFIX + "Quad", group)

        val flowMarker: GeoItem = GeoPrimitive.createMarker(center.project(angle, distance * 0.8), GeoIcon.builder()
                .setBitmap(createBitmap(R.drawable.type_event))
                .setRotation(angle).build())
        layer.put(TESTLAYER_KEY_PREFIX + "FlowMarker", flowMarker)

        //a series of markers with changing icons
        val startDynamicMarkers: Geopoint = center.project(180, 80)
        for (Int i = 0; i < 20; i++) {
            val iconI: GeoPrimitive = GeoPrimitive.createMarker(startDynamicMarkers.project(90, 4 * i), ICON_ALL[(index + i) % ICON_ALL.length])
            layer.put(TESTLAYER_KEY_PREFIX + "dynamicIconMarker-" + i, iconI)
        }



    }

    private static Bitmap createBitmap(@DrawableRes final Int drawableId) {
        return ImageParam.id(drawableId).getAsBitmap()
    }

}
