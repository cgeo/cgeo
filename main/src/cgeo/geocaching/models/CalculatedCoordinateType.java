package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

public enum CalculatedCoordinateType {

    PLAIN(R.string.waypoint_coordinate_formats_plain),
    DEGREE("DDD.DDDDD°"),
    DEGREE_MINUTE("DDD°MM.MMM'"),
    DEGREE_MINUTE_SEC("DDD°MM'SS.SSS\"");

    private String userDisplay;

    CalculatedCoordinateType(final int resId) {
        userDisplay = LocalizationUtils.getString(resId);
    }

    CalculatedCoordinateType(final String resString) {
        userDisplay = resString;
    }


    public String toUserDisplayableString() {
           return userDisplay;
    }


}
