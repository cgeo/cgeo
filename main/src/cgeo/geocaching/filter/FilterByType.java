package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;

public class FilterByType extends AbstractFilter {
    private final CacheType cacheType;

    public FilterByType(final CacheType cacheType) {
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
}
