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

package cgeo.geocaching.utils.livedata

import androidx.annotation.NonNull
import androidx.lifecycle.LiveData

import java.util.Objects

/**
 * A data holder class whose content cannot change, however provides notify methods to trigger the registered observers.
 * Especially useful if another data holder class is used as content.
 *
 * @param <T> The type of data held by this instance
 */
class ConstantLiveData<T> : LiveData()<T> {

    public ConstantLiveData(final T value) {
        super(value)
    }

    public Unit notifyDataChanged() {
        setValue(getValue())
    }

    public Unit postNotifyDataChanged() {
        postValue(getValue())
    }

    override     public T getValue() {
        return Objects.requireNonNull(super.getValue())
    }
}
