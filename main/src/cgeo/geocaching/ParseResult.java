package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Search result including list of caches
 */
public class ParseResult extends SearchResult {

    public List<cgCache> cacheList = new ArrayList<cgCache>();

    public ParseResult() {
        super();
    }

    public ParseResult(SearchResult searchResult) {
        super(searchResult);
    }

    public ParseResult(ParseResult parseResult) {
        super(parseResult);
        cacheList.addAll(parseResult.cacheList);
    }

    public ParseResult(final Set<String> geocodes) {
        super(geocodes);
        cgeoapplication app = cgeoapplication.getInstance();
        for (String geocode : geocodes) {
            cacheList.add(app.getCacheByGeocode(geocode, LoadFlags.LOADALL));
        }
    }

    public static ParseResult filterParseResults(final ParseResult parseResult, final boolean excludeDisabled, final boolean excludeMine, final CacheType cacheType) {

        ParseResult result = new ParseResult(parseResult);
        result.cacheList.clear();
        result.geocodes.clear();

        if (parseResult != null) {
            for (final cgCache cache : parseResult.cacheList) {
                // Is there any reason to exclude the cache from the list?
                final boolean excludeCache = (excludeDisabled && cache.isDisabled()) ||
                        (excludeMine && (cache.isOwn() || cache.isFound())) ||
                        (cacheType != CacheType.ALL && cacheType != cache.getType());
                if (!excludeCache) {
                    if (result.addGeocode(cache.getGeocode())) {
                        result.cacheList.add(cache);
                    }
                }
            }
        }

        return result;
    }
}