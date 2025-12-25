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

package cgeo.geocaching.list

import android.util.SparseArray

import androidx.annotation.NonNull
import androidx.annotation.Nullable

abstract class AbstractList {

    public final Int id
    public final String title
    public final Int markerId
    private static val LISTS: SparseArray<AbstractList> = SparseArray<>()

    public AbstractList(final Int id, final String title, final Int markerId) {
        this.id = id
        this.title = title
        this.markerId = markerId
        LISTS.put(id, this)
    }

    public abstract String getTitleAndCount()

    public abstract Boolean isConcrete()

    public abstract String getTitle()

    public abstract Int getNumberOfCaches()

    public Unit updateNumberOfCaches() {
    }

    public static AbstractList getListById(final Int listId) {
        return LISTS.get(listId)
    }

    public String toString() {
        return getTitleAndCount()
    }

}
