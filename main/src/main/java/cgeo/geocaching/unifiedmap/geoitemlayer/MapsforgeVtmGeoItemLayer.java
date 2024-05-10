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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;
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
import org.oscim.utils.geom.GeomBuilder;

public class MapsforgeVtmGeoItemLayer implements IProviderGeoItemLayer<Pair<Drawable, MarkerInterface>> {

    private static final GeopointConverter<GeoPoint> GP_CONVERTER = new GeopointConverter<>(
            gc -> new GeoPoint(gc.getLatitude(), gc.getLongitude()),
            ll -> new Geopoint(ll.latitudeE6, ll.longitudeE6)
    );

    private Map map;
    private GroupedList<Layer> mapLayers;

    private final java.util.Map<Integer, PopulateControlledItemizedLayer> markerLayerMap = new HashMap<>();
    private final java.util.Map<Integer, VectorLayer> vectorLayerMap = new HashMap<>();
    private final Lock layerMapLock = new ReentrantLock();

    private MarkerSymbol defaultMarkerSymbol;

    private int defaultZLevel = 0;

    private final Set<Integer> markerLayersForRefresh = new HashSet<>();

    public static class PopulateControlledItemizedLayer extends ItemizedLayer {

        public PopulateControlledItemizedLayer(final Map map, final MarkerSymbol defaultMarker) {
            super(map, defaultMarker);
        }

        /** mimics {@link #addItem(MarkerInterface)} but w/o calling 'populate' */
        public boolean addItemNoPopulate(final MarkerInterface marker) {
            return this.mItemList.add(marker);
        }

        /** mimics {@link #removeItem(MarkerInterface)} but w/o calling 'populate' */
        public boolean removeItemNoPopulate(final MarkerInterface marker) {
            return this.mItemList.remove(marker);
        }
    }

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
                    //create marker layer first, then vectorlayer. That way, markers will always be shown above shapes with same zLevel
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

    private PopulateControlledItemizedLayer getMarkerLayer(final int zLevel, final boolean createIfNonexisting) {
        return getZLevelLayer(zLevel, markerLayerMap, PopulateControlledItemizedLayer.class, createIfNonexisting ? () -> new PopulateControlledItemizedLayer(map, defaultMarkerSymbol) : null);
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
        final float rawStrokeWidth = GeoStyle.getStrokeWidth(item.getStyle()) / 1.5f;
        final Style style = Style.builder()
                .strokeWidth(ViewUtils.dpToPixelFloat(rawStrokeWidth))
                .strokeColor(GeoStyle.getStrokeColor(item.getStyle()))
                .fillAlpha(1f) // GeoJsonUtils.colorFromJson() already calculates the color using fill and fill-opacity, don't apply it again
                .fillColor(fillColor)
                .transparent(true) ////See #15029. Following parameter prevents rendering of "darker edges" for overlapping semi-transparent route parts
                .dropDistance(ViewUtils.dpToPixelFloat(1)) //see #15029. This setting stops rendering route parts at some point when zooming out
                .cap(Paint.Cap.BUTT)
                .fixed(true)
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
                //we have to construct our own GeomBuilder
                //because standard constructorss of PoygonDrawable doesn't support multiple holes
                final GeomBuilder gb = new GeomBuilder();
                addRingToGeoBuilder(gb, item.getPoints());
                if (item.getHoles() != null) {
                    for (List<Geopoint> hole : item.getHoles()) {
                        addRingToGeoBuilder(gb, hole);
                    }
                }
                drawable = new PolygonDrawable(gb.toPolygon(), style);
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
            final PopulateControlledItemizedLayer markerLayer = getMarkerLayer(zLevel, true);
            final GeoIcon icon = item.getIcon();
            marker = new MarkerItem("", "", GP_CONVERTER.to(item.getCenter()));
            marker.setMarker(new MarkerSymbol(new AndroidBitmap(icon.getBitmap()),
                    icon.getXAnchor(), icon.getYAnchor(), !icon.isFlat()));
            marker.setRotation(item.getIcon().getRotation());
            markerLayer.addItemNoPopulate(marker);
            markerLayersForRefresh.add(zLevel);
        }


        return new Pair<>(drawable, marker);
    }

    private static void addRingToGeoBuilder(final GeomBuilder gb, final List<Geopoint> ring) {
        for (Geopoint pt : ring) {
            final GeoPoint gpt = GP_CONVERTER.to(pt);
            gb.point(gpt.getLongitude(), gpt.getLatitude());
        }
        gb.ring();
    }

    @Override
    public void remove(final GeoPrimitive item, final Pair<Drawable, MarkerInterface> context) {

        if (context == null) {
            return;
        }

        final int zLevel = getZLevel(item);
        if (context.first != null) {
            final VectorLayer vectorLayer = getVectorLayer(zLevel, false);
            if (vectorLayer != null) {
                vectorLayer.remove(context.first);
                vectorLayer.update();
            }
        }
        if (context.second != null) {
            final PopulateControlledItemizedLayer markerLayer = getMarkerLayer(zLevel, false);
            if (markerLayer != null) {
                markerLayer.removeItemNoPopulate(context.second);
                markerLayersForRefresh.add(zLevel);
            }
        }
    }

    @Override
    public void onMapChangeBatchEnd(final long processedCount) {
        if (map == null || processedCount == 0) {
            return;
        }

        //populate and update marker layers which were touched
        for (int markerZLevelToRefresh : markerLayersForRefresh) {
            final PopulateControlledItemizedLayer markerLayer = getMarkerLayer(markerZLevelToRefresh, false);
            if (markerLayer == null) {
                continue;
            }
            markerLayer.populate();
            markerLayer.update();
        }
        markerLayersForRefresh.clear();

        //make sure map is redrawn. See e.g. #14787
        map.updateMap(true);

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
