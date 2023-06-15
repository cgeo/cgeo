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
import cgeo.geocaching.unifiedmap.geoitemlayer.ILayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class GeoItemLayer implements ILayer {

    private final cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer<String> geoItemLayer = new cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer<>("geoitems");

    public GeoItemLayer(final AppCompatActivity activity) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);
        viewModel.geoItems.observeAddEvents(activity, key -> {
            final IWaypoint item = viewModel.geoItems.getMap().get(key);

            if (item == null) {
                Log.e("GeoItemLayer: IWaypoint item to be added was null");
                return;
            }

            final CacheMarker cm = item instanceof Geocache
                    ? MapMarkerUtils.getCacheMarker(CgeoApplication.getInstance().getResources(), (Geocache) item, null)
                    : MapMarkerUtils.getWaypointMarker(CgeoApplication.getInstance().getResources(), (Waypoint) item, true);

            geoItemLayer.put(key, GeoPrimitive.createMarker(item.getCoords(), GeoIcon.builder().setBitmap(cm.getBitmap()).setYAnchor(cm.getBitmap().getHeight() / 2f).build()));
        });

        viewModel.geoItems.observeRemoveEvents(activity, geoItemLayer::remove);
    }

    @Override
    public void init(final IProviderGeoItemLayer<?> provider) {
        geoItemLayer.setProvider(provider, LayerHelper.ZINDEX_GEOCACHE);
    }

    @Override
    public void destroy() {
        geoItemLayer.destroy();
    }

}
