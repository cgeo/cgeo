package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;

class ModifiedFilter extends AbstractFilter implements IFilterFactory {

    public ModifiedFilter() {
        super(cgeoapplication.getInstance().getString(R.string.caches_filter_modified));
    }

    @Override
    public boolean accepts(final Geocache cache) {
        // modified on GC
        return cache.hasUserModifiedCoords() || cache.hasFinalDefined();
    }

    @Override
    public IFilter[] getFilters() {
        return new IFilter[] { this };
    }
}
