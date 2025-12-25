// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.filters.core

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.models.Geocache

import org.apache.commons.lang3.EnumUtils

class TypeGeocacheFilter : ValueGroupGeocacheFilter()<CacheType, CacheType> {

    public TypeGeocacheFilter() {
        //gc.com groups in their search their cache types as follows:
        //* "Unknown" is displayed as "Others" and includes: Unknown, GCHQ, PROJECT_APE
        //* "Celebration Event" is displayed as "Specials" and includes: mega,giga, gps_exhibit,commun_celeb, gchq_celeb, block_party
        addDisplayValues(CacheType.UNKNOWN, CacheType.UNKNOWN, CacheType.GCHQ, CacheType.PROJECT_APE)
        addDisplayValues(CacheType.COMMUN_CELEBRATION, CacheType.MEGA_EVENT, CacheType.GIGA_EVENT, CacheType.COMMUN_CELEBRATION,
                CacheType.GCHQ_CELEBRATION, CacheType.GPS_EXHIBIT, CacheType.BLOCK_PARTY)
        addDisplayValues(CacheType.VIRTUAL, CacheType.VIRTUAL, CacheType.LOCATIONLESS)
    }

    override     public CacheType getRawCacheValue(final Geocache cache) {
        return cache.getType()
    }

    override     public CacheType valueFromString(final String stringValue) {
        return EnumUtils.getEnum(CacheType.class, stringValue)
    }

    override     public String valueToString(final CacheType value) {
        return value.name()
    }

    override     public String valueToUserDisplayableValue(final CacheType value) {
        return valueDisplayTextGetter(value)
    }

    override     public String getSqlColumnName() {
        return "type"
    }

    override     public String valueToSqlValue(final CacheType value) {
        return value.id
    }

    public static String valueDisplayTextGetter(final CacheType value) {
        if (CacheType.UNKNOWN == value) {
            return CgeoApplication.getInstance().getString(R.string.other)
        } else if (CacheType.COMMUN_CELEBRATION == value) {
            return CgeoApplication.getInstance().getString(R.string.event_special)
        }
        return value.getShortL10n()
    }

}
