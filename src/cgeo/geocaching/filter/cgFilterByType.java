package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

public class cgFilterByType extends cgFilter {	
	private String type;

	public cgFilterByType(String type){
		this.type = type;
	}
	
	@Override
	boolean applyFilter(cgCache cache) {
		return cache.type.equals(type);
	}

}
