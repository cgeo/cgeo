package cgeo.geocaching.utils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class LazyInitializedList<ElementType> extends AbstractList<ElementType> implements Callable<List<ElementType>> {

    private volatile List<ElementType> list;

    private List<ElementType> getList() {
        if (list == null) {
            synchronized(this) {
                try {
                    list = call();
                    if (list == null) {
                        Log.e("LazyInitializedList.getList: null result");
                    }
                } catch (final Exception e) {
                    Log.e("LazyInitializedList.getList", e);
                }
            }
        }
        return list;
    }

    @Override
    public boolean add(final ElementType element) {
        return getList().add(element);
    }

    @Override
    public ElementType set(final int index, final ElementType element) {
        return getList().set(index, element);
    }

    @Override
    public ElementType remove(final int index) {
        return getList().remove(index);
    }

    @Override
    public void add(int index, final ElementType element) {
        getList().add(index, element);
    }

    @Override
    public int size() {
        return getList().size();
    }

    @Override
    public ElementType get(final int index) {
        return getList().get(index);
    }

    @Override
    public void clear() {
        list = new ArrayList<ElementType>();
    }

}
