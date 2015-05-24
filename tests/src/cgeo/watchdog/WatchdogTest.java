package cgeo.watchdog;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.oc.OCApiConnector;

public class WatchdogTest extends CGeoTestCase {

    public static void testOpenCachingDE() {
        downloadOpenCaching("OC1234");
    }

    public static void testOpenCachingUS() {
        downloadOpenCaching("OU0331");
    }

    private static void downloadOpenCaching(final String geocode) {
        final OCApiConnector connector = (OCApiConnector) ConnectorFactory.getConnector(geocode);
        final SearchResult searchResult = connector.searchByGeocode(geocode, null, null);
        assertThat(searchResult.getCount()).overridingErrorMessage("Failed to download from " + connector.getName()).isNotNull();
    }

}
