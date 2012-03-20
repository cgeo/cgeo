package cgeo.geocaching.utils;

import cgeo.geocaching.Settings;

import android.util.Log;

import java.util.LinkedHashMap;

/**
 * Base class for a caching cache. Don't mix up with a geocache !
 *
 * @author blafoo
 */
public class LeastRecentlyUsedCache<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = -5077882607489806620L;
    private final int maxEntries;
    private RemoveHandler<V> removeHandler;

    public LeastRecentlyUsedCache(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }

    @Override
    public V remove(Object key) {

        V toBeRemoved = super.remove(key);

        if (null != toBeRemoved) {
            Log.d(Settings.tag, "Removing " + toBeRemoved.toString());

            if (null != removeHandler) {
                removeHandler.onRemove(toBeRemoved);
            }
        }

        return toBeRemoved;
    }

    public void setRemoveHandler(RemoveHandler<V> removeHandler) {
        this.removeHandler = removeHandler;
    }

    public interface RemoveHandler<V> {

        /**
         * Method will be called on remove
         *
         * @param toBeRemoved
         */
        void onRemove(V toBeRemoved);

    }

}
