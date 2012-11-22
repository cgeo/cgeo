package cgeo.geocaching.maps;

import cgeo.geocaching.Settings;
import cgeo.geocaching.maps.google.GoogleMapProvider;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;

import android.view.Menu;

import java.util.ArrayList;
import java.util.List;

public class MapProviderFactory {

    private final static ArrayList<MapSource> mapSources = new ArrayList<MapSource>();

    static {
        // add GoogleMapProvider only if google api is available in order to support x86 android emulator
        if (isGoogleMapsInstalled()) {
            GoogleMapProvider.getInstance();
        }
        MapsforgeMapProvider.getInstance();
    }

    public static boolean isGoogleMapsInstalled() {
        boolean googleMaps = true;
        try {
            Class.forName("com.google.android.maps.MapActivity");
        } catch (ClassNotFoundException e) {
            googleMaps = false;
        }
        return googleMaps;
    }

    public static List<MapSource> getMapSources() {
        return mapSources;
    }

    public static boolean isSameActivity(final MapSource source1, final MapSource source2) {
        final MapProvider provider1 = source1.getMapProvider();
        final MapProvider provider2 = source2.getMapProvider();
        return provider1 == provider2 && provider1.isSameActivity(source1, source2);
    }

    public static void addMapviewMenuItems(final Menu parentMenu, final int groupId) {
        final int currentSource = Settings.getMapSource().getNumericalId();
        for (int i = 0; i < mapSources.size(); i++) {
            final MapSource mapSource = mapSources.get(i);
            final int id = mapSource.getNumericalId();
            parentMenu.add(groupId, id, i, mapSource.getName()).setCheckable(true).setChecked(id == currentSource);
        }
    }

    public static MapSource getMapSource(int id) {
        for (MapSource mapSource : mapSources) {
            if (mapSource.getNumericalId() == id) {
                return mapSource;
            }
        }
        return null;
    }

    public static void registerMapSource(final MapSource mapSource) {
        mapSources.add(mapSource);
    }

    public static MapSource getDefaultSource() {
        return mapSources.get(0);
    }

    /**
     * remove offline map sources after changes of the settings
     */
    public static void deleteOfflineMapSources() {
        final ArrayList<MapSource> deletion = new ArrayList<MapSource>();
        for (MapSource mapSource : mapSources) {
            if (mapSource instanceof MapsforgeMapProvider.OfflineMapSource) {
                deletion.add(mapSource);
            }
        }
        mapSources.removeAll(deletion);
    }
}
