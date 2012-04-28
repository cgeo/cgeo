package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.go4cache.Go4CacheUser;

import android.content.Context;

public interface MapItemFactory {

    public GeoPointImpl getGeoPointBase(final Geopoint coords);

    public CachesOverlayItemImpl getCachesOverlayItem(final IWaypoint iWaypoint, final CacheType type);

    public OtherCachersOverlayItemImpl getOtherCachersOverlayItemBase(Context context,
            Go4CacheUser userOne);
}
