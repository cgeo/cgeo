package cgeo.geocaching.test;

import cgeo.geocaching.cgeogpxes;

import android.test.AndroidTestCase;

public class cgeogpxesTest extends AndroidTestCase {
    private cgeogpxes cgeogpxes = new cgeogpxes();

    public void testFileNameMatches() {
        assertTrue(cgeogpxes.filenameBelongsToList("1234567.gpx"));
        assertTrue(cgeogpxes.filenameBelongsToList("1234567.GPX"));
        assertTrue(cgeogpxes.filenameBelongsToList(".gpx"));
        assertTrue(cgeogpxes.filenameBelongsToList("1234567.loc"));
        assertTrue(cgeogpxes.filenameBelongsToList("1234567.LOC"));

        assertFalse(cgeogpxes.filenameBelongsToList("1234567.gpy"));
        assertFalse(cgeogpxes.filenameBelongsToList("1234567.agpx"));
        assertFalse(cgeogpxes.filenameBelongsToList("1234567"));
        assertFalse(cgeogpxes.filenameBelongsToList(""));
        assertFalse(cgeogpxes.filenameBelongsToList("gpx"));

        assertFalse(cgeogpxes.filenameBelongsToList("1234567-wpts.gpx"));
    }
}
