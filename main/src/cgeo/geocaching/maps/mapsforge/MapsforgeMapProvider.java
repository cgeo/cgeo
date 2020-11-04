package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.AbstractMapProvider;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.maps.mapsforge.v6.layers.DownloadLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.MultiRendererLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.RendererLayer;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.PublicLocalFolder;
import cgeo.geocaching.storage.PublicLocalStorage;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileException;
import org.mapsforge.v3.android.maps.mapgenerator.MapGeneratorInternal;



public final class MapsforgeMapProvider extends AbstractMapProvider {

    public static final String MAPSFORGE_MAPNIK_ID = "MAPSFORGE_MAPNIK";
    public static final String MAPSFORGE_OSMDE_ID = "MAPSFORGE_OSMDE";
    public static final String MAPSFORGE_CYCLOSM_ID = "MAPSFORGE_CYCLOSM";

    private MapItemFactory mapItemFactory = new MapsforgeMapItemFactory();

    private MapsforgeMapProvider() {
        final Resources resources = CgeoApplication.getInstance().getResources();

        registerMapSource(new MapsforgeMapSource(MAPSFORGE_MAPNIK_ID, this, resources.getString(R.string.map_source_osm_mapnik), MapGeneratorInternal.MAPNIK));

        if (!Settings.useOldMapsforgeAPI()) {
            registerMapSource(new OsmdeMapSource(MAPSFORGE_OSMDE_ID, this, resources.getString(R.string.map_source_osm_osmde), MapGeneratorInternal.MAPNIK));
            registerMapSource(new CyclosmMapSource(MAPSFORGE_CYCLOSM_ID, this, resources.getString(R.string.map_source_osm_cyclosm), MapGeneratorInternal.MAPNIK));
        }

        updateOfflineMaps();
    }

    private static final class Holder {
        private static final MapsforgeMapProvider INSTANCE = new MapsforgeMapProvider();
    }

    public static MapsforgeMapProvider getInstance() {
        return Holder.INSTANCE;
    }

    public static List<ImmutablePair<String, Uri>> getOfflineMaps() {
        //TODO
        return PublicLocalStorage.get().listDocuments(PublicLocalFolder.OFFLINE_MAP_DEFAULT);

    }

//    public static List<String> getOfflineMaps() {
//        final String directoryPath = Settings.getMapFileDirectory();
//        if (StringUtils.isBlank(directoryPath)) {
//            return Collections.emptyList();
//        }
//
//        final File directory = new File(directoryPath);
//        if (directory.isDirectory()) {
//            try {
//                final List<String> mapFileList = new ArrayList<>();
//                final File[] files = directory.listFiles();
//                if (ArrayUtils.isNotEmpty(files)) {
//                    for (final File file : files) {
//                        if (file.getName().endsWith(".map") && isValidMapFile(file.getAbsolutePath())) {
//                            mapFileList.add(file.getAbsolutePath());
//                        }
//                    }
//                    Collections.sort(mapFileList, TextUtils.COLLATOR);
//                }
//                return mapFileList;
//            } catch (final Exception e) {
//                Log.e("MapsforgeMapProvider.getOfflineMaps: ", e);
//            }
//        }
//        return Collections.emptyList();
//    }

    public static boolean isValidMapFile(final Uri mapUri) {

        if (!PublicLocalStorage.get().checkUriForRead(mapUri)) {
            return false;
        }

        try {
            final MapFile mapFile = createMapFile(mapUri);
            return mapFile.getMapFileInfo().fileVersion <= 3 || !Settings.useOldMapsforgeAPI();
        } catch (MapFileException ex) {
            Log.w(String.format("Exception reading mapfile '%s'", mapUri.toString()), ex);
        }
        return false;
    }

    @Override
    public boolean isSameActivity(final MapSource source1, final MapSource source2) {
        return source1.getNumericalId() == source2.getNumericalId() || (!(source1 instanceof OfflineMapSource) && !(source2 instanceof OfflineMapSource));
    }

    @Override
    public Class<? extends Activity> getMapClass() {
        mapItemFactory = new MapsforgeMapItemFactory();
        return Settings.useOldMapsforgeAPI() ? MapsforgeMapActivity.class : NewMap.class;
    }

    @Override
    public int getMapViewId() {
        return R.id.mfmap;
    }

    @Override
    public int getMapLayoutId() {
        return R.layout.map_mapsforge;
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
    public static final class OfflineMapSource extends MapsforgeMapSource {

        private final Uri mapUri;

        public OfflineMapSource(final String id, final Uri mapUri, final MapProvider mapProvider, final String name, final MapGeneratorInternal generator) {
            super(id, mapProvider, name, generator);
            this.mapUri = mapUri;
        }

        @Override
        public boolean isAvailable() {
            return isValidMapFile(mapUri);
        }

        public Uri getMapUri() {
            return mapUri;
        }

        /** Create new render layer, if mapfile exists */
        @Override
        public ITileLayer createTileLayer(final TileCache tileCache, final IMapViewPosition mapViewPosition) {
            final MapFile mf = createMapFile(this.mapUri);
            if (mf != null) {
                MapProviderFactory.setLanguages(mf.getMapLanguages());
                return new RendererLayer(tileCache, mf, mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
            }
            return null;
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return new ImmutablePair<>(getAttributionFromMapFile(this.mapUri), true);
        }

   }

    public static final class CyclosmMapSource extends MapsforgeMapSource {

        public CyclosmMapSource(final String fileName, final MapProvider mapProvider, final String name, final MapGeneratorInternal generator) {
            super(fileName, mapProvider, name, generator);
        }

        @Override
        public ITileLayer createTileLayer(final TileCache tileCache, final IMapViewPosition mapViewPosition) {
            final AbstractTileSource source = TileSourceCyclosm.INSTANCE;
            source.setUserAgent(MAPNIK_TILE_DOWNLOAD_UA);
            return new DownloadLayer(tileCache, mapViewPosition, source, AndroidGraphicFactory.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return new ImmutablePair<>(ctx.getString(R.string.map_attribution_cyclosm_html), false);
        }

    }

    public static final class OsmdeMapSource extends MapsforgeMapSource {

        public OsmdeMapSource(final String fileName, final MapProvider mapProvider, final String name, final MapGeneratorInternal generator) {
            super(fileName, mapProvider, name, generator);
        }

        @Override
        public ITileLayer createTileLayer(final TileCache tileCache, final IMapViewPosition mapViewPosition) {
            final AbstractTileSource source = TileSourceOsmde.INSTANCE;
            source.setUserAgent(MAPNIK_TILE_DOWNLOAD_UA);
            return new DownloadLayer(tileCache, mapViewPosition, source, AndroidGraphicFactory.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return new ImmutablePair<>(ctx.getString(R.string.map_attribution_openstreetmapde_html), false);
        }

    }

    public static final class OfflineMultiMapSource extends MapsforgeMapSource {
        private final List<ImmutablePair<String, Uri>> fileNames;

        public OfflineMultiMapSource(final List<ImmutablePair<String, Uri>> fileNames, final MapProvider mapProvider, final String name, final MapGeneratorInternal generator) {
            super(StringUtils.join(fileNames, ";"), mapProvider, name, generator);
            this.fileNames = fileNames;
        }

        @Override
        public boolean isAvailable() {
            boolean isValid = true;

            for (ImmutablePair<String, Uri> fileName : fileNames) {
                isValid &= isValidMapFile(fileName.right);
            }
            return isValid;
        }

        //public String getFileName() {
        //    return fileNames.get(0);
        //}

        /**
         * Create new render layer, if mapfiles exist
         */
        @Override
        public ITileLayer createTileLayer(final TileCache tileCache, final IMapViewPosition mapViewPosition) {
            final List<MapFile> mapFiles = new ArrayList<>();
            for (ImmutablePair<String, Uri> fileName : fileNames) {
                mapFiles.add(createMapFile(fileName.right));
            }

            return new MultiRendererLayer(tileCache, mapFiles, mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {

            final StringBuilder attributation = new StringBuilder();
            for (ImmutablePair<String, Uri> fileName : fileNames) {
                attributation.append("<p><b>" + fileName.left + "</b>:<br>");
                attributation.append(getAttributionFromMapFile(fileName.right));
                attributation.append("</p>");
            }
            return new ImmutablePair<>(attributation.toString(), true);
        }


     }

    @NonNull
    private static String getAttributionFromMapFile(final Uri filePath) {

        MapFile mapFile = null;
        try {
            mapFile = createMapFile(filePath);
            if (mapFile != null && mapFile.getMapFileInfo() != null) {
                if (!StringUtils.isBlank(mapFile.getMapFileInfo().comment)) {
                    return mapFile.getMapFileInfo().comment;
                }
                if (!StringUtils.isBlank(mapFile.getMapFileInfo().createdBy)) {
                    return mapFile.getMapFileInfo().createdBy;
                }
            }
            return "---";
        } finally {
            if (mapFile != null) {
                mapFile.close();
            }
        }
    }

    private static MapFile createMapFile(final String fileName) {

        final File file = new File(fileName);
        if (file.exists()) {
            return new MapFile(file, MapProviderFactory.getLanguage(Settings.getMapLanguage()));
        }
        return null;
    }

    private static MapFile createMapFile(final Uri mapUri) {
        final InputStream fis = PublicLocalStorage.get().openForRead(mapUri);
        return new MapFile((FileInputStream) fis, 0, MapProviderFactory.getLanguage(Settings.getMapLanguage()));
    }


    public void updateOfflineMaps() {
        MapProviderFactory.deleteOfflineMapSources();
        final Resources resources = CgeoApplication.getInstance().getResources();
        final List<ImmutablePair<String, Uri>> offlineMaps = getOfflineMaps();
        if (offlineMaps.size() > 1) {
            registerMapSource(new OfflineMultiMapSource(offlineMaps, this, resources.getString(R.string.map_source_osm_offline_combined), MapGeneratorInternal.DATABASE_RENDERER));
        }
        for (final ImmutablePair<String, Uri> mapFile : offlineMaps) {
            final String mapName = StringUtils.capitalize(StringUtils.substringBeforeLast(mapFile.left, "."));
            registerMapSource(new OfflineMapSource(mapFile.left, mapFile.right, this, mapName + " (" + resources.getString(R.string.map_source_osm_offline) + ")", MapGeneratorInternal.DATABASE_RENDERER));
        }
    }

    //Help for SAF framework
    //following code was copied here as reference provided thankfully by pstorch from
    //https://github.com/OpenTracksApp/OSMDashboard/blob/main/src/main/java/de/storchp/opentracks/osmplugin/MapsActivity.java#L316

//    protected MapDataStore getMapFile() {
//        final MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
//        final Set<Uri> mapFiles = PreferencesUtils.getMapUris(this);
//        if (mapFiles.isEmpty()) {
//            return null;
//        }
//        int mapsCount = 0;
//        for (final Uri mapUri: mapFiles) {
//            try {
//                final boolean documentUri = DocumentFile.isDocumentUri(this, mapUri);
//                if (documentUri) {
//                    final DocumentFile documentFile = DocumentFile.fromSingleUri(this, mapUri);
//                    if (documentFile != null && documentFile.canRead()) {
//                        final FileInputStream inputStream = (FileInputStream) getContentResolver().openInputStream(mapUri);
//                        mapDataStore.addMapDataStore(new MapFile(inputStream, 0, null), false, false);
//                        mapsCount++;
//                    }
//                }
//            } catch (final Exception e) {
//                Log.e(TAG, "Can't open mapFile", e);
//            }
//        }
//
//        if (mapsCount == 0 && !mapFiles.isEmpty()) {
//            Toast.makeText(this, R.string.error_loading_offline_map, Toast.LENGTH_LONG).show();
//        }
//
//        return mapsCount > 0 ? mapDataStore : null;
//    }




}
