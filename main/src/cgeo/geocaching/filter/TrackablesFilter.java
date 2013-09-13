package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import java.util.Collections;
import java.util.List;

class TrackablesFilter extends AbstractFilter implements IFilterFactory {
    public TrackablesFilter() {
        super(CgeoApplication.getInstance().getString(R.string.caches_filter_track));
    }

    @Override
    public boolean accepts(final Geocache cache) {
        return cache.hasTrackables();
    }

    @Override
    public List<TrackablesFilter> getFilters() {
        return Collections.singletonList(this);
    }

}
