package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;

import java.util.Collections;
import java.util.List;

class TrackablesFilter extends AbstractFilter implements IFilterFactory {
    public TrackablesFilter() {
        super(cgeoapplication.getInstance().getString(R.string.caches_filter_track));
    }

    @Override
    public boolean accepts(Geocache cache) {
        return cache.hasTrackables();
    }

    @Override
    public List<TrackablesFilter> getFilters() {
        return Collections.singletonList(this);
    }

}
