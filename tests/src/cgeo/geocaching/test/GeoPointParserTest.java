package cgeo.geocaching.test;

import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointParser;
import junit.framework.Assert;
import android.test.AndroidTestCase;

public class GeoPointParserTest extends AndroidTestCase {

	private static final double refLongitude = 8.0 + 38.564 / 60.0;
	private static final double refLatitude = 49.0 + 56.031 / 60.0;

	public void testParseLatitude() throws Throwable {

		Assert.assertEquals(refLatitude, GeopointParser.parseLatitude("N 49° 56.031"), 1e-8);
	}

	public void testParseLongitude() throws Throwable {

		Assert.assertEquals(refLongitude, GeopointParser.parseLongitude("E 8° 38.564"), 1e-8);
	}

	public void testFullCoordinates() throws Throwable {
		final Geopoint goal = new Geopoint(refLatitude, refLongitude);
		Assert.assertTrue(goal.isEqualTo(GeopointParser.parse("N 49° 56.031 | E 8° 38.564"), 1e-6));
	}

	public void testSouth() throws Throwable {
		Assert.assertEquals(-refLatitude, GeopointParser.parseLatitude("S 49° 56.031"), 1e-8);
	}

	public void testWest() throws Throwable {
		Assert.assertEquals(-refLongitude, GeopointParser.parseLongitude("W 8° 38.564"), 1e-8);
	}

	public void testLowerCase() throws Throwable {
		Assert.assertEquals(refLongitude, GeopointParser.parseLongitude("e 8° 38.564"), 1e-8);
	}
	
	public void testVariousFormats() throws Throwable {
		final Geopoint goal1 = GeopointParser.parse("N 49° 43' 57\" | E 2 12' 35");
		final Geopoint goal2 = GeopointParser.parse("N 49 43.95 E2°12.5833333333");
		Assert.assertTrue(goal1.isEqualTo(goal2, 1e-6));
	}
}
