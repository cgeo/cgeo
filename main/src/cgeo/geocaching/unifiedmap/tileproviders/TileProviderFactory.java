package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.unifiedmap.googlemaps.GoogleMaps;
import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeVTM;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class TileProviderFactory {
    public static final int MAP_LANGUAGE_DEFAULT_ID = 432198765;

    public static final GoogleMaps MAP_GOOGLE = new GoogleMaps();
    public static final MapsforgeVTM MAP_MAPSFORGE = new MapsforgeVTM();
    private static final HashMap<String, AbstractTileProvider> tileProviders = new LinkedHashMap<>();
    private static String[] languages;

    private TileProviderFactory() {
        // Singleton
    }

    @NonNull
    public static TileProviderFactory getInstance() {
        return Holder.INSTANCE;
    }

    public static void addMapviewMenuItems(final Activity activity, final Menu menu) {
        final SubMenu parentMenu = menu.findItem(R.id.menu_select_mapview).getSubMenu();

        final int currentTileProvider = Settings.getTileProvider().getNumericalId();
        int i = 0;
        for (AbstractTileProvider tileProvider : tileProviders.values()) {
            final int id = tileProvider.getNumericalId();
            parentMenu.add(R.id.menu_group_map_sources, id, i, tileProvider.getTileProviderName()).setCheckable(true).setChecked(id == currentTileProvider);
            i++;
        }
        parentMenu.setGroupCheckable(R.id.menu_group_map_sources, true, true);
        parentMenu.add(R.id.menu_group_map_sources, R.id.menu_download_offlinemap, tileProviders.size(), '<' + activity.getString(R.string.downloadmap_title) + '>');
    }

    private static void buildTileProviderList() {
        tileProviders.clear();

        // Google Map based tile providers
        if (isGoogleMapsInstalled()) {
            registerTileProvider(new GoogleMapSource());
            registerTileProvider(new GoogleSatelliteSource());
        }

        // OSM online tile providers
        registerTileProvider(new OsmOrgSource());
        registerTileProvider(new OsmDeSource());
        registerTileProvider(new CyclosmSource());
        registerTileProvider(new MapyCzSource());

        // OSM offline tile providers
        final List<ImmutablePair<String, Uri>> offlineMaps =
            CollectionStream.of(ContentStorage.get().list(PersistableFolder.OFFLINE_MAPS, true))
                .filter(fi -> !fi.isDirectory && fi.name.toLowerCase(Locale.getDefault()).endsWith(FileUtils.MAP_FILE_EXTENSION) && isValidMapFile(fi.uri))
                .map(fi -> new ImmutablePair<>(StringUtils.capitalize(StringUtils.substringBeforeLast(fi.name, ".")), fi.uri)).toList();
        Collections.sort(offlineMaps, (o1, o2) -> TextUtils.COLLATOR.compare(o1.left, o2.left));
        for (ImmutablePair<String, Uri> data : offlineMaps) {
            registerTileProvider(new AbstractMapsforgeOfflineTileProvider(data.left, data.right, 0, 18));   // @todo: get actual values for zoomMin/zoomMax
        }
        if (offlineMaps.size() > 1) {
            registerTileProvider(new MapsforgeMultiOfflineTileProvider(offlineMaps));
        }
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
        if (tileProviders.isEmpty()) {
            buildTileProviderList();
        }
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
        if (tileProviders.isEmpty()) {
            buildTileProviderList();
        }
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

    // -----------------------------------------------------------------------------------------------
    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull
        private static final TileProviderFactory INSTANCE = new TileProviderFactory();
    }
}
