package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.PersistableFolder;

import android.content.Context;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.hills.DemFolderAndroidContent;
import org.mapsforge.map.layer.hills.DemFolder;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.hills.SimpleShadingAlgorithm;

public class HillShadingLayerHelper {

    private HillShadingLayerHelper() {
        // Do not instantiate, utility class
    }

    public static HillsRenderConfig getHillsRenderConfig() {
        if (!Settings.getBoolean(R.string.pref_maphillshading, false)) {
            return null;
        }

        final Context context = CgeoApplication.getInstance();
        final DemFolder shadingFolder = new DemFolderAndroidContent(PersistableFolder.OFFLINE_MAP_SHADING.getUri(), context, context.getContentResolver());

        final MemoryCachingHgtReaderTileSource hillTileSource = new MemoryCachingHgtReaderTileSource(shadingFolder, new SimpleShadingAlgorithm(Settings.getMapShadingLinearity(), Settings.getMapShadingScale()), AndroidGraphicFactory.INSTANCE);
        // avoid lines at between tiles
        hillTileSource.setEnableInterpolationOverlap(true);
        final HillsRenderConfig hillsConfig = new HillsRenderConfig(hillTileSource);
        hillsConfig.indexOnThread();
        return hillsConfig;

    }
}
