package cgeo.geocaching.utils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public abstract class LazyInitializedList<ElementType> extends AbstractList<ElementType> {

    private volatile List<ElementType> list;

    private void initializeList() {
        if (list == null) {
            synchronized (this) {
                if (list == null) {
                    list = loadFromDatabase();
                }
            }
        }
    }

    protected abstract List<ElementType> loadFromDatabase();

    @Override
    public boolean add(final ElementType element) {
        initializeList();
        return list.add(element);
    }

    public void prepend(final ElementType element) {
        add(0, element);
    }

    public void set(final List<ElementType> elements) {
        list = elements != null ? new ArrayList<ElementType>(elements) : new ArrayList<ElementType>();
    }

    @Override
    public ElementType set(final int index, final ElementType element) {
        initializeList();
        return list.set(index, element);
    }

    @Override
    public ElementType remove(final int index) {
        initializeList();
        return list.remove(index);
    }

    @Override
    public void add(int index, final ElementType element) {
        initializeList();
        list.add(index, element);
    }

    @Override
    public int size() {
        initializeList();
        return list.size();
    }

    @Override
    public ElementType get(final int index) {
        initializeList();
        return list.get(index);
    }

}
