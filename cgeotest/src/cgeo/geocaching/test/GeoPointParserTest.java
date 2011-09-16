package cgeo.geocaching.test;

import cgeo.geocaching.geopoint.GeopointParser;
import junit.framework.Assert;
import android.test.AndroidTestCase;

public class GeoPointParserTest extends AndroidTestCase {

    public void testParseLatitude() throws Throwable {
    	
    	Assert.assertEquals(49.0 + 56.031 / 60.0, GeopointParser.parseLatitude("N 49° 56.031"));
    }

    public void testParseLongitude() throws Throwable {
    	
    	Assert.assertEquals(8.0 + 38.564 / 60.0, GeopointParser.parseLongitude("E 8° 38.564"));
    }
}
