package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.maps.AbstractMapSource;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.v024.MapsforgeMapActivity024;
import cgeo.geocaching.maps.mapsforge.v024.MapsforgeMapItemFactory024;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;

import android.app.Activity;
import android.content.res.Resources;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MapsforgeMapProvider implements MapProvider {

    private final static int MAPNIK = 1;
    final static int CYCLEMAP = 3;
    final static int OFFLINE = 4;

    private final Map<Integer, MapSource> mapSources;

    private final int baseId;
    private boolean oldMap = false;
    private MapItemFactory mapItemFactory = new MapsforgeMapItemFactory();

    public MapsforgeMapProvider(int _baseId) {
        baseId = _baseId;
        final Resources resources = cgeoapplication.getInstance().getResources();

        mapSources = new HashMap<Integer, MapSource>();
        mapSources.put(baseId + MAPNIK, new AbstractMapSource(this, resources.getString(R.string.map_source_osm_mapnik)));
        mapSources.put(baseId + CYCLEMAP, new AbstractMapSource(this, resources.getString(R.string.map_source_osm_cyclemap)));
        mapSources.put(baseId + OFFLINE, new OfflineMapSource(this, resources.getString(R.string.map_source_osm_offline)));
    }

    @Override
    public Map<Integer, MapSource> getMapSources() {
        return mapSources;
    }

    @Override
    public boolean isMySource(int sourceId) {
        return sourceId >= baseId + MAPNIK && sourceId <= baseId + OFFLINE;
    }

    public static int getMapsforgeSource(int sourceId) {
        MapProvider mp = MapProviderFactory.getMapProvider(sourceId);
        if (mp instanceof MapsforgeMapProvider) {
            MapsforgeMapProvider mfp = (MapsforgeMapProvider) mp;
            return sourceId - mfp.baseId;
        }
        return 0;
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
    public boolean isSameActivity(int sourceId1, int sourceId2) {
        final int mfSourceId1 = getMapsforgeSource(sourceId1);
        final int mfSourceId2 = getMapsforgeSource(sourceId2);
        return mfSourceId1 == mfSourceId2 ||
                !isMapfile024(Settings.getMapFile()) ||
                mfSourceId1 != OFFLINE && mfSourceId2 != OFFLINE;
    }

    @Override
    public Class<? extends Activity> getMapClass() {
        int sourceId = getMapsforgeSource(Settings.getMapSource());

        if (sourceId == OFFLINE && isMapfile024(Settings.getMapFile())) {
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

    private final class OfflineMapSource extends AbstractMapSource {

        public OfflineMapSource(MapProvider mapProvider, final String name) {
            super(mapProvider, name);
        }

        @Override
        public boolean isAvailable() {
            return Settings.isValidMapFile();
        }
    }
}
