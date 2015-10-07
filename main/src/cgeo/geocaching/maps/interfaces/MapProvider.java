package cgeo.geocaching.maps.interfaces;

import android.app.Activity;

/**
 * Defines functions of a factory class to get implementation specific objects
 * (GeoPoints, OverlayItems, ...)
 */
public interface MapProvider {

    public boolean isSameActivity(final MapSource source1, final MapSource source2);

    public Class<? extends Activity> getMapClass();

    public int getMapViewId();

    public int getMapLayoutId();

    public MapItemFactory getMapItemFactory();

    public void registerMapSource(final MapSource mapSource);
}
