package cgeo.geocaching.maps.interfaces;

import android.app.Activity;

/**
 * Defines functions of a factory class to get implementation specific objects
 * (GeoPoints, OverlayItems, ...)
 */
public interface MapProvider {

    boolean isSameActivity(MapSource source1, MapSource source2);

    Class<? extends Activity> getMapClass();

    int getMapViewId();

    int getMapLayoutId();

    default int getMapAttributionViewId() {
        return 0;
    }

    MapItemFactory getMapItemFactory();

    void registerMapSource(MapSource mapSource);
}
