package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.EmojiUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.DrawableRes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.disposables.Disposable;

/** This is a test layer to experiment with unified geoitem layers. It is only activated with a specific developer setting */
public class GeoItemTestLayer {

    public static final String TESTLAYER_KEY_PREFIX = "TESTLAYER_";

    private static final Geopoint CENTER = new Geopoint(40, 10);

    private GeoItemLayer<String> testLayer = new GeoItemLayer<>("test");
    private Disposable stopper;

    private static final GeoIcon ICON_SMILEY = GeoIcon.builder().setBitmap(ImageParam.emoji(EmojiUtils.SMILEY_LIKE).getAsBitmap()).build();
    private static final GeoIcon ICON_SPARKLES = GeoIcon.builder().setBitmap(ImageParam.emoji(EmojiUtils.SPARKLES).getAsBitmap()).build();

    private static final GeoIcon ICON_CAMERA = GeoIcon.builder().setBitmap(ImageParam.id(R.drawable.ic_menu_camera).getAsBitmap()).build();
    private static final GeoIcon ICON_TYPE_GIGA = GeoIcon.builder().setBitmap(ImageParam.id(R.drawable.type_giga).getAsBitmap()).build();
    private static final GeoIcon ICON_CGEO_LOGO = GeoIcon.builder().setBitmap(ImageParam.id(R.drawable.cgeo_borderless).getAsBitmap()).build();

    private static final GeoIcon[] ICON_ALL = new GeoIcon[] { ICON_SMILEY, ICON_SPARKLES, ICON_CAMERA, ICON_TYPE_GIGA, ICON_CGEO_LOGO };

    public void initforUnifiedMap(final GeoItemLayer<String> layer) {
        if (!Settings.enableFeatureUnifiedGeoItemLayer()) {
            return;
        }
        destroy();
        testLayer = layer;
        this.stopper = startData();
    }

    public void init(final IProviderGeoItemLayer<?> provider) {
        if (!Settings.enableFeatureUnifiedGeoItemLayer()) {
            return;
        }
        destroy();
        testLayer = new GeoItemLayer<>("test");
        testLayer.setProvider(provider, 0);
        this.stopper = startData();
    }

    public void destroy() {
        if (!Settings.enableFeatureUnifiedGeoItemLayer()) {
            return;
        }
        if (stopper != null) {
            stopper.dispose();
            stopper = null;
        }
        if (testLayer != null) {
            testLayer.destroy();
            testLayer = null;
        }
    }

    public static void handleTapTest(final GeoItemLayer<String> testLayer, final Context ctx, final Geopoint tapped, final String key, final boolean isLongTap) {
        if (!Settings.enableFeatureUnifiedGeoItemLayer() || testLayer == null) {
            return;
        }
        SimpleDialog.ofContext(ctx)
            .setTitle(TextParam.text("Touched Test item"))
            .setMessage(TextParam.text("**" + key + "**\n\n*Point: " + tapped + "*\n\n" + testLayer.get(key)).setMarkdown(true))
            .show();
    }

    private Disposable startData() {

        //static test data
        createStaticElements(CENTER.project(90, 50), testLayer);

        final int[] indexStore = { 0 };
        final int indexLength = 360;
        return AndroidRxUtils.runPeriodically(AndroidRxUtils.computationScheduler, () -> {
            final int index = indexStore[0];
            indexStore[0] = (indexStore[0] + 1) % indexLength;

            createDynamicElements(CENTER, testLayer, index);

            }, 0, 1000);
    }

    private static void createStaticElements(final Geopoint center, final GeoItemLayer<String> layer) {

        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                final Geopoint point = center.project(90, x * 20).project(180, y * 20);
                final GeoItem gi = testIconWithPoly(point, Color.GREEN, x * 0.5f, y * 0.5f, 0);
                layer.put(TESTLAYER_KEY_PREFIX + "MarkerOffset-" + x + "-" + y, gi);
            }
        }

        layer.put(TESTLAYER_KEY_PREFIX + "MarkerOffsetRightTop", testIconWithPoly(center.project(90, 100), Color.GRAY, GeoIcon.Hotspot.UPPER_RIGHT_CORNER.xAnchor, GeoIcon.Hotspot.UPPER_RIGHT_CORNER.yAnchor, 0));

        final Geopoint circleCenter = center.project(90, 200);
        for (int a = 0; a < 360; a += 40) {
            final Geopoint point = circleCenter.project(a, 40);
            final GeoItem gi = testIconWithPoly(point, Color.RED, GeoIcon.Hotspot.CENTER.xAnchor, GeoIcon.Hotspot.CENTER.yAnchor, a);
            layer.put(TESTLAYER_KEY_PREFIX + "MarkerAngle-" + a, gi);
        }

        final Geopoint staticPolylineCenter = center.project(180, 50);
        layer.put(TESTLAYER_KEY_PREFIX + "staticPolyline", polygonAround(staticPolylineCenter, 50, Color.RED));

        final Geopoint staticCircle = staticPolylineCenter.project(90, 50);
        layer.put(TESTLAYER_KEY_PREFIX + "staticCircle", GeoPrimitive.createCircle(staticCircle, 40, GeoStyle.builder().setStrokeColor(Color.DKGRAY).setFillColor(Color.YELLOW).build()));

        final Geopoint staticPolygon = staticCircle.project(90, 50);
        layer.put(TESTLAYER_KEY_PREFIX + "staticPolygonWithText", GeoPrimitive.createPolygon(geoGridPoints(
                staticPolygon, 10, 0, 0, 3, 0, 3, 1, 2, 1, 2, 2, 3, 2, 3, 3, 1, 3, 0, 2
        ), GeoStyle.builder().setStrokeColor(Color.YELLOW).setStrokeWidth(5f).setFillColor(Color.GREEN).build())
                .buildUpon().setIcon(GeoIcon.builder().setText("This is my polygon").build()).build());

        final Geopoint staticPolWithHole = staticPolygon.project(90, 50);
        final GeoPrimitive pol = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYGON)
                .setStyle(GeoStyle.builder().setStrokeColor(Color.YELLOW).setStrokeWidth(5f).setFillColor(Color.GREEN).build())
                .addPoints(geoGridPoints(staticPolWithHole, 10, 0, 0, 7, 0, 7, 7, 0, 7))
                .addHole(geoGridPoints(staticPolWithHole, 10, 1, 1, 1, 2, 2, 2, 2, 1))
                .addHole(geoGridPoints(staticPolWithHole, 10, 4, 4, 4, 5, 5, 5, 5, 4))
                .build();
        layer.put(TESTLAYER_KEY_PREFIX + "staticPolygonWithTwoHoles", pol);

        final Geopoint zLevelStuff = staticPolygon.project(180, 50);
        final GeoPrimitive zGreen = GeoPrimitive.createPolygon(geoGridPoints(zLevelStuff, 10, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0), GeoStyle.builder().setStrokeColor(Color.BLUE).setStrokeWidth(5f).setFillColor(Color.GREEN).build());
        final GeoPrimitive zYellow = GeoPrimitive.createPolygon(geoGridPoints(zLevelStuff.project(110, 8), 10, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0), GeoStyle.builder().setStrokeColor(Color.BLUE).setStrokeWidth(5f).setFillColor(Color.YELLOW).build());
        final GeoPrimitive zRed = GeoPrimitive.createPolygon(geoGridPoints(zLevelStuff.project(140, 6), 10, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0), GeoStyle.builder().setStrokeColor(Color.BLUE).setStrokeWidth(5f).setFillColor(Color.RED).build());

        //order visible should be yellow, red, green
        layer.put(TESTLAYER_KEY_PREFIX + "zGreen-z0(most background)(placed-0)", zGreen.buildUpon().setZLevel(0).build());
        layer.put(TESTLAYER_KEY_PREFIX + "zYellow-z2(most foreground)(placed-1)", zYellow.buildUpon().setZLevel(2).build());
        layer.put(TESTLAYER_KEY_PREFIX + "zRed-z1(placed-2)", zRed.buildUpon().setZLevel(1).build());

        //line thicknesses
        final Geopoint lineThickness = zLevelStuff.project(180, 50);
        for (int i = 0; i < 10 ; i++) {
            final int widthInDp = (i + 1) * 5;
            final Geopoint start = lineThickness.project(90, i * 10);
            final Geopoint end = start.project(180, 10);
            final GeoStyle style = GeoStyle.builder().setStrokeColor(Color.BLUE).setStrokeWidth((float) widthInDp).build();
            layer.put(TESTLAYER_KEY_PREFIX + "lineThickness-" + widthInDp,
                GeoPrimitive.createPolyline(Arrays.asList(start, end), style).buildUpon().setIcon(GeoIcon.builder().setText(widthInDp + "dp").build()).build());
        }

        //button width: 38dp
        layer.put(TESTLAYER_KEY_PREFIX + "lineThickness-oneButton",
            GeoPrimitive.createPolyline(Arrays.asList(lineThickness.project(180, 20), lineThickness.project(180, 20).project(90, 50)),
                GeoStyle.builder().setStrokeColor(Color.GREEN).setStrokeWidth(38f).build()));
        layer.put(TESTLAYER_KEY_PREFIX + "lineThickness-twoButton",
            GeoPrimitive.createPolyline(Arrays.asList(lineThickness.project(180, 25), lineThickness.project(180, 25).project(90, 50)),
                GeoStyle.builder().setStrokeColor(Color.RED).setStrokeWidth(38f * 2).build()));




    }

    private static GeoGroup testIconWithPoly(final Geopoint point, final int polyColor, final float xAnchor, final float yAnchor, final float angle) {


        final GeoItem poly = polygonAround(point, 20, polyColor);
        final GeoItem marker = GeoPrimitive.createMarker(point,
                GeoIcon.builder()
                        .setXAnchor(xAnchor)
                        .setYAnchor(yAnchor)
                        .setRotation(angle)
                        .setBitmap(createBitmap(R.drawable.cgeo_borderless)).build());

        return GeoGroup.builder().addItems(poly, marker).build();

    }

    private static GeoPrimitive polygonAround(final Geopoint center, final float distance, final int color) {
        final Geopoint start = center.project(0, distance / 2).project(270, distance / 2);
        return GeoPrimitive.builder().setType(GeoItem.GeoType.POLYLINE).addPoints(
                start,
                start.project(90, distance),
                start.project(90, distance).project(180, distance),
                start.project(180, distance),
                start).setStyle(GeoStyle.builder().setStrokeColor(color).build()).build();
    }

    private static List<Geopoint> geoGridPoints(final Geopoint start, final float dist, final int ... coords) {
        final List<Geopoint> result = new ArrayList<>();
        for (int i = 0; i < coords.length; i += 2) {
            final int x = coords[i];
            final int y = coords[i + 1];
            result.add(start.project(90, x * dist).project(180, y * dist));
        }
        return result;
    }


    private static void createDynamicElements(final Geopoint center, final GeoItemLayer<String> layer, final int index) {
        //index runs from 0 to 359, then back to 0

        final int angle = (index * 20) % 360;

        final double distance = 20; //km

        //dynamic test data
        final GeoItem poly = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYLINE).addPoints(
                center.project(angle, distance),
                center.project((angle + 90) % 360, distance),
                center.project((angle + 180) % 360, distance),
                center.project((angle + 270) % 360, distance),
                center.project(angle, distance)).build();
        final GeoItem point = GeoPrimitive.createPoint(center.project(angle, distance), GeoStyle.builder().setStrokeColor(Color.GREEN).build());
        final GeoItem group = GeoGroup.builder().addItems(point, poly).build();

        layer.put(TESTLAYER_KEY_PREFIX + "Quad", group);

        final GeoItem flowMarker = GeoPrimitive.createMarker(center.project(angle, distance * 0.8), GeoIcon.builder()
                .setBitmap(createBitmap(R.drawable.type_event))
                .setRotation(angle).build());
        layer.put(TESTLAYER_KEY_PREFIX + "FlowMarker", flowMarker);

        //a series of markers with changing icons
        final Geopoint startDynamicMarkers = center.project(180, 80);
        for (int i = 0; i < 20; i++) {
            final GeoPrimitive iconI = GeoPrimitive.createMarker(startDynamicMarkers.project(90, 4 * i), ICON_ALL[(index + i) % ICON_ALL.length]);
            layer.put(TESTLAYER_KEY_PREFIX + "dynamicIconMarker-" + i, iconI);
        }



    }

    private static Bitmap createBitmap(@DrawableRes final int drawableId) {
        return ImageParam.id(drawableId).getAsBitmap();
    }

}
