package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.content.res.Resources;
import android.os.SystemClock;

import androidx.test.platform.app.InstrumentationRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Before;
import org.junit.Test;

/**
 * Measures getCacheMarker throughput for before/after comparison across the marker-performance
 * commit series.  Uses only the pre-existing public API (getCacheMarker, resetAllCaches) so it
 * compiles and runs identically whether or not the dirty-flag / lazy-assignedMarkers commits are
 * present.
 *
 * Run via:  ./gradlew :main:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cgeo.geocaching.MapMarkerPerformanceTest
 * Results appear in logcat tagged with "MapMarkerPerf".
 *
 * Scenarios
 * ---------
 * cold  – 5 000 distinct caches, first render each (overlaysCache miss, no dirty-flag benefit)
 * warm  – same 5 000 caches rendered a second time without any state change
 *           before commits: full 22-field hash recomputed on every call
 *           after  commits: dirty flag clear → cached hash returned immediately, getAssignedMarkers skipped
 * dirty – same caches but one flag toggled before each call, forcing full recomputation every time
 *           shows the irreducible per-call cost; should be similar before and after
 */
public class MapMarkerPerformanceTest {

    private static final int CACHE_COUNT = 500000;
    private static final CacheType[] CACHE_TYPES = Arrays.stream(CacheType.values())
            .filter(t -> t != CacheType.ALL)
            .toArray(CacheType[]::new);

    private Resources res;
    private List<Geocache> caches;

    @Before
    public void setUp() {
        res = InstrumentationRegistry.getInstrumentation().getTargetContext().getResources();
        caches = buildCaches(CACHE_COUNT);
    }

    @Test
    public void testColdPath() {
        MapMarkerUtils.resetAllCaches();
        measure("cold (5 000 distinct caches, first render)", () -> {
            for (final Geocache cache : caches) {
                MapMarkerUtils.getCacheMarker(res, cache, null, true);
            }
        });
    }

    @Test
    public void testWarmPath() {
        MapMarkerUtils.resetAllCaches();
        for (final Geocache cache : caches) {
            MapMarkerUtils.getCacheMarker(res, cache, null, true);
        }
        measure("warm (5 000 stable caches, repeated render)", () -> {
            for (final Geocache cache : caches) {
                MapMarkerUtils.getCacheMarker(res, cache, null, true);
            }
        });
    }

    @Test
    public void testDirtyPath() {
        MapMarkerUtils.resetAllCaches();
        measure("dirty (5 000 caches, flag toggled before each call)", () -> {
            for (final Geocache cache : caches) {
                cache.setFound(!cache.isFound());
                MapMarkerUtils.getCacheMarker(res, cache, null, true);
            }
        });
    }

    private static List<Geocache> buildCaches(final int count) {
        final List<Geocache> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final Geocache cache = new Geocache();
            cache.setGeocode("GC" + i);
            cache.setType(CACHE_TYPES[i % CACHE_TYPES.length]);
            cache.setFound(false);
            cache.setDisabled(i % 11 == 0);
            cache.setArchived(i % 23 == 0);
            cache.setDifficulty(1.0f + (i % 9) * 0.5f);
            cache.setTerrain(1.0f + (i % 9) * 0.5f);
            list.add(cache);
        }
        return list;
    }

    @SuppressFBWarnings("DM_GC")
    private static void measure(final String label, final Runnable runnable) {
        System.gc();
        final long start = SystemClock.elapsedRealtime();
        runnable.run();
        final long ms = SystemClock.elapsedRealtime() - start;
        Log.d("MapMarkerPerf [" + label + "]: " + ms + " ms");
    }
}
