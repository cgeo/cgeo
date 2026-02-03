/*
Copyright (c) 2007-2008 Kristofer Karlsson <kristofer.karlsson@gmail.com>
Portions of this code Copyright (c) 2007 Andre Bogus <andre@m3n.de>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
--
File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package se.krka.kahlua.vm
 */
package cgeo.geocaching.wherigo.kahlua.vm;

import org.apache.commons.collections4.IteratorUtils;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Objects;

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib;


/**
 * Implementation of a Lua table using a hash table with separate chaining.
 * 
 * <p>This class provides a Java implementation of Lua's table data structure, which serves
 * as both an array and a hash map. The implementation uses open addressing with chaining
 * for collision resolution.</p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Supports weak keys and weak values via metatable __mode field</li>
 *   <li>Automatically resizes when capacity is exceeded</li>
 *   <li>Implements Lua's special semantics for Double keys (NaN handling, equality)</li>
 *   <li>Single-entry key cache for performance optimization</li>
 * </ul>
 * 
 * <h3>Thread Safety:</h3>
 * This class is thread-safe. All public methods that read or modify the table are synchronized.
 * The implementation uses fine-grained locking to minimize performance impact while ensuring
 * safe concurrent access from multiple threads.
 * 
 * <h3>Internal Structure:</h3>
 * The table uses three parallel arrays:
 * <ul>
 *   <li>{@code keys[]} - Stores keys (may be wrapped in WeakReference)</li>
 *   <li>{@code values[]} - Stores values (may be wrapped in WeakReference)</li>
 *   <li>{@code next[]} - Stores collision chain pointers (-1 means end of chain)</li>
 * </ul>
 * 
 * @see LuaTable
 */
public class LuaTableImpl implements LuaTable {
    private volatile boolean weakKeys, weakValues;

    // Hash part
    private Object[] keys;
    private Object[] values;
    private int[] next;
    private int freeIndex;

    // Hash cache
    private Object keyIndexCacheKey;
    private int keyIndexCacheValue = -1;

    private static final int[] log_2 = new int[] {
        0,1,2,2,3,3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
        8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
        8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
        8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8
    };

    private static int luaO_log2 (int x) {
        int l = -1;
        while (x >= 256) {
            l += 8;
            x >>= 8;
        }
        return l + log_2[x];

    }

    private static int neededBits (int x) {
        return 1 + luaO_log2(x);
    }

    private static int nearestPowerOfTwo(int x) {
        int p = 1 << luaO_log2(x);
        return p;
    }

    public LuaTableImpl() {
        int capacity = 1;

        keys = new Object[capacity];
        values = new Object[capacity];
        next = new int[capacity];

        freeIndex = capacity;
    }

    /**
     * Gets the main position (MP) for a key in the hash table.
     * The main position is the preferred index where a key should be stored,
     * calculated by hashing the key and masking to fit within the table capacity.
     * 
     * @param key The key to hash (must not be null)
     * @return The index in the range [0, capacity-1]
     */
    private int getMP(Object key) {
        // assert key != null
        int capacity = keys.length;
        return luaHashcode(key) & (capacity - 1);
    }

    /**
     * Unwraps a weak reference if weak mode is enabled.
     * If the object is not a candidate for weak references (String, Double, Boolean, null),
     * returns it unchanged. Otherwise, dereferences the WeakReference.
     * 
     * @param o The object to unwrap (may be a WeakReference)
     * @return The actual object, or null if the WeakReference was garbage collected
     */
    private Object unref(Object o) {
        if (!canBeWeakObject(o)) {
            return o;
        }

        // Assertion: o instanceof WeakReference
        return ((WeakReference<?>) o).get();
    }

    /**
     * Wraps an object in a WeakReference if weak mode is enabled.
     * Strings, Doubles, Booleans, and null are never wrapped because they are
     * either interned or require strong references for proper semantics.
     * 
     * @param o The object to wrap
     * @return A WeakReference wrapping the object, or the object unchanged
     */
    private Object ref(Object o) {
        if (!canBeWeakObject(o)) {
            return o;
        }

        return new WeakReference<>(o);
    }

    /**
     * Determines if an object can be stored as a weak reference.
     * Strings, Doubles, and Booleans are never weak because:
     * - Strings may be interned
     * - Doubles need special equality handling for NaN
     * - Booleans have only two instances
     * 
     * @param o The object to check
     * @return true if the object can be weakly referenced, false otherwise
     */
    private boolean canBeWeakObject(Object o) {
        return !(o == null || o instanceof String
                || o instanceof Double || o instanceof Boolean);
    }

    private Object __getKey(int index) {
        Object key = keys[index];
        if (weakKeys) {
            return unref(key);
        }
        return key;
    }

    private void __setKey(int index, Object key) {
        if (weakKeys) {
            key = ref(key);
        }
        keys[index] = key;
    }

    private Object __getValue(int index) {
        Object value = values[index];
        if (weakValues) {
            return unref(value);
        }
        return value;
    }

    private void __setValue(int index, Object value) {
        if (weakValues) {
            value = ref(value);
        }
        values[index] = value;
    }

    /**
     * Searches for a key in the hash table starting from a given index.
     * Follows the collision chain using the next[] array until the key is found or
     * the end of the chain is reached.
     * 
     * <p>Special handling for different types:</p>
     * <ul>
     *   <li>Doubles: Uses value equality (==) instead of object identity to handle NaN correctly</li>
     *   <li>Strings: Uses equals() for value comparison</li>
     *   <li>Other types: Uses identity equality (==) for performance</li>
     * </ul>
     * 
     * @param key The key to search for
     * @param index The starting index (typically the main position)
     * @return The index where the key was found, or -1 if not found
     */
    private int hash_primitiveFindKey(Object key, int index) {
        Object currentKey = __getKey(index);

        if (currentKey == null) {
            return -1;
        }
        /*
         * Doubles need special treatment due to how
         * java implements equals and hashcode for Double
         */
        if (key instanceof Double) {
            double dkey = LuaState.fromDouble(key);
            while (true) {
                if (currentKey instanceof Double) {
                    double dCurrentKey = LuaState.fromDouble(currentKey);
                    if (dkey == dCurrentKey) {
                        return index;
                    }
                }

                index = next[index];
                if (index == -1) {
                    return -1;
                }
                currentKey = __getKey(index);
            }

        }

        if (key instanceof String) {
            while (true) {
                if (key.equals(currentKey)) {
                    return index;
                }
                index = next[index];
                if (index == -1) {
                    return -1;
                }
                currentKey = __getKey(index);
            }
        }

        // Assume equality == identity for all types except for doubles and strings
        while (true) {
            if (key == currentKey) {
                return index;
            }
            index = next[index];
            if (index == -1) {
                return -1;
            }
            currentKey = __getKey(index);
        }
    }

    /**
     * Inserts a new key into the hash table at its main position or finds a free slot.
     * This method handles collision resolution using separate chaining.
     * 
     * <p>Algorithm:</p>
     * <ol>
     *   <li>If main position (mp) is free, store the key there</li>
     *   <li>If mp is occupied by a key with the same mp, add new key to the chain</li>
     *   <li>If mp is occupied by a key with a different mp (collision), move the old key
     *       to a free slot and store the new key at mp</li>
     *   <li>If no free slots exist, trigger rehashing and return -1</li>
     * </ol>
     * 
     * @param key The key to insert (must not already exist in the table)
     * @param mp The main position for this key
     * @return The index where the key was inserted, or -1 if rehashing was triggered
     */
    private int hash_primitiveNewKey(Object key, int mp) {
        keyIndexCacheKey = null;
        keyIndexCacheValue = -1;

        // assert key not in table
        // Assert key != null

        Object key2 = __getKey(mp);

        // mainPosition is unoccupied
        if (key2 == null) {
            __setKey(mp, key);
            next[mp] = -1;

            return mp;
        }

        // need to find a free index, either for key, or for the conflicting key
        // since java checks bounds all the time, using try-catch may be faster than manually
        // checking
        try {
            while (__getKey(--freeIndex) != null);
        } catch (ArrayIndexOutOfBoundsException e) {
            hash_rehash(key);
            return -1;
        }

        int mp2 = getMP(key2);
        // index is occupied by something with the same main index
        if (mp2 == mp) {
            __setKey(freeIndex, key);
            next[freeIndex] = next[mp];

            next[mp] = freeIndex;
            return freeIndex;
        }

        // old key is not in its main position
        // move old key to free index
        keys[freeIndex] = keys[mp];
        values[freeIndex] = values[mp];
        next[freeIndex] = next[mp];

        __setKey(mp, key);
        // unnecessary to set value - the main set method will do this.
        // values[mp] = null;
        next[mp] = -1;

        // fix next link for the moved key
        int prev = mp2;
        while (true) {
            int tmp = next[prev];
            if (tmp == mp) {
                next[prev] = freeIndex;
                break;
            }
            prev = tmp;
        }

        return mp;
    }

    /**
     * Rehashes the table to a larger capacity when the current capacity is exceeded.
     * 
     * <p>The rehashing process:</p>
     * <ol>
     *   <li>Temporarily disables weak references to prevent garbage collection during rehash</li>
     *   <li>Counts the number of used entries</li>
     *   <li>Calculates new capacity as 2 * nearest_power_of_2(used_count + 1)</li>
     *   <li>Creates new arrays with the larger capacity</li>
     *   <li>Re-inserts all existing entries into the new arrays</li>
     *   <li>Restores weak reference settings</li>
     * </ol>
     * 
     * <p>Note: The newKey parameter is included in the count but is not inserted during
     * rehashing. The caller must insert it after this method returns.</p>
     * 
     * @param newKey The key that triggered the rehash (used for counting only)
     */
    private void hash_rehash(Object newKey) {
        // NOTE: it's important to avoid GC of weak stuff here, so convert it
        // to plain before rehashing
        boolean oldWeakKeys = weakKeys, oldWeakValues = weakValues;
        updateWeakSettings(false, false);

        Object[] oldKeys = keys;
        Object[] oldValues = values;
        int hashLength = oldKeys.length;

        int usedTotal = 0 + 1; // include the newKey

        for (int i = hashLength - 1; i >= 0; --i) {
            Object key = keys[i];
            if (key != null && values[i] != null) {
                usedTotal++;
            }
        }

        int hashCapacity = 2 * nearestPowerOfTwo(usedTotal);
        if (hashCapacity < 2) {
            hashCapacity = 2;
        }

        keys = new Object[hashCapacity];
        values = new Object[hashCapacity];
        next = new int[hashCapacity];

        freeIndex = hashCapacity;

        for (int i = hashLength - 1; i >= 0; --i) {
            Object key = oldKeys[i];
            if (key != null) {
                Object value = oldValues[i];
                if (value != null) {
                    rawset(key, value);
                }
            }
        }
        updateWeakSettings(oldWeakKeys, oldWeakValues);
    }

    private LuaTable metatable;

    /**
     * Sets a value in the table for a given key.
     * If the key doesn't exist, it will be added. If it exists, the value will be updated.
     * Setting a value to null effectively removes the key from the table.
     * 
     * <p>Thread-safe: This method is synchronized to ensure safe concurrent access.</p>
     * 
     * @param key The key to set (must not be null or NaN)
     * @param value The value to associate with the key, or null to remove the key
     * @throws RuntimeException if key is null (via checkKey)
     */
    @Override
    public synchronized void rawset(Object key, Object value) {
        checkKey(key);
        rawsetHash(key, value);
    }

    private void rawsetHash(Object key, Object value) {
        int index = getHashIndex(key);
        if (index < 0) {
            int mp = getMP(key);
            index = hash_primitiveNewKey(key, mp);
            if (index < 0) {
                rawset(key, value);
                return;
            }
        }
        __setValue(index, value);
    }

    /**
     * Gets a value from the table by integer index.
     * Converts the integer to a Double key as per Lua semantics.
     * 
     * <p>Thread-safe: This method is synchronized to ensure safe concurrent access.</p>
     * 
     * @param index The integer index
     * @return The value at that index, or null if not present
     */
    public synchronized Object rawget(int index) {
        return rawgetHash(LuaState.toDouble(index));
    }

    /**
     * Sets a value in the table by integer index.
     * Converts the integer to a Double key as per Lua semantics.
     * 
     * <p>Thread-safe: This method is synchronized to ensure safe concurrent access.</p>
     * 
     * @param index The integer index
     * @param value The value to set
     */
    public synchronized void rawset(int index, Object value) {
        rawsetHash(LuaState.toDouble(index), value);
    }

    /**
     * Gets a value from the table for a given key.
     * 
     * <p>Thread-safe: This method is synchronized to ensure safe concurrent access.</p>
     * 
     * @param <T> The expected return type (unchecked cast)
     * @param key The key to look up (must not be null or NaN)
     * @return The value associated with the key, or null if not present
     * @throws RuntimeException if key is null or NaN
     */
    @SuppressWarnings("unchecked")
    @Override
    public final synchronized <T> T rawget(Object key) {
        checkKey(key);
        if (key instanceof Double) {
            BaseLib.luaAssert(!((Double) key).isNaN(), "table index is NaN");
        }
        return (T) rawgetHash(key);
    }

    private Object rawgetHash(Object key) {
        int index = getHashIndex(key);
        if (index >= 0) {
            return __getValue(index);
        }
        return null;
    }

    private int getHashIndex(Object key) {
        if (key == keyIndexCacheKey) {
            return keyIndexCacheValue;
        }
        int mp = getMP(key);
        int index = hash_primitiveFindKey(key, mp);
        if (!weakKeys) {
            keyIndexCacheKey = key;
            keyIndexCacheValue = index;
        }
        return index;
    }

    public static void checkKey(Object key) {
        BaseLib.luaAssert(key != null, "table index is nil");
    }

    private Object nextHash(Object key) {
        int index = 0;
        if (key != null) {
            index = 1 + getHashIndex(key);
            if (index <= 0) {
                BaseLib.fail("invalid key to 'next'");
                return null;
            }
        }

        while (true) {
            if (index == keys.length) {
                return null;
            }
            Object next = __getKey(index);
            if (next != null && __getValue(index) != null) {
                return next;
            }
            index++;
        }
    }

    @Override
    public final synchronized Object next(Object key) {
        return nextHash(key);
    }

    @Override
    public final synchronized int len() {
        int high = 2 * keys.length;
        int low = 0;
        while (low < high) {
            int middle = (high + low + 1) >> 1;
            Object value = rawget(middle);
            if (value == null) {
                high = middle - 1;
            } else {
                low = middle;
            }
        }
        while (rawget(low + 1) != null) {
            low++;
        }
        return low;
    }

    @Override
    public synchronized Iterator<Object> keys() {
        return IteratorUtils.filteredIterator(IteratorUtils.arrayIterator(keys), Objects::nonNull);
    }



    public static int luaHashcode(Object a) {
        if (a instanceof Double ad) {
            long l = Double.doubleToLongBits(ad.doubleValue()) & 0x7fffffffffffffffL;
            return (int) (l ^ (l >>> 32));
        }
        if (a instanceof String) {
            return a.hashCode();
        }
        return System.identityHashCode(a);
    }

    private void updateWeakSettings(boolean k, boolean v) {
        keyIndexCacheKey = null;
        keyIndexCacheValue = -1;
        if (k != weakKeys) {
            fixWeakRefs(keys, k);
            weakKeys = k;
        }
        if (v != weakValues) {
            fixWeakRefs(values, v);
            weakValues = v;
        }
    }

    private void fixWeakRefs(Object[] entries, boolean weak) {
        /*
         * Assertion: if the entries are already weak,
         * the parameter "weak" is false, and vice versa.
         * Thus, don't try to fix it to weak if it's already weak.
         */

        //if (entries == null) return;

        for (int i = entries.length - 1; i >= 0; i--) {
            Object o = entries[i];
            if (weak) {
                o = ref(o);
            } else {
                o = unref(o);
            }
            entries[i] = o;
        }
    }

    @Override
    public synchronized LuaTable getMetatable() {
        return metatable;
    }

    @Override
    public synchronized void setMetatable(LuaTable metatable) {
        this.metatable = metatable;
        boolean weakKeys = false, weakValues = false;
        if (metatable != null) {
            Object modeObj = metatable.rawget(BaseLib.MODE_KEY);
            if (modeObj instanceof String mode) {
                weakKeys = (mode.indexOf('k') >= 0);
                weakValues = (mode.indexOf('v') >= 0);
            }
        }
        updateWeakSettings(weakKeys, weakValues);
    }

}
