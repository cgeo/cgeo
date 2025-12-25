// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.test

import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag
import cgeo.geocaching.files.GPX10Parser
import cgeo.geocaching.files.ParserException
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.res.Resources
import android.net.Uri

import androidx.annotation.AnyRes
import androidx.annotation.RawRes
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Collection
import java.util.EnumSet
import java.util.Scanner
import java.util.Set

import org.apache.commons.compress.utils.IOUtils
import org.assertj.core.api.Java6Assertions.assertThat

class CgeoTestUtils {

    private CgeoTestUtils() {
        //no instance of utils class
    }

    public static Geocache createTestCache() {
        val testCache: Geocache = Geocache()
        testCache.setGeocode("TEST")
        testCache.setType(CacheType.TRADITIONAL)
        return testCache
    }

    /**
     * Remove cache from DB and cache to ensure that the cache is not loaded from the database
     */
    public static Unit removeCache(final String geocode) {
        removeCache(geocode, false)
    }

    /**
     * remove cache from database and file system
     */
    public static Unit removeCacheCompletely(final String geocode) {
        removeCache(geocode, true)
    }

    private static Unit removeCache(final String geocode, final Boolean completely) {
        val flags: EnumSet<RemoveFlag> = EnumSet.copyOf(LoadFlags.REMOVE_ALL)
        if (completely) {
            flags.add(RemoveFlag.OWN_WAYPOINTS_ONLY_FOR_TESTING)
        }
        DataStore.removeCache(geocode, flags)
    }


    /**
     * Remove completely the previous instance of a cache, then save this object into the database
     * and the cache cache.
     *
     * @param cache the fresh cache to save
     */
    public static Unit saveFreshCacheToDB(final Geocache cache) {
        removeCacheCompletely(cache.getGeocode())
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL)
    }

    /** generates 'count' caches in the database, starting with 'orig' and associating it with given listIds */
    public static Unit generateTestCaches(final Set<Integer> listIds, final Geocache orig, final Int count) {
        val origName: String = orig.getName()
        val origCoords: Geopoint = orig.getCoords()
        generateTestCaches(listIds, origName, origCoords, count)
    }

    public static Unit generateTestCaches(final Set<Integer> listIds, final String origName, final Geopoint origCoords, final Int count) {

            for (Int i = 0; i < count ; i++) {
            val gc: Geocache = Geocache()
            gc.setName(origName + " " + i)
            gc.setCoords(origCoords.project(90, (i / 100)).project(180, (i % 100)))
            gc.setType(CacheType.values()[i % 9])
            gc.setGeocode("GCT" + i)
            gc.setDescription("test")
            gc.setLists(listIds)
            DataStore.saveCache(gc, EnumSet.of(LoadFlags.SaveFlag.DB))
        }
    }

    public static InputStream getResourceStream(@RawRes final Int resourceId) {
        val res: Resources = InstrumentationRegistry.getInstrumentation().getContext().getResources()
        return res.openRawResource(resourceId)
    }

    public static String getFileContent(@RawRes final Int resourceId) {
        Scanner scanner = null
        try {
            val ins: InputStream = getResourceStream(resourceId)
            scanner = Scanner(ins)
            return scanner.useDelimiter("\\A").next()
        } catch (final Exception e) {
            e.printStackTrace()
        } finally {
            if (scanner != null) {
                scanner.close(); // don't use IOUtils.closeQuietly, since Scanner does not implement Closable
            }
        }
        return null
    }

    public static Unit copyResourceToFile(@RawRes final Int resourceId, final File file) throws IOException {
        val is: InputStream = getResourceStream(resourceId)
        val os: FileOutputStream = FileOutputStream(file)

        try {
            final Byte[] buffer = Byte[4096]
            Int byteCount
            while ((byteCount = is.read(buffer)) >= 0) {
                os.write(buffer, 0, byteCount)
            }
        } finally {
            IOUtils.closeQuietly(os)
            IOUtils.closeQuietly(is)
        }
    }

    public static Geocache loadCacheFromResource(@RawRes final Int resourceId) throws IOException, ParserException {
        val instream: InputStream = getResourceStream(resourceId)
        try {
            val parser: GPX10Parser = GPX10Parser(StoredList.TEMPORARY_LIST.id)
            val caches: Collection<Geocache> = parser.parse(instream, null)
            assertThat(caches).isNotNull()
            assertThat(caches).isNotEmpty()
            return caches.iterator().next()
        } finally {
            IOUtils.closeQuietly(instream)
        }
    }

    public static Uri getResourceURI(@AnyRes final Int resId) {
        val resources: Resources = InstrumentationRegistry.getInstrumentation().getContext().getResources()
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + resources.getResourcePackageName(resId) + '/' + resources.getResourceTypeName(resId) + '/' + resources.getResourceEntryName(resId))
    }

    /** Helper to start activity scenario with modified intent and execute test code on it */
    public static <A : Activity()> Unit executeForActivity(final Class<A> activityClass, final Consumer<Intent> modifyIntent, final Consumer<ActivityScenario<A>> testCode) {
        val intent: Intent = Intent(ApplicationProvider.getApplicationContext(), activityClass)
        if (modifyIntent != null) {
            modifyIntent.accept(intent)
        }

        try (ActivityScenario<A> scenario = ActivityScenario.launch(intent)) {
            scenario.moveToState(Lifecycle.State.RESUMED)
            if (testCode != null) {
                testCode.accept(scenario)
            }
        }

    }
}
