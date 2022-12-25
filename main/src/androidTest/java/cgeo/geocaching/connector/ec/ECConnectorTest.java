package cgeo.geocaching.connector.ec;

import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ECConnectorTest {

    @Test
    public void testCanHandle() throws Exception {
        assertThat(ECConnector.getInstance().canHandle("EC380")).isTrue();
        assertThat(ECConnector.getInstance().canHandle("GC380")).isFalse();
        assertThat(ECConnector.getInstance().canHandle("GCEC380")).overridingErrorMessage("faked EC codes must be handled during the import, otherwise GCECxxxx codes belong to 2 connectors").isFalse();
    }

    @Test
    public void testGetPossibleLogTypes() throws Exception {
        final List<LogType> possibleLogTypes = ECConnector.getInstance().getPossibleLogTypes(createCache());
        assertThat(possibleLogTypes).isNotNull();
        assertThat(possibleLogTypes).isNotEmpty();
        assertThat(possibleLogTypes).contains(LogType.FOUND_IT);
    }

    private static Geocache createCache() {
        final Geocache geocache = new Geocache();
        geocache.setType(CacheType.TRADITIONAL);
        geocache.setGeocode("EC727");
        return geocache;
    }

    @Test
    public void testGetGeocodeFromUrl() throws Exception {
        assertThat(ECConnector.getInstance().getGeocodeFromUrl("http://extremcaching.com/index.php/output-2/738")).isEqualTo("EC738");
    }

    @Test
    public void testHandledGeocodes() {
        final Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(ECConnector.getInstance().handledGeocodes(geocodes)).containsOnly("EC1234", "EC5678");
    }
}
