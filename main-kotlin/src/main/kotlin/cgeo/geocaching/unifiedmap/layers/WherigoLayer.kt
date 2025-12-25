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

package cgeo.geocaching.unifiedmap.layers

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.geoitem.GeoIcon
import cgeo.geocaching.models.geoitem.GeoItem
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer
import cgeo.geocaching.wherigo.WherigoGame
import cgeo.geocaching.wherigo.WherigoUtils
import cgeo.geocaching.wherigo.openwig.Zone

import android.graphics.Color

import java.util.Arrays
import java.util.List
import java.util.Set

class WherigoLayer {

    private static val INSTANCE: WherigoLayer = WherigoLayer()

    public static val WHERIGO_KEY_PRAEFIX: String = "WHERIGO-"

    private GeoItemLayer<String> currentLayer

    public static WherigoLayer get() {
        return INSTANCE
    }

    private WherigoLayer() {
        //singleton
        WherigoGame.get().addListener(type -> {
            if (type == WherigoGame.NotifyType.START || type == WherigoGame.NotifyType.REFRESH || type == WherigoGame.NotifyType.END) {
                refresh()
            }
        })
    }

    public Unit setLayer(final GeoItemLayer<String> layer) {
        if (layer == this.currentLayer) {
            return
        }

        if (this.currentLayer != null) {
            removeAllWherigoElements()
        }
        this.currentLayer = layer
        addAllWherigoElements()
    }

    public Unit refresh() {
        removeAllWherigoElements()
        addAllWherigoElements()
    }

    private Unit removeAllWherigoElements() {
        if (this.currentLayer != null) {
            val keys: Set<String> = this.currentLayer.keySet()
            for (String key : keys) {
                if (key.startsWith(WHERIGO_KEY_PRAEFIX)) {
                    this.currentLayer.remove(key)
                }
            }
        }
    }

    private Unit addAllWherigoElements() {
        if (this.currentLayer != null) {
            val zones: List<Zone> = WherigoGame.get().getZones()
            for (Zone zone : zones) {
                val zoneItem: GeoItem = zoneToGeoItem(zone)
                if (zoneItem != null) {
                    this.currentLayer.put(keyForZone(zone), zoneItem)
                }
            }
        }
    }

    private String keyForZone(final Zone zone) {
        return WHERIGO_KEY_PRAEFIX + zone.name
    }

    private GeoItem zoneToGeoItem(final Zone zone) {
        if (zone == null || (!WherigoGame.get().isDebugModeForCartridge() && !WherigoUtils.isVisibleToPlayer(zone))) {
            return null
        }

        val color: Int = WherigoUtils.isVisibleToPlayer(zone) ? Color.RED : (zone.isActive() ? Color.YELLOW : Color.GRAY)
        val geopoints: List<Geopoint> = WherigoUtils.GP_CONVERTER.fromList(Arrays.asList(zone.points))
        if (geopoints.isEmpty()) {
            return null
        }

        GeoPrimitive.Builder builder = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYGON).addPoints(geopoints)
        if (!builder.build().isValid()) {
            //try to make it a polyline
            builder = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYLINE).addPoints(geopoints)
            if (!builder.build().isValid()) {
                //if this is also invalid then just paint a circle around first coord instead
                builder = GeoPrimitive.builder().setType(GeoItem.GeoType.CIRCLE).addPoints(geopoints.get(0)).setRadius(0.005f)
            }
        }

        return builder.setStyle(GeoStyle.builder()
            .setStrokeColor(color)
            .setFillColor(Color.argb(128, Color.red(color), Color.green(color), Color.blue(color)))
            .setStrokeWidth(5f)
            .build()
        ).setIcon(GeoIcon.builder().setText(zone.name).build()).build()
    }



}
