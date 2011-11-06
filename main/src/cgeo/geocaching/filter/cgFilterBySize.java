package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheSize;

public class cgFilterBySize extends cgFilter {
    private final CacheSize cacheSize;

    public cgFilterBySize(CacheSize cacheSize) {
        super(cacheSize.id);
        this.cacheSize = cacheSize;
    }

    @Override
    boolean applyFilter(cgCache cache) {
        return this.cacheSize == cache.getSize();
    }

    @Override
    public String getFilterName() {
        return this.cacheSize.getL10n();
    }
}
