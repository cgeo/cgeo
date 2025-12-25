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

import android.os.Parcel
import android.os.Parcelable

/** A parcelable holder for all values which can be processed by {@link android.os.Parcel#writeValue(java.lang.Object)} */
class ParcelableValue<T> : Parcelable {

    private T value

    public ParcelableValue() {
        //empty
    }

    public ParcelableValue<T> set(final T value) {
        this.value = value
        return this
    }

    public T get() {
        return value
    }

    //Parcelable stuff

    @SuppressWarnings("unchecked")
    protected ParcelableValue(final Parcel in) {
        this.value = (T) in.readValue(getClass().getClassLoader())
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeValue(value)
    }

    override     public Int describeContents() {
        return 0
    }

    @SuppressWarnings("rawtypes")
    public static val CREATOR: Creator<ParcelableValue> = Creator<ParcelableValue>() {

        override         public ParcelableValue createFromParcel(final Parcel in) {
            return ParcelableValue(in)
        }

        override         public ParcelableValue[] newArray(final Int size) {
            return ParcelableValue[size]
        }
    }

}
