package cgeo.geocaching.maps.interfaces;

import android.app.Activity;

import java.util.Map;

/**
 * Defines functions of a factory class to get implementation specific objects
 * (GeoPoints, OverlayItems, ...)
 *
 * @author rsudev
 *
 */
public interface MapProvider {

    public Map<Integer, MapSource> getMapSources();

    public boolean isMySource(int sourceId);

    public boolean isSameActivity(int sourceId1, int sourceId2);

    public Class<? extends Activity> getMapClass();

    public int getMapViewId();

    public int getMapLayoutId();

    public MapItemFactory getMapItemFactory();
}
