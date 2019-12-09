package cgeo.geocaching.filter;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

class TypeFilter extends AbstractFilter {
    private final CacheType cacheType;

    TypeFilter(@NonNull final CacheType cacheType) {
        super(cacheType.id);
        this.cacheType = cacheType;
    }

    protected TypeFilter(final Parcel in) {
        super(in);
        cacheType = CacheType.values()[in.readInt()];
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cacheType == cache.getType();
    }

    @Override
    @NonNull
    public String getName() {
        return cacheType.getL10n();
    }

    public static class Factory implements IFilterFactory {

        @Override
        @NonNull
        public List<IFilter> getFilters() {
            final CacheType[] types = CacheType.values();
            final List<IFilter> filters = new LinkedList<>();
            for (final CacheType cacheType : types) {
                if (cacheType != CacheType.ALL) {
                    filters.add(new TypeFilter(cacheType));
                }
            }
            return filters;
        }
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(cacheType.ordinal());
    }

    public static final Creator<TypeFilter> CREATOR
            = new Parcelable.Creator<TypeFilter>() {

        @Override
        public TypeFilter createFromParcel(final Parcel in) {
            return new TypeFilter(in);
        }

        @Override
        public TypeFilter[] newArray(final int size) {
            return new TypeFilter[size];
        }
    };
}
