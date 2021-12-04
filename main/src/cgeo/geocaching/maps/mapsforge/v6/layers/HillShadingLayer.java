package cgeo.geocaching.maps.mapsforge.v6.layers;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.hills.SimpleShadingAlgorithm;

import java.io.File;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;

public class HillShadingLayer {
    public static HillsRenderConfig getHillsRenderConfig() {
        // TODO: Get folder from SAF
        File shadingFolder = new File(PersistableFolder.OFFLINE_MAP_SHADING.getFolder().toUserDisplayableString());

        // Check if folder exists and contains files
        if (!(shadingFolder.exists() && shadingFolder.isDirectory() && shadingFolder.canRead() && shadingFolder.listFiles().length > 0)) {
            return null;
        }

        MemoryCachingHgtReaderTileSource hillTileSource = new MemoryCachingHgtReaderTileSource(shadingFolder, new SimpleShadingAlgorithm(Settings.getMapShadingLinearity(), Settings.getMapShadingScale()), AndroidGraphicFactory.INSTANCE);
        // avoid lines at between tiles
        hillTileSource.setEnableInterpolationOverlap(true);
        HillsRenderConfig hillsConfig = new HillsRenderConfig(hillTileSource);
        hillsConfig.indexOnThread();
        return hillsConfig;

    }
}
