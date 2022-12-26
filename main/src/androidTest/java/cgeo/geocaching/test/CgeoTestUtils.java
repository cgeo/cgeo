package cgeo.geocaching.test;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.files.GPX10Parser;
import cgeo.geocaching.files.ParserException;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.net.Uri;

import androidx.annotation.AnyRes;
import androidx.annotation.RawRes;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Scanner;

import org.apache.commons.compress.utils.IOUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

public final class CgeoTestUtils {

    private CgeoTestUtils() {
        //no instance of utils class
    }

    /** Replacement for JUNit 5 assert "does not Throw" */
    public static void assertDoesNotThrow(final Runnable r) {
        r.run();
    }

    /**
     * Remove cache from DB and cache to ensure that the cache is not loaded from the database
     */
    public static void deleteCacheFromDB(final String geocode) {
        DataStore.removeCache(geocode, LoadFlags.REMOVE_ALL);
    }

    /**
     * remove cache from database and file system
     */
    public static void removeCacheCompletely(final String geocode) {
        final EnumSet<RemoveFlag> flags = EnumSet.copyOf(LoadFlags.REMOVE_ALL);
        flags.add(RemoveFlag.OWN_WAYPOINTS_ONLY_FOR_TESTING);
        DataStore.removeCache(geocode, flags);
    }

    /**
     * Remove completely the previous instance of a cache, then save this object into the database
     * and the cache cache.
     *
     * @param cache the fresh cache to save
     */
    public static void saveFreshCacheToDB(final Geocache cache) {
        removeCacheCompletely(cache.getGeocode());
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
    }

    public static InputStream getResourceStream(@RawRes final int resourceId) {
        final Resources res = InstrumentationRegistry.getInstrumentation().getContext().getResources();
        return res.openRawResource(resourceId);
    }

    public static String getFileContent(@RawRes final int resourceId) {
        Scanner scanner = null;
        try {
            final InputStream ins = getResourceStream(resourceId);
            scanner = new Scanner(ins);
            return scanner.useDelimiter("\\A").next();
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close(); // don't use IOUtils.closeQuietly, since Scanner does not implement Closable
            }
        }
        return null;
    }

    public static void copyResourceToFile(@RawRes final int resourceId, final File file) throws IOException {
        final InputStream is = getResourceStream(resourceId);
        final FileOutputStream os = new FileOutputStream(file);

        try {
            final byte[] buffer = new byte[4096];
            int byteCount;
            while ((byteCount = is.read(buffer)) >= 0) {
                os.write(buffer, 0, byteCount);
            }
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
    }

    public static final Geocache loadCacheFromResource(@RawRes final int resourceId) throws IOException, ParserException {
        final InputStream instream = getResourceStream(resourceId);
        try {
            final GPX10Parser parser = new GPX10Parser(StoredList.TEMPORARY_LIST.id);
            final Collection<Geocache> caches = parser.parse(instream, null);
            assertThat(caches).isNotNull();
            assertThat(caches).isNotEmpty();
            return caches.iterator().next();
        } finally {
            IOUtils.closeQuietly(instream);
        }
    }

    public static Uri getResourceURI(@AnyRes final int resId) {
        final Resources resources = InstrumentationRegistry.getInstrumentation().getContext().getResources();
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + resources.getResourcePackageName(resId) + '/' + resources.getResourceTypeName(resId) + '/' + resources.getResourceEntryName(resId));
    }
}
