package cgeo.geocaching.connector;

import static org.assertj.core.api.Assertions.assertThat;
import junit.framework.TestCase;

public class TerraCachingConnectorTest extends TestCase {

    public static void testCanHandle() {
        final IConnector tcConnector = ConnectorFactory.getConnector("TCABC");
        assertThat(tcConnector).isNotNull();

        assertThat(tcConnector.canHandle("TCABC")).isTrue();
        assertThat(tcConnector.canHandle("TC2JP")).isTrue();

        assertThat(tcConnector.canHandle("TC1234")).isFalse();
        assertThat(tcConnector.canHandle("GC1234")).isFalse();
    }

}
