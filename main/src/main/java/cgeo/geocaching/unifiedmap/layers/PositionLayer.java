package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.MapLineUtils;

import android.graphics.Bitmap;
import android.location.Location;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;

public class PositionLayer {

    private static final String KEY_POSITION = "positionMarker";
    private static final String KEY_ACCURACY = "accuracy";
    private static final String KEY_LONG_TAP = "longtapmarker";


    private final Bitmap markerPosition = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron, null));
    private final Bitmap markerLongTap = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.map_pin, null));

    private final GeoStyle accuracyStyle = GeoStyle.builder()
            .setStrokeWidth(1.0f)
            .setStrokeColor(MapLineUtils.getAccuracyCircleColor())
            .setFillColor(MapLineUtils.getAccuracyCircleFillColor())
            .build();


    UnifiedMapViewModel.PositionAndHeading lastPos = null;

    public PositionLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);
        viewModel.positionAndHeading.observe(activity, positionAndHeading -> {

            if (!positionAndHeading.equals(lastPos)) {
                lastPos = positionAndHeading;

                layer.put(KEY_ACCURACY, GeoPrimitive.createCircle(new Geopoint(positionAndHeading.location),
                                positionAndHeading.location.getAccuracy() / 1000.0f, accuracyStyle)
                        .buildUpon().setZLevel(LayerHelper.ZINDEX_POSITION_ACCURACY_CIRCLE).build());

                final Float currentBearing = viewModel.bearing.getValue();
                layer.put(KEY_POSITION,
                        GeoPrimitive.createMarker(new Geopoint(positionAndHeading.location), GeoIcon.builder()
                                        .setRotation(AngleUtils.normalize(positionAndHeading.heading + (currentBearing != null ? currentBearing : 0.0f)))
                                        .setBitmap(markerPosition).build())
                                .buildUpon().setZLevel(LayerHelper.ZINDEX_POSITION).build());
            }
        });

        // todo: hmm, maybe move to different layer?
        viewModel.longTapCoords.observe(activity, gp -> {
            if (gp == null) {
                layer.remove(KEY_LONG_TAP);
            } else {
                layer.put(KEY_LONG_TAP, GeoPrimitive.createMarker(gp, GeoIcon.builder()
                                .setYAnchor(markerLongTap.getHeight())
                                .setBitmap(markerLongTap).build())
                        .buildUpon().setZLevel(LayerHelper.ZINDEX_POSITION).build());
            }
        });

    }


}
