package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

class ModifiedFilter extends AbstractFilter implements IFilterFactory {

    public static final Creator<ModifiedFilter> CREATOR
            = new Parcelable.Creator<ModifiedFilter>() {

        @Override
        public ModifiedFilter createFromParcel(final Parcel in) {
            return new ModifiedFilter(in);
        }

        @Override
        public ModifiedFilter[] newArray(final int size) {
            return new ModifiedFilter[size];
        }
    };

    ModifiedFilter() {
        super(R.string.caches_filter_modified);
    }

    protected ModifiedFilter(final Parcel in) {
        super(in);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        // modified on GC
        return cache.hasUserModifiedCoords() || cache.hasFinalDefined();
    }

    @Override
    @NonNull
    public List<IFilter> getFilters() {
        return Collections.<IFilter> singletonList(this);
    }
}
