package cgeo.geocaching.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for caching objects. Don't mix up with a geocache !
 *
 * The LeastRecentlyUsedMap is basically a LinkedHashMap which can be configured to have certain modes of operation:
 * <ul>
 * <li> LRU_CACHE means that the elements are updated in the LinkedList on every get() access,
 *      so the objects that are dropped are the ones that haven't been used the longest</li>
 * <li> BOUNDED means that objects are updated only when they are put,
 *      so the objects that are dropped are the ones that haven't been written the longest</li>
 * <li> BOUNDED_IGNORE_REINSERT means that objects are updated only when they are put initially,
 *      so the objects that are dropped are the ones which were initially written the longest time ago
 *      (default behavior for a LinkedHashMap, and possibly the least useful)</li>
 * </ul>
 *
 * @author blafoo
 * @author Teschi
 */
public class LeastRecentlyUsedMap<K, V> extends LinkedHashMap<K, V> {

    public static enum OperationModes {LRU_CACHE, BOUNDED, BOUNDED_IGNORE_REINSERT}

    private static final long serialVersionUID = -5077882607489806620L;

    private final int maxEntries;
    private final OperationModes opMode;

    // store the HashMap parameters for serialization, as we can't access the originals in the LinkedHashMap
    final int initialCapacity;
    final float loadFactor;

    public LeastRecentlyUsedMap(int maxEntries, int initialCapacity, float loadFactor, OperationModes opMode) {
    	super(initialCapacity, loadFactor, (opMode==OperationModes.LRU_CACHE));
    	this.initialCapacity = initialCapacity;
    	this.loadFactor = loadFactor;
        this.maxEntries = maxEntries;
        this.opMode = opMode;
    }

    public LeastRecentlyUsedMap(int maxEntries, OperationModes opMode) {
    	this(maxEntries, 16, 0.75f, opMode);
    }

    public LeastRecentlyUsedMap(int maxEntries) {
    	this(maxEntries, OperationModes.LRU_CACHE);
    }

    @Override
    public V put(K key, V value) {
    	// in case the underlying Map is not running with accessOrder==true, the map won't notice any changes
    	// of existing keys, so for the normal BOUNDED mode we remove and put the value to get its order updated.
    	if (opMode == OperationModes.BOUNDED && containsKey(key)) {
    		V oldVal = remove(key);
    		put(key, value);
    		return oldVal;
    	} else {
    		return super.put(key, value);
    	}
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }

    public int getMaxEntries() {
		return maxEntries;
	}

    public OperationModes getOpMode() {
		return opMode;
	}
}
