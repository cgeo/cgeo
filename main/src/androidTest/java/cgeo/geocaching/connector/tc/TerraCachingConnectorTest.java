package cgeo.geocaching.connector.tc;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.connector.IConnector;

import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class TerraCachingConnectorTest {

    private static IConnector getTerraCachingConnector() {
        final IConnector tcConnector = ConnectorFactory.getConnector("TCABC");
        assertThat(tcConnector).isNotNull();
        return tcConnector;
    }

    @Test
    public void testHandleTerraCaches() {
        final IConnector tcConnector = getTerraCachingConnector();

        assertThat(tcConnector.canHandle("TCABC")).isTrue();
        assertThat(tcConnector.canHandle("TC2JP")).isTrue();
        assertThat(tcConnector.canHandle("TC9")).isTrue();

        assertThat(tcConnector.canHandle("GC1234")).isFalse();
    }

    @Test
    public void testHandleCyberCaches() {
        final IConnector tcConnector = getTerraCachingConnector();

        assertThat(tcConnector.canHandle("CC6KVG")).isTrue();
        assertThat(tcConnector.canHandle("CC7TMQ")).isTrue();
        assertThat(tcConnector.canHandle("CC9")).isTrue();
    }

    @Test
    public void testHandleLocationLessCaches() {
        final IConnector tcConnector = getTerraCachingConnector();

        assertThat(tcConnector.canHandle("LC5U28")).isTrue();
        assertThat(tcConnector.canHandle("LC9")).isTrue();
    }

    @Test
    public void testHandledGeocodes() {
        final Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(getTerraCachingConnector().handledGeocodes(geocodes)).containsOnly("TCABC", "TC2JP");
    }
}
