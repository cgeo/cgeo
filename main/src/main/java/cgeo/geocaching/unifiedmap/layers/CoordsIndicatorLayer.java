package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.ImageUtils;

import android.graphics.Bitmap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

public class CoordsIndicatorLayer {
    private static final String KEY_COORDS_INDICATOR = "COORDS_INDICATOR";

    private final Bitmap marker = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.coords_indicator, null));

    public CoordsIndicatorLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);

        viewModel.coordsIndicator.observe(activity, geopoint -> {
            if (geopoint != null) {
                layer.put(KEY_COORDS_INDICATOR, GeoPrimitive.createMarker(geopoint, GeoIcon.builder().setBitmap(marker).build()).buildUpon().setZLevel(LayerHelper.ZINDEX_SEARCHCENTER).build());
            } else {
                layer.remove(KEY_COORDS_INDICATOR);
            }
        });
    }
}
