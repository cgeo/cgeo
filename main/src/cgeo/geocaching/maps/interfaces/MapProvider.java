package cgeo.geocaching.maps.interfaces;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Defines functions of a factory class to get implementation specific objects
 * (GeoPoints, OverlayItems, ...)
 */
public interface MapProvider {

    boolean isSameActivity(MapSource source1, MapSource source2);

    Class<? extends AppCompatActivity> getMapClass();

    int getMapViewId();

    default int getMapAttributionViewId() {
        return 0;
    }

    MapItemFactory getMapItemFactory();

    void registerMapSource(MapSource mapSource);
}
