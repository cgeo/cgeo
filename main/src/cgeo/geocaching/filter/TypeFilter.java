package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheType;

import org.eclipse.jdt.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

class TypeFilter extends AbstractFilter {
    private final CacheType cacheType;

    public TypeFilter(@NonNull final CacheType cacheType) {
        super(cacheType.id);
        this.cacheType = cacheType;
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
}
