package cgeo.geocaching.filters;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.GeocodeGeocacheFilter;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.functions.Action1;

import android.util.Pair;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit test helpers for Geocache filters
 */
public class GeocacheFilterTestUtils {

    private static final AtomicInteger DB_CACHE_INDEX = new AtomicInteger(0);

    private GeocacheFilterTestUtils() {
        //no instance for utils
    }

    /**
     * Standard method. Tests for a given cache and filter whether it is correctly filtered via logic AND DB!
     */
    public static <T extends BaseGeocacheFilter> void testSingle(final GeocacheFilterType type, final Action1<Geocache> cacheSetter, final Action1<T> filterSetter, final Boolean expectedResult) {
        testSingleLogic(type, cacheSetter, filterSetter, expectedResult);
        testSingleDb(type, cacheSetter, filterSetter, expectedResult);

    }

    public static boolean testSingleLogic(final Geocache cache, final IGeocacheFilter filter, final Boolean expectedResult) {
        assertThat(filter.filter(cache)).as("LOGIC: Wrong test result for filter: " + filter.getConfig() + "/cache: " + cache).isEqualTo(expectedResult);
        return true;
    }

    public static <T extends BaseGeocacheFilter> boolean testSingleLogic(final GeocacheFilterType type, final Action1<Geocache> cacheSetter, final Action1<T> filterSetter, final Boolean expectedResult) {
        final Pair<Geocache, T> data = prepareData(type, cacheSetter, filterSetter);
        return testSingleLogic(data.first, data.second, expectedResult);
    }

    public static <T extends BaseGeocacheFilter> boolean testSingleDb(final GeocacheFilterType type, final Action1<Geocache> cacheSetter, final Action1<T> filterSetter, final Boolean expectedResult) {
        if (expectedResult == null) {
            return false;
        }

        final Pair<Geocache, T> data = prepareData(type, cacheSetter, filterSetter);
        final Geocache cache = data.first;
        final T filter = data.second;

        final String geocode = "GCFILTERTEST" + DB_CACHE_INDEX.addAndGet(1);
        cache.setGeocode(geocode);

        final AndGeocacheFilter filterConfig = new AndGeocacheFilter();
        filterConfig.addChild(filter);
        final GeocodeGeocacheFilter geocodeFilter = new GeocodeGeocacheFilter();
        geocodeFilter.getStringFilter().setTextValue(geocode);
        filterConfig.addChild(geocodeFilter);

        final GeocacheFilter gcFilter = GeocacheFilter.create("", false, false, filterConfig);
        final SearchResult sr;
        try {
            DataStore.storeIntoDatabase(cache);
            sr = DataStore.getBatchOfStoredCaches(null, -1, gcFilter, null, false, 1);

        } finally {
            DataStore.removeCache(cache.getGeocode(), EnumSet.of(LoadFlags.RemoveFlag.CACHE, LoadFlags.RemoveFlag.DB));
        }


        final SqlBuilder sqlBuilder = new SqlBuilder("cache-table", new String[]{"*"});
        filter.addToSql(sqlBuilder);
        final String descr = "DB: Wrong test result for filter: " + filter.getConfig() + "/cache: " + cache + "/SQL: " + sqlBuilder.toString();

        if (sr.getCount() > 1) {
            fail("DB: very wrong result, cnt=" + sr.getCount() + " for '" + descr + "'");
        }

        final boolean result = sr.getCount() == 1 && sr.getGeocodes().iterator().next().equals(geocode);

        assertThat(result).as(descr).isEqualTo(expectedResult);

        return result;
    }

    private static <T extends BaseGeocacheFilter> Pair<Geocache, T> prepareData(final GeocacheFilterType type, final Action1<Geocache> cacheSetter, final Action1<T> filterSetter) {
        final T filter = type.create();
        if (filterSetter != null) {
            filterSetter.call(filter);
        }
        final Geocache cache = new Geocache();
        if (cacheSetter != null) {
            cacheSetter.call(cache);
        }
        return new Pair<>(cache, filter);
    }

}
