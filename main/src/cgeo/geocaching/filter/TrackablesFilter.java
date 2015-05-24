package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

class TrackablesFilter extends AbstractFilter {
    private static final long serialVersionUID = 2280421779859292315L;

    public TrackablesFilter() {
        super(R.string.caches_filter_track);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.hasTrackables();
    }

}
