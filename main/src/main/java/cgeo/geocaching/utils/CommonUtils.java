package cgeo.geocaching.utils;

import cgeo.geocaching.utils.functions.Action3;
import cgeo.geocaching.utils.functions.Action4;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public static <T> Comparator<T> getNullHandlingComparator(@Nullable final Comparator<T> source, final boolean sortNullTop) {
        return (g1, g2) -> {
            if (g1 == g2) {
                return 0;
            }
            if (g1 == null) {
                return sortNullTop ? -1 : 1;
            }
            if (g2 == null) {
                return sortNullTop ? 1 : -1;
            }
            return source == null ? g1.toString().compareTo(g2.toString()) : source.compare(g1, g2);
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

    @SuppressWarnings({"PMD.NPathComplexity"})
    public static <T, G> void groupList(final List<T> items, @Nullable final Func1<T, G> groupMapper, @Nullable final Comparator<G> groupOrder, final int minGroupCount,
                                        final Action3<G, Integer, Integer> groupAdder, final Action4<T, Integer, G, Integer> itemAdder) {

        //create lists per group
        final Map<G, List<Pair<Integer, T>>> groupedListMap = new HashMap<>();
        int pos = 0;
        for (T value : items) {
            final G group = groupMapper == null ? null : groupMapper.call(value);
            List<Pair<Integer, T>> groupList = groupedListMap.get(group);
            if (groupList == null) {
                groupList = new ArrayList<>();
                groupedListMap.put(group, groupList);
            }
            groupList.add(new Pair<>(pos, value));
            pos++;
        }

        //check whether to abandom
        if (groupedListMap.size() < minGroupCount) {
            //no grouping shall take place
            int idx = 0;
            for (T item : items) {
                itemAdder.call(item, idx++, null, -1);
            }
            return;
        }

        //sort groups
        final List<G> sortedGroupList = new ArrayList<>(groupedListMap.keySet());
        Collections.sort(sortedGroupList, getNullHandlingComparator(groupOrder, true));

        //construct result
        int listIdx = 0;
        int groupIdx = -1;
        for (G group : sortedGroupList) {

            //group item, if not null
            if (group != null) {
                groupAdder.call(group, listIdx + 1, Objects.requireNonNull(groupedListMap.get(group)).size());
                groupIdx = listIdx;
                listIdx++;
            }

            //items in group
            for (Pair<Integer, T> valuePair : Objects.requireNonNull(groupedListMap.get(group))) {
                itemAdder.call(valuePair.second, valuePair.first, group, groupIdx);
                listIdx++;
            }
        }
    }

    public static boolean containsClassReference(final Object obj, @NonNull final Class<?> clazz) {
        if (obj == null) {
            return false;
        }
        try {
            final List<Field> fields = FieldUtils.getAllFieldsList(obj.getClass());
            for (Field field : fields) {
                if (clazz.isAssignableFrom(field.getType())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            //ignore
            return false;
        }
    }


}
