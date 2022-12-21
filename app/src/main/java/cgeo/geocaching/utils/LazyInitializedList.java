package cgeo.geocaching.utils;

import androidx.annotation.NonNull;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class LazyInitializedList<ElementType> extends AbstractList<ElementType> implements Callable<List<ElementType>> {

    private volatile List<ElementType> list;

    @NonNull
    public List<ElementType> getUnderlyingList() {
        if (list == null) {
            synchronized (this) {
                try {
                    list = call();
                    if (list == null) {
                        Log.w("LazyInitializedList.getList: null result");
                    }
                } catch (final Exception e) {
                    Log.w("LazyInitializedList.getList", e);
                }
                if (list == null) {
                    Log.w("LazyInitializedList.getList: using an empty list as a fallback");
                    list = Collections.emptyList();
                }
            }
        }
        return list;
    }

    @Override
    public boolean add(final ElementType element) {
        return getUnderlyingList().add(element);
    }

    @Override
    public ElementType set(final int index, final ElementType element) {
        return getUnderlyingList().set(index, element);
    }

    @Override
    public ElementType remove(final int index) {
        return getUnderlyingList().remove(index);
    }

    @Override
    public void add(final int index, final ElementType element) {
        getUnderlyingList().add(index, element);
    }

    @Override
    public int size() {
        return getUnderlyingList().size();
    }

    @Override
    public ElementType get(final int index) {
        return getUnderlyingList().get(index);
    }

    @Override
    public void clear() {
        list = new ArrayList<>();
    }

}
