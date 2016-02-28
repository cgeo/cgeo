package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.location.Geopoint;

public interface MapItemFactory {

    GeoPointImpl getGeoPointBase(final Geopoint coords);

    CachesOverlayItemImpl getCachesOverlayItem(final IWaypoint iWaypoint, final boolean applyDistanceRule);

}
