package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.wherigo.WherigoGame;
import cgeo.geocaching.wherigo.WherigoUtils;
import cgeo.geocaching.wherigo.openwig.Zone;

import android.graphics.Color;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class WherigoLayer {

    private static final WherigoLayer INSTANCE = new WherigoLayer();

    public static final String WHERIGO_KEY_PRAEFIX = "WHERIGO-";

    private GeoItemLayer<String> currentLayer;

    public static WherigoLayer get() {
        return INSTANCE;
    }

    private WherigoLayer() {
        //singleton
        WherigoGame.get().addListener(type -> {
            if (type == WherigoGame.NotifyType.START || type == WherigoGame.NotifyType.REFRESH || type == WherigoGame.NotifyType.END) {
                refresh();
            }
        });
    }

    public void setLayer(final GeoItemLayer<String> layer) {
        if (layer == this.currentLayer) {
            return;
        }

        if (this.currentLayer != null) {
            removeAllWherigoElements();
        }
        this.currentLayer = layer;
        addAllWherigoElements();
    }

    public void refresh() {
        removeAllWherigoElements();
        addAllWherigoElements();
    }

    private void removeAllWherigoElements() {
        if (this.currentLayer != null) {
            final Set<String> keys = this.currentLayer.keySet();
            for (String key : keys) {
                if (key.startsWith(WHERIGO_KEY_PRAEFIX)) {
                    this.currentLayer.remove(key);
                }
            }
        }
    }

    private void addAllWherigoElements() {
        if (this.currentLayer != null) {
            final List<Zone> zones = WherigoGame.get().getZones();
            for (Zone zone : zones) {
                final GeoItem zoneItem = zoneToGeoItem(zone);
                if (zoneItem != null) {
                    this.currentLayer.put(keyForZone(zone), zoneItem);
                }
            }
        }
    }

    private String keyForZone(final Zone zone) {
        return WHERIGO_KEY_PRAEFIX + zone.name;
    }

    private GeoItem zoneToGeoItem(final Zone zone) {
        if (zone == null || (!WherigoGame.get().isDebugModeForCartridge() && !WherigoUtils.isVisibleToPlayer(zone))) {
            return null;
        }

        final int color = WherigoUtils.isVisibleToPlayer(zone) ? Color.RED : Color.GRAY;
        final List<Geopoint> geopoints = WherigoUtils.GP_CONVERTER.fromList(Arrays.asList(zone.points));
        if (geopoints.isEmpty()) {
            return null;
        }

        GeoPrimitive.Builder builder = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYGON).addPoints(geopoints);
        if (!builder.build().isValid()) {
            //try to make it a polyline
            builder = GeoPrimitive.builder().setType(GeoItem.GeoType.POLYLINE).addPoints(geopoints);
            if (!builder.build().isValid()) {
                //if this is also invalid then just paint a circle around first coord instead
                builder = GeoPrimitive.builder().setType(GeoItem.GeoType.CIRCLE).addPoints(geopoints.get(0)).setRadius(0.005f);
            }
        }

        return builder.setStyle(GeoStyle.builder()
            .setStrokeColor(color)
            .setFillColor(Color.argb(128, Color.red(color), Color.green(color), Color.blue(color)))
            .setStrokeWidth(5f)
            .build()
        ).setIcon(GeoIcon.builder().setText(zone.name).build()).build();
    }



}
