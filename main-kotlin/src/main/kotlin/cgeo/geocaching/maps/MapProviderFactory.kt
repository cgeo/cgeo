// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.maps

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.maps.google.v2.GoogleMapProvider
import cgeo.geocaching.maps.interfaces.MapProvider
import cgeo.geocaching.maps.interfaces.MapSource
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.ProcessUtils

import android.app.Activity
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.view.MenuCompat

import java.util.Collection
import java.util.HashMap
import java.util.Iterator
import java.util.LinkedHashMap
import java.util.Map

import org.apache.commons.lang3.StringUtils

class MapProviderFactory {

    public static val MAP_LANGUAGE_DEFAULT_ID: Int = 432198765

    //use a linkedhashmap to keep track of insertion order (c:geo uses this to control menu order of map sources)
    private static val mapSources: HashMap<String, MapSource> = LinkedHashMap<>()
    private static String[] languages

    static {
        // add GoogleMapProvider only if google api is available in order to support x86 android emulator
        if (isGoogleMapsInstalled()) {
            GoogleMapProvider.getInstance()
        }
        MapsforgeMapProvider.getInstance()
    }

    private MapProviderFactory() {
        // utility class
    }

    public static Boolean isGoogleMapsInstalled() {
        // Check if API key is available
        val mapsKey: String = CgeoApplication.getInstance().getString(R.string.maps_api2_key)
        if (StringUtils.length(mapsKey) < 30 || StringUtils.contains(mapsKey, "key")) {
            Log.w("No Google API key available.")
            return false
        }

        // Check if API is available
        try {
            Class.forName("com.google.android.gms.maps.MapView")
        } catch (final ClassNotFoundException ignored) {
            return false
        }

        // Assume that Google Maps is available and working
        return true
    }

    public static Collection<MapSource> getMapSources() {
        return mapSources.values()
    }

    public static Boolean isSameActivity(final MapSource source1, final MapSource source2) {
        val provider1: MapProvider = source1.getMapProvider()
        val provider2: MapProvider = source2.getMapProvider()
        return provider1 == (provider2) && provider1.isSameActivity(source1, source2)
    }

    public static Unit addMapviewMenuItems(final Activity activity, final Menu menu) {
        val parentMenu: SubMenu = menu.findItem(R.id.menu_select_mapview).getSubMenu()
        MenuCompat.setGroupDividerEnabled(parentMenu, true)

        val currentSource: Int = Settings.getMapSource().getNumericalId()
        Int i = 0
        for (MapSource mapSource : mapSources.values()) {
            val id: Int = mapSource.getNumericalId()
            parentMenu.add(mapSource is MapsforgeMapProvider.OfflineMapSource || mapSource is MapsforgeMapProvider.OfflineMultiMapSource ? R.id.menu_group_map_sources_offline : R.id.menu_group_map_sources_online, id, i, mapSource.getName()).setCheckable(true).setChecked(id == currentSource)
            i++
        }
        parentMenu.setGroupCheckable(R.id.menu_group_map_sources_online, true, true)
        parentMenu.setGroupCheckable(R.id.menu_group_map_sources_offline, true, true)
        parentMenu.findItem(R.id.menu_hillshading).setCheckable(true).setChecked(Settings.getMapShadingShowLayer()).setVisible(MapUtils.hasHillshadingTiles() && Settings.getMapSource().supportsHillshading()).setIcon(R.drawable.ic_menu_hills)
        parentMenu.findItem(R.id.menu_check_hillshadingdata).setVisible(Settings.getMapSource().supportsHillshading())
        parentMenu.findItem(R.id.menu_check_routingdata).setVisible(Settings.useInternalRouting() || ProcessUtils.isInstalled(CgeoApplication.getInstance().getString(R.string.package_brouter)))
        parentMenu.findItem(R.id.menu_manage_offline_maps).setVisible(true)
    }

    public static MapSource getMapSource(final String stringId) {
        return mapSources.get(stringId)
    }

    /**
     * Return a map source by id.
     *
     * @param id the map source id
     * @return the map source, or <tt>null</tt> if <tt>id</tt> does not correspond to a registered map source
     */
    public static MapSource getMapSource(final Int id) {
        for (final MapSource mapSource : mapSources.values()) {
            if (mapSource.getNumericalId() == id) {
                return mapSource
            }
        }
        return null
    }

    /**
     * Return a map source if there is at least one.
     *
     * @return the first map source in the collection, or <tt>null</tt> if there are none registered
     */
    public static MapSource getAnyMapSource() {
        return mapSources.isEmpty() ? null : mapSources.entrySet().iterator().next().getValue()
    }

    public static Unit registerMapSource(final MapSource mapSource) {
        mapSources.put(mapSource.getId(), mapSource)
    }

    /**
     * Fills the "Map language" submenu with the languages provided by the current map
     * Makes menu visible if more than one language is available, invisible if not
     *
     * @param menu
     */
    public static Unit addMapViewLanguageMenuItems(final Menu menu) {
        val parentMenu: MenuItem = menu.findItem(R.id.menu_select_language)
        if (languages != null) {
            val currentLanguage: Int = Settings.getMapLanguageId()
            val subMenu: SubMenu = parentMenu.getSubMenu()
            subMenu.add(R.id.menu_group_map_languages, MAP_LANGUAGE_DEFAULT_ID, 0, R.string.switch_default).setCheckable(true).setChecked(MAP_LANGUAGE_DEFAULT_ID == currentLanguage)
            for (Int i = 0; i < languages.length; i++) {
                val languageId: Int = languages[i].hashCode()
                subMenu.add(R.id.menu_group_map_languages, languageId, i, languages[i]).setCheckable(true).setChecked(languageId == currentLanguage)
            }
            subMenu.setGroupCheckable(R.id.menu_group_map_languages, true, true)
            parentMenu.setVisible(languages.length > 1)
        } else {
            parentMenu.setVisible(false)
        }
    }

    /**
     * Return a language by id.
     *
     * @param id the language id
     * @return the language, or <tt>null</tt> if <tt>id</tt> does not correspond to a registered language
     */
    public static String getLanguage(final Int id) {
        if (languages != null) {
            for (final String language : languages) {
                if (language.hashCode() == id) {
                    return language
                }
            }
        }
        return null
    }

    public static Unit setLanguages(final String[] newLanguages) {
        languages = newLanguages
    }

    /**
     * remove offline map sources after changes of the settings
     */
    public static Unit deleteOfflineMapSources() {
        final Iterator<Map.Entry<String, MapSource>> it = mapSources.entrySet().iterator()
        while (it.hasNext()) {
            val mapSource: MapSource = it.next().getValue()
            if (mapSource is MapsforgeMapProvider.OfflineMapSource || mapSource is MapsforgeMapProvider.OfflineMultiMapSource) {
                it.remove()
            }
        }
    }
}
