package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.unifiedmap.AbstractGeoitemLayer;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.graphics.BitmapFactory;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.map.Map;

class MapsforgeGeoitemLayer extends AbstractGeoitemLayer<MarkerItem> {

    ItemizedLayer mMarkerLayer;
    MarkerSymbol mDefaultMarkerSymbol;

    MapsforgeGeoitemLayer(final AbstractTileProvider tileProvider, final Map map) {
        final Bitmap bitmap = new AndroidBitmap(BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.cgeo_notification));
        mDefaultMarkerSymbol = new MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
        mMarkerLayer = new ItemizedLayer(map, mDefaultMarkerSymbol);
        ((MapsforgeVtmView) tileProvider.getMap()).addLayer(mMarkerLayer);
        Log.e("addGeoitemLayer");
    }

    @Override
    protected void add(final Geocache cache) {
        final Geopoint coords = cache.getCoords();
        final MarkerItem item = new MarkerItem(cache.getGeocode(), "", new GeoPoint(coords.getLatitudeE6(), coords.getLongitudeE6())); // @todo add marker touch handling

        final CacheMarker cm = MapMarkerUtils.getCacheMarker(CgeoApplication.getInstance().getResources(), cache, null);
        final MarkerSymbol symbol = new MarkerSymbol(new AndroidBitmap(cm.getBitmap()), MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
        item.setMarker(symbol);
        mMarkerLayer.addItem(item);

        Log.e("addGeoitem");
        synchronized (items) {
            items.put(cache.getGeocode(), new GeoItemCache<>(new RouteItem(cache), item));
        }
    }

    @Override
    protected void remove(final String geocode) {
        if (mMarkerLayer != null) {
            synchronized (items) {
                final GeoItemCache<MarkerItem> item = items.get(geocode);
                if (item != null) {
                    mMarkerLayer.removeItem(item.mapItem);
                }
            }
        }
        super.remove(geocode);
    }
}
