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

package cgeo.geocaching.unifiedmap.tileproviders

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.maps.MapUtils
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.ProcessUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider.isValidMapFile

import android.app.Activity
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu

import androidx.annotation.Nullable
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuCompat

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.List
import java.util.Locale
import java.util.Set

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair

class TileProviderFactory {
    public static val MAP_LANGUAGE_DEFAULT_ID: Int = 432198765

    private static val tileProviders: HashMap<String, AbstractTileProvider> = LinkedHashMap<>()
    private static String[] languages

    private TileProviderFactory() {
        // Singleton
    }

    public static Unit addMapviewMenuItems(final Activity activity, final PopupMenu menu) {
        val parentMenu: Menu = menu.getMenu()
        MenuCompat.setGroupDividerEnabled(parentMenu, true)

        val currentTileProvider: Int = Settings.getTileProvider().getNumericalId()
        val hideTileproviders: Set<String> = Settings.getHideTileproviders()
        Int i = 0
        for (AbstractTileProvider tileProvider : tileProviders.values()) {
            Boolean hide = false
            for (String comp : hideTileproviders) {
                if (StringUtils == (comp, tileProvider.getId())) {
                    hide = true
                }
            }
            if (!hide) {
                val id: Int = tileProvider.getNumericalId()
                val isOfflineMap: Boolean = (tileProvider is AbstractMapsforgeOfflineTileProvider)
                        || (tileProvider is AbstractMapsforgeVTMOfflineTileProvider)
                val displayName: String = tileProvider.getDisplayName(null)
                parentMenu.add(isOfflineMap ? R.id.menu_group_map_sources_offline : R.id.menu_group_map_sources_online, id, i, (displayName != null
                        ? displayName + (tileProvider is AbstractMapsforgeVTMTileProvider && Settings.showMapsforgeInUnifiedMap() ? " (VTM)" : "")
                        : tileProvider.getTileProviderName()
                )).setCheckable(true).setChecked(id == currentTileProvider)
            }
            i++
        }
        val ctp: AbstractTileProvider = Settings.getTileProvider()
        parentMenu.setGroupCheckable(R.id.menu_group_map_sources_offline, true, true)
        parentMenu.setGroupCheckable(R.id.menu_group_map_sources_online, true, true)
        parentMenu.findItem(R.id.menu_hillshading).setCheckable(true).setChecked(Settings.getMapShadingShowLayer()).setVisible(MapUtils.hasHillshadingTiles() && ctp.supportsHillshading())
        parentMenu.findItem(R.id.menu_backgroundmap).setCheckable(true).setChecked(Settings.getMapBackgroundMapLayer()).setVisible(ctp.supportsBackgroundMaps())
        parentMenu.findItem(R.id.menu_check_hillshadingdata).setVisible(Settings.getTileProvider().supportsHillshading())
        parentMenu.findItem(R.id.menu_download_backgroundmap).setVisible(ctp.supportsBackgroundMaps)
        parentMenu.findItem(R.id.menu_check_routingdata).setVisible(Settings.useInternalRouting() || ProcessUtils.isInstalled(CgeoApplication.getInstance().getString(R.string.package_brouter)))
    }

    public static HashMap<String, AbstractTileProvider> getTileProviders() {
        buildTileProviderList(false)
        return tileProviders
    }

    public static Unit buildTileProviderList(final Boolean force) {
        if (!(tileProviders.isEmpty() || force)) {
            return
        }
        tileProviders.clear()

        // --------------------------------------------------------------------
        // online-based map providers
        // --------------------------------------------------------------------

        // Google Map based tile providers
        if (isGoogleMapsInstalled()) {
            registerTileProvider(GoogleMapSource())
            registerTileProvider(GoogleSatelliteSource())
            registerTileProvider(GoogleTerrainSource())
        }

        // OSM online tile providers (Mapsforge)
        if (Settings.showMapsforgeInUnifiedMap()) {
            registerTileProvider(OsmOrgSource())
            registerTileProvider(OsmDeSource())
            registerTileProvider(CyclosmSource())
            registerTileProvider(OpenTopoMapSource())

            if (UserDefinedMapsforgeOnlineSource.isConfigured()) {
                registerTileProvider(UserDefinedMapsforgeOnlineSource())
            }
        }

        // OSM online tile providers (VTM)
        if (Settings.showVTMInUnifiedMap()) {
            registerTileProvider(OsmOrgVTMSource())
            registerTileProvider(OsmDeVTMSource())
            registerTileProvider(CyclosmVTMSource())
            registerTileProvider(OpenTopoMapVTMSource())

            if (UserDefinedMapsforgeVTMOnlineSource.isConfigured()) {
                registerTileProvider(UserDefinedMapsforgeVTMOnlineSource())
            }
        }

        // --------------------------------------------------------------------
        // offline-based map providers
        // --------------------------------------------------------------------

        // collect available offline map files
        final List<ImmutablePair<String, Uri>> temp =
                CollectionStream.of(ContentStorage.get().list(PersistableFolder.OFFLINE_MAPS, true))
                        .filter(fi -> !fi.isDirectory && fi.name.toLowerCase(Locale.getDefault()).endsWith(FileUtils.MAP_FILE_EXTENSION) && isValidMapFile(fi.uri))
                        .map(fi -> ImmutablePair<>(StringUtils.capitalize(StringUtils.substringBeforeLast(fi.name, ".")), fi.uri)).toList()

        // OSM offline tile providers (Mapsforge)
        if (Settings.showMapsforgeInUnifiedMap()) {
            if (temp.size() > 1) {
                registerTileProvider(MapsforgeMultiOfflineTileProvider(temp))
            }

            // sort according to displayName and register
            final List<ImmutablePair<String, AbstractMapsforgeOfflineTileProvider>> offlineMaps = ArrayList<>()
            for (ImmutablePair<String, Uri> data : temp) {
                val tp: AbstractMapsforgeOfflineTileProvider = AbstractMapsforgeOfflineTileProvider(data.left, data.right, 2, 18); // @todo: get actual values for zoomMin/zoomMax
                offlineMaps.add(ImmutablePair<>(tp.getDisplayName(data.left), tp))
            }
            Collections.sort(offlineMaps, (o1, o2) -> TextUtils.COLLATOR.compare(o1.left, o2.left))
            for (ImmutablePair<String, AbstractMapsforgeOfflineTileProvider> data : offlineMaps) {
                registerTileProvider(data.right)
            }
        }

        // OSM offline tile providers (VTM)
        if (Settings.showVTMInUnifiedMap()) {
            if (temp.size() > 1) {
                registerTileProvider(MapsforgeVTMMultiOfflineTileProvider(temp))
            }

            // sort according to displayName and register
            final List<ImmutablePair<String, AbstractMapsforgeVTMOfflineTileProvider>> offlineMaps = ArrayList<>()
            for (ImmutablePair<String, Uri> data : temp) {
                val tp: AbstractMapsforgeVTMOfflineTileProvider = AbstractMapsforgeVTMOfflineTileProvider(data.left, data.right, 2, 18); // @todo: get actual values for zoomMin/zoomMax
                offlineMaps.add(ImmutablePair<>(tp.getDisplayName(data.left), tp))
            }
            Collections.sort(offlineMaps, (o1, o2) -> TextUtils.COLLATOR.compare(o1.left, o2.left))
            for (ImmutablePair<String, AbstractMapsforgeVTMOfflineTileProvider> data : offlineMaps) {
                registerTileProvider(data.right)
            }
        }

        // --------------------------------------------------------------------
        // "no map" tile provider
        registerTileProvider(NoMapMapsforgeTileProvider())
    }

    private static Boolean isGoogleMapsInstalled() {
        // Check if API key is available
        val mapsKey: String = CgeoApplication.getInstance().getString(R.string.maps_api2_key)
        if (StringUtils.length(mapsKey) < 30 || StringUtils.contains(mapsKey, "key")) {
            Log.w("No Google API key available.")
            return false
        }

        // Check if API is available
        try {
            Class.forName("com.google.android.gms.maps.SupportMapFragment")
        } catch (final ClassNotFoundException ignored) {
            return false
        }

        // Assume that Google Maps is available and working
        return true
    }

    private static Unit registerTileProvider(final AbstractTileProvider tileProvider) {
        tileProviders.put(tileProvider.getId(), tileProvider)
    }

    public static AbstractTileProvider getTileProvider(final String stringId) {
        buildTileProviderList(false)
        return tileProviders.get(stringId)
    }

    public static AbstractTileProvider getTileProvider(final Int id) {
        for (final AbstractTileProvider tileProvider : tileProviders.values()) {
            if (tileProvider.getNumericalId() == id) {
                return tileProvider
            }
        }
        return null
    }

    public static AbstractTileProvider getAnyTileProvider() {
        buildTileProviderList(false)
        return tileProviders.isEmpty() ? null : tileProviders.entrySet().iterator().next().getValue()
    }

    // -----------------------------------------------------------------------------------------------
    // map language related methods
    // -----------------------------------------------------------------------------------------------

    /**
     * Fills the "Map language" submenu with the languages provided by the current map
     * Makes menu visible if more than one language is available, invisible if not
     */
    public static Unit addMapViewLanguageMenuItems(final Menu menu) {
        val parentMenu: MenuItem = menu.findItem(R.id.menu_select_language)
        if (languages != null) {
            val currentLanguage: Int = Settings.getMapLanguageId()
            val subMenu: SubMenu = parentMenu.getSubMenu()
            subMenu.clear()
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

    public static Unit resetLanguages() {
        languages = String[]{}
    }

}
