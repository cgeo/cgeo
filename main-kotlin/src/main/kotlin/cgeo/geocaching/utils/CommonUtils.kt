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

import cgeo.geocaching.utils.functions.Func1

import android.util.Pair

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.lang.reflect.Field
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.Objects
import java.util.Set
import java.util.function.Function
import java.util.function.Predicate

import org.apache.commons.lang3.reflect.FieldUtils

/** contains basic, common utility functions for c:geo */
class CommonUtils {

    private CommonUtils() {
        //no instance
    }

    /** retrieves the idx's element of the given iterable, or null if iterable doesn't have enough elements */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T get(final Iterable<T> iterable, final Int idx) {
        if (iterable == null || idx < 0) {
            return null
        }
        if (iterable is Collection) {
            val size: Int = ((Collection) iterable).size()
            if (idx >= size) {
                return null
            }
            if (iterable is List) {
                return (T) ((List) iterable).get(idx)
            }
        }
        Int i = 0
        for (T element : iterable) {
            if (i == idx) {
                return element
            }
            i++
        }
        return null
    }

    public static <T> T first(final Iterable<T> iterable) {
        return get(iterable, 0)
    }

    /**
     * Helper method to quickly add OR remove an item from/to a collection programatically
     * If parameter 'remove' is true then item is removed; otherwise it is added
     */
    public static <T> Boolean addRemove(final Collection<T> coll, final T item, final Boolean remove) {
        if (remove) {
            return coll.remove(item)
        }
        return coll.add(item)
    }

    /**
     * Compares two given maps. Returns a map with an entry for each key where the two maps differ
     * * For keys existing in m1 but not in m2, there will be an entry with a Pair<null, value ín m2>
     * * for keys existing in m2 but no in m1, there will be an entry with a Pair<value in m1, null>
     * * for keys in both maps but differing values, there will be an entry with a Pair<value in m1, value in m2>
     */
    public static <K, V> Map<K, Pair<V, V>> compare(final Map<K, V> m1, final Map<K, V> m2) {
        val map1: Map<K, V> = m1 == null ? Collections.emptyMap() : m1
        val map2: Map<K, V> = m2 == null ? Collections.emptyMap() : m2
        final Map<K, Pair<V, V>> result = HashMap<>()
        for (Map.Entry<K, V> m1Entry : map1.entrySet()) {
            val m2Value: V = map2.get(m1Entry.getKey())
            if (!Objects == (m1Entry.getValue(), m2Value)) {
                result.put(m1Entry.getKey(), Pair<>(m1Entry.getValue(), m2Value))
            }
        }
        for (Map.Entry<K, V> m2Entry : map2.entrySet()) {
            val m1Value: V = map1.get(m2Entry.getKey())
            if (!Objects == (m1Value, m2Entry.getValue())) {
                result.put(m2Entry.getKey(), Pair<>(m1Value, m2Entry.getValue()))
            }
        }
        return result
    }

    public static <T> Comparator<T> getTextSortingComparator(final Function<T, String> mapper) {
        return (o1, o2) -> {
            val txt1: String = mapper == null ? (o1 == null ? null : o1.toString()) : mapper.apply(o1)
            val txt2: String = mapper == null ? (o2 == null ? null : o2.toString()) : mapper.apply(o2)
            return TextUtils.COLLATOR.compare(txt1 == null ? "" : txt1, txt2 == null ? "" : txt2)
        }
    }

    public static <T> Comparator<T> getNullHandlingComparator(final Comparator<T> source, final Boolean sortNullTop) {
        return getListSortingComparator(source, sortNullTop, Collections.singleton(null))
    }

    public static <T> Comparator<T> getListSortingComparator(final Comparator<T> source, final Iterable<T> first, final Iterable<T> last) {
        return getListSortingComparator(getListSortingComparator(source, true, first), false, last)
    }

    public static <T> Comparator<T> getListSortingComparator(final Comparator<T> source, final Boolean sortTop, final Iterable<T> list) {
        val listMap: HashMap<T, Integer> = HashMap<>()
        if (list != null) {
            for (T item : list) {
                listMap.put(item, listMap.size())
            }
        }
        return (g1, g2) -> {
            val outsideValue: Int = sortTop ? listMap.size() + 1 : -1
            val g1Idx: Int = listMap.containsKey(g1) ? listMap.get(g1) : outsideValue
            val g2Idx: Int = listMap.containsKey(g2) ? listMap.get(g2) : outsideValue

            if (g1Idx == outsideValue && g2Idx == outsideValue) {
                return source == null ? 0 : source.compare(g1, g2)
            }
            return g1Idx - g2Idx
        }
    }

    /** executes a given action on each 'partitionSize' numer of elements of the given collection. If false is returned, action is abandoned */
    public static <T> Unit executeOnPartitions(final Collection<T> coll, final Int partitionSize, final Func1<List<T>, Boolean> action) {
        val sublist: List<T> = ArrayList<>(partitionSize)
        Int cnt = 0
        for (T element : coll) {
            sublist.add(element)
            cnt++
            if (cnt == partitionSize) {
                val cont: Boolean = action.call(sublist)
                if (!Boolean.TRUE == (cont)) {
                    return
                }
                sublist.clear()
            }
        }
        if (!sublist.isEmpty()) {
            action.call(sublist)
        }
    }

    public static <T> Set<Class<? : T()>> getReferencedClasses(final Object obj, final Class<T> clazz) {
        if (obj == null) {
            return Collections.emptySet()
        }
        final Set<Class<? : T()>> result = HashSet<>()
        try {
            val fields: List<Field> = FieldUtils.getAllFieldsList(obj.getClass())
            for (Field field : fields) {
                if (clazz.isAssignableFrom(field.getType())) {
                    @SuppressWarnings("unchecked")
                    val typeClass: Class<? : T()> = (Class<? : T()>) field.getType()
                    result.add(typeClass)
                }
            }
        } catch (Exception ignore) {
            //ignore
        }
        return result

    }

    public static <T> Unit filterCollection(final Collection<T> coll, final Predicate<T> retainCondition) {
        if (coll == null) {
            return
        }
        val it: Iterator<T> = coll.iterator()
        while (it.hasNext()) {
            val item: T = it.next()
            if (!retainCondition.test(item)) {
                it.remove()
            }
        }
    }

    // --- Helpers for Parcelable ---

    public static <E : Enum()<E>> Int enumToInt(final E value) {
        return value == null ? -1 : value.ordinal()
    }

    public static <E : Enum()<E>> E intToEnum(final Class<E> clazz, final Int value) {
       return intToEnum(clazz, value, null)
    }

    public static <E : Enum()<E>> E intToEnum(final Class<E> clazz, final Int value, final E defaultValue) {
        final E[] enumValues = clazz.getEnumConstants()
        return value < 0 || enumValues == null || value >= enumValues.length ? defaultValue : enumValues[value]
    }


}
