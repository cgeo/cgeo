package cgeo.geocaching.connector.ox;

import static org.assertj.core.api.Assertions.assertThat;
import junit.framework.TestCase;

public class OXConnectorTest extends TestCase {

    public static void testCanHandle() {
        // http://www.opencaching.com/api_doc/concepts/oxcodes.html
        final OXConnector oxConnector = new OXConnector();
        assertThat(oxConnector.canHandle("OXZZZZZ")).isTrue();
        assertThat(oxConnector.canHandle("OX1")).isTrue();
        assertThat(oxConnector.canHandle("GCABCDE")).isFalse();
        assertThat(oxConnector.canHandle("OX_")).isFalse();
    }

}
