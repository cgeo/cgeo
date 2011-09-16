package cgeo.geocaching.filter;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheSize;

public class cgFilterBySize extends cgFilter {
    private final CacheSize size;

    public cgFilterBySize(CacheSize size) {
        super(size.id);
        this.size = size;
    }

    @Override
    boolean applyFilter(cgCache cache) {
        return this.size == cache.size;
    }

    @Override
    public String getFilterName() {
        return cgBase.cacheSizesInv.get(this.size);
    }
}
