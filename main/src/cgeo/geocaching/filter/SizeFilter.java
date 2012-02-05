package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheSize;

public class SizeFilter extends AbstractFilter {
    private final CacheSize cacheSize;

    public SizeFilter(CacheSize cacheSize) {
        super(cacheSize.id);
        this.cacheSize = cacheSize;
    }

    @Override
    public boolean accepts(cgCache cache) {
        return cacheSize == cache.getSize();
    }

    @Override
    public String getName() {
        return cacheSize.getL10n();
    }

    public static AbstractFilter[] getAllFilters() {
        final CacheSize[] cacheSizes = CacheSize.values();
        SizeFilter[] filters = new SizeFilter[cacheSizes.length];
        for (int i = 0; i < cacheSizes.length; i++) {
            filters[i] = new SizeFilter(cacheSizes[i]);
        }
        return filters;
    }
}
