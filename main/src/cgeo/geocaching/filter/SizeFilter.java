package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheSize;

import java.util.ArrayList;

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
        ArrayList<SizeFilter> filters = new ArrayList<SizeFilter>();
        for (CacheSize cacheSize : cacheSizes) {
            if (cacheSize != CacheSize.UNKNOWN) {
                filters.add(new SizeFilter(cacheSize));
            }
        }
        return filters.toArray(new SizeFilter[filters.size()]);
    }
}
