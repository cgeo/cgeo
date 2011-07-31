package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

public class cgFilterBySize extends cgFilter {
	private String size;

	public cgFilterBySize(String size){
		this.size = size;
	}

	@Override
	boolean applyFilter(cgCache cache) {
		return size.equals(cache.size);
	}

}
