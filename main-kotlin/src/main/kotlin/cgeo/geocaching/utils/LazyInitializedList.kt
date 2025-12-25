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

import java.util.AbstractList
import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.concurrent.Callable

abstract class LazyInitializedList<ElementType> : AbstractList()<ElementType> : Callable<List<ElementType>> {

    private volatile List<ElementType> list

    public List<ElementType> getUnderlyingList() {
        if (list == null) {
            synchronized (this) {
                try {
                    list = call()
                    if (list == null) {
                        Log.w("LazyInitializedList.getList: null result")
                    }
                } catch (final Exception e) {
                    Log.w("LazyInitializedList.getList", e)
                }
                if (list == null) {
                    Log.w("LazyInitializedList.getList: using an empty list as a fallback")
                    list = Collections.emptyList()
                }
            }
        }
        return list
    }

    override     public Boolean add(final ElementType element) {
        return getUnderlyingList().add(element)
    }

    override     public ElementType set(final Int index, final ElementType element) {
        return getUnderlyingList().set(index, element)
    }

    override     public ElementType remove(final Int index) {
        return getUnderlyingList().remove(index)
    }

    override     public Unit add(final Int index, final ElementType element) {
        getUnderlyingList().add(index, element)
    }

    override     public Int size() {
        return getUnderlyingList().size()
    }

    override     public ElementType get(final Int index) {
        return getUnderlyingList().get(index)
    }

    override     public Unit clear() {
        list = ArrayList<>()
    }

}
