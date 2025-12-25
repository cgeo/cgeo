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
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.geoitem.GeoIcon
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.models.geoitem.ToScreenProjector
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.Log

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Pair

import androidx.annotation.ColorInt
import androidx.annotation.NonNull

import java.util.Collection
import java.util.List

import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.LayerManager
import org.mapsforge.map.layer.overlay.Circle
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.overlay.Polygon
import org.mapsforge.map.layer.overlay.Polyline

class MapsforgeV6GeoItemLayer : IProviderGeoItemLayer<Int[]> {

    private MapView mapView
    private LayerManager layerManager
    private Int defaultZLevel

    private MapsforgeV6ZLevelGroupLayer groupLayer

    public MapsforgeV6GeoItemLayer(final MapView mapView) {
        this.mapView = mapView
        this.layerManager = mapView.getLayerManager()
    }

    override     public Unit init(final Int defaultZLevel) {
        Log.iForce("AsyncMapWrapper: Init Layer")
        this.defaultZLevel = defaultZLevel

        //find or create zLeve-aware group layer
        synchronized (this.layerManager.getLayers()) {
            for (Layer layer : this.layerManager.getLayers()) {
                if (layer is MapsforgeV6ZLevelGroupLayer) {
                    groupLayer = (MapsforgeV6ZLevelGroupLayer) layer
                    break
                }
            }
            if (groupLayer == null) {
                groupLayer = MapsforgeV6ZLevelGroupLayer()
                this.layerManager.getLayers().add(groupLayer)
            }
        }
    }

    override     public Unit destroy(final Collection<Pair<GeoPrimitive, Int[]>> values) {
        Log.iForce("Destroy Layer")
        this.layerManager = null
        if (groupLayer != null) {
            for (Pair<GeoPrimitive, Int[]> entry : values) {
                groupLayer.remove(false, entry.second)
            }
            groupLayer.requestRedraw()
        }
        this.mapView = null
    }

    override     public Int[] add(final GeoPrimitive item) {

        val strokePaint: Paint = createPaint(GeoStyle.getStrokeColor(item.getStyle()))
        strokePaint.setStrokeWidth(ViewUtils.dpToPixelFloat(GeoStyle.getStrokeWidth(item.getStyle())))
        strokePaint.setStyle(Style.STROKE)
        val fillPaint: Paint = createPaint(GeoStyle.getFillColor(item.getStyle()))
        fillPaint.setStyle(Style.FILL)
        final Layer goLayer
        switch (item.getType()) {
            case MARKER:
                goLayer = null
                break
            case CIRCLE:
                if (item.getCenter() == null || item.getRadius() <= 0) {
                    goLayer = null
                } else {
                    goLayer = Circle(latLong(item.getCenter()), item.getRadius() * 1000, fillPaint, strokePaint)
                }
                break
            case POLYLINE:
                val pl: Polyline = Polyline(strokePaint, AndroidGraphicFactory.INSTANCE)
                pl.addPoints(CollectionStream.of(item.getPoints()).map(MapsforgeV6GeoItemLayer::latLong).toList())
                goLayer = pl
                break
            case POLYGON:
            default:
                val po: Polygon = Polygon(fillPaint, strokePaint, AndroidGraphicFactory.INSTANCE)
                po.addPoints(CollectionStream.of(item.getPoints()).map(MapsforgeV6GeoItemLayer::latLong).toList())
                if (item.getHoles() != null) {
                    for (List<Geopoint> hole : item.getHoles()) {
                        po.addHole(CollectionStream.of(hole).map(MapsforgeV6GeoItemLayer::latLong).toList())
                    }
                }
                goLayer = po
                break
        }
        if (goLayer != null) {
            goLayer.setDisplayModel(groupLayer.getDisplayModel())
        }

        val marker: Marker = createMarker(item.getCenter(), item.getIcon())
        if (marker != null) {
            marker.setDisplayModel(groupLayer.getDisplayModel())
        }

        val zlevel: Int = item.getZLevel() <= 0 ? defaultZLevel : item.getZLevel()

        return groupLayer.add(zlevel, false, goLayer, marker)
    }

    override     public Unit remove(final GeoPrimitive item, final Int[] context) {
        groupLayer.remove(false, context)
    }

    override     public String onMapChangeBatchEnd(final Long processedCount) {
        if (layerManager == null || processedCount == 0) {
            return null
        }
        groupLayer.requestRedraw()
        return null
    }

    private static Marker createMarker(final Geopoint point, final GeoIcon icon) {
        if (point == null || icon == null || icon.getBitmap() == null) {
            return null
        }
        val bitmap: Bitmap = icon.getRotatedBitmap()
        if (bitmap == null) {
            return null
        }

        val newMarker: Marker = Marker(
                latLong(point),
                AndroidGraphicFactory.convertToBitmap(BitmapDrawable(CgeoApplication.getInstance().getResources(), bitmap)),
                (Int) ((-icon.getXAnchor() + 0.5f) * bitmap.getWidth()),
                (Int) ((-icon.getYAnchor() + 0.5f) * bitmap.getHeight()))
        newMarker.setBillboard(!icon.isFlat())
        return newMarker
    }

    private static Paint createPaint(@ColorInt final Int color) {
        val p: Paint = AndroidGraphicFactory.INSTANCE.createPaint()
        p.setColor(color)
        return p
    }

    private static LatLong latLong(final Geopoint gp) {
        return LatLong(gp.getLatitude(), gp.getLongitude())
    }

    override     public ToScreenProjector getScreenCoordCalculator() {
        return gp -> {
            if (mapView == null || gp == null) {
                return Int[] { 0, 0 }
            }
            val pt: Point = projectLatLon(mapView, latLong(gp))
            return Int[]{(Int) pt.x, (Int) pt.y}
        }
    }

    /**
     * projects a latlon to a screen coordinate. Accomodates for all visual effects
     * eg zooming, rotation, tilting.
     */
    private static Point projectLatLon(final MapView mapView, final LatLong latLong) {
        return mapView.getMapViewProjection().toPixels(latLong, true)
    }

}
