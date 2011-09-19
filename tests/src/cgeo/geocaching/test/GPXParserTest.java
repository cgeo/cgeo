package cgeo.geocaching.test;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.files.GPX10Parser;
import cgeo.geocaching.geopoint.Geopoint;

import android.content.res.Resources;
import android.os.Handler;
import android.test.InstrumentationTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class GPXParserTest extends InstrumentationTestCase {

    public void testGPXVersion100() {
        testGPXVersion(R.raw.gc1bkp3_gpx100);
    }

    private cgCache testGPXVersion(int resourceId) {
        final List<cgCache> caches = readGPX(resourceId);
        assertNotNull(caches);
        assertEquals(1, caches.size());
        final cgCache cache = caches.get(0);
        assertEquals("GC1BKP3", cache.geocode);
        assertEquals("9946f030-a514-46d8-a050-a60e92fd2e1a", cache.guid);
        assertEquals("traditional", cache.type);
        assertEquals(false, cache.archived);
        assertEquals(false, cache.disabled);
        assertEquals("Die Schatzinsel / treasure island", cache.name);
        assertEquals("Die unbesiegbaren Geo - Geparden", cache.owner);
        assertEquals(CacheSize.MICRO, cache.size);
        assertEquals(1.0f, cache.difficulty.floatValue());
        assertEquals(5.0f, cache.terrain.floatValue());
        assertEquals("Baden-Württemberg, Germany", cache.location);
        assertEquals("Ein alter Kindheitstraum, ein Schatz auf einer unbewohnten Insel.\nA old dream of my childhood, a treasure on a lonely island.", cache.shortdesc);
        assertTrue(new Geopoint(48.859683, 9.1874).isEqualTo(cache.coords));
        return cache;
    }

    public void testGPXVersion101() {
        final cgCache cache = testGPXVersion(R.raw.gc1bkp3_gpx101);
        assertNotNull(cache.attributes);
        assertEquals(10, cache.attributes.size());
    }

    public void testOC() {
        final List<cgCache> caches = readGPX(R.raw.oc5952_gpx);
        final cgCache cache = caches.get(0);
        assertEquals("OC5952", cache.geocode);
        assertEquals("traditional", cache.type);
        assertEquals(false, cache.archived);
        assertEquals(false, cache.disabled);
        assertEquals("Die Schatzinsel / treasure island", cache.name);
        assertEquals("Die unbesiegbaren Geo - Geparden", cache.owner);
        assertEquals(CacheSize.SMALL, cache.size);
        assertEquals(1.0f, cache.difficulty.floatValue());
        assertEquals(4.0f, cache.terrain.floatValue());
        assertEquals("Baden-Württemberg, Germany", cache.location);
        assertEquals("Ein alter Kindheitstraum, ein Schatz auf einer unbewohnten Insel. A old dream of my childhood, a treasure on a lonely is", cache.shortdesc);
        assertTrue(new Geopoint(48.85968, 9.18740).isEqualTo(cache.coords));
    }

    private List<cgCache> readGPX(int resourceId) {
        List<cgCache> caches = null;
        final Resources res = getInstrumentation().getContext().getResources();
        final InputStream instream = res.openRawResource(resourceId);
        try {
            final GPX10Parser parser = new GPX10Parser(1);
            caches = parser.parse(instream, new Handler());
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

}
