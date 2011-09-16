package cgeo.geocaching.test;

import junit.framework.Assert;
import android.test.AndroidTestCase;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.geopoint.DistanceParser;

public class DistanceParserTest extends AndroidTestCase {

	static private final double MM = 1e-6;  // 1mm, in kilometers

	public void testFormats() {
		Assert.assertEquals(1.2, DistanceParser.parseDistance("1200 m", cgSettings.unitsMetric), MM);
		Assert.assertEquals(1.2, DistanceParser.parseDistance("1.2 km", cgSettings.unitsMetric), MM);
		Assert.assertEquals(0.36576, DistanceParser.parseDistance("1200 ft", cgSettings.unitsMetric), MM);
		Assert.assertEquals(1.09728, DistanceParser.parseDistance("1200 yd", cgSettings.unitsMetric), MM);
		Assert.assertEquals(1.9312128, DistanceParser.parseDistance("1.2 mi", cgSettings.unitsMetric), MM);
	}

	public void testImplicit() {
		Assert.assertEquals(1.2, DistanceParser.parseDistance("1200", cgSettings.unitsMetric), MM);
		Assert.assertEquals(0.36576, DistanceParser.parseDistance("1200", cgSettings.unitsImperial), MM);
	}

	public void testComma() {
		Assert.assertEquals(1.2, DistanceParser.parseDistance("1,2km", cgSettings.unitsMetric), MM);
	}

	public void testCase() {
		Assert.assertEquals(0.36576, DistanceParser.parseDistance("1200 FT", cgSettings.unitsMetric), MM);
	}

}