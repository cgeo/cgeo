package cgeo.geocaching.filters.core;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

import org.apache.commons.lang3.EnumUtils;

public class TypeGeocacheFilter extends ValueGroupGeocacheFilter<CacheType, CacheType> {

    public TypeGeocacheFilter() {
        //gc.com groups in their search their cache types as follows:
        //* "Unknown" is displayed as "Others" and includes: Unknown, GCHQ, PROJECT_APE
        //* "Celebration Event" is displayed as "Specials" and includes: mega,giga, gps_exhibit,commun_celeb, gchq_celeb, block_party
        addDisplayValues(CacheType.UNKNOWN, CacheType.UNKNOWN, CacheType.GCHQ, CacheType.PROJECT_APE);
        addDisplayValues(CacheType.COMMUN_CELEBRATION, CacheType.MEGA_EVENT, CacheType.GIGA_EVENT, CacheType.COMMUN_CELEBRATION,
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
        return valueDisplayTextGetter(value);
    }

    @Override
    public String getSqlColumnName() {
        return "type";
    }

    @Override
    public String valueToSqlValue(final CacheType value) {
        return value.id;
    }

    public static String valueDisplayTextGetter(final CacheType value) {
        if (CacheType.UNKNOWN == value) {
            return CgeoApplication.getInstance().getString(R.string.other);
        } else if (CacheType.COMMUN_CELEBRATION == value) {
            return CgeoApplication.getInstance().getString(R.string.event_special);
        }
        return value.getShortL10n();
    }

}
