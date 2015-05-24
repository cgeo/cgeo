package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;

import org.eclipse.jdt.annotation.NonNull;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

abstract class AbstractFilter implements IFilter, Serializable {
    private static final long serialVersionUID = -5918429378818997180L;
    @NonNull
    private final String name;

    protected AbstractFilter(final int nameResourceId) {
        this(CgeoApplication.getInstance().getString(nameResourceId));
    }

    protected AbstractFilter(@NonNull final String name) {
        this.name = name;
    }

    @Override
    public void filter(@NonNull final List<Geocache> list) {
        final List<Geocache> itemsToRemove = new ArrayList<>();
        for (final Geocache item : list) {
            if (!accepts(item)) {
                itemsToRemove.add(item);
            }
        }
        list.removeAll(itemsToRemove);
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    /*
     * show name in array adapter
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeSerializable(this);
    }

    public static final Creator<AbstractFilter> CREATOR
            = new Parcelable.Creator<AbstractFilter>() {

      /**
      * Read the serialized concrete Filter from the parcel.
      * @param in The parcel to read from
      * @return An AbstractFilter
      */
        public AbstractFilter createFromParcel(final Parcel in) {
            // Read serialized concrete Filter from parcel
            return (AbstractFilter) in.readSerializable();
        }

        /**
         * Required by Creator
         */
        public AbstractFilter[] newArray(final int size) {
            return new AbstractFilter[size];
        }
    };

}
