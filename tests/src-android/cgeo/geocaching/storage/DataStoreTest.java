package cgeo.geocaching.storage;

import cgeo.CGeoTestCase;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import static cgeo.geocaching.enumerations.LoadFlags.REMOVE_ALL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class DataStoreTest extends CGeoTestCase {

    static final String ARTIFICIAL_GEOCODE = "TEST";

    private static final long MILLISECONDS_PER_DAY = 1000 * 60 * 60 * 24;

    public static void testStoredLists() {
        int listId1 = StoredList.STANDARD_LIST_ID;
        int listId2 = StoredList.STANDARD_LIST_ID;

        // create caches
        final Geocache cache1 = new Geocache();
        cache1.setGeocode("Cache 1");
        final Geocache cache2 = new Geocache();
        cache2.setGeocode("Cache 2");
        assertThat(cache2).isNotNull();

        try {

            // create lists
            listId1 = DataStore.createList("DataStore Test");
            assertThat(listId1).isGreaterThan(StoredList.STANDARD_LIST_ID);
            listId2 = DataStore.createList("DataStoreTest");
            assertThat(listId2).isGreaterThan(StoredList.STANDARD_LIST_ID);
            assertThat(DataStore.getLists().size()).isGreaterThanOrEqualTo(2);

            cache1.setDetailed(true);
            cache1.getLists().add(listId1);
            cache2.setDetailed(true);
            cache2.getLists().add(listId1);

            // save caches to DB (cache1=listId1, cache2=listId1)
            DataStore.saveCache(cache1, LoadFlags.SAVE_ALL);
            DataStore.saveCache(cache2, LoadFlags.SAVE_ALL);
            assertThat(DataStore.getAllCachesCount()).isGreaterThanOrEqualTo(2);

            // move list (cache1=listId1, cache2=listId1)
            assertThat(DataStore.renameList(listId1, "DataStore Test (renamed)")).isEqualTo(1);

            // get list
            final StoredList list1 = DataStore.getList(listId1);
            assertThat(list1).isNotNull();
            assertThat(list1.title).isEqualTo("DataStore Test (renamed)");

            // move to list (cache1=listId2, cache2=listId2)
            DataStore.moveToList(Collections.singletonList(cache1), listId1, listId2);
            assertThat(DataStore.getAllStoredCachesCount(listId2)).isEqualTo(1);

            // remove list (cache1=listId2, cache2=listId2)
            assertThat(DataStore.removeList(listId1)).isTrue();

            // mark dropped (cache1=1, cache2=0)
            DataStore.markDropped(Collections.singletonList(cache2));

            // mark stored (cache1=1, cache2=listId2)
            DataStore.moveToList(Collections.singletonList(cache2), listId1, listId2);
            assertThat(DataStore.getAllStoredCachesCount(listId2)).isEqualTo(2);

            // drop stored (cache1=0, cache2=0)
            DataStore.removeList(listId2);

        } finally {

            // remove caches
            final Set<String> geocodes = new HashSet<>();
            geocodes.add(cache1.getGeocode());
            geocodes.add(cache2.getGeocode());
            DataStore.removeCaches(geocodes, REMOVE_ALL);

            // remove list
            DataStore.removeList(listId1);
            DataStore.removeList(listId2);
        }
    }

    // Check that queries don't throw an exception (see issue #1429).
    public static void testLoadWaypoints() {
        final Viewport viewport = new Viewport(new Geopoint(-1, -2), new Geopoint(3, 4));
        DataStore.loadWaypoints(viewport);
    }

    // Check that saving a cache and trackable without logs works (see #2199)
    public static void testSaveWithoutLogs() {

        // create cache and trackable
        final Geocache cache = new Geocache();
        cache.setGeocode(ARTIFICIAL_GEOCODE);
        cache.setDetailed(true);
        final Trackable trackable = new Trackable();
        trackable.setLogs(null);
        final List<Trackable> inventory = new ArrayList<>();
        inventory.add(trackable);
        cache.setInventory(inventory);

        try {
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
            final Geocache loadedCache = DataStore.loadCache(ARTIFICIAL_GEOCODE, LoadFlags.LOAD_ALL_DB_ONLY);
            assert loadedCache != null;
            assertThat(loadedCache).isNotNull();
            assertThat(loadedCache).overridingErrorMessage("Cache was not saved.").isNotNull();
            assertThat(loadedCache.getInventory()).hasSize(1);
        } finally {
            DataStore.removeCache(ARTIFICIAL_GEOCODE, REMOVE_ALL);
        }
    }

    // Check that loading a cache by case insensitive geo code works correctly (see #3139)
    public static void testGeocodeCaseInsensitive() {

        final String upperCase = ARTIFICIAL_GEOCODE;
        final String lowerCase = StringUtils.lowerCase(upperCase);
        assertThat(upperCase).isNotEqualTo(lowerCase);

        // create cache and trackable
        final Geocache cache = new Geocache();
        cache.setGeocode(upperCase);
        cache.setDetailed(true);

        try {
            final Geocache oldCache = DataStore.loadCache(upperCase, LoadFlags.LOAD_ALL_DB_ONLY);
            assertThat(oldCache).as("Old cache").isNull();

            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
            final Geocache cacheWithOriginalCode = DataStore.loadCache(upperCase, LoadFlags.LOAD_ALL_DB_ONLY);
            assertThat(cacheWithOriginalCode).overridingErrorMessage("Cache was not saved correctly!").isNotNull();

            final Geocache cacheLowerCase = DataStore.loadCache(lowerCase, LoadFlags.LOAD_ALL_DB_ONLY);
            assertThat(cacheLowerCase).overridingErrorMessage("Could not find cache by case insensitive geocode").isNotNull();

        } finally {
            DataStore.removeCache(upperCase, REMOVE_ALL);
        }
    }

    // Loading logs for an empty geocode should return an empty list, not null!
    public static void testLoadLogsFromEmptyGeocode() {
        final List<LogEntry> logs = DataStore.loadLogs("");

        assertThat(logs).as("Logs for empty geocode").isNotNull();
        assertThat(logs).as("Logs for empty geocode").isEmpty();
    }

    public static void testLog() {
        //ensure that we don't overwrite anything in database before starting this test
        DataStore.removeCache(ARTIFICIAL_GEOCODE, REMOVE_ALL);
        assertThat(DataStore.loadLogs(ARTIFICIAL_GEOCODE)).isEmpty();

        final List<LogEntry> logs = new ArrayList<>();
        logs.add(new LogEntry.Builder().setDate(new Date().getTime() - MILLISECONDS_PER_DAY * 3).setLog("testlog").setLogType(LogType.NOTE).setServiceLogId("pid").build());
        logs.add(new LogEntry.Builder().setDate(new Date().getTime() - MILLISECONDS_PER_DAY * 2).setLog("testlog2").setLogType(LogType.NOTE).build());
        DataStore.saveLogs(ARTIFICIAL_GEOCODE, logs, true);

        final List<LogEntry> logsLoadeded = DataStore.loadLogs(ARTIFICIAL_GEOCODE);

        assertThat(logsLoadeded).containsExactlyInAnyOrderElementsOf(logs);
    }

    public static void testLoadCacheHistory() {
        final SearchResult history = DataStore.getBatchOfStoredCaches(null, PseudoList.HISTORY_LIST.id);
        assertThat(history).isNotNull();

        // check that two different routines behave the same
        assertThat(history.getCount()).isEqualTo(DataStore.getAllStoredCachesCount(PseudoList.HISTORY_LIST.id));
    }

    public static void testOfflineLog() {
        final String geocode = ARTIFICIAL_GEOCODE + "-O";
        final Date logDate = new Date(new Date().getTime() - MILLISECONDS_PER_DAY * 3);
        //ensure that we don't overwrite anything in database before starting this test
        DataStore.clearLogOffline(geocode);
        final OfflineLogEntry logEntry = DataStore.loadLogOffline(geocode);
        assertThat(logEntry).isNull();
        assertThat(DataStore.clearLogOffline(geocode)).isEqualTo(false);

        try {
            final OfflineLogEntry.Builder<?> builder = new OfflineLogEntry.Builder<>()
                    .setServiceLogId("pid")
                    .setCacheGeocode(geocode)
                    .setDate(logDate.getTime())
                    .setLogType(LogType.ARCHIVE)
                    .setLog("this is my log message")
                    .setReportProblem(ReportProblemType.MISSING)
                    .setImageScale(123)
                    .setImageTitlePraefix("ImagePraefix")
                    .setFavorite(false)
                    .setTweet(true)
                    .setRating(4.5f)
                    .addLogImage(new Image.Builder().setUrl("https://www.cgeo.org/images/logo.png").setTitle("The logo").setDescription("This is the logo").build())
                    .addLogImage(new Image.Builder().setUrl("https://manual.cgeo.org/_media/type_multi.png").setTitle("Multicache icon").setDescription("This is the icon for a multicache").build())
                    .addTrackableAction("TBFAKE1", LogTypeTrackable.VISITED)
                    .addTrackableAction("TBFAKE2", LogTypeTrackable.DROPPED_OFF);

            //test insert and reload
            DataStore.saveLogOffline(geocode, builder.build());

            OfflineLogEntry loadedLogEntry = DataStore.loadLogOffline(geocode);
            assertEqualToBuilder(loadedLogEntry, builder);
            final int logId = loadedLogEntry.id;

            builder.setDate(logDate.getTime())
                    .setLogType(LogType.DIDNT_FIND_IT)
                    .setLog("this is my new log message")
                    .setReportProblem(ReportProblemType.DAMAGED)
                    .setImageScale(456)
                    .setImageTitlePraefix("NewImagePraefix")
                    .setFavorite(true)
                    .setTweet(false)
                    .setRating(1.0f)
                    .setPassword("pwd")
                    .setLogImages(new ArrayList<>())
                    .addLogImage(new Image.Builder().setUrl("https://www.cgeo.org/images/logo.png").setTitle("The logo").setDescription("This is the logo").build())
                    .addLogImage(new Image.Builder().setUrl("https://manual.cgeo.org/_media/type_virtual.png").setTitle("Virtual icon").setDescription("This is the icon for a virtualcache").build())
                    .addLogImage(new Image.Builder().setUrl("https://manual.cgeo.org/_media/type_tradi.png").setTitle("Tradicache icon").setDescription("This is the icon for a traditionalcache").build())
                    .clearTrackableActions()
                    .addTrackableAction("TBFAKE1", LogTypeTrackable.DO_NOTHING)
                    .addTrackableAction("TBFAKE3", LogTypeTrackable.ARCHIVED);

            //test update and reload
            DataStore.saveLogOffline(geocode, builder.build());

            loadedLogEntry = DataStore.loadLogOffline(geocode);
            assertThat(loadedLogEntry.id).isEqualTo(logId);
            assertEqualToBuilder(loadedLogEntry, builder);

            //remove
            assertThat(DataStore.clearLogOffline(geocode)).isEqualTo(true);

        } finally {
            DataStore.clearLogOffline(geocode);
        }
    }

    public static void testEmptyOfflineLog() {
        final String geocode = ARTIFICIAL_GEOCODE + "-O";

        //ensure that we don't overwrite anything in database before starting this test
        DataStore.clearLogOffline(geocode);
        final OfflineLogEntry logEntry = DataStore.loadLogOffline(geocode);
        assertThat(logEntry).isNull();

        try {
            final OfflineLogEntry.Builder<?> builder = new OfflineLogEntry.Builder<>();
            assertThat(DataStore.saveLogOffline(geocode, builder.build())).isEqualTo(false);

            assertThat(DataStore.saveLogOffline(geocode, builder.setCacheGeocode(geocode).setLog("test").build())).isEqualTo(true);
            final OfflineLogEntry loadedLogEntry = DataStore.loadLogOffline(geocode);
            assertThat(loadedLogEntry.id).isGreaterThanOrEqualTo(0);
            assertEqualToBuilder(loadedLogEntry, builder);

        } finally {
            DataStore.clearLogOffline(geocode);
        }
    }

    private static void assertEqualToBuilder(final OfflineLogEntry dbLogEntry, final OfflineLogEntry.Builder<?> builder) {
        final OfflineLogEntry expectedLogEntry = builder.build();

        assertThat(dbLogEntry).isNotNull();
        assertThat(dbLogEntry.id).isGreaterThanOrEqualTo(0);
        assertThat(dbLogEntry.cacheGeocode).isEqualTo(expectedLogEntry.cacheGeocode);
        assertThat(dbLogEntry.logType).isEqualTo(expectedLogEntry.logType);
        assertThat(dbLogEntry.log).isEqualTo(expectedLogEntry.log);
        assertThat(dbLogEntry.date).isEqualTo(expectedLogEntry.date);
        assertThat(dbLogEntry.reportProblem).isEqualTo(expectedLogEntry.reportProblem);
        assertThat(dbLogEntry.imageScale).isEqualTo(expectedLogEntry.imageScale);
        assertThat(dbLogEntry.imageTitlePraefix).isEqualTo(expectedLogEntry.imageTitlePraefix);
        assertThat(dbLogEntry.favorite).isEqualTo(expectedLogEntry.favorite);
        assertThat(dbLogEntry.tweet).isEqualTo(expectedLogEntry.tweet);
        assertThat(dbLogEntry.rating).isEqualTo(expectedLogEntry.rating);
        assertThat(dbLogEntry.password).isEqualTo(expectedLogEntry.password);

        assertThat(dbLogEntry.logImages.size()).isEqualTo(expectedLogEntry.logImages.size());
        final List<Image> dbImages = new ArrayList<>(dbLogEntry.logImages);
        final List<Image> expImages = new ArrayList<>(expectedLogEntry.logImages);
        //ensure that the order of images is determined for valid content assertion: sort by title
        final Comparator<Image> imgComp = (image, image2) -> image.getTitle().compareTo(image2.getTitle());
        Collections.sort(dbImages, imgComp);
        Collections.sort(expImages, imgComp);
        for (int i = 0; i < dbImages.size(); i++) {
            assertThat(dbImages.get(i).getTitle()).isEqualTo(expImages.get(i).getTitle());
            assertThat(dbImages.get(i).getUrl()).isEqualTo(expImages.get(i).getUrl());
            assertThat(dbImages.get(i).getDescription()).isEqualTo(expImages.get(i).getDescription());
        }

        assertThat(dbLogEntry.trackableActions).isEqualTo(expectedLogEntry.trackableActions);
    }


}
