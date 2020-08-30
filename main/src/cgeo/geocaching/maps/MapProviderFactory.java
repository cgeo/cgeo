package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.google.v2.GoogleMapProvider;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class MapProviderFactory {

    public static final int MAP_LANGUAGE_DEFAULT = 432198765;

    private static final ArrayList<MapSource> mapSources = new ArrayList<>();
    private static String[] languages;

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

    public static void addMapviewMenuItems(final Activity activity, final Menu menu) {
        final SubMenu parentMenu = menu.findItem(R.id.menu_select_mapview).getSubMenu();

        final int currentSource = Settings.getMapSource().getNumericalId();
        for (int i = 0; i < mapSources.size(); i++) {
            final MapSource mapSource = mapSources.get(i);
            final int id = mapSource.getNumericalId();
            parentMenu.add(R.id.menu_group_map_sources, id, i, mapSource.getName()).setCheckable(true).setChecked(id == currentSource);
        }
        parentMenu.setGroupCheckable(R.id.menu_group_map_sources, true, true);
        parentMenu.add(R.id.menu_group_map_sources, R.id.menu_download_offlinemap, mapSources.size(), '<' + activity.getString(R.string.downloadmap_title) + '>');
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
     * Fills the "Map language" submenu with the languages provided by the current map
     * Makes menu visible if more than one language is available, invisible if not
     *
     * @param menu
     */
    public static void addMapViewLanguageMenuItems(final Menu menu) {
        final MenuItem parentMenu = menu.findItem(R.id.menu_select_language);
        if (languages != null) {
            final int currentLanguage = Settings.getMapLanguage();
            final SubMenu subMenu = parentMenu.getSubMenu();
            subMenu.add(R.id.menu_group_map_languages, MAP_LANGUAGE_DEFAULT, 0, R.string.switch_default).setCheckable(true).setChecked(MAP_LANGUAGE_DEFAULT == currentLanguage);
            for (int i = 0; i < languages.length; i++) {
                final int languageId = languages[i].hashCode();
                subMenu.add(R.id.menu_group_map_languages, languageId, i, languages[i]).setCheckable(true).setChecked(languageId == currentLanguage);
            }
            subMenu.setGroupCheckable(R.id.menu_group_map_languages, true, true);
            parentMenu.setVisible(languages.length > 1);
        } else {
            parentMenu.setVisible(false);
        }
    }

    /**
     * Return a language by id.
     *
     * @param id the language id
     * @return the language, or <tt>null</tt> if <tt>id</tt> does not correspond to a registered language
     */
    @Nullable
    public static String getLanguage(final int id) {
        if (languages != null) {
            for (final String language : languages) {
                if (language.hashCode() == id) {
                    return language;
                }
            }
        }
        return null;
    }

    public static void setLanguages (final String[] newLanguages) {
        languages = newLanguages;
    }

    /**
     * remove offline map sources after changes of the settings
     */
    public static void deleteOfflineMapSources() {
        final List<MapSource> deletion = new ArrayList<>();
        for (final MapSource mapSource : mapSources) {
            if (mapSource instanceof MapsforgeMapProvider.OfflineMapSource || mapSource instanceof MapsforgeMapProvider.OfflineMultiMapSource) {
                deletion.add(mapSource);
            }
        }
        mapSources.removeAll(deletion);
    }
}
