package cgeo.geocaching.filter;

import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

class AttributeFilter extends AbstractFilter {

    private final String attribute;

    public static final Creator<AttributeFilter> CREATOR = new Parcelable.Creator<AttributeFilter>() {

        @Override
        public AttributeFilter createFromParcel(final Parcel in) {
            return new AttributeFilter(in);
        }

        @Override
        public AttributeFilter[] newArray(final int size) {
            return new AttributeFilter[size];
        }
    };

    AttributeFilter(@NonNull final String name, final String attribute) {
        super(name);
        this.attribute = attribute;
    }

    protected AttributeFilter(final Parcel in) {
        super(in);
        attribute = in.readString();
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.getAttributes().contains(attribute);
    }

    public static class Factory implements IFilterFactory {

        @Override
        @NonNull
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new LinkedList<>();
            for (final CacheAttribute cacheAttribute : CacheAttribute.values()) {
                filters.add(new AttributeFilter(cacheAttribute.getL10n(true), cacheAttribute.getValue(true)));
                filters.add(new AttributeFilter(cacheAttribute.getL10n(false), cacheAttribute.getValue(false)));
            }
            return filters;
        }

    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(attribute);
    }
}
