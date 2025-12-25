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
import cgeo.geocaching.models.geoitem.GeoGroup
import cgeo.geocaching.models.geoitem.GeoItem
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.functions.Func1

import androidx.annotation.ColorInt

import java.util.HashMap
import java.util.List
import java.util.Map

import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.GroupLayer
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.overlay.FixedPixelCircle
import org.mapsforge.map.layer.overlay.Polygon
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.model.DisplayModel

class GeoObjectLayer : GroupLayer() {

    private val geoObjectLayers: Map<String, Layer> = HashMap<>()

    public Unit addGeoObjectLayer(final String key, final Layer goLayer) {
        removeGeoObjectLayer(key)
        geoObjectLayers.put(key, goLayer)
        this.layers.add(goLayer)
    }

    public static Layer createGeoObjectLayer(final GeoItem objects, final DisplayModel displayModel,
            final Float defaultWidth, final Int defaultStrokeColor, final Int defaultFillColor, final Func1<Float, Float> widthAdjuster) {
        val gl: GroupLayer = GroupLayer()
        gl.setDisplayModel(displayModel)
        GeoGroup.forAllPrimitives(objects, item -> {
            val strokePaint: Paint = createPaint(GeoStyle.getStrokeColor(item.getStyle(), defaultStrokeColor))
            strokePaint.setStrokeWidth(widthAdjuster.call(GeoStyle.getStrokeWidth(item.getStyle(), defaultWidth)))
            strokePaint.setStyle(Style.STROKE)
            val fillPaint: Paint = createPaint(GeoStyle.getFillColor(item.getStyle(), defaultFillColor))
            fillPaint.setStyle(Style.FILL)
            final Layer goLayer
            switch (item.getType()) {
                case MARKER:
                case CIRCLE:
                    val radius: Float = item.getType() == GeoItem.GeoType.MARKER ? 5f : item.getRadius() * 10
                    goLayer = FixedPixelCircle(latLong(item.getPoints().get(0)), widthAdjuster.call(radius), strokePaint, strokePaint)
                    break
                case POLYLINE:
                    val pl: Polyline = Polyline(strokePaint, AndroidGraphicFactory.INSTANCE)
                    pl.addPoints(CollectionStream.of(item.getPoints()).map(GeoObjectLayer::latLong).toList())
                    goLayer = pl
                    break
                case POLYGON:
                default:
                    val po: Polygon = Polygon(fillPaint, strokePaint, AndroidGraphicFactory.INSTANCE)
                    po.addPoints(CollectionStream.of(item.getPoints()).map(GeoObjectLayer::latLong).toList())
                    if (item.getHoles() != null) {
                        for (List<Geopoint> hole : item.getHoles()) {
                            po.addHole(CollectionStream.of(hole).map(GeoObjectLayer::latLong).toList())
                        }
                    }
                    goLayer = po
                    break
            }
            goLayer.setDisplayModel(displayModel)
            gl.layers.add(goLayer)
        })
        return gl
    }

    public Unit removeGeoObjectLayer(final String key) {
        val oldLayer: Layer = geoObjectLayers.remove(key)
        if (oldLayer != null) {
            this.layers.remove(oldLayer)
        }
    }

    private static Paint createPaint(@ColorInt final Int color) {
        val p: Paint = AndroidGraphicFactory.INSTANCE.createPaint()
        p.setColor(color)
        return p
    }

    private static LatLong latLong(final Geopoint gp) {
        return LatLong(gp.getLatitude(), gp.getLongitude())
    }


}
