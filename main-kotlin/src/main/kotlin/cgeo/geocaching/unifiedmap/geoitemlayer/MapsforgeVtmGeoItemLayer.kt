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

package cgeo.geocaching.unifiedmap.geoitemlayer

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointConverter
import cgeo.geocaching.models.geoitem.GeoIcon
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.models.geoitem.ToScreenProjector
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.ContextLogger
import cgeo.geocaching.utils.GroupedList
import cgeo.geocaching.utils.Log

import android.graphics.BitmapFactory
import android.util.Pair

import androidx.core.util.Supplier

import java.util.Collection
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Objects
import java.util.Set
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import org.oscim.android.canvas.AndroidBitmap
import org.oscim.backend.canvas.Bitmap
import org.oscim.backend.canvas.Paint
import org.oscim.core.GeoPoint
import org.oscim.core.Point
import org.oscim.layers.Layer
import org.oscim.layers.marker.ItemizedLayer
import org.oscim.layers.marker.MarkerInterface
import org.oscim.layers.marker.MarkerItem
import org.oscim.layers.marker.MarkerSymbol
import org.oscim.layers.vector.VectorLayer
import org.oscim.layers.vector.geometries.CircleDrawable
import org.oscim.layers.vector.geometries.Drawable
import org.oscim.layers.vector.geometries.LineDrawable
import org.oscim.layers.vector.geometries.PolygonDrawable
import org.oscim.layers.vector.geometries.Style
import org.oscim.map.Map
import org.oscim.renderer.atlas.TextureRegion
import org.oscim.utils.BitmapPacker
import org.oscim.utils.geom.GeomBuilder

class MapsforgeVtmGeoItemLayer : IProviderGeoItemLayer<Pair<Drawable, MarkerInterface>> {

    private static val GP_CONVERTER: GeopointConverter<GeoPoint> = GeopointConverter<>(
            gc -> GeoPoint(gc.getLatitude(), gc.getLongitude()),
            ll -> Geopoint(ll.latitudeE6, ll.longitudeE6)
    )

    private Map map
    private GroupedList<Layer> mapLayers

    private final java.util.Map<Integer, PopulateControlledItemizedLayer> markerLayerMap = HashMap<>()
    private final java.util.Map<Integer, VectorLayer> vectorLayerMap = HashMap<>()
    private val layerMapLock: Lock = ReentrantLock()

    private MarkerSymbol defaultMarkerSymbol

    private var defaultZLevel: Int = 0

    private val markerLayersForRefresh: Set<Integer> = HashSet<>()

    private final java.util.Map<MarkerSymbolCacheKey, MarkerSymbol> markerSymbolCache = HashMap<>()

    public static class PopulateControlledItemizedLayer : ItemizedLayer() {

        public PopulateControlledItemizedLayer(final Map map, final MarkerSymbol defaultMarker) {
            super(map, defaultMarker)
        }

        /** mimics {@link #addItem(MarkerInterface)} but w/o calling 'populate' */
        public Boolean addItemNoPopulate(final MarkerInterface marker) {
            return this.mItemList.add(marker)
        }

        /** mimics {@link #removeItem(MarkerInterface)} but w/o calling 'populate' */
        public Boolean removeItemNoPopulate(final MarkerInterface marker) {
            return this.mItemList.remove(marker)
        }
    }

    public static class MarkerSymbolCacheKey {

        private final android.graphics.Bitmap bitmap
        private final Float xAnchor
        private final Float yAnchor
        private final Boolean isFlat

        private MarkerSymbolCacheKey(final android.graphics.Bitmap bitmap, final Float xAnchor, final Float yAnchor, final Boolean isFlat) {
            this.bitmap = bitmap
            this.xAnchor = xAnchor
            this.yAnchor = yAnchor
            this.isFlat = isFlat
        }

        override         public Boolean equals(final Object obj) {
            if (!(obj is MarkerSymbolCacheKey)) {
                return false
            }
            val other: MarkerSymbolCacheKey = (MarkerSymbolCacheKey) obj
            return bitmap == other.bitmap // identity!
                && Objects == (xAnchor, other.xAnchor)
                && Objects == (yAnchor, other.yAnchor)
                && isFlat == other.isFlat
        }

        override         public Int hashCode() {
            return bitmap == null ? 7 : System.identityHashCode(bitmap)
        }

    }

    public MapsforgeVtmGeoItemLayer(final Map map, final GroupedList<Layer> mapLayers) {
        this.map = map
        this.mapLayers = mapLayers
    }

    override     public Unit init(final Int zLevel) {
        defaultZLevel = Math.max(0, zLevel)

        //initialize marker layer stuff
        val bitmap: Bitmap = AndroidBitmap(BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.cgeo_notification))
        defaultMarkerSymbol = MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.BOTTOM_CENTER, true)

    }

    @SuppressWarnings("unchecked")
    private <T : Layer()> T getZLevelLayer(final Int zLevel, final java.util.Map<Integer, T> layerMap, final Class<T> layerClass, final Supplier<T> layerCreator) {
        layerMapLock.lock()
        try {
            T zLayer = layerMap.get(zLevel)
            if (zLayer != null || layerCreator == null) {
                return zLayer
            }

            //search for an existing layer, create if not existing
            synchronized (mapLayers) {
                val layerIdx: Int = mapLayers.groupIndexOf(zLevel, c -> layerClass.isAssignableFrom(c.getClass()))
                if (layerIdx > 0) {
                    zLayer = (T) mapLayers.get(layerIdx)
                } else {
                    //create marker layer first, then vectorlayer. That way, markers will always be shown above shapes with same zLevel
                    zLayer = layerCreator.get()
                    mapLayers.addToGroup(zLayer, zLevel)
                }
            }

            layerMap.put(zLevel, zLayer)

            return zLayer

        } finally {
            layerMapLock.unlock()
        }
    }

    private PopulateControlledItemizedLayer getMarkerLayer(final Int zLevel, final Boolean createIfNonexisting) {
        return getZLevelLayer(zLevel, markerLayerMap, PopulateControlledItemizedLayer.class, createIfNonexisting ? () -> PopulateControlledItemizedLayer(map, defaultMarkerSymbol) : null)
    }

    private VectorLayer getVectorLayer(final Int zLevel, final Boolean createIfNonexisting) {
        return getZLevelLayer(zLevel, vectorLayerMap, VectorLayer.class, createIfNonexisting ? () -> VectorLayer(map) : null)
    }

    private Int getZLevel(final GeoPrimitive item) {
        if (item != null && item.getZLevel() >= 0) {
            return item.getZLevel() + 20
        }
        return defaultZLevel + 20
    }

    override     public Pair<Drawable, MarkerInterface> add(final GeoPrimitive item) {

        val fillColor: Int = GeoStyle.getFillColor(item.getStyle())
        val rawStrokeWidth: Float = GeoStyle.getStrokeWidth(item.getStyle()) / 1.5f
        val style: Style = Style.builder()
                .strokeWidth(ViewUtils.dpToPixelFloat(rawStrokeWidth))
                .strokeColor(GeoStyle.getStrokeColor(item.getStyle()))
                .fillAlpha(1f) // GeoJsonUtils.colorFromJson() already calculates the color using fill and fill-opacity, don't apply it again
                .fillColor(fillColor)
                .transparent(true) ////See #15029. Following parameter prevents rendering of "darker edges" for overlapping semi-transparent route parts
                .dropDistance(ViewUtils.dpToPixelFloat(1)) //see #15029. This setting stops rendering route parts at some point when zooming out
                .cap(Paint.Cap.BUTT)
                .fixed(true)
                .build()
        val zLevel: Int = getZLevel(item)

        Drawable drawable = null
        switch (item.getType()) {
            case MARKER:
                break
            case CIRCLE:
                drawable = CircleDrawable(GP_CONVERTER.to(item.getCenter()), item.getRadius(), style)
                break
            case POLYGON:
                //we have to construct our own GeomBuilder
                //because standard constructorss of PoygonDrawable doesn't support multiple holes
                val gb: GeomBuilder = GeomBuilder()
                addRingToGeoBuilder(gb, item.getPoints())
                if (item.getHoles() != null) {
                    for (List<Geopoint> hole : item.getHoles()) {
                        addRingToGeoBuilder(gb, hole)
                    }
                }
                drawable = PolygonDrawable(gb.toPolygon(), style)
                break
            case POLYLINE:
            default:
                drawable = LineDrawable(GP_CONVERTER.toList(item.getPoints()), style)
                break
        }

        if (drawable != null) {
            val vectorLayer: VectorLayer = getVectorLayer(zLevel, true)
            vectorLayer.add(drawable)
            vectorLayer.update()
        }

        MarkerItem marker = null
        if (item.getIcon() != null) {
            val markerLayer: PopulateControlledItemizedLayer = getMarkerLayer(zLevel, true)
            val icon: GeoIcon = item.getIcon()
            marker = MarkerItem("", "", GP_CONVERTER.to(item.getCenter()))
            marker.setMarker(getMarkerSymbol(icon.getBitmap(), icon.getXAnchor(), icon.getYAnchor(), icon.isFlat()))
            marker.setRotation(item.getIcon().getRotation())
            markerLayer.addItemNoPopulate(marker)
            markerLayersForRefresh.add(zLevel)
        }


        return Pair<>(drawable, marker)
    }

    private static Unit addRingToGeoBuilder(final GeomBuilder gb, final List<Geopoint> ring) {
        for (Geopoint pt : ring) {
            val gpt: GeoPoint = GP_CONVERTER.to(pt)
            gb.point(gpt.getLongitude(), gpt.getLatitude())
        }
        gb.ring()
    }

    override     public Unit remove(final GeoPrimitive item, final Pair<Drawable, MarkerInterface> context) {

        if (context == null) {
            return
        }

        val zLevel: Int = getZLevel(item)
        if (context.first != null) {
            val vectorLayer: VectorLayer = getVectorLayer(zLevel, false)
            if (vectorLayer != null) {
                vectorLayer.remove(context.first)
                vectorLayer.update()
            }
        }
        if (context.second != null) {
            val markerLayer: PopulateControlledItemizedLayer = getMarkerLayer(zLevel, false)
            if (markerLayer != null) {
                markerLayer.removeItemNoPopulate(context.second)
                markerLayersForRefresh.add(zLevel)
            }
        }
    }

    override     public String onMapChangeBatchEnd(final Long processedCount) {
        if (map == null || processedCount == 0) {
            return null
        }
        return flushMapChanges()
    }

    private String flushMapChanges() {

        val layersSize: Int = markerLayersForRefresh.size()

        //populate and update marker layers which were touched
        for (Int markerZLevelToRefresh : markerLayersForRefresh) {
            val markerLayer: PopulateControlledItemizedLayer = getMarkerLayer(markerZLevelToRefresh, false)
            if (markerLayer == null) {
                continue
            }
            markerLayer.populate()
            markerLayer.update()
        }
        markerLayersForRefresh.clear()

        //make sure map is redrawn. See e.g. #14787
        map.updateMap(true)
        return "l:" + layersSize + ",s:" + markerSymbolCache.size()
    }

    private val markerPacker: BitmapPacker = BitmapPacker(2048, 2048, 2, BitmapPacker.SkylineStrategy(), false)
    private val markerCounter: AtomicInteger = AtomicInteger(0)

    private MarkerSymbol getMarkerSymbol(final android.graphics.Bitmap bitmap, final Float xAnchor, final Float yAnchor, final Boolean isFlat) {
        if (Settings.enableVtmSingleMarkerSymbol()) {
            return defaultMarkerSymbol
        }

        val ctx: ContextLogger = ContextLogger(Log.LogLevel.DEBUG, "VTM:getMarkerSymbol")
        try {

            val key: MarkerSymbolCacheKey = MarkerSymbolCacheKey(bitmap, xAnchor, yAnchor, isFlat)
            synchronized (markerSymbolCache) {
                MarkerSymbol symbol = markerSymbolCache.get(key)
                if (symbol != null) {
                    return symbol
                }

                //Create a Marker Symbol
                if (Settings.enableVtmMarkerAtlasUsage()) {
                    //For efficiency we use an image atlas (which is provided by VTM library)
                    //General info about image atlas can be found e.g. here: https://en.wikipedia.org/wiki/Texture_atlas

                    //1. Place the bitmap on an atlas of our bitmap packer. Use an arbitrarily id to reference it afterwards
                    val id: Int = markerCounter.addAndGet(1)
                    markerPacker.add(id, AndroidBitmap(bitmap))
                    //2. Get the TextureRegion for the just added bitmap. Naturally it has to be in the last atlas of the bitmappacker
                    val region: TextureRegion = markerPacker.getAtlasItem(markerPacker.getAtlasCount() - 1).getAtlas().getTextureRegion(id)
                    //3. Use the TextureRegion to create the symbol
                    symbol = MarkerSymbol(region, xAnchor, yAnchor, !isFlat)
                } else {
                    //create marker symbol w/o usage of atlas
                    symbol = MarkerSymbol(AndroidBitmap(bitmap), xAnchor, yAnchor, !isFlat)
                }
                //cache the marker symbol and return it
                markerSymbolCache.put(key, symbol)
                return symbol
            }
        } catch (RuntimeException re) {
            ctx.setException(re, true)
            return defaultMarkerSymbol
        } finally {
            ctx.close()
        }

    }


    override     public Unit destroy(final Collection<Pair<GeoPrimitive, Pair<Drawable, MarkerInterface>>> values) {

        for (Pair<GeoPrimitive, Pair<Drawable, MarkerInterface>> v : values) {
            remove(v.first, v.second)
        }
        flushMapChanges()

        map = null
        mapLayers = null
        markerLayerMap.clear()
        vectorLayerMap.clear()
        defaultMarkerSymbol = null
    }

    override     public ToScreenProjector getScreenCoordCalculator() {

          return gp -> {
              if (map == null || map.viewport() == null) {
                  return null
              }
              val pt: Point = Point()
              map.viewport().toScreenPoint(GP_CONVERTER.to(gp), false, pt)
              return Int[]{(Int) pt.x, (Int) pt.y}
        }
    }

}
