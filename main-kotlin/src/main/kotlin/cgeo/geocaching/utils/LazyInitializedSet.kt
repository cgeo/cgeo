// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import androidx.annotation.NonNull
import androidx.core.util.Supplier

import java.util.AbstractSet
import java.util.Collection
import java.util.HashSet
import java.util.Iterator
import java.util.Set

class LazyInitializedSet<T> : AbstractSet()<T> {

    private volatile Set<T> set
    private final Supplier<Collection<T>> setSupplier

    public LazyInitializedSet(final Supplier<Collection<T>> setSupplier) {
        this.setSupplier = setSupplier
    }

    public Set<T> getUnderlyingSet() {
        if (set == null) {
            synchronized (this) {
                try {
                    val rawSet: Collection<T> = setSupplier.get()
                    if (rawSet != null && Set.class.isAssignableFrom(rawSet.getClass())) {
                        this.set = (Set<T>) rawSet
                    } else {
                        this.set = HashSet<>()
                        if (rawSet != null) {
                            this.set.addAll(rawSet)
                        }
                    }

                } catch (final Exception e) {
                    Log.w("LazyInitializedList.getList", e)
                }
                if (set == null) {
                    Log.w("LazyInitializedList.getList: using an empty list as a fallback")
                    set = HashSet<>()
                }
            }
        }
        return set
    }

    override     public Boolean add(final T element) {
        return getUnderlyingSet().add(element)
    }

    override     public Iterator<T> iterator() {
        return getUnderlyingSet().iterator()
    }

    override     public Int size() {
        return getUnderlyingSet().size()
    }

    override     public Unit clear() {
        set = HashSet<>()
    }

}
