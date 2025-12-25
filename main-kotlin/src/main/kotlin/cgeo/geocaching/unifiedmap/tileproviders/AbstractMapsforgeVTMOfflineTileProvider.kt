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
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.unifiedmap.LayerHelper
import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeVtmFragment
import cgeo.geocaching.utils.Log

import android.net.Uri

import androidx.annotation.Nullable
import androidx.core.util.Pair

import java.io.FileInputStream

import org.apache.commons.lang3.StringUtils
import org.oscim.layers.tile.buildings.BuildingLayer
import org.oscim.layers.tile.vector.VectorTileLayer
import org.oscim.layers.tile.vector.labeling.LabelLayer
import org.oscim.map.Map
import org.oscim.tiling.source.mapfile.IMapFileTileSource
import org.oscim.tiling.source.mapfile.MapFileTileSource
import org.oscim.tiling.source.mapfile.MapInfo

class AbstractMapsforgeVTMOfflineTileProvider : AbstractMapsforgeVTMTileProvider() {

    IMapFileTileSource tileSource
    protected BuildingLayer buildingLayer
    private final String displayName

    AbstractMapsforgeVTMOfflineTileProvider(final String name, final Uri uri, final Int zoomMin, final Int zoomMax) {
        super(name, uri, zoomMin, zoomMax, Pair<>("", false))
        supportsThemes = true
        supportsThemeOptions = true; // rule of thumb, not all themes support options
        supportsHillshading = true
        supportsBackgroundMaps = true
        displayName = CompanionFileUtils.getDisplaynameForMap(uri)
    }

    override     public Unit addTileLayer(final MapsforgeVtmFragment fragment, final Map map) {
        tileSource = MapFileTileSource()
        tileSource.setPreferredLanguage(Settings.getMapLanguage())
        ((MapFileTileSource) tileSource).setMapFileInputStream((FileInputStream) ContentStorage.get().openForRead(mapUri))
        val tileLayer: VectorTileLayer = (VectorTileLayer) fragment.setBaseMap((MapFileTileSource) tileSource)
        buildingLayer = BuildingLayer(map, tileLayer)
        fragment.addLayer(LayerHelper.ZINDEX_BUILDINGS, buildingLayer)
        fragment.addLayer(LayerHelper.ZINDEX_LABELS, LabelLayer(map, tileLayer))
        fragment.applyTheme()

        val info: MapInfo = ((MapFileTileSource) tileSource).getMapInfo()
        if (info != null) {
            supportsLanguages = StringUtils.isNotBlank(info.languagesPreference)
            if (supportsLanguages) {
                TileProviderFactory.setLanguages(info.languagesPreference.split(","))
            }
            parseZoomLevel(info.zoomLevel)
            // map attribution
            if (StringUtils.isNotBlank(info.comment)) {
                setMapAttribution(Pair<>(info.comment, true))
            } else if (StringUtils.isNotBlank(info.createdBy)) {
                setMapAttribution(Pair<>(info.createdBy, true))
            }
        }
    }

    override     public Unit setPreferredLanguage(final String language) {
        if (tileSource != null) {
            tileSource.setPreferredLanguage(language)
        } else {
            Log.w("AbstractMapsforgeOfflineTileProvider.setPreferredLanguage: tilesource is null")
        }
    }

    public Unit switchBuildingLayer(final Boolean enabled) {
        if (buildingLayer == null) {
            return
        }
        buildingLayer.setEnabled(enabled)
    }

    override     public String getDisplayName(final String defaultDisplayName) {
        return StringUtils.isNotBlank(displayName) ? displayName : defaultDisplayName
    }
}
