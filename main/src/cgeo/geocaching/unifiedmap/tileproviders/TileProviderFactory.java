package cgeo.geocaching.unifiedmap.tileproviders;

import static cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider.isValidMapFile;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.unifiedmap.googlemaps.GoogleMaps;
import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeVTM;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.TextUtils;

import android.app.Activity;
import android.net.Uri;
import android.view.Menu;
import android.view.SubMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class TileProviderFactory {
    public static final GoogleMaps MAP_GOOGLE = new GoogleMaps();
    public static final MapsforgeVTM MAP_MAPSFORGE = new MapsforgeVTM();
    private static final HashMap<String, AbstractTileProvider> tileProviders = new LinkedHashMap<>();

    private TileProviderFactory() {
        // Singleton
    }

    @NonNull
    public static TileProviderFactory getInstance() {
        return Holder.INSTANCE;
    }

    public static void addMapviewMenuItems(final Activity activity, final Menu menu) {
        final SubMenu parentMenu = menu.findItem(R.id.menu_select_mapview).getSubMenu();

        final int currentSource = Settings.getMapSource().getNumericalId();
        int i = 0;
        for (AbstractTileProvider tileProvider : tileProviders.values()) {
            final int id = tileProvider.getNumericalId();
            parentMenu.add(R.id.menu_group_map_sources, id, i, tileProvider.getTileProviderName()).setCheckable(true).setChecked(id == currentSource);
            i++;
        }
        parentMenu.setGroupCheckable(R.id.menu_group_map_sources, true, true);
        parentMenu.add(R.id.menu_group_map_sources, R.id.menu_download_offlinemap, tileProviders.size(), '<' + activity.getString(R.string.downloadmap_title) + '>');
    }

    private static void buildTileProviderList() {
        tileProviders.clear();

        // Google Map based tile providers
        // @todo: Check for availability
        registerTileProvider(new GoogleMapSource());
        registerTileProvider(new GoogleSatelliteSource());

        // OSM online tile providers
        registerTileProvider(new OsmDeSource());

        // OSM offline tile providers
        final List<ImmutablePair<String, Uri>> offlineMaps =
            CollectionStream.of(ContentStorage.get().list(PersistableFolder.OFFLINE_MAPS, true))
                .filter(fi -> !fi.isDirectory && fi.name.toLowerCase(Locale.getDefault()).endsWith(FileUtils.MAP_FILE_EXTENSION) && isValidMapFile(fi.uri))
                .map(fi -> new ImmutablePair<>(StringUtils.capitalize(StringUtils.substringBeforeLast(fi.name, ".")), fi.uri)).toList();
        Collections.sort(offlineMaps, (o1, o2) -> TextUtils.COLLATOR.compare(o1.left, o2.left));
        for (ImmutablePair<String, Uri> data : offlineMaps) {
            registerTileProvider(new AbstractMapsforgeOfflineTileProvider(data.left, data.right));
        }
        if (offlineMaps.size() > 0) {
            // @todo: add "combined" map type
        }
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

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull
        private static final TileProviderFactory INSTANCE = new TileProviderFactory();
    }
}
