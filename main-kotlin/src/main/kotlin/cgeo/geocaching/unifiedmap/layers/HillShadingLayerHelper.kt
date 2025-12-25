// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.unifiedmap.layers

import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.storage.PersistableFolder

import android.content.Context
import android.graphics.Color

import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.hills.DemFolderAndroidContent
import org.mapsforge.map.layer.hills.AdaptiveClasyHillShading
import org.mapsforge.map.layer.hills.DemFolder
import org.oscim.android.cache.TileCache
import org.oscim.layers.tile.bitmap.BitmapTileLayer
import org.oscim.map.Map
import org.oscim.map.Viewport
import org.oscim.tiling.source.hills.HillshadingTileSource

class HillShadingLayerHelper {

    private HillShadingLayerHelper() {
        //no instance
    }

    public static BitmapTileLayer getBitmapTileLayer(final Context context, final Map map) {
        if (!Settings.getMapShadingShowLayer()) {
            return null
        }

        val shadingFolder: DemFolder = DemFolderAndroidContent(PersistableFolder.OFFLINE_MAP_SHADING.getUri(), context, context.getContentResolver())
        val algorithm: AdaptiveClasyHillShading = AdaptiveClasyHillShading(Settings.getMapShadingHq()).setZoomMinOverride(9).setZoomMaxOverride(20)
        val hillshadingTileSource: HillshadingTileSource = HillshadingTileSource(Viewport.MIN_ZOOM_LEVEL, Viewport.MAX_ZOOM_LEVEL, shadingFolder, algorithm, 128, Color.BLACK, AndroidGraphicFactory.INSTANCE)
        hillshadingTileSource.setCache(TileCache(context, LocalStorage.getExternalPrivateCgeoDirectory().getAbsolutePath(), "hillshading"))
        return BitmapTileLayer(map, hillshadingTileSource, 150)
    }
}
