package cgeo.geocaching.files;

import cgeo.geocaching.cgCoord;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.test.R;

import android.content.res.Resources;
import android.test.InstrumentationTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class LocParserTest extends InstrumentationTestCase {
    @SuppressWarnings("null")
    private Map<String, cgCoord> readLoc(int resourceId) {
        Map<String, cgCoord> caches = null;
        final Resources res = getInstrumentation().getContext().getResources();
        final InputStream instream = res.openRawResource(resourceId);
        try {
            final StringBuilder buffer = new StringBuilder();
            int ch;
            while ((ch = instream.read()) != -1) {
                buffer.append((char) ch);
            }
            caches = LocParser.parseCoordinates(buffer.toString());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                instream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        assertNotNull(caches);
        assertTrue(caches.size() > 0);
        return caches;
    }

    public void testOCLoc() {
        final Map<String, cgCoord> coords = readLoc(R.raw.oc5952_loc);
        final cgCoord coord = coords.get("OC5952");
        assertNotNull(coord);
        assertEquals("OC5952", coord.geocode);
        assertEquals("Die Schatzinsel / treasure island", coord.name);
        assertTrue(new Geopoint(48.85968, 9.18740).isEqualTo(coord.coords));
    }

    public void testGCLoc() {
        final Map<String, cgCoord> coords = readLoc(R.raw.gc1bkp3_loc);
        final cgCoord coord = coords.get("GC1BKP3");
        assertNotNull(coord);
        assertEquals("GC1BKP3", coord.geocode);
        assertEquals("Die Schatzinsel / treasure island", coord.name);
        assertTrue(new Geopoint(48.859683, 9.1874).isEqualTo(coord.coords));
        assertEquals(1.0f, coord.difficulty.floatValue());
        assertEquals(5.0f, coord.terrain.floatValue());
        assertEquals(CacheSize.MICRO, coord.size);
    }

}
