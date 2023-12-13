package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.MapLineUtils;

import android.graphics.Bitmap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

public class IndividualRouteLayer {
    public static final String KEY_INDIVIDUAL_ROUTE = "INDIVIDUALROUTE";

    private final Bitmap marker = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.marker_routepoint, null));

    private final GeoStyle lineStyle = GeoStyle.builder()
            .setStrokeColor(MapLineUtils.getRouteColor())
            .setStrokeWidth(MapLineUtils.getRouteLineWidth(true))
            .build();

    public IndividualRouteLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);

        viewModel.individualRoute.observe(activity, individualRoute -> {

            for (String key : layer.keySet()) {
                if (key.startsWith(UnifiedMapViewModel.COORDSPOINT_KEY_PREFIX)) {
                    layer.remove(key);
                }
            }

            if (individualRoute.isHidden() || individualRoute.getRouteItems().isEmpty()) {
                layer.remove(KEY_INDIVIDUAL_ROUTE);
            } else {
                final GeoGroup.Builder geoGroup = GeoGroup.builder();
                GeoGroup.forAllPrimitives(individualRoute.getItem(), segment ->
                        geoGroup.addItems(GeoPrimitive.createPolyline(segment.getPoints(), lineStyle).buildUpon().setZLevel(LayerHelper.ZINDEX_TRACK_ROUTE).build()));
                layer.put(KEY_INDIVIDUAL_ROUTE, geoGroup.build());

                for (RouteItem item : individualRoute.getRouteItems()) {
                    if (item.getType() == RouteItem.RouteItemType.COORDS) {
                        layer.put(UnifiedMapViewModel.COORDSPOINT_KEY_PREFIX + item.getIdentifier(), GeoPrimitive.createMarker(item.getPoint(), GeoIcon.builder().setBitmap(marker).build()).buildUpon().setZLevel(LayerHelper.ZINDEX_COORD_POINT).build());
                    }
                }
            }
        });

    }

}
