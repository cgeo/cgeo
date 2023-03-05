package cgeo.geocaching.unifiedmap.geoitemlayer;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AsynchronousMapWrapper;
import cgeo.geocaching.utils.Log;

import android.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GeoItemLayer<K> {

    private final String id;
    private final Map<K, Pair<GeoItem, Boolean>> itemMap = new HashMap<>();
    //private final Lock lock = new ReentrantLock(); //-> locking is done via synchronized

    private AsynchronousMapWrapper<MapWriterKey<K>, GeoPrimitive, Object> mapWriter;

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
        public Object add(final GeoPrimitive item) {
            return null;
        }

        @Override
        public void remove(final GeoPrimitive item, final Object context) {
            //do nothing
        }

    };

    private static class MapWriterKey<T> {

        public final T key;
        public final int index;

        MapWriterKey(final T key, final int index) {
            this.key = key;
            this.index = index;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof MapWriterKey<?>)) {
                return false;
            }
            final MapWriterKey<?> other = (MapWriterKey<?>) o;
            return Objects.equals(key, other.key) && index == other.index;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ index;
        }
    }

    private static class MapWriter<T> implements AsynchronousMapWrapper.IMapChangeExecutor<MapWriterKey<T>, GeoPrimitive, Object> {

        private final IProviderGeoItemLayer<Object> providerLayer;

        @SuppressWarnings("unchecked")
        MapWriter(final IProviderGeoItemLayer<?> providerLayer) {
            this.providerLayer = (IProviderGeoItemLayer<Object>) providerLayer;
        }

        @Override
        public Object add(final MapWriterKey<T> key, final GeoPrimitive value) {
            if (value != null && value.isValid()) {
                return providerLayer.add(value);
            }
            return null;
        }

        @Override
        public void remove(final MapWriterKey<T> key, final GeoPrimitive value, final Object context) {
            providerLayer.remove(value, context);
        }

        @Override
        public Object replace(final MapWriterKey<T> key, final GeoPrimitive oldValue, final Object oldContext, final GeoPrimitive newValue) {
            return providerLayer.replace(oldValue, oldContext, newValue);
        }

        @Override
        public void runCommandChain(final Runnable runnable) {
            AndroidRxUtils.computationScheduler.scheduleDirect(runnable);
        }

        @Override
        public void runMapChanges(final Runnable runnable) {
            // inspired by http://stackoverflow.com/questions/12850143/android-basics-running-code-in-the-ui-thread/25250494#25250494
            // modifications of google map must be run on main (UI) thread
            //new Handler(Looper.getMainLooper()).post(runnable);
            Log.iForce("AsyncMapWrapper: request Thread for: " + runnable.getClass().getName());

            AndroidRxUtils.runOnUi(runnable);
        }

        @Override
        public void destroy() {
            if (providerLayer != null) {
                providerLayer.destroy();
            }
        }

        @Override
        public boolean continueMapChangeExecutions(final long startTime, final  int queueLength) {
            return System.currentTimeMillis() - startTime < 40;
        }
    }

    public GeoItemLayer(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public synchronized void destroy() {
        if (this.mapWriter != null) {
            this.mapWriter.destroy();
            this.mapWriter = null;
        }
    }

    public synchronized void setProvider(final IProviderGeoItemLayer<?> newProviderLayer, final int zLevel) {
        destroy();
        final IProviderGeoItemLayer<?>  providerLayer = newProviderLayer == null ? NOOP_GEOITEM_LAYER : newProviderLayer;
        providerLayer.init(zLevel);
        this.mapWriter = new AsynchronousMapWrapper<>(new MapWriter<>(providerLayer));
        for (Map.Entry<K, Pair<GeoItem, Boolean>> entry : this.itemMap.entrySet()) {
            if (entry.getValue().second) {
                putToMap(entry.getKey(), entry.getValue().first, null);
            }
        }
    }

    public synchronized void put(final K key, final GeoItem item) {
        put(key, item, true);
    }

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

    public synchronized void remove(final K key) {
        final Pair<GeoItem, Boolean> value = itemMap.get(key);
        if (value != null) {
            itemMap.remove(key);
            if (value.second) {
                removeFromMap(key, value.first);
            }
        }
    }

    private void putToMap(final K key, final GeoItem item, final GeoItem previousItem) {
        //shortcut for replacements of GeoPrimitives
        if (item instanceof GeoPrimitive && previousItem instanceof GeoPrimitive) {
            mapWriter.put(new MapWriterKey<>(key, 0), (GeoPrimitive) item);
        } else {
            if (previousItem != null) {
                removeFromMap(key, previousItem);
            }
            final int[] index = {0};
            GeoGroup.forAllPrimitives(item, p -> mapWriter.put(new MapWriterKey<>(key, index[0]++), p));
        }
    }

    private void removeFromMap(final K key, final GeoItem oldItem) {
        final int[] index = { 0 };
        GeoGroup.forAllPrimitives(oldItem, p -> mapWriter.remove(new MapWriterKey<>(key, index[0]++)));
    }

    public synchronized void show(final K key) {
        final Pair<GeoItem, Boolean> value = itemMap.get(key);
        if (value != null && !value.second) {
            itemMap.put(key, new Pair<>(value.first, true));
            putToMap(key, value.first, null);
        }
    }

    public synchronized void hide(final K key) {
        final Pair<GeoItem, Boolean> value = itemMap.get(key);
        if (value != null && value.second) {
            itemMap.put(key, new Pair<>(value.first, false));
            removeFromMap(key, value.first);
        }
    }

    public synchronized void showAll() {
        for (Map.Entry<K, Pair<GeoItem, Boolean>> entry : this.itemMap.entrySet()) {
            if (!entry.getValue().second) {
                itemMap.put(entry.getKey(), new Pair<>(entry.getValue().first, true));
                putToMap(entry.getKey(), entry.getValue().first, null);
            }
        }
    }

    public synchronized void hideAll() {
        for (Map.Entry<K, Pair<GeoItem, Boolean>> entry : this.itemMap.entrySet()) {
            if (entry.getValue().second) {
                itemMap.put(entry.getKey(), new Pair<>(entry.getValue().first, false));
                removeFromMap(entry.getKey(), entry.getValue().first);
            }
        }
    }

    public synchronized Viewport getViewport() {
        final Viewport.ContainingViewportBuilder vpBuilder = new Viewport.ContainingViewportBuilder();
        for (Map.Entry<K, Pair<GeoItem, Boolean>> entry : this.itemMap.entrySet()) {
            vpBuilder.add(entry.getValue().first.getViewport());
        }
        return vpBuilder.getViewport();
    }

    public synchronized Set<K> getTouched(final Geopoint gp, final Viewport mapShown, final int viewWidth, final int viewHeight) {
        final int bb = 20;
        final Viewport box = new Viewport(
                new Geopoint(gp.getLatitudeE6() - bb, gp.getLongitudeE6()  - bb),
                new Geopoint(gp.getLatitudeE6() + bb, gp.getLongitudeE6()  + bb));

        final float yPerLatL6 = viewHeight / (float) Math.abs(mapShown.bottomLeft.getLatitudeE6() - mapShown.topRight.getLatitudeE6());
        final float xPerLonL6 = viewWidth / (float) Math.abs(mapShown.bottomLeft.getLongitudeE6() - mapShown.topRight.getLongitudeE6());

        final Set<K> result = new HashSet<>();
        for (Map.Entry<K, Pair<GeoItem, Boolean>> entry : this.itemMap.entrySet()) {
            if (entry.getValue().first.intersects(box, yPerLatL6, xPerLonL6)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
