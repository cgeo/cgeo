package cgeo.geocaching;

import cgeo.geocaching.geopoint.Geopoint;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class DestinationTest extends AndroidTestCase {

    private Destination dest = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dest = new Destination(1, 10000, new Geopoint(52.5, 9.33));
    }

    public void testSomething() {
        Assert.assertEquals(1, dest.getId());
        Assert.assertEquals(10000, dest.getDate());
        Assert.assertEquals(52.5, dest.getCoords().getLatitude());
        Assert.assertEquals(9.33, dest.getCoords().getLongitude());
    }
}