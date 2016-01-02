package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.MfMapView;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.maps.mapsforge.v6.TapHandler;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapUtils;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;

import java.util.Collection;
import java.util.Set;

public abstract class AbstractCachesOverlay {

    private final int overlayId;
    private final Set<GeoEntry> geoEntries;
    private final MfMapView mapView;
    private final Layer anchorLayer;
    private final GeoitemLayers layerList = new GeoitemLayers();
    private final MapHandlers mapHandlers;

    public AbstractCachesOverlay(final int overlayId, final Set<GeoEntry> geoEntries, final MfMapView mapView, final Layer anchorLayer, final MapHandlers mapHandlers) {
        this.overlayId = overlayId;
        this.geoEntries = geoEntries;
        this.mapView = mapView;
        this.anchorLayer = anchorLayer;
        this.mapHandlers = mapHandlers;
    }

    public void onDestroy() {
        clearLayers();
    }

    public int getVisibleItemsCount() {
        return mapView.getViewport().count(DataStore.loadCaches(getGeocodes(), LoadFlags.LOAD_CACHE_OR_DB));
    }

    public int getItemsCount() {
        return layerList.size();
    }

    protected final boolean addItem(final Geocache cache) {
        GeoEntry entry = new GeoEntry(cache.getGeocode(), overlayId);
        if (geoEntries.add(entry)) {
            layerList.add(getCacheItem(cache, this.mapHandlers.getTapHandler()));

            Log.d(String.format("Cache %s for id %d added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()));

            return true;
        }

        Log.d(String.format("Cache %s for id %d not added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()));

        return false;
    }

    protected final boolean addItem(final Waypoint waypoint) {
        GeoEntry entry = new GeoEntry(waypoint.getGpxId(), overlayId);
        final GeoitemLayer waypointItem = getWaypointItem(waypoint, this.mapHandlers.getTapHandler());
        if (waypointItem != null && geoEntries.add(entry)) {
            layerList.add(waypointItem);

            Log.d(String.format("Waypoint %s for id %d added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()));

            return true;
        }

        Log.d(String.format("Waypoint %s for id %d not added, geoEntries: %d", entry.geocode, overlayId, geoEntries.size()));

        return false;
    }

    protected void addLayers() {
        final Layers layers = this.mapView.getLayerManager().getLayers();
        final int index = layers.indexOf(anchorLayer) + 1;
        layers.addAll(index, layerList.getAsLayers());
    }

    protected Collection<String> getGeocodes() {
        return layerList.getGeocodes();
    }

    protected Viewport getViewport() {
        return this.mapView.getViewport();
    }

    protected int getMapZoomLevel() {
        return this.mapView.getMapZoomLevel();
    }

    protected void showProgress() {
        mapHandlers.sendEmptyProgressMessage(NewMap.SHOW_PROGRESS);
    }

    protected void hideProgress() {
        mapHandlers.sendEmptyProgressMessage(NewMap.HIDE_PROGRESS);
    }

    protected void repaint() {
        mapHandlers.sendEmptyDisplayMessage(NewMap.INVALIDATE_MAP);
        mapHandlers.sendEmptyDisplayMessage(NewMap.UPDATE_TITLE);
    }

    protected void clearLayers() {
        final Layers layers = this.mapView.getLayerManager().getLayers();

        for (final GeoitemLayer layer : layerList) {
            geoEntries.remove(new GeoEntry(layer.getItemCode(), overlayId));
            layers.remove(layer);
        }

        layerList.clear();

        Log.d(String.format("Layers for id %d cleared, remaining geoEntries: %d", overlayId, geoEntries.size()));
    }

    protected void syncLayers(final Collection<String> removeCodes, final Collection<String> newCodes) {
        final Layers layers = this.mapView.getLayerManager().getLayers();
        for (final String code : removeCodes) {
            final GeoitemLayer item = layerList.getItem(code);
            geoEntries.remove(new GeoEntry(code, overlayId));
            layers.remove(item);
            layerList.remove(item);
        }
        final int index = layers.indexOf(anchorLayer) + 1;
        layers.addAll(index, layerList.getMatchingLayers(newCodes));

        Log.d(String.format("Layers for id %d synced. Codes removed: %d, new codes: %d, geoEntries: %d", overlayId, removeCodes.size(), newCodes.size(), geoEntries.size()));
    }

    private static GeoitemLayer getCacheItem(final Geocache cache, final TapHandler tapHandler) {
        final Geopoint target = cache.getCoords();
        final Bitmap marker = AndroidGraphicFactory.convertToBitmap(MapUtils.getCacheMarker(CgeoApplication.getInstance().getResources(), cache));
        return new GeoitemLayer(cache.getGeoitemRef(), tapHandler, new LatLong(target.getLatitude(), target.getLongitude()), marker, 0, -marker.getHeight() / 2);
    }

    private static GeoitemLayer getWaypointItem(final Waypoint waypoint, final TapHandler tapHandler) {
        final Geopoint target = waypoint.getCoords();

        if (target != null) {
            final Bitmap marker = AndroidGraphicFactory.convertToBitmap(MapUtils.getWaypointMarker(CgeoApplication.getInstance().getResources(), waypoint));
            return new GeoitemLayer(waypoint.getGeoitemRef(), tapHandler, new LatLong(target.getLatitude(), target.getLongitude()), marker, 0, -marker.getHeight() / 2);
        }

        return null;
    }
 }