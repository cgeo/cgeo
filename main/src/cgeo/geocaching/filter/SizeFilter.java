package cgeo.geocaching.filter;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

class SizeFilter extends AbstractFilter {
    private final CacheSize cacheSize;

    public static final Creator<SizeFilter> CREATOR = new Parcelable.Creator<SizeFilter>() {

        @Override
        public SizeFilter createFromParcel(final Parcel in) {
            return new SizeFilter(in);
        }

        @Override
        public SizeFilter[] newArray(final int size) {
            return new SizeFilter[size];
        }
    };

    SizeFilter(@NonNull final CacheSize cacheSize) {
        super(cacheSize.id);
        this.cacheSize = cacheSize;
    }

    protected SizeFilter(final Parcel in) {
        super(in);
        cacheSize = CacheSize.values()[in.readInt()];
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cacheSize == cache.getSize();
    }

    @Override
    @NonNull
    public String getName() {
        return cacheSize.getL10n();
    }

    public static class Factory implements IFilterFactory {

        @Override
        @NonNull
        public List<IFilter> getFilters() {
            final CacheSize[] cacheSizes = CacheSize.values();
            final List<IFilter> filters = new LinkedList<>();
            for (final CacheSize cacheSize : cacheSizes) {
                if (cacheSize != CacheSize.UNKNOWN) {
                    filters.add(new SizeFilter(cacheSize));
                }
            }
            return filters;
        }
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(cacheSize.ordinal());
    }
}
