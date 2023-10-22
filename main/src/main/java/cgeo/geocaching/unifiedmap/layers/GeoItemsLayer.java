package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.MapMarkerUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.HashMap;
import java.util.Map;

public class GeoItemsLayer {

    private Map<String, Integer> lastDisplayedGeocaches = new HashMap<>();


    public GeoItemsLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        final UnifiedMapViewModel viewModel = new ViewModelProvider(activity).get(UnifiedMapViewModel.class);


        viewModel.caches.observe(activity, caches -> { // this is always executed on UI thread, thus doesn't need to be thread save

            final Map<String, Integer> currentlyDisplayedGeocaches = new HashMap<>();

            for (Geocache cache : caches.getAsList()) { // Creates a clone to avoid ConcurrentModificationExceptions
                final CacheMarker cm = MapMarkerUtils.getCacheMarker(CgeoApplication.getInstance().getResources(), cache, null, true);
                currentlyDisplayedGeocaches.put(cache.getGeocode(), cm.hashCode());

                if (!lastDisplayedGeocaches.containsKey(cache.getGeocode()) || !lastDisplayedGeocaches.get(cache.getGeocode()).equals(cm.hashCode())) {

                    layer.put(UnifiedMapViewModel.CACHE_KEY_PREFIX + cache.getGeocode(), GeoPrimitive.createMarker(cache.getCoords(),
                            GeoIcon.builder()
                                    .setBitmap(cm.getBitmap())
                                    .setYAnchor(cm.getBitmap().getHeight() / 2f)
                                    .build()
                    ).buildUpon().setZLevel(LayerHelper.ZINDEX_GEOCACHE).build());
                }
            }

            for (String geocode : currentlyDisplayedGeocaches.keySet()) {
                lastDisplayedGeocaches.remove(geocode);
            }

            for (String geocode : lastDisplayedGeocaches.keySet()) {
                layer.remove(UnifiedMapViewModel.CACHE_KEY_PREFIX + geocode);
            }

            lastDisplayedGeocaches = currentlyDisplayedGeocaches;
        });

    }

    // TODO: similar code for WPs should be added here...

}
