package cgeo.geocaching.connector.ec;

import org.apache.commons.lang3.StringUtils;

final class ECApi {

    private ECApi() {
        // utility class with static methods
    }

    static String getIdFromGeocode(final String geocode) {
        return StringUtils.removeStartIgnoreCase(geocode, "EC");
    }

}
