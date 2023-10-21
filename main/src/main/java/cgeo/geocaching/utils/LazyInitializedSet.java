package cgeo.geocaching.utils;

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class LazyInitializedSet<T> extends AbstractSet<T> {

    private volatile Set<T> set;
    private final Supplier<Collection<T>> setSupplier;

    public LazyInitializedSet(final Supplier<Collection<T>> setSupplier) {
        this.setSupplier = setSupplier;
    }

    @NonNull
    public Set<T> getUnderlyingSet() {
        if (set == null) {
            synchronized (this) {
                try {
                    final Collection<T> rawSet = setSupplier.get();
                    if (rawSet != null && Set.class.isAssignableFrom(rawSet.getClass())) {
                        this.set = (Set<T>) rawSet;
                    } else {
                        this.set = new HashSet<>();
                        if (rawSet != null) {
                            this.set.addAll(rawSet);
                        }
                    }

                } catch (final Exception e) {
                    Log.w("LazyInitializedList.getList", e);
                }
                if (set == null) {
                    Log.w("LazyInitializedList.getList: using an empty list as a fallback");
                    set = new HashSet<>();
                }
            }
        }
        return set;
    }

    @Override
    public boolean add(final T element) {
        return getUnderlyingSet().add(element);
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return getUnderlyingSet().iterator();
    }

    @Override
    public int size() {
        return getUnderlyingSet().size();
    }

    @Override
    public void clear() {
        set = new HashSet<>();
    }

}
