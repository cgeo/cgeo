package cgeo.geocaching.brouter.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Behaves like an Array of list
 * with lazy list-allocation at getList
 *
 * @author ab
 */
public class LazyArrayOfLists<E> {
    private final ArrayList<ArrayList<E>> lists;

    public LazyArrayOfLists(int size) {
        lists = new ArrayList<ArrayList<E>>(size);
        for (int i = 0; i < size; i++) {
            lists.add(null);
        }
    }

    public List<E> getList(int idx) {
        ArrayList<E> list = lists.get(idx);
        if (list == null) {
            list = new ArrayList<E>();
            lists.set(idx, list);
        }
        return list;
    }

    public int getSize(int idx) {
        List<E> list = lists.get(idx);
        return list == null ? 0 : list.size();
    }

    public void trimAll() {
        for (int idx = 0; idx < lists.size(); idx++) {
            ArrayList<E> list = lists.get(idx);
            if (list != null) {
                list.trimToSize();
            }
        }
    }
}
