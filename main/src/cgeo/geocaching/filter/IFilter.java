package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;

import org.eclipse.jdt.annotation.NonNull;

import java.util.List;

public interface IFilter {

    String getName();

    /**
     * @param cache
     * @return true if the filter accepts the cache, false otherwise
     */
    boolean accepts(@NonNull final Geocache cache);

    void filter(@NonNull final List<Geocache> list);
}