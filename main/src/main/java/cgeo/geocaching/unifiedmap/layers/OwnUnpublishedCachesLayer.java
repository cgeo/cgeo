package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.utils.MapMarkerUtils;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Shows the caches on {@link PseudoList#OWN_UNPUBLISHED_LIST} as an overlay on the live map, using the same
 * key prefix as the regular cache markers so tapping one opens the normal cache detail popup. The list's
 * membership is refreshed from geocaching.com whenever its own Activity is opened, not by this layer, so this
 * is a one-time render of whatever is currently stored rather than a live-updating layer.
 */
public class OwnUnpublishedCachesLayer {

    public OwnUnpublishedCachesLayer(final AppCompatActivity activity, final GeoItemLayer<String> layer) {
        final SearchResult ownUnpublished = DataStore.getBatchOfStoredCaches(null, PseudoList.OWN_UNPUBLISHED_LIST.id);
        for (final Geocache cache : ownUnpublished.getCachesFromSearchResult()) {
            final CacheMarker cm = MapMarkerUtils.getCacheMarker(activity.getResources(), cache, null, true);
            layer.put(UnifiedMapViewModel.CACHE_KEY_PREFIX + cache.getGeocode(), GeoPrimitive.createMarker(cache.getCoords(),
                    GeoIcon.builder()
                            .setBitmap(cm.getBitmap())
                            .setHotspot(GeoIcon.Hotspot.BOTTOM_CENTER)
                            .build()
            ).buildUpon().setZLevel(LayerHelper.ZINDEX_GEOCACHE).build());
        }
    }
}
