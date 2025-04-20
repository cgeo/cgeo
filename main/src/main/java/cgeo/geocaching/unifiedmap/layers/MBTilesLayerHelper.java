package cgeo.geocaching.unifiedmap.layers;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.oscim.android.tiling.source.mbtiles.MBTilesBitmapTileSource;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Map;

public class MBTilesLayerHelper {

    private MBTilesLayerHelper() {
        //no instance
    }

    /** returns a list of BitmapTileLayes for all .mbtiles files found in app-specific media folder, typically /Android/media/(app-id)/*.mbtiles */
    public static ArrayList<BitmapTileLayer> getBitmapTileLayers(final Context context, final Map map) {
        final ArrayList<BitmapTileLayer> result = new ArrayList<>();
        final File[] files = context.getExternalMediaDirs()[0].listFiles((dir, name) -> StringUtils.endsWith(name, ".mbtiles"));
        if (files != null) {
            for (File file : files) {
                result.add(new BitmapTileLayer(map, new MBTilesBitmapTileSource(file.getAbsolutePath(), 192, null)));
            }
        }
        return result;
    }

}
