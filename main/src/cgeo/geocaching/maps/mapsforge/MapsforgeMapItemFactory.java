package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;

public class MapsforgeMapItemFactory implements MapItemFactory {

    @Override
    public GeoPointImpl getGeoPointBase(final Geopoint coords) {
        return new MapsforgeGeoPoint(coords.getLatitudeE6(), coords.getLongitudeE6());
    }

    @Override
    public CachesOverlayItemImpl getCachesOverlayItem(final IWaypoint coordinate, boolean applyDistanceRule) {
        return new MapsforgeCacheOverlayItem(coordinate, applyDistanceRule);
    }

}
