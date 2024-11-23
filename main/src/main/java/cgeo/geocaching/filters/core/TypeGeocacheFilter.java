package cgeo.geocaching.filters.core;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

import org.apache.commons.lang3.EnumUtils;

public class TypeGeocacheFilter extends ValueGroupGeocacheFilter<CacheType, CacheType> {

    public TypeGeocacheFilter() {
        //gc.com groups in their search their cache types as follows:
        //* "Unknown" also includes: Unknown, GCHQ, PROJECT_APE
        //* "Celebration Event" also includes: mega,giga, gps_exhibit,commun_celeb, gchq_celeb, block_party
        //-> unlike gc.com, currently CITO is an OWN search box below (to make number even)
        addDisplayValues(CacheType.OTHER, CacheType.UNKNOWN, CacheType.GCHQ, CacheType.PROJECT_APE);
        addDisplayValues(CacheType.SPECIALEVENT, CacheType.MEGA_EVENT, CacheType.GIGA_EVENT, CacheType.COMMUN_CELEBRATION,
                CacheType.GCHQ_CELEBRATION, CacheType.GPS_EXHIBIT, CacheType.BLOCK_PARTY);
        addDisplayValues(CacheType.VIRTUAL, CacheType.VIRTUAL, CacheType.LOCATIONLESS);
    }

    @Override
    public CacheType getRawCacheValue(final Geocache cache) {
        return cache.getType();
    }

    @Override
    public CacheType valueFromString(final String stringValue) {
        return EnumUtils.getEnum(CacheType.class, stringValue);
    }

    @Override
    public String valueToString(final CacheType value) {
        return value.name();
    }

    @Override
    public String valueToUserDisplayableValue(final CacheType value) {
        return value.getShortL10n();
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
