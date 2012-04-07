package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;

import java.util.ArrayList;

public class TypeFilter extends AbstractFilter {
    private final CacheType cacheType;

    public TypeFilter(final CacheType cacheType) {
        super(cacheType.id);
        this.cacheType = cacheType;
    }

    @Override
    public boolean accepts(final cgCache cache) {
        return cacheType == cache.getType();
    }

    @Override
    public String getName() {
        return cacheType.getL10n();
    }

    public static IFilter[] getAllFilters() {
        final CacheType[] types = CacheType.values();
        final ArrayList<IFilter> filters = new ArrayList<IFilter>(types.length);
        for (CacheType cacheType : types) {
            if (cacheType != CacheType.ALL) {
                filters.add(new TypeFilter(cacheType));
            }
        }
        return filters.toArray(new TypeFilter[filters.size()]);
    }
}
