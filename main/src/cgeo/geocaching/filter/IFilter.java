package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;

import java.util.List;

public interface IFilter {

    public abstract String getName();

    /**
     * @param cache
     * @return true if the filter accepts the cache, false otherwise
     */
    public abstract boolean accepts(final Geocache cache);

    public void filter(final List<Geocache> list);
}