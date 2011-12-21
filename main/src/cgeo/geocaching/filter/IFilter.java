package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

import java.util.List;

public interface IFilter {

    public abstract String getName();

    /**
     * @param cache
     * @return true if the filter accepts the cache, false otherwise
     */
    public abstract boolean accepts(final cgCache cache);

    public void filter(final List<cgCache> list);
}