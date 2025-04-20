package cgeo.geocaching.utils;

import cgeo.geocaching.utils.functions.Func1;

import android.os.Build;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Supplier;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.reflect.FieldUtils;

/** contains basic, common utility functions for c:geo */
public class CommonUtils {

    private CommonUtils() {
        //no instance
    }

    /** retrieves the idx's element of the given iterable, or null if iterable doesn't have enough elements */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T get(@Nullable final Iterable<T> iterable, final int idx) {
        if (iterable == null || idx < 0) {
            return null;
        }
        if (iterable instanceof Collection) {
            final int size = ((Collection) iterable).size();
            if (idx >= size) {
                return null;
            }
            if (iterable instanceof List) {
                return (T) ((List) iterable).get(idx);
            }
        }
        int i = 0;
        for (T element : iterable) {
            if (i == idx) {
                return element;
            }
            i++;
        }
        return null;
    }

    public static <T> T first(final Iterable<T> iterable) {
        return get(iterable, 0);
    }

    /**
     * Helper method to quickly add OR remove an item from/to a collection programatically
     * If parameter 'remove' is true then item is removed; otherwise it is added
     */
    public static <T> boolean addRemove(final Collection<T> coll, final T item, final boolean remove) {
        if (remove) {
            return coll.remove(item);
        }
        return coll.add(item);
    }

    /**
     * Compares two given maps. Returns a map with an entry for each key where the two maps differ
     * * For keys existing in m1 but not in m2, there will be an entry with a Pair<null, value ín m2>
     * * for keys existing in m2 but no in m1, there will be an entry with a Pair<value in m1, null>
     * * for keys in both maps but differing values, there will be an entry with a Pair<value in m1, value in m2>
     */
    public static <K, V> Map<K, Pair<V, V>> compare(final Map<K, V> m1, final Map<K, V> m2) {
        final Map<K, V> map1 = m1 == null ? Collections.emptyMap() : m1;
        final Map<K, V> map2 = m2 == null ? Collections.emptyMap() : m2;
        final Map<K, Pair<V, V>> result = new HashMap<>();
        for (Map.Entry<K, V> m1Entry : map1.entrySet()) {
            final V m2Value = map2.get(m1Entry.getKey());
            if (!Objects.equals(m1Entry.getValue(), m2Value)) {
                result.put(m1Entry.getKey(), new Pair<>(m1Entry.getValue(), m2Value));
            }
        }
        for (Map.Entry<K, V> m2Entry : map2.entrySet()) {
            final V m1Value = map1.get(m2Entry.getKey());
            if (!Objects.equals(m1Value, m2Entry.getValue())) {
                result.put(m2Entry.getKey(), new Pair<>(m1Value, m2Entry.getValue()));
            }
        }
        return result;
    }

    public static <T> Comparator<T> getTextSortingComparator(final Function<T, String> mapper) {
        return (o1, o2) -> {
            final String txt1 = mapper == null ? (o1 == null ? null : o1.toString()) : mapper.apply(o1);
            final String txt2 = mapper == null ? (o2 == null ? null : o2.toString()) : mapper.apply(o2);
            return TextUtils.COLLATOR.compare(txt1 == null ? "" : txt1, txt2 == null ? "" : txt2);
        };
    }

    public static <T> Comparator<T> getNullHandlingComparator(@Nullable final Comparator<T> source, final boolean sortNullTop) {
        return getListSortingComparator(source, sortNullTop, Collections.singleton(null));
    }

    public static <T> Comparator<T> getListSortingComparator(@Nullable final Comparator<T> source, @Nullable final Iterable<T> first, @Nullable final Iterable<T> last) {
        return getListSortingComparator(getListSortingComparator(source, true, first), false, last);
    }

    public static <T> Comparator<T> getListSortingComparator(@Nullable final Comparator<T> source, final boolean sortTop, @Nullable final Iterable<T> list) {
        final HashMap<T, Integer> listMap = new HashMap<>();
        if (list != null) {
            for (T item : list) {
                listMap.put(item, listMap.size());
            }
        }
        return (g1, g2) -> {
            final int outsideValue = sortTop ? listMap.size() + 1 : -1;
            final int g1Idx = listMap.containsKey(g1) ? listMap.get(g1) : outsideValue;
            final int g2Idx = listMap.containsKey(g2) ? listMap.get(g2) : outsideValue;

            if (g1Idx == outsideValue && g2Idx == outsideValue) {
                return source == null ? 0 : source.compare(g1, g2);
            }
            return g1Idx - g2Idx;
        };
    }


    /** Returns a ThreadLocal with initial value given by passed Supplier.
     * Use this method instead of ThreadLocal.withInitial() for SDK<26;
     */
    public static <T> ThreadLocal<T> threadLocalWithInitial(final Supplier<T> supplier) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ThreadLocal.withInitial(supplier::get);
        }

        return new ThreadLocal<T>() {

            @Override
            protected T initialValue() {
                return supplier.get();
            }
        };
    }

    /** executes a given action on each 'partitionSize' numer of elements of the given collection. If false is returned, action is abandoned */
    public static <T> void executeOnPartitions(final Collection<T> coll, final int partitionSize, final Func1<List<T>, Boolean> action) {
        final List<T> sublist = new ArrayList<>(partitionSize);
        int cnt = 0;
        for (T element : coll) {
            sublist.add(element);
            cnt++;
            if (cnt == partitionSize) {
                final Boolean cont = action.call(sublist);
                if (!Boolean.TRUE.equals(cont)) {
                    return;
                }
                sublist.clear();
            }
        }
        if (!sublist.isEmpty()) {
            action.call(sublist);
        }
    }

    public static <T> Set<Class<? extends T>> getReferencedClasses(@Nullable final Object obj, @NonNull final Class<T> clazz) {
        if (obj == null) {
            return Collections.emptySet();
        }
        final Set<Class<? extends T>> result = new HashSet<>();
        try {
            final List<Field> fields = FieldUtils.getAllFieldsList(obj.getClass());
            for (Field field : fields) {
                if (clazz.isAssignableFrom(field.getType())) {
                    @SuppressWarnings("unchecked")
                    final Class<? extends T> typeClass = (Class<? extends T>) field.getType();
                    result.add(typeClass);
                }
            }
        } catch (Exception ignore) {
            //ignore
        }
        return result;

    }

    public static <T> void filterCollection(final Collection<T> coll, @NonNull final Predicate<T> retainCondition) {
        if (coll == null) {
            return;
        }
        final Iterator<T> it = coll.iterator();
        while (it.hasNext()) {
            final T item = it.next();
            if (!retainCondition.test(item)) {
                it.remove();
            }
        }
    }

    // --- Helpers for Parcelable ---

    public static <E extends Enum<E>> int enumToInt(final E value) {
        return value == null ? -1 : value.ordinal();
    }

    public static <E extends Enum<E>> E intToEnum(final Class<E> clazz, final int value) {
       return intToEnum(clazz, value, null);
    }

    public static <E extends Enum<E>> E intToEnum(final Class<E> clazz, final int value, final E defaultValue) {
        final E[] enumValues = clazz.getEnumConstants();
        return value < 0 || enumValues == null || value >= enumValues.length ? defaultValue : enumValues[value];
    }


}
