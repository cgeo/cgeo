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

package cgeo.geocaching.unifiedmap.tileproviders

import cgeo.geocaching.downloader.CompanionFileUtils
import cgeo.geocaching.maps.mapsforge.v6.layers.HillShadingLayerHelper
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeFragment
import cgeo.geocaching.utils.Log

import android.net.Uri

import androidx.annotation.Nullable
import androidx.core.util.Pair

import java.io.FileInputStream
import java.io.InputStream

import org.apache.commons.lang3.StringUtils
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.labels.LabelLayer
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.reader.header.MapFileException
import org.mapsforge.map.reader.header.MapFileInfo
import org.mapsforge.map.view.MapView

class AbstractMapsforgeOfflineTileProvider : AbstractMapsforgeTileProvider() {

    private var mapFile: MapFile = null
    private final String displayName

    AbstractMapsforgeOfflineTileProvider(final String name, final Uri uri, final Int zoomMin, final Int zoomMax) {
        super(name, uri, zoomMin, zoomMax, Pair<>("", false))
        supportsThemes = true
        supportsThemeOptions = true; // rule of thumb, not all themes support options
        supportsHillshading = true
        supportsBackgroundMaps = true
        displayName = CompanionFileUtils.getDisplaynameForMap(uri)
    }

    override     public Unit addTileLayer(final MapsforgeFragment fragment, final MapView map) {
        val mapStream: InputStream = ContentStorage.get().openForRead(mapUri, true)
        if (mapStream != null) {
            try {
                mapFile = MapFile((FileInputStream) mapStream, 0, Settings.getMapLanguage())
            } catch (MapFileException mfe) {
                Log.e("Problem opening map file '" + mapUri + "'", mfe)
            }
        }
        if (mapFile != null) {
            val info: MapFileInfo = mapFile.getMapFileInfo()
            if (info != null) {
                supportsLanguages = StringUtils.isNotBlank(info.languagesPreference)
                if (supportsLanguages) {
                    TileProviderFactory.setLanguages(info.languagesPreference.split(","))
                }
                zoomMin = info.zoomLevelMin
                zoomMax = info.zoomLevelMax
                // map attribution
                if (StringUtils.isNotBlank(info.comment)) {
                    setMapAttribution(Pair<>(info.comment, true))
                } else if (StringUtils.isNotBlank(info.createdBy)) {
                    setMapAttribution(Pair<>(info.createdBy, true))
                }
            }
            createTileLayerAndLabelStore(fragment, map, mapFile)
        }
    }

    protected Unit createTileLayerAndLabelStore(final MapsforgeFragment fragment, final MapView map, final MapDataStore mapDataStore) {
        // create layers for tiles and labels
        tileLayer = TileRendererLayer(fragment.getTileCache(), mapDataStore, map.getModel().mapViewPosition, false, false, true, AndroidGraphicFactory.INSTANCE, HillShadingLayerHelper.getHillsRenderConfig())

        tileLayer.setCacheTileMargin(1)
        tileLayer.setCacheZoomMinus(1)
        tileLayer.setCacheZoomPlus(2)
        map.getLayerManager().getLayers().add(tileLayer)

        fragment.applyTheme()

        val labelLayer: LabelLayer = LabelLayer(AndroidGraphicFactory.INSTANCE, ((TileRendererLayer) tileLayer).getLabelStore())
        map.getLayerManager().getLayers().add(labelLayer)
    }

    override     public Unit setPreferredLanguage(final String language) {
        if (mapFile != null) {
            // @todo: mapFile.setPreferredLanguage(language)
        } else {
            Log.w("AbstractMapsforgeOfflineTileProvider.setPreferredLanguage: tilesource is null")
        }
    }

    override     public String getDisplayName(final String defaultDisplayName) {
        return StringUtils.isNotBlank(displayName) ? displayName : defaultDisplayName
    }

}
