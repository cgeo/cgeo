package cgeo.geocaching.connector.ec;

import org.apache.commons.lang3.Strings;

final class ECApi {

    private ECApi() {
        // utility class with static methods
    }

    static String getIdFromGeocode(final String geocode) {
        return Strings.CI.removeStart(geocode, "EC");
    }
}
