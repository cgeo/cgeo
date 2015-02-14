package cgeo.geocaching.connector.ec;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;

import java.util.List;

import junit.framework.TestCase;

public class ECConnectorTest extends TestCase {

    public static void testCanHandle() throws Exception {
        assertThat(ECConnector.getInstance().canHandle("EC380")).isTrue();
        assertThat(ECConnector.getInstance().canHandle("GC380")).isFalse();
        assertThat(ECConnector.getInstance().canHandle("GCEC380")).overridingErrorMessage("faked EC codes must be handled during the import, otherwise GCECxxxx codes belong to 2 connectors").isFalse();
    }

    public static void testGetPossibleLogTypes() throws Exception {
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

    public static void testGetGeocodeFromUrl() throws Exception {
        assertThat(ECConnector.getInstance().getGeocodeFromUrl("http://extremcaching.com/index.php/output-2/738")).isEqualTo("EC738");
    }

}
