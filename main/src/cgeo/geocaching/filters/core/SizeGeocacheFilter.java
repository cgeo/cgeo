package cgeo.geocaching.filters.core;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.models.Geocache;

public class SizeGeocacheFilter extends OneOfManyGeocacheFilter<CacheSize> {


    @Override
    public CacheSize getValue(final Geocache cache) {
        return cache.getSize();
    }

    @Override
    public CacheSize valueFromString(final String stringValue) {
        return CacheSize.valueOf(stringValue);
    }

    @Override
    public String valueToString(final CacheSize value) {
        return value.name();
    }
}
