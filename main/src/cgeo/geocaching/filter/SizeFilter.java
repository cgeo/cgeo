package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheSize;

import org.eclipse.jdt.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

class SizeFilter extends AbstractFilter {
    private static final long serialVersionUID = -2338070360641435415L;
    private final CacheSize cacheSize;

    public SizeFilter(@NonNull final CacheSize cacheSize) {
        super(cacheSize.id);
        this.cacheSize = cacheSize;
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

        private static final long serialVersionUID = 5375423577550361483L;

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

}
