package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.MapMarkerUtils;

import java.util.HashMap;
import java.util.LinkedList;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;

public abstract class AbstractGeoitemLayer<T> {

    protected final HashMap<String, GeoItemCache<T>> items = new HashMap<>();

    public static class GeoItemCache<T> {
        public RouteItem routeItem;
        public T mapItem;

        public GeoItemCache(final RouteItem routeItem, final T mapItem) {
            this.routeItem = routeItem;
            this.mapItem = mapItem;
        }
    }

    protected void add(final IWaypoint item) {
        final Geopoint coords = item.getCoords();
        if (coords == null) {
            return;
        }

        final boolean isCache = item instanceof Geocache;
        final RouteItem routeItem = new RouteItem(isCache ? (Geocache) item : (Waypoint) item);
        final CacheMarker cm = isCache
                ? MapMarkerUtils.getCacheMarker(CgeoApplication.getInstance().getResources(), (Geocache) item, null)
                : MapMarkerUtils.getWaypointMarker(CgeoApplication.getInstance().getResources(), (Waypoint) item, true);
        final GeoItemCache<T> cacheItem = addInternal(item, isCache, coords, routeItem, cm);
        synchronized (items) {
            items.put(routeItem.getIdentifier(), cacheItem);
        }
    }

    protected abstract GeoItemCache<T> addInternal(IWaypoint item, boolean isCache, Geopoint coords, RouteItem routeItem, CacheMarker cm);

    /** removes item from internal data structure, needs to be removed from actual layer by superclass */
    protected void remove(final String geocode) {
        synchronized (items) {
            items.remove(geocode);
        }
    }

    public LinkedList<RouteItem> find(final BoundingBox boundingBox) {
        final LinkedList<RouteItem> result = new LinkedList<>();
        synchronized (items) {
            for (String geocode : items.keySet()) {
                final GeoItemCache<T> item = items.get(geocode);
                assert item != null;
                final Geopoint geopoint = item.routeItem.getPoint();
                if (boundingBox.contains(new GeoPoint(geopoint.getLatitudeE6(), geopoint.getLongitudeE6()))) {
                    result.add(item.routeItem);
                }
            }
        }
        return result;
    }

}
