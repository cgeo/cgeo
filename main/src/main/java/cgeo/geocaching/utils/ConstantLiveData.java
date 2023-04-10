package cgeo.geocaching.utils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.Objects;

public class ConstantLiveData<T> extends LiveData<T> {

    public ConstantLiveData(@NonNull T value) {
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
