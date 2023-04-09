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
import cgeo.geocaching.unifiedmap.geoitemlayer.ILayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.MapLineUtils;

import android.graphics.Bitmap;
import android.location.Location;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;

public class PositionLayer implements ILayer {

    private final GeoItemLayer<String> positionLayer = new GeoItemLayer<>("position");
    private final GeoItemLayer<String> accuracyLayer = new GeoItemLayer<>("accuracy");

    private final Bitmap marker = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron, null));

    private final GeoStyle accuracyStyle = GeoStyle.builder()
            .setStrokeWidth(1.0f)
            .setStrokeColor(MapLineUtils.getAccuracyCircleColor())
            .setFillColor(MapLineUtils.getAccuracyCircleFillColor())
            .build();


    Pair<Location, Float> lastPos = null;

    public PositionLayer(final AppCompatActivity activity) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);
        viewModel.getPositionAndHeading().observe(activity, positionAndHeading -> {

            if (!positionAndHeading.equals(lastPos)) {
                lastPos = positionAndHeading;

                accuracyLayer.put("accuracy", GeoPrimitive.createCircle(new Geopoint(positionAndHeading.first),
                        positionAndHeading.first.getAccuracy() / 1000.0f, accuracyStyle));

                positionLayer.put("positionMarker",
                        GeoPrimitive.createMarker(new Geopoint(positionAndHeading.first), GeoIcon.builder()
                                .setRotation(positionAndHeading.second)
                                .setBitmap(marker).build()));
            }
        });

    }

    @Override
    public void init(final IProviderGeoItemLayer<?> provider) {
        positionLayer.setProvider(provider, LayerHelper.ZINDEX_POSITION);
        accuracyLayer.setProvider(provider, LayerHelper.ZINDEX_POSITION_ACCURACY_CIRCLE);
    }

    @Override
    public void destroy() {
        positionLayer.destroy();
        accuracyLayer.destroy();
    }


}
