package cgeo.geocaching.connector;

import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class IConnectorTest extends AbstractResourceInstrumentationTestCase {

    public static void testName() {
        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            if (connector != ConnectorFactory.UNKNOWN_CONNECTOR) {
                assertThat(connector.getName()).isNotEmpty().isNotBlank();
                assertThat(connector.getNameAbbreviated()).isNotEmpty().isNotBlank();
            }
        }
    }
}
