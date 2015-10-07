package cgeo.geocaching.connector.tc;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.connector.IConnector;

import java.util.Set;

import junit.framework.TestCase;

public class TerraCachingConnectorTest extends TestCase {

    private static IConnector getTerraCachingConnector() {
        final IConnector tcConnector = ConnectorFactory.getConnector("TCABC");
        assertThat(tcConnector).isNotNull();
        return tcConnector;
    }

    public static void testHandleTerraCaches() {
        final IConnector tcConnector = getTerraCachingConnector();

        assertThat(tcConnector.canHandle("TCABC")).isTrue();
        assertThat(tcConnector.canHandle("TC2JP")).isTrue();
        assertThat(tcConnector.canHandle("TC9")).isTrue();

        assertThat(tcConnector.canHandle("GC1234")).isFalse();
    }

    public static void testHandleCyberCaches() {
        final IConnector tcConnector = getTerraCachingConnector();

        assertThat(tcConnector.canHandle("CC6KVG")).isTrue();
        assertThat(tcConnector.canHandle("CC7TMQ")).isTrue();
        assertThat(tcConnector.canHandle("CC9")).isTrue();
    }

    public static void testHandleLocationLessCaches() {
        final IConnector tcConnector = getTerraCachingConnector();

        assertThat(tcConnector.canHandle("LC5U28")).isTrue();
        assertThat(tcConnector.canHandle("LC9")).isTrue();
    }

    public static void testHandledGeocodes() {
        final Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(getTerraCachingConnector().handledGeocodes(geocodes)).containsOnly("TCABC", "TC2JP");
    }
}
