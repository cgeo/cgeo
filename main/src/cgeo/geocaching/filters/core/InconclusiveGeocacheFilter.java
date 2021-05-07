package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class InconclusiveGeocacheFilter extends AndGeocacheFilter {

    @Override
    public String getId() {
        return "inconclusive";
    }

    @Override
    public Boolean filter(final Geocache cache) {
        final Boolean superResult = super.filter(cache);
        return superResult == null ? true : superResult;
    }

}
