package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.location.Geopoint;

public interface MapItemFactory {

    GeoPointImpl getGeoPointBase(Geopoint coords);

    CachesOverlayItemImpl getCachesOverlayItem(IWaypoint iWaypoint, boolean applyDistanceRule);

}
