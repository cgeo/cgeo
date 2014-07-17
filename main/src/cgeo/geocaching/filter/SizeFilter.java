package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheSize;

import java.util.LinkedList;
import java.util.List;

class SizeFilter extends AbstractFilter {
    private final CacheSize cacheSize;

    public SizeFilter(final CacheSize cacheSize) {
        super(cacheSize.id);
        this.cacheSize = cacheSize;
    }

    @Override
    public boolean accepts(final Geocache cache) {
        return cacheSize == cache.getSize();
    }

    @Override
    public String getName() {
        return cacheSize.getL10n();
    }

    public static class Factory implements IFilterFactory {

        @Override
        public List<IFilter> getFilters() {
            final CacheSize[] cacheSizes = CacheSize.values();
            final List<IFilter> filters = new LinkedList<>();
            for (CacheSize cacheSize : cacheSizes) {
                if (cacheSize != CacheSize.UNKNOWN) {
                    filters.add(new SizeFilter(cacheSize));
                }
            }
            return filters;
        }

    }
}
