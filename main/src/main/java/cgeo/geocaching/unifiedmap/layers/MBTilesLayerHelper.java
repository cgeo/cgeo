package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.unifiedmap.layers.mbtiles.MBTilesFile;
import cgeo.geocaching.unifiedmap.layers.mbtiles.MBTilesLayer;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.view.MapView;
import org.oscim.android.tiling.source.mbtiles.MBTilesBitmapTileSource;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Map;

public class MBTilesLayerHelper {

    private MBTilesLayerHelper() {
        //no instance
    }

    /** returns a list of BitmapTileLayers for all .mbtiles used for background maps (VTM variant) */
    public static ArrayList<BitmapTileLayer> getBitmapTileLayersVTM(final Context context, final Map map) {
        final ArrayList<BitmapTileLayer> result = new ArrayList<>();
        final File[] files = getMBTilesSources(context);
        if (files != null) {
            for (File file : files) {
                result.add(new BitmapTileLayer(map, new MBTilesBitmapTileSource(file.getAbsolutePath(), 192, null)));
            }
        }
        return result;
    }

    /** returns a list of BitmapTileLayers for all .mbtiles used for background maps (Mapsforge variant) */
    public static ArrayList<MBTilesLayer> getBitmapTileLayersMapsforge(final Context context, final MapView mapView) {
        final ArrayList<MBTilesLayer> result = new ArrayList<>();
        final File[] files = getMBTilesSources(context);
        if (files != null) {
            for (File file : files) {
                Log.e("file: " + file);
                result.add(new MBTilesLayer(new InMemoryTileCache(500), mapView.getModel().mapViewPosition, true, new MBTilesFile(file), AndroidGraphicFactory.INSTANCE));
            }
        }
        return result;
    }

    /** returns a list of .mbtiles files found in app-specific media folder, typically /Android/media/(app-id)/*.mbtiles */
    private static File[] getMBTilesSources(final Context context) {
        return context.getExternalMediaDirs()[0].listFiles((dir, name) -> StringUtils.endsWith(name, FileUtils.BACKGROUND_MAP_FILE_EXTENSION));
    }

}
