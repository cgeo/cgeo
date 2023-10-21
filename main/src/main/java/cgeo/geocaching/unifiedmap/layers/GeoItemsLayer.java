package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class GeoItemsLayer {

    public GeoItemsLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);
        viewModel.geoItems.observeAddEvents(activity, key -> {
            final IWaypoint item = viewModel.geoItems.getMap().get(key);

            if (item == null) {
                Log.e("GeoItemLayer: IWaypoint item to be added was null");
                return;
            }

            final CacheMarker cm = item instanceof Geocache
                    ? MapMarkerUtils.getCacheMarker(CgeoApplication.getInstance().getResources(), (Geocache) item, null, true)
                    : MapMarkerUtils.getWaypointMarker(CgeoApplication.getInstance().getResources(), (Waypoint) item, true, true);

            layer.put(key, GeoPrimitive.createMarker(item.getCoords(),
                    GeoIcon.builder()
                            .setBitmap(cm.getBitmap())
                            .setYAnchor(cm.getBitmap().getHeight() / 2f)
                            .build()
            ).buildUpon().setZLevel(item instanceof Geocache ? LayerHelper.ZINDEX_GEOCACHE : LayerHelper.ZINDEX_WAYPOINT).build());
        });

        viewModel.geoItems.observeRemoveEvents(activity, layer::remove);
    }

}
