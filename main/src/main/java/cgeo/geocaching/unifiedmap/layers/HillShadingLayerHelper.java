package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;

import android.content.Context;
import android.graphics.Color;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.hills.DemFolderAndroidContent;
import org.mapsforge.map.layer.hills.AdaptiveClasyHillShading;
import org.mapsforge.map.layer.hills.DemFolder;
import org.oscim.android.cache.TileCache;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.tiling.source.hills.HillshadingTileSource;

public class HillShadingLayerHelper {

    private HillShadingLayerHelper() {
        //no instance
    }

    public static BitmapTileLayer getBitmapTileLayer(final Context context, final Map map) {
        if (!Settings.getMapShadingShowLayer()) {
            return null;
        }

        final DemFolder shadingFolder = new DemFolderAndroidContent(PersistableFolder.OFFLINE_MAP_SHADING.getUri(), context, context.getContentResolver());
        final AdaptiveClasyHillShading algorithm = new AdaptiveClasyHillShading(Settings.getMapShadingHq()).setZoomMinOverride(9).setZoomMaxOverride(20);
        final HillshadingTileSource hillshadingTileSource = new HillshadingTileSource(Viewport.MIN_ZOOM_LEVEL, Viewport.MAX_ZOOM_LEVEL, shadingFolder, algorithm, 128, Color.BLACK, AndroidGraphicFactory.INSTANCE);
        hillshadingTileSource.setCache(new TileCache(context, LocalStorage.getExternalPrivateCgeoDirectory().getAbsolutePath(), "hillshading"));
        return new BitmapTileLayer(map, hillshadingTileSource, 150);
    }
}
