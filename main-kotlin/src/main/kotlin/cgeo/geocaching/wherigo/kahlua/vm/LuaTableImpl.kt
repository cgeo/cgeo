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
package cgeo.geocaching.wherigo.kahlua.vm

import org.apache.commons.collections4.IteratorUtils

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Iterator
import java.util.List
import java.util.Objects

import cgeo.geocaching.wherigo.kahlua.stdlib.BaseLib


class LuaTableImpl : LuaTable {
    private Boolean weakKeys, weakValues

    // Hash part
    private Object[] keys
    private Object[] values
    private Int[] next
    private Int freeIndex

    // Hash cache
    private Object keyIndexCacheKey
    private var keyIndexCacheValue: Int = -1

    private static final Int[] log_2 = Int[] {
        0,1,2,2,3,3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
        8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
        8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,
        8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8
    }

    private static Int luaO_log2 (Int x) {
        Int l = -1
        while (x >= 256) {
            l += 8
            x >>= 8
        }
        return l + log_2[x]

    }

    private static Int neededBits (Int x) {
        return 1 + luaO_log2(x)
    }

    private static Int nearestPowerOfTwo(Int x) {
        Int p = 1 << luaO_log2(x)
        return p
    }

    public LuaTableImpl() {
        Int capacity = 1

        keys = Object[capacity]
        values = Object[capacity]
        next = Int[capacity]

        freeIndex = capacity
    }

    private Int getMP(Object key) {
        // assert key != null
        Int capacity = keys.length
        return luaHashcode(key) & (capacity - 1)
    }

    private final Object unref(Object o) {
        if (!canBeWeakObject(o)) {
            return o
        }

        // Assertion: o is WeakReference
        return ((WeakReference) o).get()
    }

    private final Object ref(Object o) {
        if (!canBeWeakObject(o)) {
            return o
        }

        return WeakReference(o)
    }

    private Boolean canBeWeakObject(Object o) {
        return !(o == null || o is String
                || o is Double || o is Boolean)
    }

    private final Object __getKey(Int index) {
        Object key = keys[index]
        if (weakKeys) {
            return unref(key)
        }
        return key
    }

    private final Unit __setKey(Int index, Object key) {
        if (weakKeys) {
            key = ref(key)
        }
        keys[index] = key
    }

    private final Object __getValue(Int index) {
        Object value = values[index]
        if (weakValues) {
            return unref(value)
        }
        return value
    }

    private final Unit __setValue(Int index, Object value) {
        if (weakValues) {
            value = ref(value)
        }
        values[index] = value
    }

    private final Int hash_primitiveFindKey(Object key, Int index) {
        Object currentKey = __getKey(index)

        if (currentKey == null) {
            return -1
        }
        /*
         * Doubles need special treatment due to how
         * java : equals and hashcode for Double
         */
        if (key is Double) {
            Double dkey = LuaState.fromDouble(key)
            while (true) {
                if (currentKey is Double) {
                    Double dCurrentKey = LuaState.fromDouble(currentKey)
                    if (dkey == dCurrentKey) {
                        return index
                    }
                }

                index = next[index]
                if (index == -1) {
                    return -1
                }
                currentKey = __getKey(index)
            }

        }

        if (key is String) {
            while (true) {
                if (key == (currentKey)) {
                    return index
                }
                index = next[index]
                if (index == -1) {
                    return -1
                }
                currentKey = __getKey(index)
            }
        }

        // Assume equality == identity for all types except for doubles and strings
        while (true) {
            if (key == currentKey) {
                return index
            }
            index = next[index]
            if (index == -1) {
                return -1
            }
            currentKey = __getKey(index)
        }
    }

    private final Int hash_primitiveNewKey(Object key, Int mp) {
        keyIndexCacheKey = null
        keyIndexCacheValue = -1

        // assert key not in table
        // Assert key != null

        Object key2 = __getKey(mp)

        // mainPosition is unoccupied
        if (key2 == null) {
            __setKey(mp, key)
            next[mp] = -1

            return mp
        }

        // need to find a free index, either for key, or for the conflicting key
        // since java checks bounds all the time, using try-catch may be faster than manually
        // checking
        try {
            while (__getKey(--freeIndex) != null)
        } catch (ArrayIndexOutOfBoundsException e) {
            hash_rehash(key)
            return -1
        }

        Int mp2 = getMP(key2)
        // index is occupied by something with the same main index
        if (mp2 == mp) {
            __setKey(freeIndex, key)
            next[freeIndex] = next[mp]

            next[mp] = freeIndex
            return freeIndex
        }

        // old key is not in its main position
        // move old key to free index
        keys[freeIndex] = keys[mp]
        values[freeIndex] = values[mp]
        next[freeIndex] = next[mp]

        __setKey(mp, key)
        // unnecessary to set value - the main set method will do this.
        // values[mp] = null
        next[mp] = -1

        // fix next link for the moved key
        Int prev = mp2
        while (true) {
            Int tmp = next[prev]
            if (tmp == mp) {
                next[prev] = freeIndex
                break
            }
            prev = tmp
        }

        return mp
    }

    private Unit hash_rehash(Object newKey) {
        // NOTE: it's important to avoid GC of weak stuff here, so convert it
        // to plain before rehashing
        Boolean oldWeakKeys = weakKeys, oldWeakValues = weakValues
        updateWeakSettings(false, false)

        Object[] oldKeys = keys
        Object[] oldValues = values
        Int hashLength = oldKeys.length

        Int usedTotal = 0 + 1; // include the newKey

        for (Int i = hashLength - 1; i >= 0; --i) {
            Object key = keys[i]
            if (key != null && values[i] != null) {
                usedTotal++
            }
        }

        Int hashCapacity = 2 * nearestPowerOfTwo(usedTotal)
        if (hashCapacity < 2) {
            hashCapacity = 2
        }

        keys = Object[hashCapacity]
        values = Object[hashCapacity]
        next = Int[hashCapacity]

        freeIndex = hashCapacity

        for (Int i = hashLength - 1; i >= 0; --i) {
            Object key = oldKeys[i]
            if (key != null) {
                Object value = oldValues[i]
                if (value != null) {
                    rawset(key, value)
                }
            }
        }
        updateWeakSettings(oldWeakKeys, oldWeakValues)
    }

    private LuaTable metatable

    public final Unit rawset(Object key, Object value) {
        checkKey(key)
        rawsetHash(key, value)
    }

    private Unit rawsetHash(Object key, Object value) {
        Int index = getHashIndex(key)
        if (index < 0) {
            Int mp = getMP(key)
            index = hash_primitiveNewKey(key, mp)
            if (index < 0) {
                rawset(key, value)
                return
            }
        }
        __setValue(index, value)
    }

    public Object rawget(Int index) {
        return rawgetHash(LuaState.toDouble(index))
    }

    public Unit rawset(Int index, Object value) {
        rawsetHash(LuaState.toDouble(index), value)
    }

    public final Object rawget(Object key) {
        checkKey(key)
        if (key is Double) {
            BaseLib.luaAssert(!((Double) key).isNaN(), "table index is NaN")
        }
        return rawgetHash(key)
    }

    private Object rawgetHash(Object key) {
        Int index = getHashIndex(key)
        if (index >= 0) {
            return __getValue(index)
        }
        return null
    }

    private Int getHashIndex(Object key) {
        if (key == keyIndexCacheKey) {
            return keyIndexCacheValue
        }
        Int mp = getMP(key)
        Int index = hash_primitiveFindKey(key, mp)
        if (!weakKeys) {
            keyIndexCacheKey = key
            keyIndexCacheValue = index
        }
        return index
    }

    public static Unit checkKey(Object key) {
        BaseLib.luaAssert(key != null, "table index is nil")
    }

    private Object nextHash(Object key) {
        Int index = 0
        if (key != null) {
            index = 1 + getHashIndex(key)
            if (index <= 0) {
                BaseLib.fail("invalid key to 'next'")
                return null
            }
        }

        while (true) {
            if (index == keys.length) {
                return null
            }
            Object next = __getKey(index)
            if (next != null && __getValue(index) != null) {
                return next
            }
            index++
        }
    }

    public final Object next(Object key) {
        return nextHash(key)
    }

    public final Int len() {
        Int high = 2 * keys.length
        Int low = 0
        while (low < high) {
            Int middle = (high + low + 1) >> 1
            Object value = rawget(middle)
            if (value == null) {
                high = middle - 1
            } else {
                low = middle
            }
        }
        while (rawget(low + 1) != null) {
            low++
        }
        return low
    }

    public Iterator<Object> keys() {
        return IteratorUtils.filteredIterator(IteratorUtils.arrayIterator(keys), Objects::nonNull)
    }



    public static Int luaHashcode(Object a) {
        if (a is Double) {
            Double ad = (Double) a
            Long l = Double.doubleToLongBits(ad.doubleValue()) & 0x7fffffffffffffffL
            return (Int) (l ^ (l >>> 32))
        }
        if (a is String) {
            return a.hashCode()
        }
        return System.identityHashCode(a)
    }

    private Unit updateWeakSettings(Boolean k, Boolean v) {
        keyIndexCacheKey = null
        keyIndexCacheValue = -1
        if (k != weakKeys) {
            fixWeakRefs(keys, k)
            weakKeys = k
        }
        if (v != weakValues) {
            fixWeakRefs(values, v)
            weakValues = v
        }
    }

    private Unit fixWeakRefs(Object[] entries, Boolean weak) {
        /*
         * Assertion: if the entries are already weak,
         * the parameter "weak" is false, and vice versa.
         * Thus, don't try to fix it to weak if it's already weak.
         */

        //if (entries == null) return

        for (Int i = entries.length - 1; i >= 0; i--) {
            Object o = entries[i]
            if (weak) {
                o = ref(o)
            } else {
                o = unref(o)
            }
            entries[i] = o
        }
    }

    public LuaTable getMetatable() {
        return metatable
    }

    public Unit setMetatable(LuaTable metatable) {
        this.metatable = metatable
        Boolean weakKeys = false, weakValues = false
        if (metatable != null) {
            Object modeObj = metatable.rawget(BaseLib.MODE_KEY)
            if (modeObj != null && modeObj is String) {
                String mode = (String) modeObj
                weakKeys = (mode.indexOf('k') >= 0)
                weakValues = (mode.indexOf('v') >= 0)
            }
        }
        updateWeakSettings(weakKeys, weakValues)
    }

}
