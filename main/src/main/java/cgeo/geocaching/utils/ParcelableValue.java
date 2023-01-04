package cgeo.geocaching.utils;

import android.os.Parcel;
import android.os.Parcelable;

/** A parcelable holder for all values which can be processed by {@link android.os.Parcel#writeValue(java.lang.Object)} */
public class ParcelableValue<T> implements Parcelable {

    private T value;

    public ParcelableValue() {
        //empty
    }

    public ParcelableValue<T> set(final T value) {
        this.value = value;
        return this;
    }

    public T get() {
        return value;
    }

    //Parcelable stuff

    @SuppressWarnings("unchecked")
    protected ParcelableValue(final Parcel in) {
        this.value = (T) in.readValue(getClass().getClassLoader());
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeValue(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings("rawtypes")
    public static final Creator<ParcelableValue> CREATOR = new Creator<ParcelableValue>() {

        @Override
        public ParcelableValue createFromParcel(final Parcel in) {
            return new ParcelableValue(in);
        }

        @Override
        public ParcelableValue[] newArray(final int size) {
            return new ParcelableValue[size];
        }
    };

}
