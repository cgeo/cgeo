package cgeo.geocaching.connector.al;

import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ALConnectorTest {

    @Test
    public void testCanHandle() throws Exception {
        assertThat(ALConnector.getInstance().canHandle("AL380")).isTrue();
        assertThat(ALConnector.getInstance().canHandle("ALc47d3f39-ac94-4c03-82bc-c6f3a611439f")).isTrue();
        assertThat(ALConnector.getInstance().canHandle("ALC47D3F39-AC94-4C03-82BC-C6F3A611439F")).isTrue();
        assertThat(ALConnector.getInstance().canHandle("GC380")).isFalse();
        assertThat(ALConnector.getInstance().canHandle("GCAL380")).overridingErrorMessage("faked AL codes must be handled during the import, otherwise GCALxxxx codes belong to 2 connectors").isFalse();
    }

    @Test
    public void testGetPossibleLogTypes() throws Exception {
        final List<LogType> possibleLogTypes = ALConnector.getInstance().getPossibleLogTypes(createCache());
        assertThat(possibleLogTypes).isNotNull();
        assertThat(possibleLogTypes).isNotEmpty();
        assertThat(possibleLogTypes).contains(LogType.FOUND_IT);
    }

    private static Geocache createCache() {
        final Geocache geocache = new Geocache();
        geocache.setType(CacheType.ADVLAB);
        geocache.setGeocode("AL727");
        return geocache;
    }

    @Test
    public void testGetGeocodeFromUrl() throws Exception {
        assertThat(ALConnector.getInstance().getGeocodeFromUrl("https://adventurelab.page.link/738")).isEqualTo("AL738");
        assertThat(ALConnector.getInstance().getGeocodeFromUrl("https://labs.geocaching.com/goto/c47d3f39-ac94-4c03-82bc-c6f3a611439f")).isEqualTo("AL738");
        assertThat(ALConnector.getInstance().getGeocodeFromUrl("ALhttps://labs.geocaching.com/goto/c47d3f39-ac94-4c03-82bc-c6f3a611439f")).isEqualTo("ALc47d3f39-ac94-4c03-82bc-c6f3a611439f");
        assertThat(ALConnector.getInstance().getGeocodeFromUrl("ALHTTPS://LABS.GEOCACHING.COM/GOTO/C47D3F39-AC94-4C03-82BC-C6F3A611439F")).isEqualTo("ALC47D3F39-AC94-4C03-82BC-C6F3A611439F");
    }

    @Test
    public void testHandledGeocodes() {
        final Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(ALConnector.getInstance().handledGeocodes(geocodes)).containsOnly("AL1234", "AL5678");
    }
}
