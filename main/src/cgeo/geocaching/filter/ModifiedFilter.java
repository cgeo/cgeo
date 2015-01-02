package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

import java.util.Collections;
import java.util.List;

class ModifiedFilter extends AbstractFilter implements IFilterFactory {

    public ModifiedFilter() {
        super(CgeoApplication.getInstance().getString(R.string.caches_filter_modified));
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        // modified on GC
        return cache.hasUserModifiedCoords() || cache.hasFinalDefined();
    }

    @Override
    @NonNull
    public List<ModifiedFilter> getFilters() {
        return Collections.singletonList(this);
    }
}
