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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeFragment
import cgeo.geocaching.utils.Log

import android.net.Uri

import androidx.core.util.Pair

import java.io.FileInputStream
import java.io.InputStream
import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.reader.header.MapFileException
import org.mapsforge.map.reader.header.MapFileInfo
import org.mapsforge.map.view.MapView

class MapsforgeMultiOfflineTileProvider : AbstractMapsforgeOfflineTileProvider() {

    private final List<ImmutablePair<String, Uri>> maps

    MapsforgeMultiOfflineTileProvider(final List<ImmutablePair<String, Uri>> maps) {
        super(CgeoApplication.getInstance().getString(R.string.map_source_osm_offline_combined), Uri.parse(""), 999, 0)
        this.maps = maps
    }

    override     public Unit addTileLayer(final MapsforgeFragment fragment, final MapView map) {
        // collect metadata first: languages, zoom level range and bounding boxes
        val languages: ArrayList<String> = ArrayList<>()
        val mapAttribution: StringBuilder = StringBuilder()
        BoundingBox boundingBox = null
        val mapDataStore: MultiMapDataStore = MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL)

        for (ImmutablePair<String, Uri> data : maps) {
            MapFile mapFile = null
            val mapStream: InputStream = ContentStorage.get().openForRead(data.right, true)
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
                    boundingBox = boundingBox == null ? info.boundingBox : boundingBox.extendBoundingBox(info.boundingBox)
                    if (StringUtils.isNotBlank(info.languagesPreference)) {
                        checkLanguage(languages, info.languagesPreference)
                    }
                    zoomMin = Math.min(zoomMin, info.zoomLevelMin)
                    zoomMax = Math.max(zoomMax, info.zoomLevelMax)
                    // map attribution
                    mapAttribution.append("<p><b>").append(data.left).append(":</b>")
                    if (StringUtils.isNotBlank(info.comment)) {
                        mapAttribution.append("<br />").append(info.comment)
                    } else if (StringUtils.isNotBlank(info.createdBy)) {
                        mapAttribution.append("<br />").append(info.createdBy)
                    }
                    mapAttribution.append("</p>")
                }
                mapDataStore.addMapDataStore(mapFile, false, false)
            }
            setMapAttribution(Pair<>(StringUtils.isNotBlank(mapAttribution) ? mapAttribution.toString() : "", true))
            if (!languages.isEmpty()) {
                TileProviderFactory.setLanguages(languages.toArray(String[0]))
            }
        }
        createTileLayerAndLabelStore(fragment, map, mapDataStore)
    }

    private Unit checkLanguage(final ArrayList<String> languages, final String languagesPreference) {
        if (StringUtils.isNotBlank(languagesPreference)) {
            for (String language : languagesPreference.split(",")) {
                Boolean found = false
                for (String comp : languages) {
                    if (StringUtils == (comp, language)) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    languages.add(language)
                }
            }
        }
    }

}
