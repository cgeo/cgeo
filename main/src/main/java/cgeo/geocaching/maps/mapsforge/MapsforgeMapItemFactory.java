package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.mapsforge.v6.MapsforgeGeoPoint;
import cgeo.geocaching.models.IWaypoint;

import org.mapsforge.core.model.LatLong;

public class MapsforgeMapItemFactory implements MapItemFactory {

    @Override
    public GeoPointImpl getGeoPointBase(final Geopoint coords) {
        return new MapsforgeGeoPoint(new LatLong(coords.getLatitude(), coords.getLongitude()));
    }

    @Override
    public CachesOverlayItemImpl getCachesOverlayItem(final IWaypoint coordinate, final boolean applyDistanceRule) {
        return null;
        // @todo
        // return new MapsforgeCacheOverlayItem(coordinate, applyDistanceRule);
    }

}
