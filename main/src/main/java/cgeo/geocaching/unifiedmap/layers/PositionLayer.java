package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.GeoHeightUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.MapLineUtils;

import android.graphics.Bitmap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

public class PositionLayer {

    private static final String KEY_POSITION = "positionMarker";
    private static final String KEY_ACCURACY = "accuracy";
    private static final String KEY_ELEVATION = "elevation";

    private final Bitmap markerPosition = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron, null));

    private final GeoStyle accuracyStyle = GeoStyle.builder()
            .setStrokeWidth(1.0f)
            .setStrokeColor(MapLineUtils.getAccuracyCircleColor())
            .setFillColor(MapLineUtils.getAccuracyCircleFillColor())
            .build();


    public PositionLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);
        viewModel.location.observe(activity, locationWrapper -> {

            if (locationWrapper.needsRepaintForDistanceOrAccuracy || !layer.isShown(KEY_ACCURACY)) {
                layer.put(KEY_ACCURACY, GeoPrimitive.createCircle(new Geopoint(locationWrapper.location),
                                locationWrapper.location.getAccuracy() / 1000.0f, accuracyStyle)
                        .buildUpon().setZLevel(LayerHelper.ZINDEX_POSITION_ACCURACY_CIRCLE).build());
            }

            // always update position indicator, no matter if it's because of position or heading
            layer.put(KEY_POSITION,
                    GeoPrimitive.createMarker(new Geopoint(locationWrapper.location), GeoIcon.builder()
                                    .setRotation(locationWrapper.heading)
                                    .setFlat(true)
                                    .setBitmap(markerPosition).build())
                            .buildUpon().setZLevel(LayerHelper.ZINDEX_POSITION).build());

            if (locationWrapper.location.hasAltitude() && Settings.showElevation()) {
                layer.put(KEY_ELEVATION,
                        GeoPrimitive.createMarker(new Geopoint(locationWrapper.location), GeoIcon.builder()
                                        .setFlat(false)
                                        .setBitmap(MapUtils.getElevationBitmap(activity.getResources(), markerPosition.getHeight(), GeoHeightUtils.getAltitude(locationWrapper.location)))
                                        .build())
                                .buildUpon().setZLevel(LayerHelper.ZINDEX_POSITION_ELEVATION).build());
            }

        });
    }
}
