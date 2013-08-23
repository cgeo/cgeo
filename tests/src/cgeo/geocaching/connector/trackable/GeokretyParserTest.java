package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

public class GeokretyParserTest extends AbstractResourceInstrumentationTestCase {

    public void testParse() {
        Trackable trackable = GeokretyParser.parse(getFileContent(R.raw.geokret141_xml));
        assertNotNull(trackable);
        assertEquals("WeltenbummlerKret", trackable.getName());
        assertEquals("GK008D", trackable.getGeocode());
        assertEquals(2235f, trackable.getDistance());
        assertEquals(cgeoapplication.getInstance().getString(cgeo.geocaching.R.string.geokret_type_traditional), trackable.getType());
    }

}
