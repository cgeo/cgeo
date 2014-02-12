package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

class TrackablesFilter extends AbstractFilter {
    public TrackablesFilter() {
        super(R.string.caches_filter_track);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.hasTrackables();
    }

}
