package cgeo.geocaching.unifiedmap.geoitemlayer;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.layer.Layer;

/** A group layer which is aware of a zlevel assigned to layers drawn on it */
public class MapsforgeV6ZLevelGroupLayer extends Layer {

    private final Lock layerLock = new ReentrantLock();

    //SORTED map of zLevels -> used to paint objects in right z-order
    private final SortedMap<Integer, Map<Integer, Layer>> layerItemMap = new TreeMap<>();
    private final AtomicInteger idProvider = new AtomicInteger(1);

    /** adds layers to this group with a given zLevel */
    public int[] add(final int zLevel, final boolean redraw, @Nullable final Layer ... layers) {
        if (layers == null) {
            return new int[0];
        }
        final int[] context = new int[layers.length + 1];

        layerLock.lock();
        try {
            Map<Integer, Layer> zLevelMap = this.layerItemMap.get(zLevel);
            if (zLevelMap == null) {
                zLevelMap = new HashMap<>();
                this.layerItemMap.put(zLevel, zLevelMap);
            }
            context[0] = zLevel;
            int pos = 1;
            for (Layer layer : layers) {
                if (layer == null) {
                    continue;
                }
                final int itemId = idProvider.addAndGet(1);
                zLevelMap.put(itemId, layer);
                context[pos++] = itemId;
            }
        } finally {
            layerLock.unlock();
        }
        if (redraw) {
            requestRedraw();
        }
        return context;
    }

    public void remove(final boolean redraw, @Nullable final int ... context) {
        if (context == null || context.length < 2) {
            return;
        }
        final List<Layer> removedLayers = new ArrayList<>(context.length);
        layerLock.lock();
        try {
            final int zLevel = context[0];
            final Map<Integer, Layer> zLevelMap = this.layerItemMap.get(zLevel);
            if (zLevelMap == null) {
                return;
            }
            for (int i = 1; i < context.length; i++) {
                if (context[i] == 0) {
                    continue;
                }
                final Layer removed = zLevelMap.remove(context[i]);
                if (removed != null) {
                    removedLayers.add(removed);
                }
            }
            if (zLevelMap.isEmpty()) {
                this.layerItemMap.remove(zLevel);
            }
        } finally {
            layerLock.unlock();
        }
        // free resources held by the removed layers (esp. bitmaps of markers). Do this outside of our
        // own lock so we never call foreign code while holding it
        destroyLayers(removedLayers);
        if (redraw) {
            requestRedraw();
        }
    }

    /** removes all layers from this group and frees the resources held by them */
    @Override
    public void onDestroy() {
        final List<Layer> removedLayers = new ArrayList<>();
        layerLock.lock();
        try {
            for (Map<Integer, Layer> zLevelMap : this.layerItemMap.values()) {
                removedLayers.addAll(zLevelMap.values());
            }
            this.layerItemMap.clear();
        } finally {
            layerLock.unlock();
        }
        destroyLayers(removedLayers);
        super.onDestroy();
    }

    /**
     * Mapsforge layers hold resources which are only released on onDestroy(); for a Marker this is the
     * reference to its bitmap. Removing a layer without destroying it therefore leaks that bitmap.
     */
    private static void destroyLayers(final Iterable<Layer> layers) {
        for (Layer layer : layers) {
            layer.onDestroy();
        }
    }

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {
        layerLock.lock();
        try {
            for (Map<Integer, Layer> zLevelMap : this.layerItemMap.values()) {
                for (Layer entry : zLevelMap.values()) {
                    if (entry != null) {
                        entry.draw(boundingBox, zoomLevel, canvas, topLeftPoint, rotation);
                    }
                }
            }
        } finally {
            layerLock.unlock();
        }
    }

}
