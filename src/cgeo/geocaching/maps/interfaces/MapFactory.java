package cgeo.geocaching.maps.interfaces;

import android.content.Context;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgUser;

/**
 * Defines functions of a factory class to get implementation specific objects
 * (GeoPoints, OverlayItems, ...)
 * @author rsudev
 *
 */
public interface MapFactory {

	public int getMapViewId();

	public int getMapLayoutId();

	public GeoPointImpl getGeoPointBase(int latE6, int lonE6);

	CachesOverlayItemImpl getCacheOverlayItem(cgCoord coordinate, String type);

	public OtherCachersOverlayItemImpl getOtherCachersOverlayItemBase(Context context,
			cgUser userOne);
}
