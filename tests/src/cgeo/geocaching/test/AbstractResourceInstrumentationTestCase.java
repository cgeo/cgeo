package cgeo.geocaching.test;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.files.GPX10Parser;
import cgeo.geocaching.files.ParserException;
import cgeo.geocaching.list.StoredList;

import android.content.res.Resources;
import android.test.InstrumentationTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Scanner;

public abstract class AbstractResourceInstrumentationTestCase extends InstrumentationTestCase {
    private int temporaryListId;

    protected static void removeCacheCompletely(final String geocode) {
        final EnumSet<RemoveFlag> flags = EnumSet.copyOf(LoadFlags.REMOVE_ALL);
        flags.add(RemoveFlag.REMOVE_OWN_WAYPOINTS_ONLY_FOR_TESTING);
        DataStore.removeCache(geocode, flags);
    }

    protected InputStream getResourceStream(int resourceId) {
        final Resources res = getInstrumentation().getContext().getResources();
        return res.openRawResource(resourceId);
    }

    protected String getFileContent(int resourceId) {
        final InputStream ins = getResourceStream(resourceId);
        final String result = new Scanner(ins).useDelimiter("\\A").next();
        try {
            ins.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    protected void copyResourceToFile(int resourceId, File file) throws IOException {
        final InputStream is = getResourceStream(resourceId);
        final FileOutputStream os = new FileOutputStream(file);

        try {
            final byte[] buffer = new byte[4096];
            int byteCount;
            while ((byteCount = is.read(buffer)) >= 0) {
                os.write(buffer, 0, byteCount);
            }
        } finally {
            os.close();
            is.close();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        temporaryListId = DataStore.createList("Temporary unit testing");
        assertTrue(temporaryListId != StoredList.TEMPORARY_LIST_ID);
        assertTrue(temporaryListId != StoredList.STANDARD_LIST_ID);
    }

    @Override
    protected void tearDown() throws Exception {
        final SearchResult search = DataStore.getBatchOfStoredCaches(null, CacheType.ALL, temporaryListId);
        assertNotNull(search);
        DataStore.removeCaches(search.getGeocodes(), LoadFlags.REMOVE_ALL);
        DataStore.removeList(temporaryListId);
        super.tearDown();
    }

    protected final int getTemporaryListId() {
        return temporaryListId;
    }

    final protected Geocache loadCacheFromResource(int resourceId) throws IOException, ParserException {
        final InputStream instream = getResourceStream(resourceId);
        try {
            GPX10Parser parser = new GPX10Parser(StoredList.TEMPORARY_LIST_ID);
            Collection<Geocache> caches = parser.parse(instream, null);
            assertNotNull(caches);
            assertFalse(caches.isEmpty());
            return caches.iterator().next();
        } finally {
            instream.close();
        }
    }
}
