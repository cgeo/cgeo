package cgeo.geocaching;

import java.util.ArrayList;

/**
 * List of caches
 */
public class cgCacheWrap {
    public String error = null;
	public String url = "";
	public String[] viewstates = null;
	public int totalCnt = 0;
	public ArrayList<cgCache> cacheList = new ArrayList<cgCache>();
}