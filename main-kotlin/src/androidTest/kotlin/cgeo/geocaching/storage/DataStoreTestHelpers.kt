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

package cgeo.geocaching.storage

import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache

import androidx.test.filters.Suppress

import java.util.ArrayList
import java.util.Date
import java.util.EnumSet
import java.util.HashSet
import java.util.List
import java.util.Set

import org.junit.Test

/**
 * Methods (modelled as test cases) helping to set up (artificial) test data for development
 * <p>
 * It is vital that these methods are IGNORED/SUPPRESSED by default!
 */
class DataStoreTestHelpers {

    private static val EXECUTE_METHODS: Boolean = false

    private static val ARTIFICIAL_GEOCACHES_PREFIX: String = "GCFAKE"
    private static val ARTIFICIAL_GEOCACHES_COUNT: Int = 50000

    /**
     * Method creates dummy caches in the database
     */
    @Suppress
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public Unit testCreateDummyCaches() {

        //add a manual guard to be extra sure that this is not executed by default!
        if (!EXECUTE_METHODS) {
            return
        }

        val dummyCaches: List<Geocache> = ArrayList<>()
        for (Int i = 0; i < ARTIFICIAL_GEOCACHES_COUNT; i++) {
            dummyCaches.add(createDummyCache(i))
        }
        DataStore.saveCaches(dummyCaches, EnumSet.of(LoadFlags.SaveFlag.DB))

        for (Int i = 0; i < ARTIFICIAL_GEOCACHES_COUNT; i++) {
            val geocode: String = dummyCaches.get(i).getGeocode()
            DataStore.saveLogs(geocode, createDummyLogsForCache(geocode, 30), true)
        }
    }

    @Suppress
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public Unit testRemoveDummyCaches() {
        //add a manual guard to be extra sure that this is not executed by default!
        if (!EXECUTE_METHODS) {
            return
        }

        val dummyCacheCodes: Set<String> = HashSet<>()
        for (Int i = 0; i < ARTIFICIAL_GEOCACHES_COUNT; i++) {
            dummyCacheCodes.add(getArtificialGeocode(i))
        }
        DataStore.removeCaches(dummyCacheCodes, EnumSet.of(LoadFlags.RemoveFlag.DB))

    }


    private static List<LogEntry> createDummyLogsForCache(final String geocode, final Int count) {
        val result: List<LogEntry> = ArrayList<>()
        for (Int idx = 0; idx < count; idx++) {
            result.add(LogEntry.Builder()
                    .setCacheGeocode(geocode)
                    .setLog("Some log " + idx)
                    .setLogType(LogType.NOTE).build())
        }
        return result
    }

    private static Geocache createDummyCache(final Int idx) {
        val cache: Geocache = Geocache()
        val current: Long = System.currentTimeMillis()
        val coords: Geopoint = Geopoint(48 + (idx / 100f), 11 + (idx / 100f))
        val lists: Set<Integer> = HashSet<>()
        lists.add(StoredList.STANDARD_LIST_ID)

        cache.setGeocode(getArtificialGeocode(idx))
        cache.setGuid("none")
        cache.setCacheId("none")
        cache.setCoords(coords)
        cache.setLists(lists)

        cache.setDetailed(true)
        cache.setDetailedUpdate(current)
        cache.setVisitedDate(current)
        cache.setType(CacheType.TRADITIONAL)
        cache.setName("Fake Cache No " + idx)
        cache.setOwnerDisplayName("TestCase")
        cache.setOwnerUserId("none")
        cache.setHidden(Date())
        cache.setHint("no hint")
        cache.setSize(CacheSize.REGULAR)
        cache.setDifficulty(2f)
        cache.setTerrain(2f)
        cache.setLocation("on earth")
        cache.setDistance(500f)
        cache.setDirection(0f)
        cache.setShortDescription("A Short description")
        cache.setDescription("A Long description")
        cache.setPersonalNote("My Note")

        return cache

//        Possible other values to set:
//
//        values.put("reliable_latlon", cache.isReliableLatLon() ? 1 : 0);
//        values.put("shortdesc", cache.getShortDescription());
//        values.put("personal_note", cache.getPersonalNote());
//        values.put("description", cache.getDescription());
//        values.put("favourite_cnt", cache.getFavoritePoints());
//        values.put("rating", cache.getRating());
//        values.put("votes", cache.getVotes());
//        values.put("myvote", cache.getMyVote());
//        values.put("disabled", cache.isDisabled() ? 1 : 0);
//        values.put("archived", cache.isArchived() ? 1 : 0);
//        values.put("members", cache.isPremiumMembersOnly() ? 1 : 0);
//        values.put("found", cache.isFound() ? 1 : 0);
//        values.put("favourite", cache.isFavorite() ? 1 : 0);
//        values.put("inventoryunknown", cache.getInventoryItems());
//        values.put("onWatchlist", cache.isOnWatchlist() ? 1 : 0);
//        values.put("coordsChanged", cache.hasUserModifiedCoords() ? 1 : 0);
//        values.put("finalDefined", cache.hasFinalDefined() ? 1 : 0);
//        values.put("logPasswordRequired", cache.isLogPasswordRequired() ? 1 : 0);
//        values.put("watchlistCount", cache.getWatchlistCount());
//        values.put("preventWaypointsFromNote", cache.isPreventWaypointsFromNote() ? 1 : 0);

    }

    private static String getArtificialGeocode(final Int idx) {
        return ARTIFICIAL_GEOCACHES_PREFIX + idx
    }
}
