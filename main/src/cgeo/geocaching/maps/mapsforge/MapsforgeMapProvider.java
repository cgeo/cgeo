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
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
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

    public static List<String> getOfflineMaps() {
        final String directoryPath = Settings.getMapFileDirectory();
        if (StringUtils.isBlank(directoryPath)) {
            return Collections.emptyList();
        }

        final File directory = new File(directoryPath);
        if (directory.isDirectory()) {
            try {
                final List<String> mapFileList = new ArrayList<>();
                final File[] files = directory.listFiles();
                if (ArrayUtils.isNotEmpty(files)) {
                    for (final File file : files) {
                        if (file.getName().endsWith(".map") && isValidMapFile(file.getAbsolutePath())) {
                            mapFileList.add(file.getAbsolutePath());
                        }
                    }
                    Collections.sort(mapFileList, TextUtils.COLLATOR);
                }
                return mapFileList;
            } catch (final Exception e) {
                Log.e("MapsforgeMapProvider.getOfflineMaps: ", e);
            }
        }
        return Collections.emptyList();
    }

    public static boolean isValidMapFile(final String mapFileIn) {

        if (StringUtils.isEmpty(mapFileIn)) {
            return false;
        }

        try {
            final MapFile mapFile = new MapFile(mapFileIn);
            return mapFile.getMapFileInfo().fileVersion <= 3 || !Settings.useOldMapsforgeAPI();
        } catch (MapFileException ex) {
            Log.w(String.format("Exception reading mapfile '%s'", mapFileIn), ex);
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
    public int getMapAttributionTextId() {
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

        private final String fileName;

        public OfflineMapSource(final String fileName, final MapProvider mapProvider, final String name, final MapGeneratorInternal generator) {
            super(fileName, mapProvider, name, generator);
            this.fileName = fileName;
        }

        @Override
        public boolean isAvailable() {
            return isValidMapFile(fileName);
        }

        public String getFileName() {
            return fileName;
        }

        /** Create new render layer, if mapfile exists */
        @Override
        public ITileLayer createTileLayer(final TileCache tileCache, final IMapViewPosition mapViewPosition) {
            final MapFile mf = createMapFile(this.fileName);
            if (mf != null) {
                MapProviderFactory.setLanguages(mf.getMapLanguages());
                return new RendererLayer(tileCache, mf, mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
            }
            return null;
        }



        @Override
        public CharSequence getMapAttribution(final Context ctx) {
            return getName() + " " + ctx.getString(R.string.map_source_osm_offline_attribution_pleasewait);
        }

        @Override
        public void setMapAttributionTo(final TextView textView) {
            super.setMapAttributionTo(textView);

            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
                final MapFile mf = createMapFile(this.fileName);
                try {
                    final String attr = getAttributionFromMapFile(mf);
                    return getName() + (attr == null ? "" : ": " + attr.trim());
                } finally {
                    if (mf != null) {
                        mf.close();
                    }
                }
            },
            textView::setText);
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
        public CharSequence getMapAttribution(final Context ctx) {
            return Html.fromHtml(ctx.getString(R.string.map_attribution_cyclosm_html));
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
        public CharSequence getMapAttribution(final Context ctx) {
            return Html.fromHtml(ctx.getString(R.string.map_attribution_openstreetmapde_html));
        }


    }

    public static final class OfflineMultiMapSource extends MapsforgeMapSource {
        private final List<String> fileNames;

        public OfflineMultiMapSource(final List<String> fileNames, final MapProvider mapProvider, final String name, final MapGeneratorInternal generator) {
            super(StringUtils.join(fileNames, ";"), mapProvider, name, generator);
            this.fileNames = fileNames;
        }

        @Override
        public boolean isAvailable() {
            boolean isValid = true;

            for (String fileName : fileNames) {
                isValid &= isValidMapFile(fileName);
            }
            return isValid;
        }

        public String getFileName() {
            return fileNames.get(0);
        }

        /**
         * Create new render layer, if mapfiles exist
         */
        @Override
        public ITileLayer createTileLayer(final TileCache tileCache, final IMapViewPosition mapViewPosition) {
            final List<MapFile> mapFiles = new ArrayList<>();
            for (String fileName : fileNames) {
                final File mapFile = new File(fileName);
                if (mapFile.exists()) {
                    mapFiles.add(new MapFile(mapFile));
                }
            }

            return new MultiRendererLayer(tileCache, mapFiles, mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
        }

        @Override
        public CharSequence getMapAttribution(final Context ctx) {
            return ctx.getString(R.string.map_source_osm_offline_combined_attribution_pleasewait, fileNames.size());
        }

        @Override
        public void setMapAttributionTo(final TextView textView) {
            super.setMapAttributionTo(textView);

            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> {
                final List<String> atts = new ArrayList<>();
                final Set<String> attsSet = new HashSet<>();
                String lastAtt = null;
                for (String fileName : fileNames) {
                    final MapFile mf = createMapFile(fileName);
                    final String attr = getAttributionFromMapFile(mf);
                    if (mf != null) {
                        mf.close();
                    }
                    atts.add(new File(fileName).getName() + (attr == null ? ": ---" : ": " + attr.trim()));
                    if (attr == null) {
                        continue;
                    }
                    attsSet.add(attr.trim());
                    lastAtt = attr.trim();
                }

                return new ImmutableTriple<>(atts, attsSet, lastAtt);
            }, triple -> {
                if (triple.middle.size() == 1) {
                    textView.setText(textView.getContext().getString(R.string.map_source_osm_offline_combined_attribution_single,
                        fileNames.size(), triple.right));
                } else {
                    textView.setText(textView.getContext().getString(R.string.map_source_osm_offline_combined_attribution_details,
                        fileNames.size(), triple.middle.size()));
                    textView.setOnClickListener(v -> new AlertDialog.Builder(textView.getContext())
                        .setTitle(textView.getContext().getString(R.string.map_source_osm_offline_combined_attribution_dialog_title))
                        .setItems((String[]) CollectionStream.of(triple.left).toArray(String.class), null)
                        .setPositiveButton(android.R.string.ok, (dialog, pos) -> dialog.dismiss())
                        .create()
                        .show());
                }
            });
        }

    }

    private static String getAttributionFromMapFile(final MapFile mapFile) {
        if (mapFile != null && mapFile.getMapFileInfo() != null) {
            if (!StringUtils.isBlank(mapFile.getMapFileInfo().comment)) {
                return mapFile.getMapFileInfo().comment;
            }
            if (!StringUtils.isBlank(mapFile.getMapFileInfo().createdBy)) {
               return mapFile.getMapFileInfo().createdBy;
            }
        }
        return null;
    }

    private static MapFile createMapFile(final String fileName) {
        final File file = new File(fileName);
        if (file.exists()) {
            return new MapFile(file, MapProviderFactory.getLanguage(Settings.getMapLanguage()));
        }
        return null;
    }

    public void updateOfflineMaps() {
        MapProviderFactory.deleteOfflineMapSources();
        final Resources resources = CgeoApplication.getInstance().getResources();
        final List<String> offlineMaps = getOfflineMaps();
        if (offlineMaps.size() > 1) {
            registerMapSource(new OfflineMultiMapSource(offlineMaps, this, resources.getString(R.string.map_source_osm_offline_combined), MapGeneratorInternal.DATABASE_RENDERER));
        }
        for (final String mapFile : offlineMaps) {
            final String mapName = StringUtils.capitalize(StringUtils.substringBeforeLast(new File(mapFile).getName(), "."));
            registerMapSource(new OfflineMapSource(mapFile, this, mapName + " (" + resources.getString(R.string.map_source_osm_offline) + ")", MapGeneratorInternal.DATABASE_RENDERER));
        }
    }




}
