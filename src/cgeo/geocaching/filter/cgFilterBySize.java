package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

public class cgFilterBySize extends cgFilter {
	public cgFilterBySize(String size) {
        super(size);
    }

    @Override
	boolean applyFilter(cgCache cache) {
		return name.equals(cache.size);
	}
}
