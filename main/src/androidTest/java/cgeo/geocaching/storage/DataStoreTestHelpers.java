package cgeo.geocaching.storage;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import androidx.test.filters.Suppress;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * Methods (modelled as test cases) helping to set up (artificial) test data for development
 * <p>
 * It is vital that these methods are IGNORED/SUPPRESSED by default!
 */
public class DataStoreTestHelpers {

    private static final boolean EXECUTE_METHODS = false;

    private static final String ARTIFICIAL_GEOCACHES_PREFIX = "GCFAKE";
    private static final int ARTIFICIAL_GEOCACHES_COUNT = 50000;

    /**
     * Method creates dummy caches in the database
     */
    @Suppress
    @Test
    public void testCreateDummyCaches() {

        //add a manual guard to be extra sure that this is not executed by default!
        if (!EXECUTE_METHODS) {
            return;
        }

        final List<Geocache> dummyCaches = new ArrayList<>();
        for (int i = 0; i < ARTIFICIAL_GEOCACHES_COUNT; i++) {
            dummyCaches.add(createDummyCache(i));
        }
        DataStore.saveCaches(dummyCaches, EnumSet.of(LoadFlags.SaveFlag.DB));

        for (int i = 0; i < ARTIFICIAL_GEOCACHES_COUNT; i++) {
            final String geocode = dummyCaches.get(i).getGeocode();
            DataStore.saveLogs(geocode, createDummyLogsForCache(geocode, 30), true);
        }
    }

    @Suppress
    @Test
    public void testRemoveDummyCaches() {
        //add a manual guard to be extra sure that this is not executed by default!
        if (!EXECUTE_METHODS) {
            return;
        }

        final Set<String> dummyCacheCodes = new HashSet<>();
        for (int i = 0; i < ARTIFICIAL_GEOCACHES_COUNT; i++) {
            dummyCacheCodes.add(getArtificialGeocode(i));
        }
        DataStore.removeCaches(dummyCacheCodes, EnumSet.of(LoadFlags.RemoveFlag.DB));
    }


    private static List<LogEntry> createDummyLogsForCache(final String geocode, final int count) {
        final List<LogEntry> result = new ArrayList<>();
        for (int idx = 0; idx < count; idx++) {
            result.add(new LogEntry.Builder<>()
                    .setCacheGeocode(geocode)
                    .setLog("Some log " + idx)
                    .setLogType(LogType.NOTE).build());
        }
        return result;
    }

    private static Geocache createDummyCache(final int idx) {
        final Geocache cache = new Geocache();
        final long current = System.currentTimeMillis();
        final Geopoint coords = new Geopoint(48 + (idx / 100f), 11 + (idx / 100f));
        final Set<Integer> lists = new HashSet<>();
        lists.add(StoredList.STANDARD_LIST_ID);

        cache.setGeocode(getArtificialGeocode(idx));
        cache.setGuid("none");
        cache.setCacheId("none");
        cache.setCoords(coords);
        cache.setLists(lists);

        cache.setDetailed(true);
        cache.setDetailedUpdate(current);
        cache.setVisitedDate(current);
        cache.setType(CacheType.TRADITIONAL);
        cache.setName("Fake Cache No " + idx);
        cache.setOwnerDisplayName("TestCase");
        cache.setOwnerUserId("none");
        cache.setHidden(new Date());
        cache.setHint("no hint");
        cache.setSize(CacheSize.REGULAR);
        cache.setDifficulty(2f);
        cache.setTerrain(2f);
        cache.setLocation("on earth");
        cache.setDistance(500f);
        cache.setDirection(0f);
        cache.setShortDescription("A short description");
        cache.setDescription("A long description");
        cache.setPersonalNote("My Note");

        return cache;

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

    private static String getArtificialGeocode(final int idx) {
        return ARTIFICIAL_GEOCACHES_PREFIX + idx;
    }
}
