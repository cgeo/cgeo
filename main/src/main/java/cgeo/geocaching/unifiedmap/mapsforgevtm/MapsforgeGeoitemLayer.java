package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.unifiedmap.AbstractGeoitemLayer;
import cgeo.geocaching.unifiedmap.LayerHelper;
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
    protected GeoItemCache<MarkerItem> addInternal(final IWaypoint item, final boolean isCache, final Geopoint coords, final RouteItem routeItem, final CacheMarker cm) {
        final MarkerItem marker = new MarkerItem(routeItem.getIdentifier(), "", new GeoPoint(coords.getLatitudeE6(), coords.getLongitudeE6()));
        marker.setMarker(new MarkerSymbol(new AndroidBitmap(cm.getBitmap()), MarkerSymbol.HotspotPlace.BOTTOM_CENTER));
        if (isCache) {
            mGeocacheLayer.addItem(marker);
        } else {
            mWaypointLayer.addItem(marker);
        }
        return new GeoItemCache<>(routeItem, marker);
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
