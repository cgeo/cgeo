package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

public class TrackablesFilter extends AbstractFilter {
    public TrackablesFilter(String name) {
        super(name);
    }

    @Override
    public boolean accepts(cgCache cache) {
        return cache.hasTrackables();
    }
}
