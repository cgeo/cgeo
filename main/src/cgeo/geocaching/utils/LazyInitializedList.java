package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class LazyInitializedList<ElementType> implements Iterable<ElementType> {

    private List<ElementType> list;

    public List<ElementType> get() {
        initializeList();
        return list;
    }

    private void initializeList() {
        if (list == null) {
            list = loadFromDatabase();
        }
    }

    protected abstract List<ElementType> loadFromDatabase();

    public void add(final ElementType element) {
        initializeList();
        list.add(element);
    }

    public void prepend(final ElementType element) {
        initializeList();
        list.add(0, element);
    }

    public void set(final List<ElementType> elements) {
        list = new ArrayList<ElementType>(elements);
    }

    public boolean isEmpty() {
        initializeList();
        return list.isEmpty();
    }

    public ElementType remove(final int index) {
        initializeList();
        return list.remove(index);
    }

    public void add(int index, final ElementType element) {
        initializeList();
        list.add(index, element);
    }

    public int size() {
        initializeList();
        return list.size();
    }

    @Override
    public Iterator<ElementType> iterator() {
        initializeList();
        return list.iterator();
    }

    public ElementType get(final int index) {
        initializeList();
        return list.get(index);
    }
}
