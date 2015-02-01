package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

import java.util.Collections;
import java.util.List;

class TrackablesFilter extends AbstractFilter implements IFilterFactory {
    public TrackablesFilter() {
        super(R.string.caches_filter_track);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.hasTrackables();
    }

    @Override
    @NonNull
    public List<TrackablesFilter> getFilters() {
        return Collections.singletonList(this);
    }

}
