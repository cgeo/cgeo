package cgeo.watchdog;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.oc.OCApiConnector;
import cgeo.geocaching.enumerations.LoadFlags;

/**
 * This test is intended to run regularly on our CI server, to verify the availability of several geocaching websites
 * and our ability to parse a cache from it.
 * <p>
 * You need all the opencaching API keys for this test to run.
 * </p>
 *
 */
public class WatchdogTest extends CGeoTestCase {

    public static void testOpenCachingDE() {
        downloadOpenCaching("OC1234");
    }

    public static void testOpenCachingNL() {
        downloadOpenCaching("OB1AF6");
    }

    public static void testOpenCachingPL() {
        downloadOpenCaching("OP89HC");
    }

    public static void testOpenCachingRO() {
        downloadOpenCaching("OR011D");
    }

    public static void testOpenCachingUK() {
        downloadOpenCaching("OK0345");
    }

    public static void testOpenCachingUS() {
        downloadOpenCaching("OU0331");
    }

    private static void downloadOpenCaching(final String geocode) {
        final OCApiConnector connector = (OCApiConnector) ConnectorFactory.getConnector(geocode);
        assertThat(connector).overridingErrorMessage("Did not find c:geo connector for %s", geocode).isNotNull();
        final SearchResult searchResult = connector.searchByGeocode(geocode, null, null);
        assertThat(searchResult).overridingErrorMessage("Failed to get response from %s", connector.getName()).isNotNull();
        assertThat(searchResult.getCount()).overridingErrorMessage("Failed to download %s from %s", geocode, connector.getName()).isGreaterThan(0);

        final Geocache geocache = searchResult.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
        assertThat(geocache).isNotNull();
        assert geocache != null; // Eclipse null analysis weakness
        assertThat(geocache.getGeocode()).isEqualTo(geocode);
    }

}
