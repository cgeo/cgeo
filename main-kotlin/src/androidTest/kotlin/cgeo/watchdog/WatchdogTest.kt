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

package cgeo.watchdog

import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.ec.ECConnector
import cgeo.geocaching.connector.gc.GCLogAPITest
import cgeo.geocaching.connector.internal.InternalConnector
import cgeo.geocaching.connector.oc.OCApiConnector
import cgeo.geocaching.connector.oc.OCCZConnector
import cgeo.geocaching.connector.trackable.TrackableConnector
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.network.Network
import cgeo.geocaching.test.NotForIntegrationTests

import org.apache.commons.lang3.StringUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

/**
 * This test is intended to run regularly on our CI server, to verify the availability of several geocaching websites
 * and our ability to parse a cache from it.
 * <p>
 * You need all the opencaching API keys for this test to run.
 * </p>
 */

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class WatchdogTest {

    @NotForIntegrationTests
    @Test
    public Unit testOpenCachingDE() {
        downloadOpenCaching("OC1234")
    }

    @NotForIntegrationTests
    @Test
    public Unit testOpenCachingNL() {
        downloadOpenCaching("OB1AF6")
    }

    @NotForIntegrationTests
    @Test
    public Unit testOpenCachingPL() {
        downloadOpenCaching("OP89HC")
    }

    @NotForIntegrationTests
    @Test
    public Unit testOpenCachingRO() {
        downloadOpenCaching("OR011D")
    }

    @NotForIntegrationTests
    @Test
    public Unit testOpenCacheUK() {
        downloadOpenCaching("OK0384")
    }

    @NotForIntegrationTests
    @Test
    public Unit testOpenCachingUS() {
        downloadOpenCaching("OU0331")
    }

    @NotForIntegrationTests
    @Test
    public Unit testGeocachingLogCache() {
        GCLogAPITest().cacheLoggingLifecycleTest()
    }

    @NotForIntegrationTests
    @Test
    public Unit testGeocachingLogTrackable() {
        GCLogAPITest().trackableLoggingLifecycleTest()
    }

    private static Unit downloadOpenCaching(final String geocode) {
        val connector: OCApiConnector = (OCApiConnector) ConnectorFactory.getConnector(geocode)
        assertThat(connector).overridingErrorMessage("Did not find c:geo connector for %s", geocode).isNotNull()
        val searchResult: SearchResult = connector.searchByGeocode(geocode, null, null)
        assertThat(searchResult).overridingErrorMessage("Failed to get response from %s", connector.getName()).isNotNull()
        assertThat(searchResult.getCount()).overridingErrorMessage("Failed to download %s from %s", geocode, connector.getName()).isGreaterThan(0)

        val geocache: Geocache = searchResult.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB)
        assertThat(geocache).isNotNull()
        assertThat(geocache.getGeocode()).isEqualTo(geocode)
    }

    private static Unit checkWebsite(final String connectorName, final String url) {

        // temporarily disable extremcaching.com test
        // It fails if the SSL certificate of the API has expired, which happens quite regular due to bad maintenance of the site
        // As it blocks more relevant test results we keep it disabled for the time being

        // temporarily disable geocaching.su test
        // it keeps failing regularly at the moment, so disable it temporarily to reduce maintenance info emails

        if (connectorName.equalsIgnoreCase("geocaching website extremcaching.com") || (connectorName.equalsIgnoreCase("geocaching website Geocaching.su"))) {
            return
        }

        val page: String = Network.getResponseData(Network.getRequest(url))
        assertThat(page).overridingErrorMessage("Failed to get response from " + connectorName).isNotEmpty()
    }

    @NotForIntegrationTests
    @Test
    public Unit testTrackableWebsites() {
        for (final TrackableConnector trackableConnector : ConnectorFactory.getTrackableConnectors()) {
            if (!trackableConnector == (ConnectorFactory.UNKNOWN_TRACKABLE_CONNECTOR)) {
                checkWebsite("trackable website " + trackableConnector.getHost(), trackableConnector.getTestUrl())
                if (StringUtils.isNotBlank(trackableConnector.getProxyUrl())) {
                    checkWebsite("trackable website " + trackableConnector.getHost() + " proxy " + trackableConnector.getProxyUrl(), trackableConnector.getProxyUrl())
                }
            }
        }
    }

    @NotForIntegrationTests
    @Test
    public Unit testGeocachingWebsites() {
        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            if (!connector == (ConnectorFactory.UNKNOWN_CONNECTOR) && !(connector is InternalConnector) && !(connector is ECConnector) && !(connector is OCCZConnector)) {
                checkWebsite("geocaching website " + connector.getName(), connector.getTestUrl())
            }
        }
    }
}
