package cgeo.geocaching.maps.interfaces;

import android.app.Activity;

/**
 * Defines functions of a factory class to get implementation specific objects
 * (GeoPoints, OverlayItems, ...)
 */
public interface MapProvider {

    boolean isSameActivity(final MapSource source1, final MapSource source2);

    Class<? extends Activity> getMapClass();

    int getMapViewId();

    int getMapLayoutId();

    MapItemFactory getMapItemFactory();

    void registerMapSource(final MapSource mapSource);
}
