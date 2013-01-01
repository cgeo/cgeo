package cgeo.geocaching.utils;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Synchronized set wrapper for the LeastRecentlyUsedMap.
 * 
 * This code is heavily based on the HashSet code that represent Map as a Set.
 * Unfortunately HashSet does not allow to use a custom Map as its Storage.
 * Therefore overriding removeEldestEntry() is impossible for a normal LinkedHashSet.
 *
 * Synchronization is added to guard against concurrent modification. Iterator
 * access has to be guarded externally or the synchronized getAsList method can be used
 * to get a clone for iteration
 */
public class LeastRecentlyUsedSet<E> extends AbstractSet<E>
        implements Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -1942301031191419547L;

    private transient LeastRecentlyUsedMap<E, Object> map;
    private static final Object PRESENT = new Object();

    public LeastRecentlyUsedSet(int maxEntries, int initialCapacity, float loadFactor) {
        // because we don't use any Map.get() methods from the Set, BOUNDED and LRU_CACHE have the exact same Behaviour
        // So we useLRU_CACHE mode because it should perform a bit better (as it doesn't re-add explicitly)
        map = new LeastRecentlyUsedMap.LruCache<E, Object>(maxEntries, initialCapacity, loadFactor);
    }

    public LeastRecentlyUsedSet(int maxEntries) {
        map = new LeastRecentlyUsedMap.LruCache<E, Object>(maxEntries);
    }

    /**
     * Copy of the HashSet code if iterator()
     * Iterator access has to be synchronized externally!
     *
     * @see HashSet
     */
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
    public synchronized boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * Synchronized addition of an item
     * Copy of the HashSet code if add()
     * 
     * @see HashSet
     */
    @Override
    public synchronized boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    /**
     * Synchronized removal of a contained item
     * Copy of the HashSet code if remove()
     * 
     * @see HashSet
     */
    @Override
    public synchronized boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    /**
     * Synchronized removal of all elements contained in another collection.
     */
    @Override
    public synchronized boolean removeAll(final Collection<?> c) {
        boolean changed = false;
        for (final Object o: c) {
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
     * (synchronized) Clone of the set
     * Copy of the HashSet code if clone()
     * 
     * @see HashSet
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {
        try {
            synchronized (this) {
                final LeastRecentlyUsedSet<E> newSet = (LeastRecentlyUsedSet<E>) super.clone();
                newSet.map = (LeastRecentlyUsedMap<E, Object>) map.clone();
                return newSet;
            }
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * Creates a clone as a list in a synchronized fashion.
     *
     * @return List based clone of the set
     */
    public synchronized List<E> getAsList() {
        return new ArrayList<E>(this);
    }

    /**
     * Serialization version of HashSet with the additional parameters for the custom Map
     *
     * @see HashSet
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Write out any hidden serialization magic
        s.defaultWriteObject();

        // Write out HashMap capacity and load factor
        s.writeInt(map.initialCapacity);
        s.writeFloat(map.loadFactor);
        s.writeInt(map.getMaxEntries());

        // Write out size
        s.writeInt(map.size());

        // Write out all elements in the proper order.
        for (final E e : map.keySet()) {
            s.writeObject(e);
        }
    }

    /**
     * Serialization version of HashSet with the additional parameters for the custom Map
     *
     * @see HashSet
     */
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Read in any hidden serialization magic
        s.defaultReadObject();

        // Read in HashMap capacity and load factor and create backing HashMap
        final int capacity = s.readInt();
        final float loadFactor = s.readFloat();
        final int maxEntries = s.readInt();

        map = new LeastRecentlyUsedMap.LruCache<E, Object>(maxEntries, capacity, loadFactor);

        // Read in size
        final int size = s.readInt();

        // Read in all elements in the proper order.
        for (int i = 0; i < size; i++) {
            E e = (E) s.readObject();
            map.put(e, PRESENT);
        }
    }

}
