package cgeo.geocaching.utils;

import cgeo.geocaching.utils.LeastRecentlyUsedMap.OperationModes;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Set wrapper for the LeastRecentlyUsedMap.
 *
 * This code is heavily based on the HashSet code that represent Map as a Set.
 * Unfortionately HashSet does not allow to use a custom Map as its Storage.
 * Therefore overriding removeEldestEntry() is impossible for a normal LinkedHashSet.
 *
 * @author Teschi
 */
public class LeastRecentlyUsedSet<E> extends AbstractSet<E>
        implements Cloneable, java.io.Serializable {

	private static final long serialVersionUID = -1942301031191419547L;

	private transient LeastRecentlyUsedMap<E,Object> map;
    private static final Object PRESENT = new Object();

    public LeastRecentlyUsedSet(int maxEntries, int initialCapacity, float loadFactor, OperationModes opMode) {
        // because we don't use any Map.get() methods from the Set, BOUNDED and LRU_CACHE have the exact same Behaviour
        // So we prefer LRU_CACHE mode because it should perform a bit better (as it doesn't re-add explicitly)
        map = new LeastRecentlyUsedMap<E, Object>(maxEntries, initialCapacity, loadFactor,
                (opMode == OperationModes.BOUNDED) ? OperationModes.LRU_CACHE : opMode);
    }

    public LeastRecentlyUsedSet(int maxEntries, OperationModes opMode) {
    	map = new LeastRecentlyUsedMap<E, Object>(maxEntries, opMode);
    }

    public LeastRecentlyUsedSet(int maxEntries) {
    	map = new LeastRecentlyUsedMap<E, Object>(maxEntries);
    }


    /**
     * Copy of the HashSet code if iterator()
     * @see HashSet
     */
    @Override
    public Iterator<E> iterator() {
	return map.keySet().iterator();
    }

    /**
     * Copy of the HashSet code if size()
     * @see HashSet
     */
    @Override
    public int size() {
	return map.size();
    }

    /**
     * Copy of the HashSet code if isEmpty()
     * @see HashSet
     */
    @Override
    public boolean isEmpty() {
	return map.isEmpty();
    }

    /**
     * Copy of the HashSet code if contains()
     * @see HashSet
     */
    @Override
    public boolean contains(Object o) {
	return map.containsKey(o);
    }

    /**
     * Copy of the HashSet code if add()
     * @see HashSet
     */
    @Override
    public boolean add(E e) {
	return map.put(e, PRESENT)==null;
    }

    /**
     * Copy of the HashSet code if remove()
     * @see HashSet
     */
    @Override
    public boolean remove(Object o) {
	return map.remove(o)==PRESENT;
    }

    /**
     * Copy of the HashSet code if clear()
     * @see HashSet
     */
    @Override
    public void clear() {
	map.clear();
    }

    /**
     * Copy of the HashSet code if clone()
     * @see HashSet
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {
	try {
	    LeastRecentlyUsedSet<E> newSet = (LeastRecentlyUsedSet<E>) super.clone();
	    newSet.map = (LeastRecentlyUsedMap<E, Object>) map.clone();
	    return newSet;
	} catch (CloneNotSupportedException e) {
	    throw new InternalError();
	}
    }


    /**
     * Serialization version of HashSet with the additional parameters for the custom Map
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
        s.writeUTF(map.getOpMode().name());

        // Write out size
        s.writeInt(map.size());

	// Write out all elements in the proper order.
	for (Iterator<E> i=map.keySet().iterator(); i.hasNext(); ) {
        s.writeObject(i.next());
    }
    }

    /**
     * Serialization version of HashSet with the additional parameters for the custom Map
     * @see HashSet
     */
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
	// Read in any hidden serialization magic
	s.defaultReadObject();

        // Read in HashMap capacity and load factor and create backing HashMap
        int capacity = s.readInt();
        float loadFactor = s.readFloat();
        int maxEntries = s.readInt();
        OperationModes opMode = OperationModes.valueOf(s.readUTF());

        map = new LeastRecentlyUsedMap<E,Object>(maxEntries, capacity, loadFactor, opMode);

        // Read in size
        int size = s.readInt();

	// Read in all elements in the proper order.
	for (int i=0; i<size; i++) {
            E e = (E) s.readObject();
            map.put(e, PRESENT);
        }
    }

}
