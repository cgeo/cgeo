package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

public class cgFilterByTrackables extends cgFilter {
    public cgFilterByTrackables(String name) {
        super(name);
    }

    @Override
    boolean applyFilter(cgCache cache) {
        return cache.hasTrackables();
    }
}
