package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractFilter implements IFilter {
    @NonNull
    private final String name;

    protected AbstractFilter(@StringRes final int nameResourceId) {
        this(CgeoApplication.getInstance().getString(nameResourceId));
    }

    protected AbstractFilter(@NonNull final String name) {
        this.name = name;
    }

    protected AbstractFilter(final Parcel in) {
        name = in.readString();
    }

    @Override
    public void filter(@NonNull final List<Geocache> list) {

        //method must be performant when used with very large lists (e.g. 50000 elements)
        // filtered into very large lists and also very short lists (e.g. 30 elements)
        //be aware that "list" most likely is an ArrayList, thus "get(i)" is very performant but "remove" is definitely not -> don't use it!

        final List<Geocache> itemsToKeep = new ArrayList<>();
        for (final Geocache item : list) {
            if (accepts(item)) {
                itemsToKeep.add(item);
            }
        }

        list.clear();
        //note that since both "list" and "itemsToKeep" are ArrayLists, the addAll-operation is very fast (two arraycopies of the references)
        list.addAll(itemsToKeep);
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
    @NonNull
    public String toString() {
        return getName();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(name);
    }
}
