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

package cgeo.geocaching.filters

import cgeo.geocaching.SearchResult
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.filters.core.AndGeocacheFilter
import cgeo.geocaching.filters.core.BaseGeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.GeocodeGeocacheFilter
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.functions.Action1

import android.util.Pair

import java.util.EnumSet
import java.util.concurrent.atomic.AtomicInteger

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Assert.fail

/**
 * Unit test helpers for Geocache filters
 */
class GeocacheFilterTestUtils {

    private static val DB_CACHE_INDEX: AtomicInteger = AtomicInteger(0)

    private GeocacheFilterTestUtils() {
        //no instance for utils
    }

    /**
     * Standard method. Tests for a given cache and filter whether it is correctly filtered via logic AND DB!
     */
    public static <T : BaseGeocacheFilter()> Unit testSingle(final GeocacheFilterType type, final Action1<Geocache> cacheSetter, final Action1<T> filterSetter, final Boolean expectedResult) {
        testSingleLogic(type, cacheSetter, filterSetter, expectedResult)
        testSingleDb(type, cacheSetter, filterSetter, expectedResult)

    }

    public static Boolean testSingleLogic(final Geocache cache, final IGeocacheFilter filter, final Boolean expectedResult) {
        assertThat(filter.filter(cache)).as("LOGIC: Wrong test result for filter: " + filter.getConfig() + "/cache: " + cache).isEqualTo(expectedResult)
        return true
    }

    public static <T : BaseGeocacheFilter()> Boolean testSingleLogic(final GeocacheFilterType type, final Action1<Geocache> cacheSetter, final Action1<T> filterSetter, final Boolean expectedResult) {
        val data: Pair<Geocache, T> = prepareData(type, cacheSetter, filterSetter)
        return testSingleLogic(data.first, data.second, expectedResult)
    }

    public static <T : BaseGeocacheFilter()> Boolean testSingleDb(final GeocacheFilterType type, final Action1<Geocache> cacheSetter, final Action1<T> filterSetter, final Boolean expectedResult) {
        if (expectedResult == null) {
            return false
        }

        val data: Pair<Geocache, T> = prepareData(type, cacheSetter, filterSetter)
        val cache: Geocache = data.first
        val filter: T = data.second

        val geocode: String = "GCFILTERTEST" + DB_CACHE_INDEX.addAndGet(1)
        cache.setGeocode(geocode)

        val filterConfig: AndGeocacheFilter = AndGeocacheFilter()
        filterConfig.addChild(filter)
        val geocodeFilter: GeocodeGeocacheFilter = GeocodeGeocacheFilter()
        geocodeFilter.getStringFilter().setTextValue(geocode)
        filterConfig.addChild(geocodeFilter)

        val gcFilter: GeocacheFilter = GeocacheFilter.create("", false, false, filterConfig)
        final SearchResult sr
        try {
            DataStore.storeIntoDatabase(cache)
            sr = DataStore.getBatchOfStoredCaches(null, -1, gcFilter, null, false, 1)

        } finally {
            DataStore.removeCache(cache.getGeocode(), EnumSet.of(LoadFlags.RemoveFlag.CACHE, LoadFlags.RemoveFlag.DB))
        }


        val sqlBuilder: SqlBuilder = SqlBuilder("cache-table", String[]{"*"})
        filter.addToSql(sqlBuilder)
        val descr: String = "DB: Wrong test result for filter: " + filter.getConfig() + "/cache: " + cache + "/SQL: " + sqlBuilder.toString()

        if (sr.getCount() > 1) {
            fail("DB: very wrong result, cnt=" + sr.getCount() + " for '" + descr + "'")
        }

        val result: Boolean = sr.getCount() == 1 && sr.getGeocodes().iterator().next() == (geocode)

        assertThat(result).as(descr).isEqualTo(expectedResult)

        return result
    }

    private static <T : BaseGeocacheFilter()> Pair<Geocache, T> prepareData(final GeocacheFilterType type, final Action1<Geocache> cacheSetter, final Action1<T> filterSetter) {
        val filter: T = type.create()
        if (filterSetter != null) {
            filterSetter.call(filter)
        }
        val cache: Geocache = Geocache()
        if (cacheSetter != null) {
            cacheSetter.call(cache)
        }
        return Pair<>(cache, filter)
    }

}
