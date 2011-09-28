package cgeo.geocaching.test;

import cgeo.geocaching.cgWaypoint;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class cgWaypointTest extends AndroidTestCase {

    @SuppressWarnings("static-method")
	public void testOrder() {
		final cgWaypoint wp1 = new cgWaypoint();
		final cgWaypoint wp2 = new cgWaypoint();

		wp1.setPrefix("PK");
		wp2.setPrefix("X");
		Assert.assertTrue(wp1.compareTo(wp2) < 0);

		wp1.setPrefix("S1");
		Assert.assertTrue(wp1.compareTo(wp2) > 0);

		wp2.setPrefix("S3");
		Assert.assertTrue(wp1.compareTo(wp2) < 0);

		wp1.setPrefix("S10");
		Assert.assertTrue(wp1.compareTo(wp2) > 0);

		wp2.setPrefix("FI");
		Assert.assertTrue(wp1.compareTo(wp2) < 0);

		wp1.setPrefix("OWN");
		Assert.assertTrue(wp1.compareTo(wp2) > 0);
	}

}
