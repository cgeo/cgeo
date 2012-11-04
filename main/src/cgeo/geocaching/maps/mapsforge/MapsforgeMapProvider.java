package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.maps.AbstractMapProvider;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.v024.MapsforgeMapActivity024;
import cgeo.geocaching.maps.mapsforge.v024.MapsforgeMapItemFactory024;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorInternal;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;

import android.app.Activity;
import android.content.res.Resources;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MapsforgeMapProvider extends AbstractMapProvider {

    public static final String MAPSFORGE_CYCLEMAP_ID = "MAPSFORGE_CYCLEMAP";
    public static final String MAPSFORGE_MAPNIK_ID = "MAPSFORGE_MAPNIK";
    private boolean oldMap = false;
    private MapItemFactory mapItemFactory = new MapsforgeMapItemFactory();
    private static MapsforgeMapProvider instance;

    private MapsforgeMapProvider() {
        final Resources resources = cgeoapplication.getInstance().getResources();

        registerMapSource(new MapsforgeMapSource(MAPSFORGE_MAPNIK_ID, this, resources.getString(R.string.map_source_osm_mapnik), MapGeneratorInternal.MAPNIK));
        registerMapSource(new MapsforgeMapSource(MAPSFORGE_CYCLEMAP_ID, this, resources.getString(R.string.map_source_osm_cyclemap), MapGeneratorInternal.OPENCYCLEMAP));

        updateOfflineMaps();
    }

    public static MapsforgeMapProvider getInstance() {
        if (instance == null) {
            instance = new MapsforgeMapProvider();
        }
        return instance;
    }

    public static List<String> getOfflineMaps() {
        final String mapFile = Settings.getMapFile();
        if (StringUtils.isEmpty(mapFile)) {
            return Collections.emptyList();
        }

        try {
            File directory = new File(mapFile).getParentFile();
            ArrayList<String> mapFileList = new ArrayList<String>();
            for (File file : directory.listFiles()) {
                if (file.getName().endsWith(".map")) {
                    if (MapsforgeMapProvider.isValidMapFile(file.getAbsolutePath())) {
                        mapFileList.add(file.getAbsolutePath());
                    }
                }
            }
            return mapFileList;
        } catch (Exception e) {
            Log.e("Settings.getOfflineMaps: " + e);
        }
        return Collections.emptyList();
    }

    public static boolean isValidMapFile(String mapFileIn) {

        if (StringUtils.isEmpty(mapFileIn)) {
            return false;
        }

        MapDatabase mapDB = new MapDatabase();
        FileOpenResult result = mapDB.openFile(new File(mapFileIn));
        mapDB.closeFile();

        boolean isValid = result.isSuccess();

        if (!isValid) {
            isValid = isMapfile024(mapFileIn);
        }

        return isValid;
    }

    private static boolean isMapfile024(String mapFileIn) {
        return org.mapsforge.android.mapsold.MapDatabase.isValidMapFile(mapFileIn);
    }

    @Override
    public boolean isSameActivity(final MapSource source1, final MapSource source2) {
        return source1 == source2 ||
                !isMapfile024(Settings.getMapFile()) ||
                (!(source1 instanceof OfflineMapSource) && !(source2 instanceof OfflineMapSource));
    }

    @Override
    public Class<? extends Activity> getMapClass() {
        final MapSource source = Settings.getMapSource();
        if (source instanceof OfflineMapSource && isMapfile024(Settings.getMapFile())) {
            oldMap = true;
            mapItemFactory = new MapsforgeMapItemFactory024();
            return MapsforgeMapActivity024.class;
        }
        oldMap = false;
        mapItemFactory = new MapsforgeMapItemFactory();
        return MapsforgeMapActivity.class;
    }

    @Override
    public int getMapViewId() {
        if (oldMap) {
            return R.id.mfmap_old;
        }
        return R.id.mfmap;
    }

    @Override
    public int getMapLayoutId() {
        if (oldMap) {
            return R.layout.map_mapsforge_old;
        }
        return R.layout.map_mapsforge;
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

        public OfflineMapSource(final String fileName, MapProvider mapProvider, final String name, final MapGeneratorInternal generator) {
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
    }

    public void updateOfflineMaps() {
        MapProviderFactory.deleteOfflineMapSources();
        final Resources resources = cgeoapplication.getInstance().getResources();
        final List<String> offlineMaps = getOfflineMaps();
        for (String mapFile : offlineMaps) {
            final String mapName = StringUtils.capitalize(StringUtils.substringBeforeLast(new File(mapFile).getName(), "."));
            registerMapSource(new OfflineMapSource(mapFile, this, resources.getString(R.string.map_source_osm_offline) + " - " + mapName, MapGeneratorInternal.DATABASE_RENDERER));
        }
        // have a default entry, if no map files are available. otherwise we cannot select "offline" in the settings
        if (offlineMaps.isEmpty()) {
            registerMapSource(new OfflineMapSource("", this, resources.getString(R.string.map_source_osm_offline), MapGeneratorInternal.DATABASE_RENDERER));
        }
    }
}
