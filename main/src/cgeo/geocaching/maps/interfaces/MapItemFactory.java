package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.location.Geopoint;

public interface MapItemFactory {

    public GeoPointImpl getGeoPointBase(final Geopoint coords);

    public CachesOverlayItemImpl getCachesOverlayItem(final IWaypoint iWaypoint, final boolean applyDistanceRule);

}
