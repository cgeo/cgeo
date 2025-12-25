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

package cgeo.geocaching.maps.mapsforge.v6.layers

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.PersistableFolder

import android.content.Context

import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.hills.DemFolderAndroidContent
import org.mapsforge.map.layer.hills.AdaptiveClasyHillShading
import org.mapsforge.map.layer.hills.DemFolder
import org.mapsforge.map.layer.hills.HillsRenderConfig
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource

class HillShadingLayerHelper {

    private HillShadingLayerHelper() {
        // Do not instantiate, utility class
    }

    public static HillsRenderConfig getHillsRenderConfig() {
        if (!Settings.getMapShadingShowLayer()) {
            return null
        }

        val context: Context = CgeoApplication.getInstance()
        val shadingFolder: DemFolder = DemFolderAndroidContent(PersistableFolder.OFFLINE_MAP_SHADING.getUri(), context, context.getContentResolver())

        val hillTileSource: MemoryCachingHgtReaderTileSource = MemoryCachingHgtReaderTileSource(shadingFolder, AdaptiveClasyHillShading(Settings.getMapShadingHq()).setZoomMinOverride(9).setZoomMaxOverride(20), AndroidGraphicFactory.INSTANCE)
        val hillsConfig: HillsRenderConfig = HillsRenderConfig(hillTileSource)
        hillsConfig.indexOnThread()
        return hillsConfig

    }
}
