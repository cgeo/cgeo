package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.AbstractMapProvider;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.MultiRendererLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.RendererLayer;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileException;


public final class MapsforgeMapProvider extends AbstractMapProvider {

    private static final String OFFLINE_MAP_DEFAULT_ATTRIBUTION = "---";
    private static final Map<Uri, String> OFFLINE_MAP_ATTRIBUTIONS = new HashMap<>();

    private MapItemFactory mapItemFactory = new MapsforgeMapItemFactory();


    private MapsforgeMapProvider() {
        final Resources resources = CgeoApplication.getInstance().getResources();

        //register fixed maps
        registerMapSource(new OsmMapSource(this, resources.getString(R.string.map_source_osm_mapnik)));
        registerMapSource(new OsmdeMapSource(this, resources.getString(R.string.map_source_osm_osmde)));
        registerMapSource(new CyclosmMapSource(this, resources.getString(R.string.map_source_osm_cyclosm)));
        registerMapSource(new MapyCzMapSource(this, resources.getString(R.string.map_source_mapy_cz)));

        //get notified if Offline Maps directory changes
        PersistableFolder.OFFLINE_MAPS.registerChangeListener(this, pf -> updateOfflineMaps());

        //initialize offline maps (necessary here in constructor only so that initial setMapSource will succeed)
        updateOfflineMaps();
    }

    private static final class Holder {
        private static final MapsforgeMapProvider INSTANCE = new MapsforgeMapProvider();
    }

    public static MapsforgeMapProvider getInstance() {
        return Holder.INSTANCE;
    }

    public static List<ContentStorage.FileInformation> getOfflineMaps() {
        return ContentStorage.get().list(PersistableFolder.OFFLINE_MAPS, true);

    }

    @Override
    public boolean isSameActivity(final MapSource source1, final MapSource source2) {
        return source1.getNumericalId() == source2.getNumericalId() || (!(source1 instanceof OfflineMapSource) && !(source2 instanceof OfflineMapSource));
    }

    @Override
    public Class<? extends AppCompatActivity> getMapClass() {
        mapItemFactory = new MapsforgeMapItemFactory();
        return NewMap.class;
    }

    @Override
    public int getMapViewId() {
        return R.id.mfmapv5;
    }

    @Override
    public int getMapAttributionViewId() {
        return R.id.map_attribution;
    }

    @Override
    public MapItemFactory getMapItemFactory() {
        return mapItemFactory;
    }

    /**
     * Offline maps use the hash of the filename as ID. That way changed files can easily be detected. Also we do no
     * longer need to differentiate between internal map sources and offline map sources, as they all just have an
     * numerical ID (based on the hash code).
     */
    public static final class OfflineMapSource extends AbstractMapsforgeMapSource {

        private final Uri mapUri;

        public OfflineMapSource(final Uri mapUri, final MapProvider mapProvider, final String name) {
            super(mapProvider, name);
            this.mapUri = mapUri;
        }

        public Uri getMapUri() {
            return mapUri;
        }

        @Override
        @NonNull
        public String getId() {
            return super.getId() + ":" + mapUri.getLastPathSegment();
        }

        @Override
        public boolean isAvailable() {
            return isValidMapFile(mapUri);
        }

        /**
         * Create new render layer, if mapfile exists
         */
        @Override
        public ITileLayer createTileLayer(final TileCache tileCache, final IMapViewPosition mapViewPosition) {
            final InputStream mapStream = createMapFileInputStream(this.mapUri);
            if (mapStream == null) {
                return null;
            }
            final MapFile mf = createMapFile(String.valueOf(this.mapUri), mapStream);
            if (mf != null) {
                MapProviderFactory.setLanguages(mf.getMapLanguages());
                return new RendererLayer(tileCache, mf, mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
            }
            MapsforgeMapProvider.getInstance().invalidateMapUri(mapUri);
            return null;
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return new ImmutablePair<>(MapsforgeMapProvider.getInstance().getAttributionFor(this.mapUri), true);
        }

    }

    public static final class MapyCzMapSource extends AbstractMapsforgeMapSource {

        MapyCzMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, TileSourceMapyCz.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context context) {
            return new ImmutablePair<>(context.getString(R.string.map_attribution_mapy_cz_html), false);
        }
    }

    public static final class CyclosmMapSource extends AbstractMapsforgeMapSource {

        public CyclosmMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, TileSourceCyclosm.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return new ImmutablePair<>(ctx.getString(R.string.map_attribution_cyclosm_html), false);
        }

    }

    public static final class OsmMapSource extends AbstractMapsforgeMapSource {

        public OsmMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, OpenStreetMapMapnik.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return new ImmutablePair<>(ctx.getString(R.string.map_attribution_openstreetmapde_html), false);
        }

    }


    public static final class OsmdeMapSource extends AbstractMapsforgeMapSource {

        public OsmdeMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, TileSourceOsmde.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return new ImmutablePair<>(ctx.getString(R.string.map_attribution_openstreetmapde_html), false);
        }

    }

    public static final class OfflineMultiMapSource extends AbstractMapsforgeMapSource {
        private final List<ImmutablePair<String, Uri>> mapUris;

        public OfflineMultiMapSource(final List<ImmutablePair<String, Uri>> mapUris, final MapProvider mapProvider, final String name) {
            super(mapProvider, name);
            this.mapUris = mapUris;
        }

        @Override
        public boolean isAvailable() {
            boolean isValid = true;
            for (ImmutablePair<String, Uri> mapUri : mapUris) {
                isValid &= isValidMapFile(mapUri.right);
            }
            return isValid;
        }

        /**
         * Create new render layer, if mapfiles exist
         */
        @Override
        public ITileLayer createTileLayer(final TileCache tileCache, final IMapViewPosition mapViewPosition) {
            final List<MapFile> mapFiles = new ArrayList<>();
            for (ImmutablePair<String, Uri> fileName : mapUris) {
                final InputStream mapStream = createMapFileInputStream(fileName.right);
                if (mapStream == null) {
                    continue;
                }
                final MapFile mf = createMapFile(String.valueOf(fileName.right), mapStream);
                if (mf != null) {
                    mapFiles.add(mf);
                } else {
                    MapsforgeMapProvider.getInstance().invalidateMapUri(fileName.right);
                }
            }

            return new MultiRendererLayer(tileCache, mapFiles, mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {

            final StringBuilder attributation = new StringBuilder();
            for (ImmutablePair<String, Uri> mapUri : mapUris) {
                attributation.append("<p><b>").append(mapUri.left).append("</b>:<br>");
                attributation.append(MapsforgeMapProvider.getInstance().getAttributionFor(mapUri.right));
                attributation.append("</p>");
            }
            return new ImmutablePair<>(attributation.toString(), true);
        }

    }

    @NonNull
    public String getAttributionFor(final Uri filePath) {
        final String att = getAttributionIfValidFor(filePath);
        return att == null ? OFFLINE_MAP_DEFAULT_ATTRIBUTION : att;
    }

    /**
     * checks whether the given Uri is a valid map file.
     * Thie methos uses cached results from previous checks
     * Note: this method MUST be static because it is indirectly used in MapsforgeMapProvider-constructur
     */
    public static boolean isValidMapFile(final Uri filePath) {
        return getAttributionIfValidFor(filePath) != null;
    }

    private static String getAttributionIfValidFor(final Uri filePath) {

        if (OFFLINE_MAP_ATTRIBUTIONS.containsKey(filePath)) {
            return OFFLINE_MAP_ATTRIBUTIONS.get(filePath);
        }
        final InputStream mapStream = createMapFileInputStream(filePath);
        if (mapStream == null) {
            //do NOT put this in cache, might be a temporary access problem
            return null;
        }

        OFFLINE_MAP_ATTRIBUTIONS.put(filePath, readAttributionFromMapFileIfValid(String.valueOf(filePath), mapStream));
        return OFFLINE_MAP_ATTRIBUTIONS.get(filePath);
    }

    private void invalidateMapUri(final Uri filePath) {
        OFFLINE_MAP_ATTRIBUTIONS.put(filePath, null);
    }

    /**
     * Tries to open given uri as a mapfile.
     * If mapfile is invalid in any way (not available, not readable, wrong version, ...), then null is returned.
     * If mapfile is valid, then its attribution is read and returned (or a default attribution value in case attribution is null)
     */
    @Nullable
    private static String readAttributionFromMapFileIfValid(final String mapFileCtx, final InputStream mapStream) {

        MapFile mapFile = null;
        try {
            mapFile = createMapFile(mapFileCtx, mapStream);
            if (mapFile != null && mapFile.getMapFileInfo() != null && mapFile.getMapFileInfo().fileVersion <= 5) {
                if (StringUtils.isNotBlank(mapFile.getMapFileInfo().comment)) {
                    return mapFile.getMapFileInfo().comment;
                }
                if (StringUtils.isNotBlank(mapFile.getMapFileInfo().createdBy)) {
                    return mapFile.getMapFileInfo().createdBy;
                }
                //map file is valid but has no attribution -> return default value
                return OFFLINE_MAP_DEFAULT_ATTRIBUTION;
            }
        } catch (MapFileException ex) {
            Log.w(String.format("Exception reading mapfile '%s'", mapFileCtx), ex);
        } finally {
            closeMapFileQuietly(mapFile);
        }
        return null;
    }

    private static InputStream createMapFileInputStream(final Uri mapUri) {
        if (mapUri == null) {
            return null;
        }
        return ContentStorage.get().openForRead(mapUri, true);
    }

    private static MapFile createMapFile(final String mapFileCtx, final InputStream fis) {

        if (fis != null) {
            try {
                return new MapFile((FileInputStream) fis, 0, Settings.getMapLanguage());
            } catch (MapFileException mfe) {
                Log.e("Problem opening map file '" + mapFileCtx + "'", mfe);
            }
        }
        return null;
    }

    private static void closeMapFileQuietly(final MapFile mapFile) {
        if (mapFile != null) {
            mapFile.close();
        }
    }

    public void updateOfflineMaps() {
        updateOfflineMaps(null);
    }

    public void updateOfflineMaps(final Uri offlineMapToSet) {
        MapSource msToSet = null;
        MapProviderFactory.deleteOfflineMapSources();
        final Resources resources = CgeoApplication.getInstance().getResources();
        final List<ImmutablePair<String, Uri>> offlineMaps =
                CollectionStream.of(getOfflineMaps())
                        .filter(fi -> !fi.isDirectory && fi.name.toLowerCase(Locale.getDefault()).endsWith(FileUtils.MAP_FILE_EXTENSION) && isValidMapFile(fi.uri))
                        .map(fi -> new ImmutablePair<>(fi.name, fi.uri)).toList();
        Collections.sort(offlineMaps, (o1, o2) -> TextUtils.COLLATOR.compare(o1.left, o2.left));
        if (offlineMaps.size() > 1) {
            registerMapSource(new OfflineMultiMapSource(offlineMaps, this, resources.getString(R.string.map_source_osm_offline_combined)));
        }
        for (final ImmutablePair<String, Uri> mapFile : offlineMaps) {
            final String mapName = StringUtils.capitalize(StringUtils.substringBeforeLast(mapFile.left, "."));
            final OfflineMapSource offlineMapSource = new OfflineMapSource(mapFile.right, this, mapName + " (" + resources.getString(R.string.map_source_osm_offline) + ")");
            registerMapSource(offlineMapSource);
            if (offlineMapToSet != null && offlineMapToSet.equals(offlineMapSource.getMapUri())) {
                msToSet = offlineMapSource;
            }
        }
        if (msToSet != null) {
            Settings.setMapSource(msToSet);
        }
    }

}
