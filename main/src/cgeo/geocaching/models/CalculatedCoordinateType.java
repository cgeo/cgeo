package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

public enum CalculatedCoordinateType {


    PLAIN("P", R.string.waypoint_coordinate_formats_plain),
    DEGREE("DDD", "DDD.DDDDD°"),
    DEGREE_MINUTE("DMM", "DDD°MM.MMM'"),
    DEGREE_MINUTE_SEC("DMS", "DDD°MM'SS.SSS\"");

    private static final Map<String, CalculatedCoordinateType> SHORT_NAMES = new HashMap<>();

    static {
        for (CalculatedCoordinateType t : CalculatedCoordinateType.values()) {
            SHORT_NAMES.put(t.shortName, t);
        }
    }

    private String shortName;
    private String userDisplay;

    CalculatedCoordinateType(final String shortName, final int resId) {
        init(shortName);
        userDisplay = LocalizationUtils.getString(resId);
    }

    CalculatedCoordinateType(final String shortName, final String resString) {
        init(shortName);
        userDisplay = resString;
    }

    private void init(final String shortName) {
        this.shortName = shortName.toUpperCase(Locale.US);
    }


    public String toUserDisplayableString() {
        return userDisplay;
    }

    public String shortName() {
        return shortName;
    }

    @NonNull
    public static CalculatedCoordinateType fromName(final String name) {
        if (StringUtils.isBlank(name)) {
            return PLAIN;
        }
        final String nameToUse = name.toUpperCase(Locale.US);
        if (SHORT_NAMES.containsKey(nameToUse)) {
            return Objects.requireNonNull(SHORT_NAMES.get(nameToUse));
        }
        return EnumUtils.getEnum(CalculatedCoordinateType.class, nameToUse, PLAIN);
    }


}
