package cgeo.geocaching.connector.tc;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.connector.IConnector;

import junit.framework.TestCase;

import java.util.Set;

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

        assertThat(tcConnector.canHandle("TC1234")).isFalse();
        assertThat(tcConnector.canHandle("GC1234")).isFalse();
    }

    public static void testHandleCyberCaches() {
        final IConnector tcConnector = getTerraCachingConnector();

        assertThat(tcConnector.canHandle("CC6KVG")).isTrue();
        assertThat(tcConnector.canHandle("CC7TMQ")).isTrue();

        assertThat(tcConnector.canHandle("CC9")).isFalse();
    }

    public static void testHandledGeocodes() {
        Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(getTerraCachingConnector().handledGeocodes(geocodes)).containsOnly("TCABC", "TC2JP");
    }
}
