package cgeo.geocaching.files;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LocParserTest extends AbstractResourceInstrumentationTestCase {
    private List<Geocache> readLoc(int resourceId) throws IOException, ParserException {
        final LocParser parser = new LocParser(getTemporaryListId());
        Collection<Geocache> caches = null;
        final InputStream instream = getResourceStream(resourceId);
        try {
            caches = parser.parse(instream, null);
            assertNotNull(caches);
            assertTrue(caches.size() > 0);
        } finally {
            instream.close();
        }

        return new ArrayList<Geocache>(caches);
    }

    public void testOCLoc() throws IOException, ParserException {
        final List<Geocache> caches = readLoc(R.raw.oc5952_loc);
        assertEquals(1, caches.size());
        final Geocache cache = caches.get(0);
        assertNotNull(cache);
        assertEquals("OC5952", cache.getGeocode());
        assertEquals("Die Schatzinsel / treasure island", cache.getName());
        assertEquals("Die unbesiegbaren Geo - Geparden", cache.getOwnerDisplayName());
        assertEquals(new Geopoint(48.85968, 9.18740), cache.getCoords());
    }

    public void testGCLoc() throws IOException, ParserException {
        final List<Geocache> caches = readLoc(R.raw.gc1bkp3_loc);
        assertEquals(1, caches.size());
        final Geocache cache = caches.get(0);
        assertNotNull(cache);
        assertEquals("GC1BKP3", cache.getGeocode());
        assertEquals("Die Schatzinsel / treasure island", cache.getName());
        assertEquals("Die unbesiegbaren Geo - Geparden", cache.getOwnerDisplayName());
        assertEquals(new Geopoint(48.859683, 9.1874), cache.getCoords());
        assertEquals(1.0f, cache.getDifficulty());
        assertEquals(5.0f, cache.getTerrain());
        assertEquals(CacheSize.MICRO, cache.getSize());
    }

    public void testWaymarkingLoc() throws IOException, ParserException {
        final List<Geocache> waymarks = readLoc(R.raw.waymarking_loc);
        assertEquals(1, waymarks.size());
        final Geocache waymark = waymarks.get(0);
        assertNotNull(waymark);
        assertEquals("WM7BK7", waymark.getGeocode());
        assertEquals("Römerstrasse Kornwestheim", waymark.getName());
        assertEquals("travelling", waymark.getOwnerDisplayName());
        assertEquals(new Geopoint(48.856733, 9.197683), waymark.getCoords());
        // links are not yet stored for single caches
        // assertEquals("http://www.waymarking.com/waymarks/WM7BK7_Rmerstrasse_Kornwestheim", waymark.getUrl());
        assertEquals(CacheSize.UNKNOWN, waymark.getSize());
        assertEquals(CacheType.UNKNOWN, waymark.getType());
    }

}
