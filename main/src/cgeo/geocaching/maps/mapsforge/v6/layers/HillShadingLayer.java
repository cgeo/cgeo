package cgeo.geocaching.maps.mapsforge.v6.layers;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.hills.DemFolderAndroidContent;
import org.mapsforge.map.layer.hills.DemFolder;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.hills.SimpleShadingAlgorithm;
import android.content.Context;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.PersistableFolder;

public class HillShadingLayer {

    public static HillsRenderConfig getHillsRenderConfig() {
        Context context = CgeoApplication.getInstance();
        DemFolder shadingFolder = new DemFolderAndroidContent(PersistableFolder.OFFLINE_MAP_SHADING.getUri(), context, context.getContentResolver());

        MemoryCachingHgtReaderTileSource hillTileSource = new MemoryCachingHgtReaderTileSource(shadingFolder, new SimpleShadingAlgorithm(Settings.getMapShadingLinearity(), Settings.getMapShadingScale()), AndroidGraphicFactory.INSTANCE);
        // avoid lines at between tiles
        hillTileSource.setEnableInterpolationOverlap(true);
        HillsRenderConfig hillsConfig = new HillsRenderConfig(hillTileSource);
        hillsConfig.indexOnThread();
        return hillsConfig;

    }
}
