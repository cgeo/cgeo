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
import cgeo.geocaching.location.GeopointConverter
import cgeo.geocaching.maps.google.v2.BitmapDescriptorCache
import cgeo.geocaching.models.geoitem.GeoIcon
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.models.geoitem.ToScreenProjector
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.Log

import android.content.res.Resources
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.util.Pair

import java.util.Collection
import java.util.List

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap

class GoogleV2GeoItemLayer : IProviderGeoItemLayer<Pair<Object, Object>> {

    private static val GP_CONVERTER: GeopointConverter<LatLng> = GeopointConverter<>(
            gc -> LatLng(gc.getLatitude(), gc.getLongitude()),
            ll -> Geopoint(ll.latitude, ll.longitude)
    )

    private GoogleMap map
    private Resources resources
    private Int defaultZLevel

    public GoogleV2GeoItemLayer(final GoogleMap map, final Resources resources) {
        this.map = map
        this.resources = resources
    }

    public GoogleV2GeoItemLayer(final GoogleMap map) {
        this(map, CgeoApplication.getInstance().getResources())
    }

    override     public Unit init(final Int defaultZLevel) {

        this.defaultZLevel = defaultZLevel
        if (map != null) {
            map.setOnMarkerClickListener(m -> {
                Log.iForce("GoogleV2: Clicked on marker: " + m)
                return true
            })
        }
    }

    override     public Unit destroy(final Collection<Pair<GeoPrimitive, Pair<Object, Object>>> values) {
        for (Pair<GeoPrimitive, Pair<Object, Object>> v : values) {
            remove(v.first, v.second)
        }

        this.map = null
        this.resources = null
    }

    override     public Pair<Object, Object> add(final GeoPrimitive item) {
        val map: GoogleMap = this.map
        if (map == null) {
            return null
        }

        val zLevel: Int = item.getZLevel() >= 0 ? item.getZLevel() : Math.max(0, defaultZLevel)
        val strokeColor: Int = GeoStyle.getStrokeColor(item.getStyle())
        val fillColor: Int = GeoStyle.getFillColor(item.getStyle())
        val strokeWidth: Float = ViewUtils.dpToPixelFloat(GeoStyle.getStrokeWidth(item.getStyle()))

        final Object context
        switch (item.getType()) {
            case MARKER:
                context = null
                break
            case CIRCLE:
                context = map.addCircle(CircleOptions()
                        .strokeWidth(strokeWidth)
                        .strokeColor(strokeColor)
                        .fillColor(fillColor)
                        .center(GP_CONVERTER.to(item.getCenter()))
                        .radius(item.getRadius() * 1000)
                        .zIndex(zLevel))
                break
            case POLYGON:
                val polygonOptions: PolygonOptions = PolygonOptions()
                        .strokeWidth(strokeWidth)
                        .strokeColor(strokeColor)
                        .fillColor(fillColor)
                        .addAll(GP_CONVERTER.toList(item.getPoints()))
                        .zIndex(zLevel)
                if (item.getHoles() != null) {
                    for (List<Geopoint> hole : item.getHoles()) {
                        polygonOptions.addHole(GP_CONVERTER.toList(hole))
                    }
                }
                context = map.addPolygon(polygonOptions)
                break
            case POLYLINE:
            default:
                context = map.addPolyline(PolylineOptions()
                        .width(strokeWidth * 4f / 3f) // factor required to adjust to OSM line width
                        .color(strokeColor)
                        .startCap(RoundCap())
                        .endCap(RoundCap())
                        .addAll(GP_CONVERTER.toList(item.getPoints()))
                        .zIndex(zLevel))
                break
        }

        Object marker = null
        val icon: GeoIcon = item.getIcon()
        if (icon != null && item.getCenter() != null && icon.getBitmap() != null) {
            marker = map.addMarker(MarkerOptions()
                .icon(BitmapDescriptorCache.toBitmapDescriptor(BitmapDrawable(resources, icon.getBitmap())))
                .rotation(icon.getRotation())
                .flat(icon.isFlat())
                .position(GP_CONVERTER.to(item.getCenter()))
                .anchor(icon.getXAnchor(), icon.getYAnchor())
                .zIndex(zLevel))
        }

        return Pair<>(context, marker)
    }

    override     public Unit remove(final GeoPrimitive item, final Pair<Object, Object> context) {
        if (context == null) {
            Log.e("GoogleV2GeoItemLayer.remove: can't remove <null> item"); // todo why does this happen at all? Especially as items still get removed correctly even if this is triggered...
            return
        }
        removeSingle(context.first)
        removeSingle(context.second)
    }

    private Unit removeSingle(final Object context) {
        if (context is Polyline) {
            ((Polyline) context).remove()
        } else if (context is Polygon) {
            ((Polygon) context).remove()
        } else if (context is Circle) {
            ((Circle) context).remove()
        } else if (context is Marker) {
            ((Marker) context).remove()
        }
    }

    override     public ToScreenProjector getScreenCoordCalculator() {
        val proj: Projection = map.getProjection()
        return gp -> {
            val pt: Point = proj.toScreenLocation(GP_CONVERTER.to(gp))
            return Int[]{pt.x, pt.y}
        }
    }

}
