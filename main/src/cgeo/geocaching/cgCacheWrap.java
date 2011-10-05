package cgeo.geocaching;

import cgeo.geocaching.enumerations.StatusCode;

import java.util.ArrayList;
import java.util.List;

/**
 * List of caches
 */
public class cgCacheWrap {
    public StatusCode error = null;
    public String url = "";
    public String[] viewstates = null;
    public int totalCnt = 0;
    public List<cgCache> cacheList = new ArrayList<cgCache>();
}