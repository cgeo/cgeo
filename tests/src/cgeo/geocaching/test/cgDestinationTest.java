package cgeo.geocaching.test;

import junit.framework.Assert;
import android.test.AndroidTestCase;
import cgeo.geocaching.cgDestination;
import cgeo.geocaching.geopoint.Geopoint;

public class cgDestinationTest extends AndroidTestCase {

	cgDestination dest = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		dest = new cgDestination(1, 10000, new Geopoint(52.5, 9.33));
	}

    public void testSomething() throws Throwable {
    	Assert.assertEquals(1, dest.getId());
    	Assert.assertEquals(10000, dest.getDate());
    	Assert.assertEquals(52.5, dest.getCoords().getLatitude());
    	Assert.assertEquals(9.33, dest.getCoords().getLongitude());
    }
}