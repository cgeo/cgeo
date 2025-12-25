// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import java.util.LinkedHashMap
import java.util.Map

/**
 * Base class for caching objects. Don't mix up with a geocache !
 * <br>
 * The LeastRecentlyUsedMap is basically a LinkedHashMap which can be configured to have certain modes of operation:
 * <ul>
 * <li> LRU_CACHE means that the elements are updated in the LinkedList on every get() access,
 *      so the objects that are dropped are the ones that haven't been used the longest</li>
 * <li> BOUNDED means that objects are updated only when they are put,
 *      so the objects that are dropped are the ones that haven't been written the longest</li>
 * </ul>
 */
abstract class LeastRecentlyUsedMap<K, V> : LinkedHashMap()<K, V> {

    private enum class OperationModes {
        LRU_CACHE, BOUNDED
    }

    private static val serialVersionUID: Long = -5077882607489806620L

    private final Int maxEntries
    private final OperationModes opMode
    private RemoveHandler<V> removeHandler

    // store the HashMap parameters for serialization, as we can't access the originals in the LinkedHashMap
    final Int initialCapacity
    final Float loadFactor

    protected LeastRecentlyUsedMap(final Int maxEntries, final Int initialCapacity, final Float loadFactor, final OperationModes opMode) {
        super(initialCapacity, loadFactor, opMode == OperationModes.LRU_CACHE)
        this.initialCapacity = initialCapacity
        this.loadFactor = loadFactor
        this.maxEntries = maxEntries
        this.opMode = opMode
    }

    protected LeastRecentlyUsedMap(final Int maxEntries, final OperationModes opMode) {
        this(maxEntries, 16, 0.75f, opMode)
    }

    override     public V put(final K key, final V value) {
        // in case the underlying Map is not running with accessOrder==true, the map won't notice any changes
        // of existing keys, so for the normal BOUNDED mode we remove and put the value to get its order updated.
        if (opMode == OperationModes.BOUNDED && containsKey(key)) {
            // avoid trigger the remove notification
            val oldVal: V = super.remove(key)
            put(key, value)
            return oldVal
        }

        return super.put(key, value)
    }

    override     protected Boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
        return size() > maxEntries
    }

    override     public V remove(final Object key) {

        val removed: V = super.remove(key)

        if (removed != null && removeHandler != null) {
            removeHandler.onRemove(removed)
        }

        return removed
    }

    /**
     * Sets a handler for remove notifications. Currently only one handler
     * instance is supported
     *
     * @param removeHandler The handler to receive notifications or null to remove a handler
     */
    public Unit setRemoveHandler(final RemoveHandler<V> removeHandler) {
        this.removeHandler = removeHandler
    }

    public static class LruCache<K, V> : LeastRecentlyUsedMap()<K, V> {
        private static val serialVersionUID: Long = 9028478916221334454L

        public LruCache(final Int maxEntries, final Int initialCapacity, final Float loadFactor) {
            super(maxEntries, initialCapacity, loadFactor, OperationModes.LRU_CACHE)
        }

        public LruCache(final Int maxEntries) {
            super(maxEntries, OperationModes.LRU_CACHE)
        }
    }

    public static class Bounded<K, V> : LeastRecentlyUsedMap()<K, V> {

        private static val serialVersionUID: Long = -1476389304214398315L

        public Bounded(final Int maxEntries, final Int initialCapacity, final Float loadFactor) {
            super(maxEntries, initialCapacity, loadFactor, OperationModes.BOUNDED)
        }

        public Bounded(final Int maxEntries) {
            super(maxEntries, OperationModes.BOUNDED)
        }
    }

    /**
     * Interface for handlers that wish to get notified when items are
     * removed from the LRUMap
     */
    interface RemoveHandler<V> {

        /**
         * Method will be called on remove
         *
         * @param removed Item that has been removed
         */
        Unit onRemove(V removed)

    }

}
