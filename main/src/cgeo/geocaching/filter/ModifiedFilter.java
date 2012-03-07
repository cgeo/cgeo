package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

public class ModifiedFilter extends AbstractFilter {

    public ModifiedFilter(String name) {
        super(name);
    }

    @Override
    public boolean accepts(final cgCache cache) {
        // modified on GC
        return cache.hasUserModifiedCoords() || cache.hasFinalDefined();
    }
}
