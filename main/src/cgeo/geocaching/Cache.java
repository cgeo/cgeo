package cgeo.geocaching;

import java.util.LinkedHashMap;

/**
 * Base class for a caching cache. Don't mix up with a geocache !
 * 
 * @author blafoo
 */
public class Cache<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = -5077882607489806620L;
    private final int maxEntries;

    public Cache(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }

}
