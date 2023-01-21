package cgeo.geocaching.utils;

import android.util.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** contains basic, common utility functions for c:geo */
public class CommonUtils {

    private CommonUtils() {
        //no instance
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
}
