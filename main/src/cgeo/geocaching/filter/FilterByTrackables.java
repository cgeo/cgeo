package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

public class FilterByTrackables extends AbstractFilter {
    public FilterByTrackables(String name) {
        super(name);
    }

    @Override
    public boolean accepts(cgCache cache) {
        return cache.hasTrackables();
    }
}
