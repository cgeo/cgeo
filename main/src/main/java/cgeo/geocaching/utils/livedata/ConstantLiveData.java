package cgeo.geocaching.utils.livedata;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.Objects;

/**
 * A data holder class whose content cannot change, however provides notify methods to trigger the registered observers.
 * Especially useful if another data holder class is used as content.
 *
 * @param <T> The type of data held by this instance
 */
public class ConstantLiveData<T> extends LiveData<T> {

    public ConstantLiveData(@NonNull final T value) {
        super(value);
    }

    public void notifyDataChanged() {
        setValue(getValue());
    }

    public void postNotifyDataChanged() {
        postValue(getValue());
    }

    @NonNull
    @Override
    public T getValue() {
        return Objects.requireNonNull(super.getValue());
    }
}
