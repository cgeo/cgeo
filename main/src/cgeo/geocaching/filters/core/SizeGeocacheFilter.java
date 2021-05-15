package cgeo.geocaching.filters.core;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.models.Geocache;

public class SizeGeocacheFilter extends ValueGroupGeocacheFilter<CacheSize> {


    @Override
    public CacheSize getValue(final Geocache cache) {
        return CacheSize.UNKNOWN.equals(cache.getSize()) ? null : cache.getSize();
    }

    @Override
    public CacheSize valueFromString(final String stringValue) {
        return CacheSize.valueOf(stringValue);
    }

    @Override
    public String valueToString(final CacheSize value) {
        return value.name();
    }

    @Override
    public String getSqlColumnName() {
        return "size";
    }

    @Override
    public String valueToSqlValue(final CacheSize value) {
        return value.id;
    }

}
