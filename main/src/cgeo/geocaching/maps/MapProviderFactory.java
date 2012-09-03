package cgeo.geocaching.maps;

import cgeo.geocaching.maps.google.GoogleMapProvider;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;

import android.view.Menu;

import java.util.SortedMap;
import java.util.TreeMap;

public class MapProviderFactory {

    private final static int GOOGLEMAP_BASEID = 30;
    private final static int MFMAP_BASEID = 40;

    private final static MapProvider[] mapProviders;
    private final static SortedMap<Integer, MapSource> mapSources;

    static {
        // add GoogleMapProvider only if google api is available in order to support x86 android emulator
        if (isGoogleMapsInstalled()) {
            mapProviders = new MapProvider[] { new GoogleMapProvider(GOOGLEMAP_BASEID), new MapsforgeMapProvider(MFMAP_BASEID) };
        }
        else {
            mapProviders = new MapProvider[] { new MapsforgeMapProvider(MFMAP_BASEID) };
        }

        mapSources = new TreeMap<Integer, MapSource>();
        for (MapProvider mp : mapProviders) {
            mapSources.putAll(mp.getMapSources());
        }
    }

    private static boolean isGoogleMapsInstalled() {
        boolean googleMaps = true;
        try {
            Class.forName("com.google.android.maps.MapActivity");
        } catch (ClassNotFoundException e) {
            googleMaps = false;
        }
        return googleMaps;
    }

    public static SortedMap<Integer, MapSource> getMapSources() {
        return mapSources;
    }

    public static boolean isValidSourceId(int sourceId) {
        return mapSources.containsKey(sourceId);
    }

    public static boolean isSameActivity(int sourceId1, int sourceId2) {
        for (MapProvider mp : mapProviders) {
            if (mp.isMySource(sourceId1) && mp.isMySource(sourceId2)) {
                return mp.isSameActivity(sourceId1, sourceId2);
            }
        }
        return false;
    }

    public static MapProvider getMapProvider(int sourceId) {
        for (MapProvider mp : mapProviders) {
            if (mp.isMySource(sourceId)) {
                return mp;
            }
        }
        return mapProviders[0];
    }

    public static int getSourceOrdinalFromId(int sourceId) {
        int sourceOrdinal = 0;
        for (int key : mapSources.keySet()) {
            if (sourceId == key) {
                return sourceOrdinal;
            }
            sourceOrdinal++;
        }
        return 0;
    }

    public static int getSourceIdFromOrdinal(int sourceOrdinal) {
        int count = 0;
        for (int key : mapSources.keySet()) {
            if (sourceOrdinal == count) {
                return key;
            }
            count++;
        }
        return mapSources.firstKey();
    }

    public static void addMapviewMenuItems(Menu parentMenu, int groupId, int currentSource) {
        for (Integer key : mapSources.keySet()) {
            parentMenu.add(groupId, key, 0, mapSources.get(key).getName()).setCheckable(true).setChecked(key == currentSource);
        }
    }

    public static int getMapSourceFromMenuId(int menuId) {
        return menuId;
    }

    public static MapSource getMapSource(int sourceId) {
        return mapSources.get(sourceId);
    }
}
