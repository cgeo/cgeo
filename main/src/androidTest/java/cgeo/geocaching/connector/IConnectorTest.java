package cgeo.geocaching.connector;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class IConnectorTest {

    @Test
    public void testName() {
        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            if (connector != ConnectorFactory.UNKNOWN_CONNECTOR) {
                assertThat(connector.getName()).isNotEmpty().isNotBlank();
                assertThat(connector.getNameAbbreviated()).isNotEmpty().isNotBlank();
            }
        }
    }
}
