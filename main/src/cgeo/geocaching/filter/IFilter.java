package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;

import java.util.List;

public interface IFilter {

    String getName();

    /**
     * @param cache
     * @return true if the filter accepts the cache, false otherwise
     */
    boolean accepts(final Geocache cache);

    void filter(final List<Geocache> list);
}