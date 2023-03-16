package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.ToScreenProjector;
import cgeo.geocaching.utils.AsynchronousMapWrapper;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.Log;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An abstracted Map Layer which allows to handle a group of related geo objects on a map layer
 * together.
 *
 * Methods of this class allow adding, removing, hiding/showing, ... of objects onto the layer in
 * a map-like fashion (means: using a key-GeoItem-concept).
 * Touch handling is also provided.
 *
 * The abstract layer is connected towards a concrete map layer implementation by using the
 * setProvider method
 *
 * @param <K> key class to use when placing and handling map geo objects on this layer
 */
public class GeoItemLayer<K> {

    private static final ThreadLocal<Map<Integer, GeoPrimitive>> LOCAL_MAP = CommonUtils.threadLocalWithInitial(HashMap::new);
    private static final ThreadLocal<Map<Integer, GeoPrimitive>> LOCAL_MAP_2 = CommonUtils.threadLocalWithInitial(HashMap::new);
    private final String id;
    private final Map<K, Pair<GeoItem, Boolean>> itemMap = new HashMap<>();
    //private final Lock lock = new ReentrantLock(); //-> locking is done via synchronized

    //Key of mapWriter is always instance of either K or GeoGroupKey<K>
    private AsynchronousMapWrapper<Object, GeoPrimitive, Object> mapWriter;
    private IProviderGeoItemLayer<?> providerLayer = null;

    public static final IProviderGeoItemLayer<Object> NOOP_GEOITEM_LAYER = new IProviderGeoItemLayer<Object>() {

        @Override
        public void init(final int zLevel) {
            //do nothing
        }

        @Override
        public void destroy() {
            //do nothing
        }

        @Override
        public ToScreenProjector getScreenCoordCalculator() {
            return null;
        }

        @Override
        public Object add(final GeoPrimitive item) {
            return null;
        }

        @Override
        public void remove(final GeoPrimitive item, final Object context) {
            //do nothing
        }

    };

    /** Key class to be used for entries in MapWriter from a GeoGroup */
    private static class MapWriterGeoGroupKey<T> {

        public final T key;
        public final int index;

        MapWriterGeoGroupKey(final T key, final int index) {
            this.key = key;
            this.index = index;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof GeoItemLayer.MapWriterGeoGroupKey<?>)) {
                return false;
            }
            final MapWriterGeoGroupKey<?> other = (MapWriterGeoGroupKey<?>) o;
            return Objects.equals(key, other.key) && index == other.index;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ index;
        }

        @NonNull
        @Override
        public String toString() {
            return key + "-" + index;
        }
    }

    private static class MapWriter implements AsynchronousMapWrapper.IMapChangeExecutor<Object, GeoPrimitive, Object> {

        public final IProviderGeoItemLayer<Object> providerLayer;

        @SuppressWarnings("unchecked")
        MapWriter(final IProviderGeoItemLayer<?> providerLayer) {
            this.providerLayer = (IProviderGeoItemLayer<Object>) providerLayer;
        }

        @Override
        public Object add(final Object key, final GeoPrimitive value) {
            if (value != null && value.isValid()) {
                return providerLayer.add(value);
            }
            return null;
        }

        @Override
        public void remove(final Object key, final GeoPrimitive value, final Object context) {
            providerLayer.remove(value, context);
        }

        @Override
        public Object replace(final Object key, final GeoPrimitive oldValue, final Object oldContext, final GeoPrimitive newValue) {
            return providerLayer.replace(oldValue, oldContext, newValue);
        }

        @Override
        public void runCommandChain(final Runnable runnable) {
            if (providerLayer != null) {
                providerLayer.runCommandChain(runnable);
            }
        }

        @Override
        public void runMapChanges(final Runnable runnable) {
            if (providerLayer != null) {
                providerLayer.runMapChanges(runnable);
            }
        }

        @Override
        public void destroy() {
            if (providerLayer != null) {
                providerLayer.destroy();
            }
        }

        @Override
        public boolean continueMapChangeExecutions(final long startTime, final  int queueLength) {
            return providerLayer != null && providerLayer.continueMapChangeExecutions(startTime, queueLength);
        }
    }

    /** Creates a new GeoItemLayer.
     *
     * Constructor is designed to be usable in a "final" fashion on construction of any Android object
     * (e.g. Activities or Views). Means: it doesn't need anything to be set up GUI-wise to be called.
     */
    public GeoItemLayer(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Destroys this layer and cleans up any resouces held by it. Do not use this object again
     * after calling destroy()!
     */
    public synchronized void destroy() {
        if (this.mapWriter != null) {
            this.mapWriter.destroy();
            this.mapWriter = null;
        }
        this.providerLayer = null;
    }

    /**
     * Sets a concrete map provider for this layer.
     * Provider can also be changed to another later by using this method.
     */
    public synchronized void setProvider(final IProviderGeoItemLayer<?> newProviderLayer, final int zLevel) {
        destroy();
        final IProviderGeoItemLayer<?>  providerLayer = newProviderLayer == null ? NOOP_GEOITEM_LAYER : newProviderLayer;
        providerLayer.init(zLevel);
        this.providerLayer = providerLayer;
        this.mapWriter = new AsynchronousMapWrapper<>(new MapWriter(providerLayer));
        for (Map.Entry<K, Pair<GeoItem, Boolean>> entry : this.itemMap.entrySet()) {
            if (entry.getValue().second) {
                putToMap(entry.getKey(), entry.getValue().first, null);
            }
        }
    }

    /** Puts a new GeoItem onto the layer. If an object for same key already exists, it is replaced */
    public synchronized void put(final K key, final GeoItem item) {
        put(key, item, true);
    }

    /**
     * Puts a new GeoItem onto the layer. If an object for same key already exists, it is replaced.
     * If "show" is false, the item is added to the layer but not (yet) shown.
     */
    public synchronized void put(final K key, final GeoItem item, final boolean show) {

        final Pair<GeoItem, Boolean> previousItem = itemMap.get(key);
        if (previousItem != null && previousItem.second && !show) {
            //item for key existed and is currently shown, but new key should NOT be shown -> remove from map
            removeFromMap(key, previousItem.first);
        }

        itemMap.put(key, new Pair<>(item, show));

        //draw new item on map if necessary
        if (show) {
            putToMap(key, item, previousItem == null ? null : previousItem.first);
        }
    }

    /** Removes an object from this layer */
    public synchronized void remove(final K key) {
        final Pair<GeoItem, Boolean> value = itemMap.get(key);
        if (value != null) {
            itemMap.remove(key);
            if (value.second) {
                removeFromMap(key, value.first);
            }
        }
    }

    /** Gets the object associated with the given key */
    public synchronized GeoItem get(final K key) {
        final Pair<GeoItem, Boolean> entry = itemMap.get(key);
        return entry == null ? null : entry.first;
    }

    /** Returns true if an object with given key exists in the map AND is currently shown */
    public synchronized boolean isShown(final K key) {
        final Pair<GeoItem, Boolean> entry = itemMap.get(key);
        return entry != null && entry.second;
    }

    /** Returns all keys for which layer objects are currently registered. The returned set
     * is a COPY of all keys NOT backed by the layer. */
    public synchronized Set<K> keySet() {
        return new HashSet<>(itemMap.keySet());
    }

    private void putToMap(final K key, final GeoItem item, final GeoItem oldItem) {

        if (item == null && oldItem == null) {
            throw new IllegalArgumentException("Progamming bug: either item or oldItem must be non-null for: " + key);
        }

        //GeoPrimitives are handled different from GeoGroups
        //- Primitives are inserted with one element with index 0
        //- for GeoGroups, the contained primitives are inserted with an index matching their hashcode (this makes replacements more efficient)
        //=> if for same key a switch is done between primitive and geogroup, this requires special handling

        final boolean itemIsPrimitive = item instanceof GeoPrimitive;
        final boolean oldItemIsPrimitive = oldItem instanceof GeoPrimitive;

        if (itemIsPrimitive && (oldItem == null || oldItemIsPrimitive)) {
            //->insert or replace one GeoPrimitive with another
            mapWriter.put(key, (GeoPrimitive) item);
        } else if (item == null && oldItemIsPrimitive) {
            //->remove a GeoPrimitive
            mapWriter.remove(key);
        } else if (!itemIsPrimitive && !oldItemIsPrimitive) {
            //->insert or replace a GeoGroup with another, or remove a GeoGroup
            replaceGroupInMap(key, oldItem, item);
        } else if (itemIsPrimitive) {
            //->replace a GeoGroup with a GeoPrimitive
            replaceGroupInMap(key, oldItem, null);
            mapWriter.put(key, (GeoPrimitive) item);
        } else {
            //->replace a GeoPrimitive with a GeoGroup
            mapWriter.remove(key);
            replaceGroupInMap(key, null, item);
        }
    }

    private void removeFromMap(final K key, final GeoItem oldItem) {
        putToMap(key, null, oldItem);
    }

    private void replaceGroupInMap(final K key, @Nullable final GeoItem oldItem, @Nullable final GeoItem newItem) {

        mapWriter.multiChange((putAction, removeAction) -> {
            final Map<Integer, GeoPrimitive> toRemove = LOCAL_MAP.get();
            final Map<Integer, GeoPrimitive> toPut = LOCAL_MAP_2.get();

            //get elements from old and new group
            fillMapFromGroup(oldItem, Objects.requireNonNull(toRemove));
            fillMapFromGroup(newItem, Objects.requireNonNull(toPut));
            //everything which is both in old and new group must not be removed
            for (Integer i : toPut.keySet()) {
                toRemove.remove(i);
            }
            for (Map.Entry<Integer, GeoPrimitive> entry : toRemove.entrySet()) {
                removeAction.call(new MapWriterGeoGroupKey<>(key, entry.getKey()));
            }
            for (Map.Entry<Integer, GeoPrimitive> entry : toPut.entrySet()) {
                putAction.call(new MapWriterGeoGroupKey<>(key, entry.getKey()), entry.getValue());
            }
            toPut.clear();
            toRemove.clear();
        });
    }

    private static void fillMapFromGroup(@Nullable final GeoItem item, final Map<Integer, GeoPrimitive> map) {
        map.clear();
        if (item == null) {
            return;
        }
        GeoGroup.forAllPrimitives(item, p -> {
            //create unique index. Use hashCode as base so similar objects get similar indexes very often
            int idx = Objects.hashCode(p);
            while (map.containsKey(idx)) {
                idx++;
            }
            map.put(idx, p);
        });
    }

    /** If an object for given key exists on the layer, it is made visible */
    public synchronized void show(final K key) {
        final Pair<GeoItem, Boolean> value = itemMap.get(key);
        if (value != null && !value.second) {
            itemMap.put(key, new Pair<>(value.first, true));
            putToMap(key, value.first, null);
        }
    }

    /** If an object for given key exists on the layer, it is made invisible */
    public synchronized void hide(final K key) {
        final Pair<GeoItem, Boolean> value = itemMap.get(key);
        if (value != null && value.second) {
            itemMap.put(key, new Pair<>(value.first, false));
            removeFromMap(key, value.first);
        }
    }

    public synchronized void setVisibility(final K key, final boolean show) {
        if (show) {
            show(key);
        } else {
            hide(key);
        }
    }


    /** All objects in this layer are made visible */
    public synchronized void showAll() {
        for (Map.Entry<K, Pair<GeoItem, Boolean>> entry : this.itemMap.entrySet()) {
            if (!entry.getValue().second) {
                entry.setValue(new Pair<>(entry.getValue().first, true));
                putToMap(entry.getKey(), entry.getValue().first, null);
            }
        }
    }

    /** All objects in this layer are made invisible */
    public synchronized void hideAll() {
        for (Map.Entry<K, Pair<GeoItem, Boolean>> entry : this.itemMap.entrySet()) {
            if (entry.getValue().second) {
                entry.setValue(new Pair<>(entry.getValue().first, false));
                removeFromMap(entry.getKey(), entry.getValue().first);
            }
        }
    }

    /** Gets the overall viewport for all objects in this layer (visible or invisible) */
    public synchronized Viewport getViewport() {
        final Viewport.ContainingViewportBuilder vpBuilder = new Viewport.ContainingViewportBuilder();
        for (Map.Entry<K, Pair<GeoItem, Boolean>> entry : this.itemMap.entrySet()) {
            vpBuilder.add(entry.getValue().first.getViewport());
        }
        return vpBuilder.getViewport();
    }

    /** Gets a list of all objects touched by a given geopoint. Only visible objects are considered */
    public synchronized Set<K> getTouched(final Geopoint tapped) {
        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "GeoItemLayer.getTouched")) {
            final ToScreenProjector toCoordFct;
            final IProviderGeoItemLayer<?> pl = this.providerLayer;
            if (pl != null) {
                toCoordFct = pl.getScreenCoordCalculator();
                Log.d("Touched: " + tapped + " at " + (toCoordFct == null ? "-" : toCoordFct.project(tapped)));
            } else {
                toCoordFct = null;
            }
            cLog.add("scc=" + (toCoordFct != null));

            final Set<K> result = new HashSet<>();
            for (Map.Entry<K, Pair<GeoItem, Boolean>> entry : this.itemMap.entrySet()) {
                if (entry.getValue().second && entry.getValue().first.touches(tapped, toCoordFct)) {
                    result.add(entry.getKey());
                }
            }
            cLog.add("t:" + result.size() + "/" + this, itemMap.size());
            return result;
        }
    }
}
