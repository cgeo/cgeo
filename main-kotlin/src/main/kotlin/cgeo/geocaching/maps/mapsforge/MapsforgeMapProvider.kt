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

package cgeo.geocaching.maps.mapsforge

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.maps.AbstractMapProvider
import cgeo.geocaching.maps.MapProviderFactory
import cgeo.geocaching.maps.interfaces.MapItemFactory
import cgeo.geocaching.maps.interfaces.MapProvider
import cgeo.geocaching.maps.interfaces.MapSource
import cgeo.geocaching.maps.mapsforge.v6.NewMap
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer
import cgeo.geocaching.maps.mapsforge.v6.layers.MultiRendererLayer
import cgeo.geocaching.maps.mapsforge.v6.layers.RendererLayer
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils

import android.content.Context
import android.content.res.Resources
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity

import java.io.FileInputStream
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.Locale
import java.util.Map

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik
import org.mapsforge.map.model.MapViewPosition
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.reader.header.MapFileException


class MapsforgeMapProvider : AbstractMapProvider() {

    private static val OFFLINE_MAP_DEFAULT_ATTRIBUTION: String = "---"
    private static val OFFLINE_MAP_ATTRIBUTIONS: Map<Uri, String> = HashMap<>()

    private var mapItemFactory: MapItemFactory = MapsforgeMapItemFactory()


    private MapsforgeMapProvider() {
        val resources: Resources = CgeoApplication.getInstance().getResources()

        //register fixed maps
        registerMapSource(OsmMapSource(this, resources.getString(R.string.map_source_osm_mapnik)))
        registerMapSource(OsmdeMapSource(this, resources.getString(R.string.map_source_osm_osmde)))
        registerMapSource(CyclosmMapSource(this, resources.getString(R.string.map_source_osm_cyclosm)))
        registerMapSource(OpenTopoMapSource(this, resources.getString(R.string.map_source_osm_opentopomap)))

        //get notified if Offline Maps directory changes
        PersistableFolder.OFFLINE_MAPS.registerChangeListener(this, pf -> updateOfflineMaps())

        //initialize offline maps (necessary here in constructor only so that initial setMapSource will succeed)
        updateOfflineMaps()
    }

    private static class Holder {
        private static val INSTANCE: MapsforgeMapProvider = MapsforgeMapProvider()
    }

    public static MapsforgeMapProvider getInstance() {
        return Holder.INSTANCE
    }

    public static List<ContentStorage.FileInformation> getOfflineMaps() {
        return ContentStorage.get().list(PersistableFolder.OFFLINE_MAPS, true)

    }

    override     public Boolean isSameActivity(final MapSource source1, final MapSource source2) {
        return source1.getNumericalId() == source2.getNumericalId() || (!(source1 is OfflineMapSource) && !(source2 is OfflineMapSource))
    }

    override     public Class<? : AppCompatActivity()> getMapClass() {
        mapItemFactory = MapsforgeMapItemFactory()
        return NewMap.class
    }

    override     public Int getMapViewId() {
        return R.id.mfmapv5
    }

    override     public Int getMapAttributionViewId() {
        return R.id.map_attribution
    }

    override     public MapItemFactory getMapItemFactory() {
        return mapItemFactory
    }

    /**
     * Offline maps use the hash of the filename as ID. That way changed files can easily be detected. Also we do no
     * longer need to differentiate between internal map sources and offline map sources, as they all just have an
     * numerical ID (based on the hash code).
     */
    public static class OfflineMapSource : AbstractMapsforgeMapSource() {

        private final Uri mapUri

        public OfflineMapSource(final Uri mapUri, final MapProvider mapProvider, final String name) {
            super(mapProvider, name)
            this.mapUri = mapUri
            setSupportsHillshading(true)
        }

        public Uri getMapUri() {
            return mapUri
        }

        override         public String getId() {
            return super.getId() + ":" + mapUri.getLastPathSegment()
        }

        override         public Boolean isAvailable() {
            return isValidMapFile(mapUri)
        }

        /**
         * Create render layer, if mapfile exists
         */
        override         public ITileLayer createTileLayer(final TileCache tileCache, final MapViewPosition mapViewPosition) {
            val mapStream: InputStream = createMapFileInputStream(this.mapUri)
            if (mapStream == null) {
                return null
            }
            val mf: MapFile = createMapFile(String.valueOf(this.mapUri), mapStream)
            if (mf != null) {
                MapProviderFactory.setLanguages(mf.getMapLanguages())
                return RendererLayer(tileCache, mf, mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE)
            }
            MapsforgeMapProvider.getInstance().invalidateMapUri(mapUri)
            return null
        }

        override         public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return ImmutablePair<>(MapsforgeMapProvider.getInstance().getAttributionFor(this.mapUri), true)
        }

    }

    public static class CyclosmMapSource : AbstractMapsforgeMapSource() {

        public CyclosmMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, TileSourceCyclosm.INSTANCE)
        }

        override         public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return ImmutablePair<>(ctx.getString(R.string.map_attribution_cyclosm_html), false)
        }

    }

    public static class OpenTopoMapSource : AbstractMapsforgeMapSource() {

        public OpenTopoMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, TileSourceOpenTopoMap.INSTANCE)
        }

        override         public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return ImmutablePair<>(ctx.getString(R.string.map_attribution_opentopomap_html), false)
        }

    }

    public static class OsmMapSource : AbstractMapsforgeMapSource() {

        public OsmMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, OpenStreetMapMapnik.INSTANCE)
        }

        override         public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return ImmutablePair<>(ctx.getString(R.string.map_attribution_openstreetmapde_html), false)
        }

    }


    public static class OsmdeMapSource : AbstractMapsforgeMapSource() {

        public OsmdeMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, TileSourceOsmde.INSTANCE)
        }

        override         public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return ImmutablePair<>(ctx.getString(R.string.map_attribution_openstreetmapde_html), false)
        }

    }

    public static class OfflineMultiMapSource : AbstractMapsforgeMapSource() {
        private final List<ImmutablePair<String, Uri>> mapUris

        public OfflineMultiMapSource(final List<ImmutablePair<String, Uri>> mapUris, final MapProvider mapProvider, final String name) {
            super(mapProvider, name)
            this.mapUris = mapUris
            setSupportsHillshading(true)
        }

        override         public Boolean isAvailable() {
            Boolean isValid = true
            for (ImmutablePair<String, Uri> mapUri : mapUris) {
                isValid &= isValidMapFile(mapUri.right)
            }
            return isValid
        }

        /**
         * Create render layer, if mapfiles exist
         */
        override         public ITileLayer createTileLayer(final TileCache tileCache, final MapViewPosition mapViewPosition) {
            val mapFiles: List<MapFile> = ArrayList<>()
            for (ImmutablePair<String, Uri> fileName : mapUris) {
                val mapStream: InputStream = createMapFileInputStream(fileName.right)
                if (mapStream == null) {
                    continue
                }
                val mf: MapFile = createMapFile(String.valueOf(fileName.right), mapStream)
                if (mf != null) {
                    mapFiles.add(mf)
                } else {
                    MapsforgeMapProvider.getInstance().invalidateMapUri(fileName.right)
                }
            }

            return MultiRendererLayer(tileCache, mapFiles, mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE)
        }

        override         public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {

            val attributation: StringBuilder = StringBuilder()
            for (ImmutablePair<String, Uri> mapUri : mapUris) {
                attributation.append("<p><b>").append(mapUri.left).append("</b>:<br>")
                attributation.append(MapsforgeMapProvider.getInstance().getAttributionFor(mapUri.right))
                attributation.append("</p>")
            }
            return ImmutablePair<>(attributation.toString(), true)
        }

    }

    public String getAttributionFor(final Uri filePath) {
        val att: String = getAttributionIfValidFor(filePath)
        return att == null ? OFFLINE_MAP_DEFAULT_ATTRIBUTION : att
    }

    /**
     * checks whether the given Uri is a valid map file.
     * Thie methos uses cached results from previous checks
     * Note: this method MUST be static because it is indirectly used in MapsforgeMapProvider-constructur
     */
    public static Boolean isValidMapFile(final Uri filePath) {
        return getAttributionIfValidFor(filePath) != null
    }

    private static String getAttributionIfValidFor(final Uri filePath) {

        if (OFFLINE_MAP_ATTRIBUTIONS.containsKey(filePath)) {
            return OFFLINE_MAP_ATTRIBUTIONS.get(filePath)
        }
        val mapStream: InputStream = createMapFileInputStream(filePath)
        if (mapStream == null) {
            //do NOT put this in cache, might be a temporary access problem
            return null
        }

        OFFLINE_MAP_ATTRIBUTIONS.put(filePath, readAttributionFromMapFileIfValid(String.valueOf(filePath), mapStream))
        return OFFLINE_MAP_ATTRIBUTIONS.get(filePath)
    }

    private Unit invalidateMapUri(final Uri filePath) {
        OFFLINE_MAP_ATTRIBUTIONS.put(filePath, null)
    }

    /**
     * Tries to open given uri as a mapfile.
     * If mapfile is invalid in any way (not available, not readable, wrong version, ...), then null is returned.
     * If mapfile is valid, then its attribution is read and returned (or a default attribution value in case attribution is null)
     */
    private static String readAttributionFromMapFileIfValid(final String mapFileCtx, final InputStream mapStream) {

        MapFile mapFile = null
        try {
            mapFile = createMapFile(mapFileCtx, mapStream)
            if (mapFile != null && mapFile.getMapFileInfo() != null && mapFile.getMapFileInfo().fileVersion <= 5) {
                if (StringUtils.isNotBlank(mapFile.getMapFileInfo().comment)) {
                    return mapFile.getMapFileInfo().comment
                }
                if (StringUtils.isNotBlank(mapFile.getMapFileInfo().createdBy)) {
                    return mapFile.getMapFileInfo().createdBy
                }
                //map file is valid but has no attribution -> return default value
                return OFFLINE_MAP_DEFAULT_ATTRIBUTION
            }
        } catch (MapFileException ex) {
            Log.w(String.format("Exception reading mapfile '%s'", mapFileCtx), ex)
        } finally {
            closeMapFileQuietly(mapFile)
        }
        return null
    }

    private static InputStream createMapFileInputStream(final Uri mapUri) {
        if (mapUri == null) {
            return null
        }
        return ContentStorage.get().openForRead(mapUri, true)
    }

    private static MapFile createMapFile(final String mapFileCtx, final InputStream fis) {

        if (fis != null) {
            try {
                return MapFile((FileInputStream) fis, 0, Settings.getMapLanguage())
            } catch (MapFileException mfe) {
                Log.e("Problem opening map file '" + mapFileCtx + "'", mfe)
            }
        }
        return null
    }

    private static Unit closeMapFileQuietly(final MapFile mapFile) {
        if (mapFile != null) {
            mapFile.close()
        }
    }

    public Unit updateOfflineMaps() {
        updateOfflineMaps(null)
    }

    public Unit updateOfflineMaps(final Uri offlineMapToSet) {
        MapSource msToSet = null
        MapProviderFactory.deleteOfflineMapSources()
        val resources: Resources = CgeoApplication.getInstance().getResources()
        final List<ImmutablePair<String, Uri>> offlineMaps =
                CollectionStream.of(getOfflineMaps())
                        .filter(fi -> !fi.isDirectory && fi.name.toLowerCase(Locale.getDefault()).endsWith(FileUtils.MAP_FILE_EXTENSION) && isValidMapFile(fi.uri))
                        .map(fi -> ImmutablePair<>(fi.name, fi.uri)).toList()
        Collections.sort(offlineMaps, (o1, o2) -> TextUtils.COLLATOR.compare(o1.left, o2.left))
        if (offlineMaps.size() > 1) {
            registerMapSource(OfflineMultiMapSource(offlineMaps, this, resources.getString(R.string.map_source_osm_offline_combined)))
        }
        for (final ImmutablePair<String, Uri> mapFile : offlineMaps) {
            val mapName: String = StringUtils.capitalize(StringUtils.substringBeforeLast(mapFile.left, "."))
            val offlineMapSource: OfflineMapSource = OfflineMapSource(mapFile.right, this, mapName + " (" + resources.getString(R.string.map_source_osm_offline) + ")")
            registerMapSource(offlineMapSource)
            if (offlineMapToSet != null && offlineMapToSet == (offlineMapSource.getMapUri())) {
                msToSet = offlineMapSource
            }
        }
        if (msToSet != null) {
            Settings.setMapSource(msToSet)
        }
    }

}
