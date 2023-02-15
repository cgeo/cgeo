package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ImageUtils;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;

import io.reactivex.rxjava3.disposables.Disposable;

/** This is a test layer to experiment with unified geoitem layers. It is only activated with a specific developer setting */
public class GeoItemTestLayer {

    private static final Geopoint CENTER = new Geopoint(40, 10);

    private GeoItemLayer<String> testLayer = new GeoItemLayer<>("test");
    private Disposable stopper;

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

    private Disposable startData() {

        //static test data
        createStaticElements(CENTER.project(90, 50), testLayer);

        final int[] angleStore = { 0 };
        return AndroidRxUtils.runPeriodically(AndroidRxUtils.computationScheduler, () -> {
            final int angle = angleStore[0];
            angleStore[0] = (angleStore[0] + 20) % 360;

            createDynamicElements(CENTER, testLayer, angle);

            }, 0, 1000);
    }

    private static void createStaticElements(final Geopoint center, final GeoItemLayer<String> layer) {

        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                final Geopoint point = center.project(90, x * 20).project(180, y * 20);
                final GeoItem gi = testIconWithPoly(point, Color.GREEN, x * 0.5f, y * 0.5f, 0);
                layer.put("testMarkerOffset-" + x + "-" + y, gi);
            }
        }


        layer.put("testMarkerOffsetRightTop", testIconWithPoly(center.project(90, 100), Color.GRAY, GeoIcon.Hotspot.UPPER_RIGHT_CORNER.xAnchor, GeoIcon.Hotspot.UPPER_RIGHT_CORNER.yAnchor, 0));

        final Geopoint circleCenter = center.project(90, 200);
        for (int a = 0; a < 360; a += 40) {
            final Geopoint point = circleCenter.project(a, 40);
            final GeoItem gi = testIconWithPoly(point, Color.RED, GeoIcon.Hotspot.CENTER.xAnchor, GeoIcon.Hotspot.CENTER.yAnchor, a);
            layer.put("testMarkerAngle-" + a, gi);
        }
    }

    private static GeoGroup testIconWithPoly(final Geopoint point, final int polyColor, final float xAnchor, final float yAnchor, final float angle) {


        final GeoItem poly = polygonAround(point, 20, polyColor);
        final GeoItem marker = GeoPrimitive.createMarker(point,
                GeoIcon.builder()
                        .setXAnchor(xAnchor)
                        .setYAnchor(yAnchor)
                        .setAngle(angle)
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
                start).setStyle(new GeoStyle.Builder().setStrokeColor(color).build()).build();
    }


    private static void createDynamicElements(final Geopoint center, final GeoItemLayer<String> layer, final int angle) {
        final double distance = 20; //km

        //dynamic test data
        final GeoItem poly = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYLINE).addPoints(
                center.project(angle, distance),
                center.project((angle + 90) % 360, distance),
                center.project((angle + 180) % 360, distance),
                center.project((angle + 270) % 360, distance),
                center.project(angle, distance)).build();
        final GeoItem point = GeoPrimitive.createPoint(center.project(angle, distance), new GeoStyle.Builder().setStrokeColor(Color.GREEN).build());
        final GeoItem group = GeoGroup.builder().addItems(point, poly).build();

        layer.put("testQuad", group);

        final GeoItem flowMarker = GeoPrimitive.createMarker(center.project(angle, distance * 0.8), GeoIcon.builder()
                .setBitmap(createBitmap(R.drawable.type_event))
                .setAngle(angle).build());
        layer.put("testFlowMarker", flowMarker);

    }

    private static Bitmap createBitmap(@DrawableRes final int drawableId) {
        return ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), drawableId, null));

    }

}
