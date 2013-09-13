package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.CgeoApplication;

import java.util.Collections;
import java.util.List;

class ModifiedFilter extends AbstractFilter implements IFilterFactory {

    public ModifiedFilter() {
        super(CgeoApplication.getInstance().getString(R.string.caches_filter_modified));
    }

    @Override
    public boolean accepts(final Geocache cache) {
        // modified on GC
        return cache.hasUserModifiedCoords() || cache.hasFinalDefined();
    }

    @Override
    public List<ModifiedFilter> getFilters() {
        return Collections.singletonList(this);
    }
}
