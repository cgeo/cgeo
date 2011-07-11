package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

public class cgFilterByTrackables extends cgFilter {

	@Override
	boolean applyFilter(cgCache cache) {
		return cache.hasTrackables();
	}

}
