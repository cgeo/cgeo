package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Tier;

public class TierGeocacheFilter extends ValueGroupGeocacheFilter<Tier, Tier> {


    @Override
    public Tier getRawCacheValue(final Geocache cache) {
        return cache.getTier() == null ? Tier.NONE : cache.getTier();
    }

    @Override
    public Tier valueFromString(final String stringValue) {
        return Tier.getByName(stringValue);
    }

    @Override
    public String valueToUserDisplayableValue(final Tier value) {
        return value.getI18nText();
    }


    @Override
    public String valueToString(final Tier value) {
        return value.getRaw();
    }

    @Override
    public String getSqlColumnName() {
        return "tier";
    }

    @Override
    public Tier getSqlNullValue() {
        return Tier.NONE;
    }

    @Override
    public String valueToSqlValue(final Tier value) {
        return value.getRaw();
    }

}
