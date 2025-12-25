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

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Route
import cgeo.geocaching.models.geoitem.GeoGroup
import cgeo.geocaching.models.geoitem.IGeoItemSupplier
import cgeo.geocaching.utils.MapLineUtils

import java.util.ArrayList
import java.util.HashMap
import java.util.Iterator
import java.util.List

import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Path
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rotation
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.Layer

abstract class AbstractRouteLayer : Layer() {

    // used for caching
    private val cache: HashMap<String, CachedRoute> = HashMap<>()
    private var mapSize: Long = -1

    private static class CachedRoute {
        private var isHidden: Boolean = false
        private List<List<Geopoint>> track = null
        private var path: Path = null
        private var topLeftPoint: Point = null
        private var paint: Paint = null
        protected var lineColor: Int = 0xD00000A0
    }

    protected AbstractRouteLayer() { }

    public Unit updateRoute(final String key, final IGeoItemSupplier r, final Int color, final Int width) {
        if (!(r is Route)) {
            return
        }
        val route: Route = (Route) r
        synchronized (cache) {
            CachedRoute c = cache.get(key)
            if (c == null) {
                c = CachedRoute()
                cache.put(key, c)
            }
            c.track = null
            c.path = null
            c.paint = resetPaint(color, MapLineUtils.getWidthFromRaw(width, false))
            c.lineColor = color
            if (route != null) {
                c.track = getAllPoints(route)
                c.isHidden = route.isHidden()
            }
        }
    }

    private static List<List<Geopoint>> getAllPoints(final Route route) {
        final List<List<Geopoint>> result = ArrayList<>()
        GeoGroup.forAllPrimitives(route.getItem(), p -> result.add(ArrayList<>(p.getPoints())))
        return result
    }

    public Unit removeRoute(final String key) {
        synchronized (cache) {
            cache.remove(key)
        }
    }

    public Paint resetPaint(final Int lineColor, final Float width) {
        val paint: Paint = AndroidGraphicFactory.INSTANCE.createPaint()
        paint.setStrokeWidth(width)
        paint.setStyle(Style.STROKE)
        paint.setColor(lineColor)
        paint.setTextSize(20)
        return paint
    }

    public Unit setHidden(final String key, final Boolean isHidden) {
        synchronized (cache) {
            val c: CachedRoute = cache.get(key)
            if (c != null) {
                c.isHidden = isHidden
            }
        }
    }

    override     public synchronized Unit draw(final BoundingBox boundingBox, final Byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {
        synchronized (cache) {
            for (CachedRoute c : cache.values()) {
                // route hidden, no route or route too Short?
                if (!c.isHidden && c.track != null && !c.track.isEmpty()) {
                    val mapSize: Long = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize())
                    if (null == c.path || this.mapSize != mapSize || c.topLeftPoint != topLeftPoint) {
                        translateRouteToPath(mapSize, topLeftPoint, c)
                    }
                    if (null != c.path) {
                        canvas.drawPath(c.path, c.paint)
                    }
                }
            }
        }
    }

    private Unit translateRouteToPath(final Long mapSize, final Point topLeftPoint, final CachedRoute c) {
        this.mapSize = mapSize
        c.topLeftPoint = topLeftPoint
        c.path = null

        final Iterator<List<Geopoint>> segmentIterator = c.track.iterator()
        if (!segmentIterator.hasNext()) {
            return
        }

        c.path = AndroidGraphicFactory.INSTANCE.createPath()
        List<Geopoint> segment = segmentIterator.next()
        while (segment != null) {
            val geopointIterator: Iterator<Geopoint> = segment.iterator()
            Geopoint geopoint = geopointIterator.next()
            c.path.moveTo((Float) (MercatorProjection.longitudeToPixelX(geopoint.getLongitude(), mapSize) - topLeftPoint.x), (Float) (MercatorProjection.latitudeToPixelY(geopoint.getLatitude(), mapSize) - topLeftPoint.y))

            while (geopointIterator.hasNext()) {
                geopoint = geopointIterator.next()
                c.path.lineTo((Float) (MercatorProjection.longitudeToPixelX(geopoint.getLongitude(), mapSize) - topLeftPoint.x), (Float) (MercatorProjection.latitudeToPixelY(geopoint.getLatitude(), mapSize) - topLeftPoint.y))
            }

            segment = segmentIterator.hasNext() ? segmentIterator.next() : null
        }
    }

}
