package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.google.v2.GoogleMapProvider;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.SubMenu;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class MapProviderFactory {

    private static final ArrayList<MapSource> mapSources = new ArrayList<>();

    static {
        // add GoogleMapProvider only if google api is available in order to support x86 android emulator
        if (isGoogleMapsInstalled()) {
            GoogleMapProvider.getInstance();
        }
        MapsforgeMapProvider.getInstance();
    }

    private MapProviderFactory() {
        // utility class
    }

    public static boolean isGoogleMapsInstalled() {
        // Check if API key is available
        final String mapsKey = CgeoApplication.getInstance().getString(R.string.maps_api2_key);
        if (StringUtils.length(mapsKey) < 30 || StringUtils.contains(mapsKey, "key")) {
            Log.w("No Google API key available.");
            return false;
        }

        // Check if API is available
        try {
            Class.forName("com.google.android.gms.maps.MapView");
        } catch (final ClassNotFoundException ignored) {
            return false;
        }

        // Assume that Google Maps is available and working
        return true;
    }

    public static List<MapSource> getMapSources() {
        return mapSources;
    }

    public static boolean isSameActivity(@NonNull final MapSource source1, @NonNull final MapSource source2) {
        final MapProvider provider1 = source1.getMapProvider();
        final MapProvider provider2 = source2.getMapProvider();
        return provider1.equals(provider2) && provider1.isSameActivity(source1, source2);
    }

    public static void addMapviewMenuItems(final Menu menu) {
        final SubMenu parentMenu = menu.findItem(R.id.menu_select_mapview).getSubMenu();

        final int currentSource = Settings.getMapSource().getNumericalId();
        for (int i = 0; i < mapSources.size(); i++) {
            final MapSource mapSource = mapSources.get(i);
            final int id = mapSource.getNumericalId();
            parentMenu.add(R.id.menu_group_map_sources, id, i, mapSource.getName()).setCheckable(true).setChecked(id == currentSource);
        }
        parentMenu.setGroupCheckable(R.id.menu_group_map_sources, true, true);
    }

    /**
     * Return a map source by id.
     *
     * @param id the map source id
     * @return the map source, or <tt>null</tt> if <tt>id</tt> does not correspond to a registered map source
     */
    @Nullable
    public static MapSource getMapSource(final int id) {
        for (final MapSource mapSource : mapSources) {
            if (mapSource.getNumericalId() == id) {
                return mapSource;
            }
        }
        return null;
    }

    /**
     * Return a map source if there is at least one.
     *
     * @return the first map source in the collection, or <tt>null</tt> if there are none registered
     */
    public static MapSource getAnyMapSource() {
        return mapSources.isEmpty() ? null : mapSources.get(0);
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
        final List<MapSource> deletion = new ArrayList<>();
        for (final MapSource mapSource : mapSources) {
            if (mapSource instanceof MapsforgeMapProvider.OfflineMapSource) {
                deletion.add(mapSource);
            }
        }
        mapSources.removeAll(deletion);
    }
}
