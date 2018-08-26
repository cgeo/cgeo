package cgeo.geocaching.connector.su;

import junit.framework.Assert;

import org.junit.Test;

public class SUConnectorTest {

    @Test
    public void testCanHandle() throws Exception {
        final GeocachingSuConnector connector = GeocachingSuConnector.getInstance();
        Assert.assertTrue(connector.canHandle("TR12"));
        Assert.assertTrue(connector.canHandle("VI12"));
        Assert.assertTrue(connector.canHandle("MS32113"));
        Assert.assertTrue(connector.canHandle("MV32113"));
        Assert.assertTrue(connector.canHandle("LT421"));
        Assert.assertTrue(connector.canHandle("LV421"));
    }
    @Test
    public void testCanNotHandle() throws Exception {
        final GeocachingSuConnector connector = GeocachingSuConnector.getInstance();
        Assert.assertFalse(connector.canHandle("GC12"));
        Assert.assertFalse(connector.canHandle("OC412"));
    }

}
