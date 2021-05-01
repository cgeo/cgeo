package cgeo.geocaching.filters.core;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

public class TypeGeocacheFilter extends ValueGroupGeocacheFilter<CacheType> {

    @Override
    public CacheType getValue(final Geocache cache) {
        return cache.getType();
    }

    @Override
    public CacheType valueFromString(final String stringValue) {
        return CacheType.valueOf(stringValue);
    }

    @Override
    public String valueToString(final CacheType value) {
        return value.name();
    }

    @Override
    public String getSqlColumnName() {
        return "type";
    }

    @Override
    public String valueToSqlValue(final CacheType value) {
        return value.id;
    }
}
