package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.downloader.CompanionFileUtils;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
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
import androidx.appcompat.widget.PopupMenu;
import androidx.core.util.Pair;
import androidx.core.view.MenuCompat;

import java.util.ArrayList;
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

    @NonNull
    public static TileProviderFactory getInstance() {
        return Holder.INSTANCE;
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
        parentMenu.add(R.id.menu_group_offlinemaps, R.id.menu_delete_offlinemap, tileProviders.size() + 1, '<' + activity.getString(R.string.delete_offlinemap_title) + '>');
    }

    /**
     * displays a list of offline map sources and deletes selected item after confirmation
     */
    public static void showDeleteMenu(final Activity activity) {
        final int currentSource = Settings.getTileProvider().getNumericalId();

        // build list of available offline map sources except currently active one
        final List<Pair<String, Integer>> list = new ArrayList<>();
        for (AbstractTileProvider tileProvider : tileProviders.values()) {
            if (tileProvider instanceof AbstractMapsforgeVTMOfflineTileProvider && tileProvider.getNumericalId() != currentSource && !(tileProvider instanceof MapsforgeVTMMultiOfflineTileProvider)) {
                list.add(new Pair<>(tileProvider.getTileProviderName(), tileProvider.getNumericalId()));
            }
        }
        if (list.isEmpty()) {
            ActivityMixin.showToast(activity, R.string.no_deletable_offlinemaps);
            return;
        }

        final SimpleDialog.ItemSelectModel<Pair<String, Integer>> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(list)
            .setDisplayMapper((l) -> TextParam.text(l.first))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        SimpleDialog.of(activity).setTitle(TextParam.id(R.string.delete_offlinemap_title))
                .selectSingle(model, (l) -> {
            final AbstractMapsforgeVTMOfflineTileProvider tileProvider = (AbstractMapsforgeVTMOfflineTileProvider) getTileProvider(l.second);
            if (tileProvider != null) {
                final ContentStorage cs = ContentStorage.get();
                final ContentStorage.FileInformation fi = cs.getFileInfo(tileProvider.getMapUri());
                if (fi != null) {
                    SimpleDialog.of(activity).setTitle(TextParam.id(R.string.delete_offlinemap_title)).setMessage(TextParam.text(String.format(activity.getString(R.string.delete_file_confirmation), fi.name))).confirm(() -> {
                        final Uri cf = CompanionFileUtils.companionFileExists(cs.list(PersistableFolder.OFFLINE_MAPS), fi.name);
                        cs.delete(tileProvider.getMapUri());
                        if (cf != null) {
                            cs.delete(cf);
                        }
                        ActivityMixin.showShortToast(activity, String.format(activity.getString(R.string.file_deleted_info), fi.name));
                        MapsforgeMapProvider.getInstance().updateOfflineMaps(); // update legacy NewMap/CGeoMap until they get removed
                        TileProviderFactory.buildTileProviderList(true);
                        ActivityMixin.invalidateOptionsMenu(activity);
                    });
                }
            }
        });
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

        // Google Map based tile providers
        if (isGoogleMapsInstalled()) {
            registerTileProvider(new GoogleMapSource());
            registerTileProvider(new GoogleSatelliteSource());
            registerTileProvider(new GoogleTerrainSource());
        }

        // OSM online tile providers (VTM)
        registerTileProvider(new OsmOrgVTMSource());
        registerTileProvider(new OsmDeVTMSource());
        registerTileProvider(new CyclosmVTMSource());
        registerTileProvider(new OpenTopoMapVTMSource());

        // OSM offline tile providers (VTM)
        final List<ImmutablePair<String, Uri>> offlineMaps =
                CollectionStream.of(ContentStorage.get().list(PersistableFolder.OFFLINE_MAPS, true))
                        .filter(fi -> !fi.isDirectory && fi.name.toLowerCase(Locale.getDefault()).endsWith(FileUtils.MAP_FILE_EXTENSION) && isValidMapFile(fi.uri))
                        .map(fi -> new ImmutablePair<>(StringUtils.capitalize(StringUtils.substringBeforeLast(fi.name, ".")), fi.uri)).toList();
        Collections.sort(offlineMaps, (o1, o2) -> TextUtils.COLLATOR.compare(o1.left, o2.left));
        if (offlineMaps.size() > 1) {
            registerTileProvider(new MapsforgeVTMMultiOfflineTileProvider(offlineMaps));
        }
        if (UserDefinedMapsforgeVTMOnlineSource.isConfigured()) {
            registerTileProvider(new UserDefinedMapsforgeVTMOnlineSource());
        }
        for (ImmutablePair<String, Uri> data : offlineMaps) {
            registerTileProvider(new AbstractMapsforgeVTMOfflineTileProvider(data.left, data.right, 0, 18));   // @todo: get actual values for zoomMin/zoomMax
        }

        // --------------------------------------------------------------------
        // test: show Mapsforge-backed tile providers below the others
        // --------------------------------------------------------------------
        if (Settings.useMapsforgeInUnifiedMap()) {
            // OSM online tile providers
            registerTileProvider(new OsmOrgSource());
            registerTileProvider(new OsmDeSource());
            registerTileProvider(new CyclosmSource());
            registerTileProvider(new OpenTopoMapSource());

            // @todo: combined, user-defined

            // OSM offline tile providers
            for (ImmutablePair<String, Uri> data : offlineMaps) {
                registerTileProvider(new AbstractMapsforgeOfflineTileProvider(data.left + " (MF)", data.right, 2, 18));   // @todo: get actual values for zoomMin/zoomMax
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

    // -----------------------------------------------------------------------------------------------

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull
        private static final TileProviderFactory INSTANCE = new TileProviderFactory();
    }
}
