package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.UriUtils;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.mbtiles.MBTilesFile;
import org.mapsforge.map.android.mbtiles.TileMBTilesLayer;
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
    public static ArrayList<TileMBTilesLayer> getBitmapTileLayersMapsforge(final Context context, final MapView mapView) {
        final ArrayList<TileMBTilesLayer> result = new ArrayList<>();
        final File[] files = getMBTilesSources(context);
        if (files != null) {
            for (File file : files) {
                Log.e("file: " + file);
                result.add(new TileMBTilesLayer(new InMemoryTileCache(500), mapView.getModel().mapViewPosition, true, new MBTilesFile(file), AndroidGraphicFactory.INSTANCE));
            }
        }
        return result;
    }

    /** returns a list of .mbtiles files found in public folder with fallback to app-specific media folder */
    private static File[] getMBTilesSources(final Context context) {
        final ArrayList<File> result = new ArrayList<>();
        
        // First, try the new location: public offline maps folder
        for (ContentStorage.FileInformation fi : ContentStorage.get().list(PersistableFolder.BACKGROUND_MAPS)) {
            if (!fi.isDirectory && StringUtils.endsWithIgnoreCase(fi.name, FileUtils.BACKGROUND_MAP_FILE_EXTENSION)) {
                final File file = UriUtils.toFile(fi.uri);
                if (file != null && file.exists()) {
                    result.add(file);
                }
            }
        }
        
        // Fallback to old location: app-specific media folder
        final File[] legacyFiles = context.getExternalMediaDirs()[0].listFiles((dir, name) -> StringUtils.endsWith(name, FileUtils.BACKGROUND_MAP_FILE_EXTENSION));
        if (legacyFiles != null) {
            for (File file : legacyFiles) {
                result.add(file);
            }
        }
        
        return result.toArray(new File[0]);
    }

}
