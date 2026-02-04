package cgeo.geocaching.filters.core;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

<<<<<<< HEAD
=======
import static cgeo.geocaching.enumerations.CacheType.BLOCK_PARTY;
import static cgeo.geocaching.enumerations.CacheType.COMMUN_CELEBRATION;
import static cgeo.geocaching.enumerations.CacheType.GCHQ;
import static cgeo.geocaching.enumerations.CacheType.GCHQ_CELEBRATION;
import static cgeo.geocaching.enumerations.CacheType.GIGA_EVENT;
import static cgeo.geocaching.enumerations.CacheType.GPS_EXHIBIT;
import static cgeo.geocaching.enumerations.CacheType.LOCATIONLESS;
import static cgeo.geocaching.enumerations.CacheType.MEGA_EVENT;
import static cgeo.geocaching.enumerations.CacheType.PROJECT_APE;
import static cgeo.geocaching.enumerations.CacheType.UNKNOWN;
import static cgeo.geocaching.enumerations.CacheType.VIRTUAL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
>>>>>>> 0d90e2570 (fix: Replace wildcard static import with explicit imports per code guidelines)
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
