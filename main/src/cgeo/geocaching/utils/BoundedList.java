package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Base class for a limited list.
 *
 * @author blafoo
 */
public class BoundedList<T> extends ArrayList<T> {

    private static final long serialVersionUID = -5077882607489806620L;
    private final int maxEntries;

    public BoundedList(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    private void removeElements(int count) {
        for (int i = 0; i < count; i++) {
            this.remove(0);
        }
    }

    @Override
    public boolean add(T item) {
        removeElements(this.size() + 1 - maxEntries);
        return super.add(item);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        if (collection.size() > this.size()) {
            this.clear();
            for (T item : collection) {
                this.add(item);
            }
            return false;
        } else {
            removeElements(this.size() + collection.size() - maxEntries);
            return super.addAll(collection);
        }
    }

}
