package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.geoitem.GeoIcon;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.models.geoitem.ToScreenProjector;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polygon;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.util.MapViewProjection;

public class MapsforgeV6GeoItemLayer extends Layer implements IProviderGeoItemLayer<Pair<Integer, Integer>> {

    private LayerManager layerManager;
    private final MapViewProjection projection;

    private final Lock layerLock = new ReentrantLock();

    //SORTED map of zLevels -> used to paint objects in right z-order
    private final SortedMap<Integer, Map<Integer, Pair<Layer, Layer>>> layerItemMap = new TreeMap<>();
    private final AtomicInteger idProvider = new AtomicInteger(0);

    //statistic values for logging + info
    private int statGeoObjectCount = 0;
    private int statMarkerCount = 0;
    private int statZLevelCount = 0;

    public MapsforgeV6GeoItemLayer(final LayerManager layerManager, final MapViewProjection projection) {
        this.layerManager = layerManager;
        this.projection = projection;
    }

    @Override
    public void init(final int zLevel) {
        Log.iForce("AsyncMapWrapper: Init Layer");
        this.layerManager.getLayers().add(this);
    }

    @Override
    public void destroy(final Collection<Pair<GeoPrimitive, Pair<Integer, Integer>>> values) {
        Log.iForce("Destroy Layer");
        layerLock.lock();
        try {
            if (this.layerManager != null) {
                this.layerManager.getLayers().remove(this);
            }
            this.layerManager = null;
            this.layerItemMap.clear();
            this.statMarkerCount = 0;
            this.statGeoObjectCount = 0;
            this.statZLevelCount = 0;
        } finally {
                layerLock.unlock();
        }

    }

    @Override
    public Pair<Integer, Integer> add(final GeoPrimitive item) {

        final Paint strokePaint = createPaint(GeoStyle.getStrokeColor(item.getStyle()));
        strokePaint.setStrokeWidth(ViewUtils.dpToPixelFloat(GeoStyle.getStrokeWidth(item.getStyle())));
        strokePaint.setStyle(Style.STROKE);
        final Paint fillPaint = createPaint(GeoStyle.getFillColor(item.getStyle()));
        fillPaint.setStyle(Style.FILL);
        final Layer goLayer;
        switch (item.getType()) {
            case MARKER:
                goLayer = null;
                break;
            case CIRCLE:
                if (item.getCenter() == null || item.getRadius() <= 0) {
                    goLayer = null;
                } else {
                    goLayer = new Circle(latLong(item.getCenter()), item.getRadius() * 1000, fillPaint, strokePaint);
                }
                break;
            case POLYLINE:
                final Polyline pl = new Polyline(strokePaint, AndroidGraphicFactory.INSTANCE);
                pl.addPoints(CollectionStream.of(item.getPoints()).map(MapsforgeV6GeoItemLayer::latLong).toList());
                goLayer = pl;
                break;
            case POLYGON:
            default:
                final Polygon po = new Polygon(fillPaint, strokePaint, AndroidGraphicFactory.INSTANCE);
                po.addPoints(CollectionStream.of(item.getPoints()).map(MapsforgeV6GeoItemLayer::latLong).toList());
                if (item.getHoles() != null) {
                    for (List<Geopoint> hole : item.getHoles()) {
                        po.addHole(CollectionStream.of(hole).map(MapsforgeV6GeoItemLayer::latLong).toList());
                    }
                }
                goLayer = po;
                break;
        }
        if (goLayer != null) {
            goLayer.setDisplayModel(getDisplayModel());
        }

        final Marker marker = createMarker(item.getCenter(), item.getIcon());
        if (marker != null) {
            marker.setDisplayModel(getDisplayModel());
        }

        final int zlevel = Math.max(0, item.getZLevel());

        return addToMap(zlevel, goLayer, marker);
    }

    private Pair<Integer, Integer> addToMap(final int zLevel, @Nullable final Layer goLayer, @Nullable final Layer marker) {
        layerLock.lock();
        try {
            final int itemId = idProvider.addAndGet(1);
            Map<Integer, Pair<Layer, Layer>> zLevelMap = this.layerItemMap.get(zLevel);
            if (zLevelMap == null) {
                zLevelMap = new HashMap<>();
                this.layerItemMap.put(zLevel, zLevelMap);
                statZLevelCount++;
            }
            zLevelMap.put(itemId, new Pair<>(goLayer, marker));
            if (goLayer != null) {
                statGeoObjectCount++;
            }
            if (marker != null) {
                statMarkerCount++;
            }
            return new Pair<>(zLevel, itemId);
        } finally {
            layerLock.unlock();
        }
    }

    private void removeFromMap(@NonNull final Pair<Integer, Integer> context) {
        layerLock.lock();
        try {
            final Map<Integer, Pair<Layer, Layer>> zLevelMap = this.layerItemMap.get(context.first);
            if (zLevelMap == null) {
                return;
            }
            final Pair<Layer, Layer> removedObject = zLevelMap.remove(context.second);
            if (removedObject != null) {
                if (removedObject.first != null) {
                    statGeoObjectCount--;
                }
                if (removedObject.second != null) {
                    statMarkerCount--;
                }
            }
        } finally {
            layerLock.unlock();
        }
    }

    @Override
    public void remove(final GeoPrimitive item, final Pair<Integer, Integer> context) {
        removeFromMap(context);
    }

    @Override
    public String onMapChangeBatchEnd(final long processedCount) {
        if (layerManager == null || processedCount == 0) {
            return null;
        }
        requestRedraw();
        return "go:" + statGeoObjectCount + ",marker:" + statMarkerCount + ",zLevel:" + statZLevelCount;
    }

    private static Marker createMarker(final Geopoint point, final GeoIcon icon) {
        if (point == null || icon == null || icon.getBitmap() == null) {
            return null;
        }
        final Bitmap bitmap = icon.getRotatedBitmap();
        if (bitmap == null) {
            return null;
        }

        final Marker newMarker = new Marker(
                latLong(point),
                AndroidGraphicFactory.convertToBitmap(new BitmapDrawable(CgeoApplication.getInstance().getResources(), bitmap)),
                (int) ((-icon.getXAnchor() + 0.5f) * bitmap.getWidth()),
                (int) ((-icon.getYAnchor() + 0.5f) * bitmap.getHeight()));
        newMarker.setBillboard(!icon.isFlat());
        return newMarker;
    }

    private static Paint createPaint(@ColorInt final int color) {
        final Paint p = AndroidGraphicFactory.INSTANCE.createPaint();
        p.setColor(color);
        return p;
    }

    private static LatLong latLong(final Geopoint gp) {
        return new LatLong(gp.getLatitude(), gp.getLongitude());
    }


    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {
        layerLock.lock();
        try {
            for (Map<Integer, Pair<Layer, Layer>> zLevelMap : this.layerItemMap.values()) {
                for (Pair<Layer, Layer> entry : zLevelMap.values()) {
                    if (entry.first != null) {
                        entry.first.draw(boundingBox, zoomLevel, canvas, topLeftPoint, rotation);
                    }
                    if (entry.second != null) {
                        entry.second.draw(boundingBox, zoomLevel, canvas, topLeftPoint, rotation);
                    }
                }
            }
        } finally {
            layerLock.unlock();
        }
    }

    @Override
    public ToScreenProjector getScreenCoordCalculator() {
        return gp -> {
            final Point pt = projection.toPixels(latLong(gp));
            return new int[]{(int) pt.x, (int) pt.y};
        };
    }

}
