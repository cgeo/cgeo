package cgeo.geocaching.connector.ec;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;

import java.util.List;

import junit.framework.TestCase;

public class ECConnectorTest extends TestCase {

    public static void testCanHandle() throws Exception {
        assertTrue(ECConnector.getInstance().canHandle("EC380"));
        assertFalse(ECConnector.getInstance().canHandle("GC380"));
        assertFalse("faked EC codes must be handled during the import, otherwise GCECxxxx codes belong to 2 connectors", ECConnector.getInstance().canHandle("GCEC380"));
    }

    public static void testGetPossibleLogTypes() throws Exception {
        final List<LogType> possibleLogTypes = ECConnector.getInstance().getPossibleLogTypes(createCache());
        assertNotNull(possibleLogTypes);
        assertFalse(possibleLogTypes.isEmpty());
        assertTrue(possibleLogTypes.contains(LogType.FOUND_IT));
    }

    private static Geocache createCache() {
        final Geocache geocache = new Geocache();
        geocache.setType(CacheType.TRADITIONAL);
        geocache.setGeocode("EC727");
        return geocache;
    }

}
