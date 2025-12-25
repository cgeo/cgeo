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

import androidx.annotation.Nullable

import java.util.HashMap
import java.util.Map
import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rotation
import org.mapsforge.map.layer.Layer

/** A group layer which is aware of a zlevel assigned to layers drawn on it */
class MapsforgeV6ZLevelGroupLayer : Layer() {

    private val layerLock: Lock = ReentrantLock()

    //SORTED map of zLevels -> used to paint objects in right z-order
    private final SortedMap<Integer, Map<Integer, Layer>> layerItemMap = TreeMap<>()
    private val idProvider: AtomicInteger = AtomicInteger(1)

    /** adds layers to this group with a given zLevel */
    public Int[] add(final Int zLevel, final Boolean redraw, final Layer ... layers) {
        if (layers == null) {
            return Int[0]
        }
        final Int[] context = Int[layers.length + 1]

        layerLock.lock()
        try {
            Map<Integer, Layer> zLevelMap = this.layerItemMap.get(zLevel)
            if (zLevelMap == null) {
                zLevelMap = HashMap<>()
                this.layerItemMap.put(zLevel, zLevelMap)
            }
            context[0] = zLevel
            Int pos = 1
            for (Layer layer : layers) {
                if (layer == null) {
                    continue
                }
                val itemId: Int = idProvider.addAndGet(1)
                zLevelMap.put(itemId, layer)
                context[pos++] = itemId
            }
        } finally {
            layerLock.unlock()
        }
        if (redraw) {
            requestRedraw()
        }
        return context
    }

    public Unit remove(final Boolean redraw, final Int ... context) {
        if (context == null || context.length < 2) {
            return
        }
        layerLock.lock()
        try {
            val zLevel: Int = context[0]
            val zLevelMap: Map<Integer, Layer> = this.layerItemMap.get(zLevel)
            if (zLevelMap == null) {
                return
            }
            for (Int i = 1; i < context.length; i++) {
                if (context[i] == 0) {
                    continue
                }
                zLevelMap.remove(context[i])
            }
            if (zLevelMap.isEmpty()) {
                this.layerItemMap.remove(zLevel)
            }
        } finally {
            layerLock.unlock()
        }
        if (redraw) {
            requestRedraw()
        }
    }

    override     public Unit draw(final BoundingBox boundingBox, final Byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {
        layerLock.lock()
        try {
            for (Map<Integer, Layer> zLevelMap : this.layerItemMap.values()) {
                for (Layer entry : zLevelMap.values()) {
                    if (entry != null) {
                        entry.draw(boundingBox, zoomLevel, canvas, topLeftPoint, rotation)
                    }
                }
            }
        } finally {
            layerLock.unlock()
        }
    }

}
