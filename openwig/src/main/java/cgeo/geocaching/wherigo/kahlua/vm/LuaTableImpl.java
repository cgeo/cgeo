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


public class LuaTableImpl implements LuaTable {
    private boolean weakKeys, weakValues;

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

    private int getMP(Object key) {
        // assert key != null
        int capacity = keys.length;
        return luaHashcode(key) & (capacity - 1);
    }

    private Object unref(Object o) {
        if (!canBeWeakObject(o)) {
            return o;
        }

        // Assertion: o instanceof WeakReference
        return ((WeakReference<?>) o).get();
    }

    private Object ref(Object o) {
        if (!canBeWeakObject(o)) {
            return o;
        }

        return new WeakReference<>(o);
    }

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

    public void rawset(Object key, Object value) {
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

    public Object rawget(int index) {
        return rawgetHash(LuaState.toDouble(index));
    }

    public void rawset(int index, Object value) {
        rawsetHash(LuaState.toDouble(index), value);
    }

    public final <T> T rawget(Object key) {
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

    public final Object next(Object key) {
        return nextHash(key);
    }

    public final int len() {
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

    public Iterator<Object> keys() {
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

    public LuaTable getMetatable() {
        return metatable;
    }

    public void setMetatable(LuaTable metatable) {
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
