package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.IWaypoint;

public interface MapItemFactory {

    GeoPointImpl getGeoPointBase(Geopoint coords);

    CachesOverlayItemImpl getCachesOverlayItem(IWaypoint iWaypoint, boolean applyDistanceRule);

}
