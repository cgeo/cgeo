package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheSize;

public class cgFilterBySize extends cgFilter {
	private CacheSize size;

	public cgFilterBySize(CacheSize size){
		this.size = size;
	}

	@Override
	boolean applyFilter(cgCache cache) {
		return size == cache.size;
	}

}
