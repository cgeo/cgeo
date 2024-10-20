package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import static cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider.isValidMapFile;

import android.app.Activity;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuCompat;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class TileProviderFactory {
    public static final int MAP_LANGUAGE_DEFAULT_ID = 432198765;

    private static final HashMap<String, AbstractTileProvider> tileProviders = new LinkedHashMap<>();
    private static String[] languages;

    private TileProviderFactory() {
        // Singleton
    }

    public static void addMapviewMenuItems(final Activity activity, final PopupMenu menu) {
        final Menu parentMenu = menu.getMenu();
        MenuCompat.setGroupDividerEnabled(parentMenu, true);

        final int currentTileProvider = Settings.getTileProvider().getNumericalId();
        final Set<String> hideTileproviders = Settings.getHideTileproviders();
        int i = 0;
        for (AbstractTileProvider tileProvider : tileProviders.values()) {
            boolean hide = false;
            for (String comp : hideTileproviders) {
                if (StringUtils.equals(comp, tileProvider.getId())) {
                    hide = true;
                }
            }
            if (!hide) {
                final int id = tileProvider.getNumericalId();
                parentMenu.add(tileProvider instanceof AbstractMapsforgeOfflineTileProvider ? R.id.menu_group_map_sources_offline : R.id.menu_group_map_sources_online, id, i, tileProvider.getTileProviderName()).setCheckable(true).setChecked(id == currentTileProvider);
            }
            i++;
        }
        parentMenu.setGroupCheckable(R.id.menu_group_map_sources_offline, true, true);
        parentMenu.setGroupCheckable(R.id.menu_group_map_sources_online, true, true);
        parentMenu.add(R.id.menu_group_offlinemaps, R.id.menu_hillshading, tileProviders.size(), activity.getString(R.string.settings_hillshading_enable)).setCheckable(true).setChecked(Settings.getMapShadingShowLayer()).setVisible(Settings.getMapShadingEnabled() && Settings.getTileProvider().supportsHillshading());
        parentMenu.add(R.id.menu_group_offlinemaps, R.id.menu_download_offlinemap, tileProviders.size(), '<' + activity.getString(R.string.downloadmap_title) + '>');
    }

    public static HashMap<String, AbstractTileProvider> getTileProviders() {
        buildTileProviderList(false);
        return tileProviders;
    }

    public static void buildTileProviderList(final boolean force) {
        if (!(tileProviders.isEmpty() || force)) {
            return;
        }
        tileProviders.clear();

        // --------------------------------------------------------------------
        // online-based map providers
        // --------------------------------------------------------------------

        // Google Map based tile providers
        if (isGoogleMapsInstalled()) {
            registerTileProvider(new GoogleMapSource());
            registerTileProvider(new GoogleSatelliteSource());
            registerTileProvider(new GoogleTerrainSource());
        }

        // OSM online tile providers (Mapsforge)
        if (Settings.showMapsforgeInUnifiedMap()) {
            registerTileProvider(new OsmOrgSource());
            registerTileProvider(new OsmDeSource());
            registerTileProvider(new CyclosmSource());
            registerTileProvider(new OpenTopoMapSource());

            if (UserDefinedMapsforgeOnlineSource.isConfigured()) {
                registerTileProvider(new UserDefinedMapsforgeOnlineSource());
            }
        }

        // OSM online tile providers (VTM)
        if (Settings.showVTMInUnifiedMap()) {
            registerTileProvider(new OsmOrgVTMSource());
            registerTileProvider(new OsmDeVTMSource());
            registerTileProvider(new CyclosmVTMSource());
            registerTileProvider(new OpenTopoMapVTMSource());

            if (UserDefinedMapsforgeVTMOnlineSource.isConfigured()) {
                registerTileProvider(new UserDefinedMapsforgeVTMOnlineSource());
            }
        }

        // --------------------------------------------------------------------
        // offline-based map providers
        // --------------------------------------------------------------------

        // collect available offline map files
        final List<ImmutablePair<String, Uri>> offlineMaps =
                CollectionStream.of(ContentStorage.get().list(PersistableFolder.OFFLINE_MAPS, true))
                        .filter(fi -> !fi.isDirectory && fi.name.toLowerCase(Locale.getDefault()).endsWith(FileUtils.MAP_FILE_EXTENSION) && isValidMapFile(fi.uri))
                        .map(fi -> new ImmutablePair<>(StringUtils.capitalize(StringUtils.substringBeforeLast(fi.name, ".")), fi.uri)).toList();
        Collections.sort(offlineMaps, (o1, o2) -> TextUtils.COLLATOR.compare(o1.left, o2.left));

        // OSM offline tile providers (Mapsforge)
        if (Settings.showMapsforgeInUnifiedMap()) {
            if (offlineMaps.size() > 1) {
                registerTileProvider(new MapsforgeMultiOfflineTileProvider(offlineMaps));
            }
            for (ImmutablePair<String, Uri> data : offlineMaps) {
                registerTileProvider(new AbstractMapsforgeOfflineTileProvider(data.left, data.right, 2, 18));   // @todo: get actual values for zoomMin/zoomMax
            }
        }

        // OSM offline tile providers (VTM)
        if (Settings.showVTMInUnifiedMap()) {
            if (offlineMaps.size() > 1) {
                registerTileProvider(new MapsforgeVTMMultiOfflineTileProvider(offlineMaps));
            }
            for (ImmutablePair<String, Uri> data : offlineMaps) {
                registerTileProvider(new AbstractMapsforgeVTMOfflineTileProvider(data.left, data.right, 2, 18));   // @todo: get actual values for zoomMin/zoomMax
            }
        }
        // --------------------------------------------------------------------
    }

    private static boolean isGoogleMapsInstalled() {
        // Check if API key is available
        final String mapsKey = CgeoApplication.getInstance().getString(R.string.maps_api2_key);
        if (StringUtils.length(mapsKey) < 30 || StringUtils.contains(mapsKey, "key")) {
            Log.w("No Google API key available.");
            return false;
        }

        // Check if API is available
        try {
            Class.forName("com.google.android.gms.maps.SupportMapFragment");
        } catch (final ClassNotFoundException ignored) {
            return false;
        }

        // Assume that Google Maps is available and working
        return true;
    }

    private static void registerTileProvider(final AbstractTileProvider tileProvider) {
        tileProviders.put(tileProvider.getId(), tileProvider);
    }

    @Nullable
    public static AbstractTileProvider getTileProvider(final String stringId) {
        buildTileProviderList(false);
        return tileProviders.get(stringId);
    }

    @Nullable
    public static AbstractTileProvider getTileProvider(final int id) {
        for (final AbstractTileProvider tileProvider : tileProviders.values()) {
            if (tileProvider.getNumericalId() == id) {
                return tileProvider;
            }
        }
        return null;
    }

    public static AbstractTileProvider getAnyTileProvider() {
        buildTileProviderList(false);
        return tileProviders.isEmpty() ? null : tileProviders.entrySet().iterator().next().getValue();
    }

    // -----------------------------------------------------------------------------------------------
    // map language related methods
    // -----------------------------------------------------------------------------------------------

    /**
     * Fills the "Map language" submenu with the languages provided by the current map
     * Makes menu visible if more than one language is available, invisible if not
     */
    public static void addMapViewLanguageMenuItems(final Menu menu) {
        final MenuItem parentMenu = menu.findItem(R.id.menu_select_language);
        if (languages != null) {
            final int currentLanguage = Settings.getMapLanguageId();
            final SubMenu subMenu = parentMenu.getSubMenu();
            subMenu.clear();
            subMenu.add(R.id.menu_group_map_languages, MAP_LANGUAGE_DEFAULT_ID, 0, R.string.switch_default).setCheckable(true).setChecked(MAP_LANGUAGE_DEFAULT_ID == currentLanguage);
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

    public static void setLanguages(final String[] newLanguages) {
        languages = newLanguages;
    }

    public static void resetLanguages() {
        languages = new String[]{};
    }

}
