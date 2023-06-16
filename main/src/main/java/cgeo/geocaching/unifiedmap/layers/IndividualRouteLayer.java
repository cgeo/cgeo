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
    private static final String KEY_INDIVIDUAL_ROUTE = "INDIVIDUALROUTE";

    private final Bitmap marker = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.marker_routepoint, null));

    final GeoStyle lineStyle = GeoStyle.builder()
            .setStrokeColor(MapLineUtils.getRouteColor())
            .setStrokeWidth(MapLineUtils.getRouteLineWidth(true))
            .build();

    final UnifiedMapViewModel viewModel;

    public IndividualRouteLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);

        viewModel.individualRoute.observe(activity, individualRoute -> {

            for (String key : layer.keySet()) {
                if (!KEY_INDIVIDUAL_ROUTE.equals(key)) {
                    layer.remove(key);
                }
            }

            if (individualRoute.isHidden() || individualRoute.getRouteItems().isEmpty()) {
                layer.remove(KEY_INDIVIDUAL_ROUTE);
            } else {
                final GeoGroup.Builder geoGroup = GeoGroup.builder();
                for (GeoPrimitive segment : individualRoute.getGeoData()) {
                    geoGroup.addItems(GeoPrimitive.createPolyline(segment.getPoints(), lineStyle).buildUpon().setZLevel(LayerHelper.ZINDEX_TRACK_ROUTE).build());
                }
                layer.put(KEY_INDIVIDUAL_ROUTE, geoGroup.build());

                for (RouteItem item : individualRoute.getRouteItems()) {
                    if (item.getType() == RouteItem.RouteItemType.COORDS) {
                        layer.put(item.getIdentifier(), GeoPrimitive.createMarker(item.getPoint(), GeoIcon.builder().setBitmap(marker).build()).buildUpon().setZLevel(LayerHelper.ZINDEX_COORD_POINT).build());
                    }
                }
            }
        });

    }

//    @Override
//    public boolean handleTap(final Context context, final Geopoint tapped, final boolean isLongTap) {
//        final Set<String> items = geoItemLayer.getTouched(tapped);
//        if (items.size() <= 2) { // route line + coords marker
//            for (String item : items) {
//                if (isLongTap && !KEY_INDIVIDUAL_ROUTE.equals(item)) {
//                    viewModel.toggleRouteItem(context, new RouteItem(geoItemLayer.get(item).getCenter()));
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
}
