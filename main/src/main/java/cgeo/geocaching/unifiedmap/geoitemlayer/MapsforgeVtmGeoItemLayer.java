package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.models.geoitem.ToScreenProjector;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.GroupedList;

import android.graphics.BitmapFactory;
import android.util.Pair;

import androidx.core.util.Supplier;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeoPoint;
import org.oscim.core.Point;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.CircleDrawable;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.PolygonDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;

public class MapsforgeVtmGeoItemLayer implements IProviderGeoItemLayer<Pair<Drawable, MarkerInterface>> {

    private static final GeopointConverter<GeoPoint> GP_CONVERTER = new GeopointConverter<>(
            gc -> new GeoPoint(gc.getLatitude(), gc.getLongitude()),
            ll -> new Geopoint(ll.latitudeE6, ll.longitudeE6)
    );

    private Map map;
    private GroupedList<Layer> mapLayers;

    private final java.util.Map<Integer, ItemizedLayer> markerLayerMap = new HashMap<>();
    private final java.util.Map<Integer, VectorLayer> vectorLayerMap = new HashMap<>();
    private final Lock layerMapLock = new ReentrantLock();

    private MarkerSymbol defaultMarkerSymbol;

    private int defaultZLevel = 0;

    public MapsforgeVtmGeoItemLayer(final Map map, final GroupedList<Layer> mapLayers) {
        this.map = map;
        this.mapLayers = mapLayers;
    }

    @Override
    public void init(final int zLevel) {
        defaultZLevel = Math.max(0, zLevel);

        //initialize marker layer stuff
        final Bitmap bitmap = new AndroidBitmap(BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.cgeo_notification));
        defaultMarkerSymbol = new MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);

    }

    @SuppressWarnings("unchecked")
    private <T extends Layer> T getZLevelLayer(final int zLevel, final java.util.Map<Integer, T> layerMap, final Class<T> layerClass, final Supplier<T> layerCreator) {
        layerMapLock.lock();
        try {
            T zLayer = layerMap.get(zLevel);
            if (zLayer != null || layerCreator == null) {
                return zLayer;
            }

            //search for an existing layer, create if not existing
            synchronized (mapLayers) {
                final int layerIdx = mapLayers.groupIndexOf(zLevel, c -> layerClass.isAssignableFrom(c.getClass()));
                if (layerIdx > 0) {
                    zLayer = (T) mapLayers.get(layerIdx);
                } else {
                    zLayer = layerCreator.get();
                    mapLayers.addToGroup(zLayer, zLevel);
                }
            }

            layerMap.put(zLevel, zLayer);

            return zLayer;

        } finally {
            layerMapLock.unlock();
        }
    }

    private ItemizedLayer getMarkerLayer(final int zLevel, final boolean createIfNonexisting) {
        return getZLevelLayer(zLevel, markerLayerMap, ItemizedLayer.class, createIfNonexisting ? () -> new ItemizedLayer(map, defaultMarkerSymbol) : null);
    }

    private VectorLayer getVectorLayer(final int zLevel, final boolean createIfNonexisting) {
        return getZLevelLayer(zLevel, vectorLayerMap, VectorLayer.class, createIfNonexisting ? () -> new VectorLayer(map) : null);
    }

    private int getZLevel(final GeoPrimitive item) {
        if (item != null && item.getZLevel() >= 0) {
            return item.getZLevel() + 20;
        }
        return defaultZLevel + 20;
    }

    @Override
    public Pair<Drawable, MarkerInterface> add(final GeoPrimitive item) {

        final int fillColor = GeoStyle.getFillColor(item.getStyle());
        final Style style = Style.builder()
                .strokeWidth(ViewUtils.dpToPixel(GeoStyle.getStrokeWidth(item.getStyle()) / 2f))
                .strokeColor(GeoStyle.getStrokeColor(item.getStyle()))
                .fillAlpha(Color.aToFloat(fillColor))
                .fillColor(fillColor)
                .build();
        final int zLevel = getZLevel(item);

        Drawable drawable = null;
        switch (item.getType()) {
            case MARKER:
                break;
            case CIRCLE:
                drawable = new CircleDrawable(GP_CONVERTER.to(item.getCenter()), item.getRadius(), style);
                break;
            case POLYGON:
                drawable = new PolygonDrawable(GP_CONVERTER.toList(item.getPoints()), style);
                break;
            case POLYLINE:
            default:
                drawable = new LineDrawable(GP_CONVERTER.toList(item.getPoints()), style);
                break;
        }

        if (drawable != null) {
            final VectorLayer vectorLayer = getVectorLayer(zLevel, true);
            vectorLayer.add(drawable);
            vectorLayer.update();
        }

        MarkerItem marker = null;
        if (item.getIcon() != null) {
            final ItemizedLayer markerLayer = getMarkerLayer(zLevel, true);
            final GeoIcon icon = item.getIcon();
            marker = new MarkerItem("", "", GP_CONVERTER.to(item.getCenter()));
            marker.setMarker(new MarkerSymbol(new AndroidBitmap(icon.getBitmap()),
                    icon.getXAnchor(), icon.getYAnchor(), true));
            marker.setRotation(item.getIcon().getRotation());
            markerLayer.addItem(marker);
            markerLayer.update();
        }

        return new Pair<>(drawable, marker);
    }

    @Override
    public void remove(final GeoPrimitive item, final Pair<Drawable, MarkerInterface> context) {
        final int zLevel = getZLevel(item);
        if (context.first != null) {
            final VectorLayer vectorLayer = getVectorLayer(zLevel, false);
            if (vectorLayer != null) {
                vectorLayer.remove(context.first);
                vectorLayer.update();
            }
        }
        if (context.second != null) {
            final ItemizedLayer markerLayer = getMarkerLayer(zLevel, false);
            if (markerLayer != null) {
                markerLayer.removeItem(context.second);
            }
        }
    }

    @Override
    public void destroy(final Collection<Pair<GeoPrimitive, Pair<Drawable, MarkerInterface>>> values) {

        for (Pair<GeoPrimitive, Pair<Drawable, MarkerInterface>> v : values) {
            remove(v.first, v.second);
        }

        map = null;
        mapLayers = null;
        markerLayerMap.clear();
        vectorLayerMap.clear();
        defaultMarkerSymbol = null;
    }

    @Override
    public ToScreenProjector getScreenCoordCalculator() {

          return gp -> {
              if (map == null || map.viewport() == null) {
                  return null;
              }
              final Point pt = new Point();
              map.viewport().toScreenPoint(GP_CONVERTER.to(gp), false, pt);
              return new int[]{(int) pt.x, (int) pt.y};
        };
    }

}
