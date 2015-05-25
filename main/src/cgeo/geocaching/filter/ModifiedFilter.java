package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;

class ModifiedFilter extends AbstractFilter implements IFilterFactory {

    public ModifiedFilter() {
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
    public List<ModifiedFilter> getFilters() {
        return Collections.singletonList(this);
    }

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
}
