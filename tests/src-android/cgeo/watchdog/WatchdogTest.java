package cgeo.watchdog;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.oc.OCApiConnector;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;

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

    private static void checkWebsite(final String connectorName, final String host) {
        final String url = "http://" + host + "/";
        final String page = Network.getResponseData(Network.getRequest(url));
        assertThat(page).overridingErrorMessage("Failed to get response from " + connectorName).isNotEmpty();
    }

    public static void testTrackableWebsites() {
        for (final TrackableConnector trackableConnector : ConnectorFactory.getTrackableConnectors()) {
            if (trackableConnector != ConnectorFactory.UNKNOWN_TRACKABLE_CONNECTOR) {
                checkWebsite("trackable website " + trackableConnector.getHost(), trackableConnector.getHost());
            }
        }
    }

    public static void testGeocachingWebsites() {
        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            if (connector != ConnectorFactory.UNKNOWN_CONNECTOR) {
                checkWebsite("geocaching website " + connector.getName(), connector.getHost());
            }
        }
    }

}
