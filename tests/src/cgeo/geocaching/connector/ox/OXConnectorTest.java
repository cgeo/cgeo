package cgeo.geocaching.connector.ox;

import junit.framework.TestCase;

public class OXConnectorTest extends TestCase {

    public static void testCanHandle() {
        // http://www.opencaching.com/api_doc/concepts/oxcodes.html
        final OXConnector oxConnector = new OXConnector();
        assertTrue(oxConnector.canHandle("OXZZZZZ"));
        assertTrue(oxConnector.canHandle("OX1"));
        assertFalse(oxConnector.canHandle("GCABCDE"));
        assertFalse(oxConnector.canHandle("OX_"));
    }

}
