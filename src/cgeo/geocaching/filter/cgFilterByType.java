package cgeo.geocaching.filter;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;

public class cgFilterByType extends cgFilter {
	public cgFilterByType(String type){
		super(type);
	}

	@Override
	boolean applyFilter(cgCache cache) {
		return name.equals(cache.name);
	}
	
	@Override
	public String getFilterName() {
	    return cgBase.cacheTypesInv.get(name);
	}
}
