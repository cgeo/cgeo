package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Context;

/**
 * Defines functions of a factory class to get implementation specific objects
 * (GeoPoints, OverlayItems, ...)
 *
 * @author rsudev
 *
 */
public interface MapFactory {

    public Class<? extends Activity> getMapClass();

    public int getMapViewId();

    public int getMapLayoutId();

    public GeoPointImpl getGeoPointBase(final Geopoint coords);

    public CachesOverlayItemImpl getCachesOverlayItem(final cgCoord coordinate, final String type);

    public OtherCachersOverlayItemImpl getOtherCachersOverlayItemBase(Context context,
            cgUser userOne);

}
