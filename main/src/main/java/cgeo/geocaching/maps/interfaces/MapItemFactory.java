package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.INamedGeoCoordinate;

public interface MapItemFactory {

    GeoPointImpl getGeoPointBase(Geopoint coords);

    CachesOverlayItemImpl getCachesOverlayItem(INamedGeoCoordinate iWaypoint, boolean applyDistanceRule, boolean setDraggable);

}
