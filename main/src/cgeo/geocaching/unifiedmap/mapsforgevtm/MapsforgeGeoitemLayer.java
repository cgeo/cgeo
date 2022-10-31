package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.unifiedmap.AbstractGeoitemLayer;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.utils.MapMarkerUtils;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_MAPSFORGE;

import android.graphics.BitmapFactory;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.map.Map;

class MapsforgeGeoitemLayer extends AbstractGeoitemLayer<MarkerItem> {

    ItemizedLayer mGeocacheLayer;
    ItemizedLayer mWaypointLayer;
    MarkerSymbol mDefaultMarkerSymbol;

    MapsforgeGeoitemLayer(final Map map) {
        final Bitmap bitmap = new AndroidBitmap(BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.cgeo_notification));
        mDefaultMarkerSymbol = new MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
        mGeocacheLayer = new ItemizedLayer(map, mDefaultMarkerSymbol);
        MAP_MAPSFORGE.addLayer(LayerHelper.ZINDEX_GEOCACHE, mGeocacheLayer);
        mWaypointLayer = new ItemizedLayer(map, mDefaultMarkerSymbol);
        MAP_MAPSFORGE.addLayer(LayerHelper.ZINDEX_WAYPOINT, mWaypointLayer);
    }

    @Override
    protected void add(final Geocache cache) {
        final Geopoint coords = cache.getCoords();
        final MarkerItem item = new MarkerItem(cache.getGeocode(), "", new GeoPoint(coords.getLatitudeE6(), coords.getLongitudeE6())); // @todo add marker touch handling

        final CacheMarker cm = MapMarkerUtils.getCacheMarker(CgeoApplication.getInstance().getResources(), cache, null);
        final MarkerSymbol symbol = new MarkerSymbol(new AndroidBitmap(cm.getBitmap()), MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
        item.setMarker(symbol);
        mGeocacheLayer.addItem(item);

        synchronized (items) {
            items.put(cache.getGeocode(), new GeoItemCache<>(new RouteItem(cache), item));
        }
    }

    @Override
    protected void remove(final String geocode) {
        // @todo: waypoints?
        if (mGeocacheLayer != null) {
            synchronized (items) {
                final GeoItemCache<MarkerItem> item = items.get(geocode);
                if (item != null) {
                    mGeocacheLayer.removeItem(item.mapItem);
                }
            }
        }
        super.remove(geocode);
    }
}
