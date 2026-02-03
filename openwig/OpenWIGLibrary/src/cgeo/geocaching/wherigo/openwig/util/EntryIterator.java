package cgeo.geocaching.wherigo.openwig.util;

import java.util.Iterator;
import java.util.Map;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.LuaTable;

public class EntryIterator<K,V> implements Iterator<Map.Entry<K,V>> {

    private final LuaTable<K,V> table;
    private K currentKey;
    private K nextKey;
    private boolean hasNextCached;

    public EntryIterator(final LuaTable<K,V> table) {
        this.table = table;
    }

    @Override
    public boolean hasNext() {
        if (!hasNextCached) {
            nextKey = table.next(currentKey);
            hasNextCached = true;
        }
        return nextKey != null;
    }

    @Override
    public Map.Entry<K,V> next() {
        if (!hasNextCached) {
            nextKey = table.next(currentKey);
        }
        if (nextKey == null) {
            throw new java.util.NoSuchElementException();
        }
        currentKey = nextKey;
        hasNextCached = false;
        final V value = table.rawget(currentKey);
        return new java.util.AbstractMap.SimpleEntry<>(currentKey, value);
    }
}
