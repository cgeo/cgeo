package cgeo.geocaching.utils;

import androidx.annotation.NonNull;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Synchronized set wrapper for the LeastRecentlyUsedMap.
 *
 * This code is heavily based on the HashSet code that represents Map as a Set.
 * Unfortunately HashSet does not allow to use a custom Map as its Storage.
 * Therefore overriding removeEldestEntry() is impossible for a normal LinkedHashSet.
 *
 * Synchronization is added to guard against concurrent modification. Iterator
 * access has to be guarded externally or the synchronized getAsList method can be used
 * to get a clone for iteration.
 */
public class LeastRecentlyUsedSet<E> extends AbstractSet<E> {

    private final LeastRecentlyUsedMap<E, Object> map;
    private static final Object PRESENT = new Object();

    public LeastRecentlyUsedSet(final int maxEntries, final int initialCapacity, final float loadFactor) {
        // because we don't use any Map.get() methods from the Set, BOUNDED and LRU_CACHE have the exact same Behaviour
        // So we use LRU_CACHE mode because it should perform a bit better (as it doesn't re-add explicitly)
        map = new LeastRecentlyUsedMap.LruCache<>(maxEntries, initialCapacity, loadFactor);
    }

    public LeastRecentlyUsedSet(final int maxEntries) {
        map = new LeastRecentlyUsedMap.LruCache<>(maxEntries);
    }

    /**
     * Copy of the HashSet code if iterator()
     * Iterator access has to be synchronized externally!
     *
     * @see HashSet
     */
    @NonNull
    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     * Synchronized access to set size
     * Copy of the HashSet code if size()
     *
     * @see HashSet
     */
    @Override
    public synchronized int size() {
        return map.size();
    }

    /**
     * Synchronized check of set emptiness
     * Copy of the HashSet code if isEmpty()
     *
     * @see HashSet
     */
    @Override
    public synchronized boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Synchronized check for containment
     * Copy of the HashSet code if contains()
     *
     * @see HashSet
     */
    @Override
    public synchronized boolean contains(final Object o) {
        return map.containsKey(o);
    }

    /**
     * Synchronized addition of an item
     * Copy of the HashSet code if add()
     *
     * @see HashSet
     */
    @Override
    public synchronized boolean add(final E e) {
        if (e == null) {
            throw new IllegalArgumentException("LeastRecentlyUsedSet cannot take null element");
        }
        return map.put(e, PRESENT) == null;
    }

    /**
     * Synchronized removal of a contained item
     * Copy of the HashSet code if remove()
     *
     * @see HashSet
     */
    @Override
    public synchronized boolean remove(final Object o) {
        return map.remove(o) == PRESENT;
    }

    /**
     * Synchronized removal of all elements contained in another collection.
     */
    @Override
    public synchronized boolean removeAll(final Collection<?> c) {
        boolean changed = false;
        for (final Object o : c) {
            changed |= remove(o);
        }
        return changed;
    }

    /**
     * Synchronized clearing of the set
     * Copy of the HashSet code if clear()
     *
     * @see HashSet
     */
    @Override
    public synchronized void clear() {
        map.clear();
    }

    /**
     * Creates a clone as a list in a synchronized fashion.
     *
     * @return List based clone of the set
     */
    public synchronized List<E> getAsList() {
        return new ArrayList<>(this);
    }

}
