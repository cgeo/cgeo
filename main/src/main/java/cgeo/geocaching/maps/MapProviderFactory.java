package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.databinding.LocSwitchActionBinding;
import cgeo.geocaching.downloader.CompanionFileUtils;
import cgeo.geocaching.maps.google.v2.GoogleMapProvider;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.core.view.MenuCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class MapProviderFactory {

    public static final int MAP_LANGUAGE_DEFAULT_ID = 432198765;

    //use a linkedhashmap to keep track of insertion order (c:geo uses this to control menu order of map sources)
    private static final HashMap<String, MapSource> mapSources = new LinkedHashMap<>();
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

    public static Collection<MapSource> getMapSources() {
        return mapSources.values();
    }

    public static boolean isSameActivity(@NonNull final MapSource source1, @NonNull final MapSource source2) {
        final MapProvider provider1 = source1.getMapProvider();
        final MapProvider provider2 = source2.getMapProvider();
        return provider1.equals(provider2) && provider1.isSameActivity(source1, source2);
    }

    public static void addMapviewMenuItems(final Activity activity, final Menu menu) {
        final SubMenu parentMenu = menu.findItem(R.id.menu_select_mapview).getSubMenu();
        MenuCompat.setGroupDividerEnabled(parentMenu, true);

        final int currentSource = Settings.getMapSource().getNumericalId();
        int i = 0;
        for (MapSource mapSource : mapSources.values()) {
            final int id = mapSource.getNumericalId();
            parentMenu.add(R.id.menu_group_map_sources, id, i, mapSource.getName()).setCheckable(true).setChecked(id == currentSource);
            i++;
        }
        parentMenu.setGroupCheckable(R.id.menu_group_map_sources, true, true);
        parentMenu.add(R.id.menu_group_offlinemaps, R.id.menu_download_offlinemap, mapSources.size(), '<' + activity.getString(R.string.downloadmap_title) + '>');
        parentMenu.add(R.id.menu_group_offlinemaps, R.id.menu_delete_offlinemap, mapSources.size() + 1, '<' + activity.getString(R.string.delete_offlinemap_title) + '>');
    }

    /** displays a list of offline map sources and deletes selected item after confirmation */
    public static void showDeleteMenu(final Activity activity) {
        final int currentSource = Settings.getMapSource().getNumericalId();

        // build list of available offline map sources except currently active one
        final List<Pair<String, Integer>> list = new ArrayList<>();
        for (MapSource mapSource : mapSources.values()) {
            if (mapSource instanceof MapsforgeMapProvider.OfflineMapSource && mapSource.getNumericalId() != currentSource) {
                list.add(new Pair<>(mapSource.getName(), mapSource.getNumericalId()));
            }
        }
        if (list.isEmpty()) {
            ActivityMixin.showToast(activity, R.string.no_deletable_offlinemaps);
            return;
        }

        SimpleDialog.of(activity).setTitle(TextParam.id(R.string.delete_offlinemap_title)).selectSingle(list, (l, pos) -> TextParam.text(l.first), -1, SimpleDialog.SingleChoiceMode.NONE, (l, pos) -> {
            final MapsforgeMapProvider.OfflineMapSource mapSource = (MapsforgeMapProvider.OfflineMapSource) getMapSource(l.second);
            if (mapSource != null) {
                final ContentStorage cs = ContentStorage.get();
                final ContentStorage.FileInformation fi = cs.getFileInfo(mapSource.getMapUri());
                if (fi != null) {
                    SimpleDialog.of(activity).setTitle(TextParam.id(R.string.delete_offlinemap_title)).setMessage(TextParam.text(String.format(activity.getString(R.string.delete_file_confirmation), fi.name))).confirm((dialog, which) -> {
                        final Uri cf = CompanionFileUtils.companionFileExists(cs.list(PersistableFolder.OFFLINE_MAPS), fi.name);
                        cs.delete(mapSource.getMapUri());
                        if (cf != null) {
                            cs.delete(cf);
                        }
                        ActivityMixin.showShortToast(activity, String.format(activity.getString(R.string.file_deleted_info), fi.name));
                        MapsforgeMapProvider.getInstance().updateOfflineMaps();
                        if (Settings.showUnifiedMap()) {
                            TileProviderFactory.buildTileProviderList(true);
                        }
                        ActivityMixin.invalidateOptionsMenu(activity);
                    });
                }
            }
        });
    }

    public static CheckBox createLocSwitchMenuItem(final Activity activity, final Menu menu) {
        final MenuItem item = menu.findItem(R.id.menu_toggle_mypos);
        final LocSwitchActionBinding binding = LocSwitchActionBinding.inflate(LayoutInflater.from(activity));
        binding.getRoot().setOnClickListener(v -> binding.locSwitch.performClick());
        item.setActionView(binding.getRoot());

        return binding.locSwitch;
    }

    @Nullable
    public static MapSource getMapSource(final String stringId) {
        return mapSources.get(stringId);
    }

    /**
     * Return a map source by id.
     *
     * @param id the map source id
     * @return the map source, or <tt>null</tt> if <tt>id</tt> does not correspond to a registered map source
     */
    @Nullable
    public static MapSource getMapSource(final int id) {
        for (final MapSource mapSource : mapSources.values()) {
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
        return mapSources.isEmpty() ? null : mapSources.entrySet().iterator().next().getValue();
    }

    public static void registerMapSource(final MapSource mapSource) {
        mapSources.put(mapSource.getId(), mapSource);
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
            final int currentLanguage = Settings.getMapLanguageId();
            final SubMenu subMenu = parentMenu.getSubMenu();
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

    /**
     * remove offline map sources after changes of the settings
     */
    public static void deleteOfflineMapSources() {
        final Iterator<Map.Entry<String, MapSource>> it = mapSources.entrySet().iterator();
        while (it.hasNext()) {
            final MapSource mapSource = it.next().getValue();
            if (mapSource instanceof MapsforgeMapProvider.OfflineMapSource || mapSource instanceof MapsforgeMapProvider.OfflineMultiMapSource) {
                it.remove();
            }
        }
    }
}
