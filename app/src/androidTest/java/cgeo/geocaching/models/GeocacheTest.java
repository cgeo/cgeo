package cgeo.geocaching.models;

import cgeo.CGeoTestCase;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeocacheTest extends CGeoTestCase {

    private static final class MockedEventCache extends Geocache {
        MockedEventCache(final Date date) {
            setHidden(date);
            setType(CacheType.EVENT);
        }
    }

    public static void testIsPastEvent() {
        final Date today = new Date();
        final Geocache cacheToday = new MockedEventCache(today);
        assertThat(cacheToday.isPastEvent()).isFalse();

        final Date yesterday = new Date(today.getTime() - 86400 * 1000);
        final MockedEventCache cacheYesterday = new MockedEventCache(yesterday);
        assertThat(cacheYesterday.isPastEvent()).isTrue();
    }

    public static void testEquality() {
        final Geocache one = new Geocache();
        final Geocache two = new Geocache();

        // identity
        assertThat(one).isEqualTo(one);

        // different objects without geocode shall not be equal
        assertThat(one).isNotEqualTo(two);

        one.setGeocode("geocode");
        two.setGeocode("geocode");

        // different objects with same geocode shall be equal
        assertThat(one).isEqualTo(two);
    }

    public static void testGeocodeUppercase() {
        final Geocache cache = new Geocache();
        cache.setGeocode("gc1234");
        assertThat(cache.getGeocode()).isEqualTo("GC1234");
    }

    /**
     * The waypoint with valid coordinates.
     * Waypoint should be extracted, so expected size is 1.
     */
    public final void testUpdateWaypointFromNote() {
        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(new Waypoint("", new Geopoint("N51 13.888 E007 03.444"), "", "", "", WaypointType.OWN));
        assertWaypointsParsed("Test N51 13.888 E007 03.444", wpList);
    }

    /**
     * Waypoints in a single line with valid coordinates.
     * Waypoints should be extracted, but the user-note contains the following text, so expected size is 3 with different user-notes.
     */
    public final void testUpdateWaypointsFromNoteSingleLine() {
        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(createWaypointWithUserNote(new Geopoint("N51 13.888 E007 03.444"), "", "", "Test N51 13.233 E007 03.444 Test N51 09.123 E007 03.444", WaypointType.OWN));
        wpList.add(createWaypointWithUserNote(new Geopoint("N51 13.233 E007 03.444"), "", "", "Test N51 09.123 E007 03.444", WaypointType.OWN));
        wpList.add(createWaypointWithUserNote(new Geopoint("N51 09.123 E007 03.444"), "", "", "", WaypointType.OWN));
        assertWaypointsParsed("Test N51 13.888 E007 03.444 Test N51 13.233 E007 03.444 Test N51 09.123 E007 03.444", wpList);
    }

    /**
     * Waypoints in different lines with valid coordinates.
     * Waypoints should be extracted, the user-note does not contain the following text, so expected size is 3 with empty user-notes.
     */
    public final void testUpdateWaypointsFromNote() {
        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(new Waypoint("", new Geopoint("N51 13.888 E007 03.444"), "", "", "", WaypointType.OWN));
        wpList.add(new Waypoint("", new Geopoint("N51 13.233 E007 03.444"), "", "", "", WaypointType.OWN));
        wpList.add(new Waypoint("", new Geopoint("N51 09.123 E007 03.444"), "", "", "", WaypointType.OWN));
        assertWaypointsParsed("Test N51 13.888 E007 03.444 \nTest N51 13.233 E007 03.444 \nTest N51 09.123 E007 03.444", wpList);
    }

    /**
     * The first and the third waypoints are identical.
     * Duplicates should be ignored, so expected size is 2.
     */
    public final void testUpdateWaypointsFromNoteWithDuplicates() {
        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(new Waypoint("", new Geopoint("N51 13.888 E007 03.444"), "", "", "", WaypointType.OWN));
        wpList.add(new Waypoint("", new Geopoint("N51 13.233 E007 03.444"), "", "", "", WaypointType.OWN));
        assertWaypointsParsed("Test N51 13.888 E007 03.444 \nTest N51 13.233 E007 03.444 \nTest N51 13.888 E007 03.444", wpList);
    }

    /**
     * The second waypoint has empty coordinates.
     * Waypoint with empty coordinates should be created, so expected size is 2.
     */
    public final void testUpdateWaypointsWithEmptyCoordsFromNote() {
        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(new Waypoint("", new Geopoint("N51 13.888 E007 03.444"), "", "", "", WaypointType.OWN));
        wpList.add(new Waypoint("", null, "", "", "", WaypointType.OWN));
        assertWaypointsParsed("Test N51 13.888 E007 03.444\nTest (NO-COORD)", wpList);
    }

    /**
     * The second and the third waypoints have different names.
     * So expected size is 3.
     */
    public final void testUpdateWaypointsWithTwoEmptyCoordsFromNote() {
        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(new Waypoint("", new Geopoint("N51 13.888 E007 03.444"), "", "", "", WaypointType.OWN));
        wpList.add(new Waypoint("", null, "", "", "", WaypointType.OWN));
        wpList.add(new Waypoint("", null, "Test", "", "", WaypointType.OWN));
        assertWaypointsParsed("Test N51 13.888 E007 03.444\nTest (NO-COORD)\n@Test (NO-COORD)", wpList);
    }

    /**
     * The second and the third waypoints have same names.
     * So expected size is 2.
     */
    public final void testUpdateWaypointsWithDuplicateEmptyCoordsFromNote() {
        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(new Waypoint("", new Geopoint("N51 13.888 E007 03.444"), "", "", "", WaypointType.OWN));
        wpList.add(new Waypoint("", null, "Test", "", "", WaypointType.OWN));
        assertWaypointsParsed("Test N51 13.888 E007 03.444\n@Test (NO-COORD)\n@Test (NO-COORD)", wpList);
    }

    /**
     * Waypoint with coordinates exist. Update waypointlist from note.
     * The first waypoint has auto generated name and coordinates.
     * The second waypoint has auto generated name and empty coordinates.
     * So expected size is 3:
     * - existing waypoint with original coordinates.
     * - new waypoint with auto generated name and coordinates.
     * - new waypoint with auto generated name and with empty coordinates.
     */
    public final void testUpdateExistingWaypointFromNote() {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);
        cache.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint userWaypoint = new Waypoint("Test", WaypointType.OWN, true);
        final Geopoint userWpGeopoint = new Geopoint(42.0, 10.0);
        userWaypoint.setCoords(userWpGeopoint);
        cache.addOrChangeWaypoint(userWaypoint, false);
        saveFreshCacheToDB(cache);

        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(new Waypoint("", userWpGeopoint, "Test", "", "", WaypointType.OWN));
        wpList.add(new Waypoint("", new Geopoint("N51 13.888 E007 03.444"), "Personal note 1", "", "", WaypointType.OWN));
        wpList.add(new Waypoint("", null, "Personal note 2", "", "", WaypointType.OWN));
        assertWaypointsParsed(cache, "Test N51 13.888 E007 03.444\nTest (NO-COORD)", wpList);

        removeCacheCompletely(geocode);
    }

    /**
     * Waypoint with coordinates exist. Update waypointlist from note.
     * The first waypoint has auto generated name and coordinates of original coordinates and note.
     * So expected size is 2:
     * - existing waypoint with original coordinates and updated note.
     * - new waypoint with auto generated name and coordinates.
     * Coordinates of first waypoint in note is changed. Update waypointlist from note.
     * The changed waypoint has auto generated name and coordinates.
     * So expected size is 3:
     * - existing waypoint with original coordinates and updated note.
     * - second waypoint with auto generated name and coordinates.
     * - new waypoint with auto generated name and coordinates.
     */
    public final void testUpdateAndAddNewWaypointFromNote() {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);
        cache.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint userWaypoint = new Waypoint("Personal note 1", WaypointType.OWN, true);
        final Geopoint userWpGeopoint = new Geopoint("N51 13.888 E007 03.444");
        userWaypoint.setCoords(userWpGeopoint);
        cache.addOrChangeWaypoint(userWaypoint, false);
        saveFreshCacheToDB(cache);

        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(createWaypointWithUserNote(new Geopoint("N51 13.888 E007 03.444"), "Personal note 1", "", "User Note", WaypointType.OWN));
        wpList.add(createWaypointWithUserNote(new Geopoint("N51 13.888 E007 03.666"), "Personal note 2", "", "", WaypointType.OWN));
        assertWaypointsParsed(cache, "N51 13.888 E007 03.444 \"User Note\" \n N51 13.888 E007 03.666", wpList);

        wpList.add(createWaypointWithUserNote(new Geopoint("N51 13.888 E007 03.555"), "Personal note 1", "", "User Note", WaypointType.OWN));
        assertWaypointsParsed(cache, "N51 13.888 E007 03.555 \"User Note\" \n N51 13.888 E007 03.666", wpList);

        removeCacheCompletely(geocode);
    }

    /**
     * Waypoint with coordinates exist. Update waypointlist from note.
     * The  waypoint has same name like the existing waypoint and a note and new coordinates.
     * Create new waypoint with new coordinates, otherwise, new coordinates will be lost.
     * So expected size is 2:
     * - existing waypoint with original coordinates and old note.
     * - new waypoint with new coordinates and new note.
     */
    public final void testUpdateExistingWaypointFromNoteWithSameNameAndCoords() {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);
        cache.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint userWaypoint = new Waypoint("Test", WaypointType.OWN, true);
        final Geopoint userWpGeopoint = new Geopoint(42.0, 10.0);
        userWaypoint.setCoords(userWpGeopoint);
        cache.addOrChangeWaypoint(userWaypoint, false);
        saveFreshCacheToDB(cache);

        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(createWaypointWithUserNote(userWpGeopoint, "Test", "", "", WaypointType.OWN));
        wpList.add(createWaypointWithUserNote(new Geopoint("N51 13.888 E007 03.444"), "Test", "", "NewNote", WaypointType.OWN));
        assertWaypointsParsed(cache, "@Test N51 13.888 E007 03.444 \"NewNote\"", wpList);

        removeCacheCompletely(geocode);
    }

    /**
     * Waypoint with empty coordinates exist. Update waypointlist from note.
     * The  waypoint has same name like the existing waypoint and a note and new coordinates.
     * So expected size is 1:
     * - existing waypoint with new coordinates and original note.
     */
    public final void testUpdateExistingWaypointWithEmptyCoordsFromNoteWithSameName() {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);
        cache.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint userWaypoint = new Waypoint("Test", WaypointType.OWN, true);
        userWaypoint.setCoords(null);
        userWaypoint.setUserNote("UserNote");
        cache.addOrChangeWaypoint(userWaypoint, false);
        saveFreshCacheToDB(cache);

        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(createWaypointWithUserNote(new Geopoint("N51 13.888 E007 03.444"), "Test", "", "UserNote", WaypointType.OWN));
        assertWaypointsParsed(cache, "@Test N51 13.888 E007 03.444 \"NewNote\"", wpList);

        removeCacheCompletely(geocode);
    }

    /**
     * Waypoint with empty coordinates exist. Update waypointlist from note.
     * The  waypoint has same name like the existing waypoint and a note and new coordinates.
     * So expected size is 1:
     * - existing waypoint with new coordinates and original note.
     */
    public final void testUpdateExistingWaypointFromNoteWithSameNameWithEmptyCoordsAndNewNote() {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);
        cache.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint userWaypoint = new Waypoint("Test", WaypointType.OWN, true);
        final Geopoint userWpGeopoint = new Geopoint(42.0, 10.0);
        userWaypoint.setCoords(userWpGeopoint);
        cache.addOrChangeWaypoint(userWaypoint, false);
        saveFreshCacheToDB(cache);

        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(createWaypointWithUserNote(userWpGeopoint, "Test", "", "NewNote", WaypointType.OWN));
        assertWaypointsParsed(cache, "@Test (NO-COORD) \"NewNote\"", wpList);

        removeCacheCompletely(geocode);
    }

    /**
     * Recreate waypoints from note. Waypoints with same name and different coordinates should be considered.
     * So expected size is 2.
     */
    public final void testUpdateWaypointFromParseableWaypointText() {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);
        cache.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint userWaypoint = new Waypoint("Personal note 1", WaypointType.OWN, true);
        final Geopoint userWpGeopoint = new Geopoint("N51 13.888 E007 03.444");
        userWaypoint.setCoords(userWpGeopoint);
        cache.addOrChangeWaypoint(userWaypoint, false);
        saveFreshCacheToDB(cache);

        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(userWaypoint);
        wpList.add(createWaypointWithUserNote(new Geopoint("N51 13.888 E007 03.555"), "Final", "", "", WaypointType.FINAL));
        wpList.add(createWaypointWithUserNote(new Geopoint("N51 13.888 E007 03.666"), "Final", "", "", WaypointType.FINAL));

        final String parseableText = WaypointParser.getParseableText(wpList, null, false);
        assertWaypointsParsed(cache, parseableText, wpList);

        removeCacheCompletely(geocode);
    }

    public final void testUpdateExistingWaypointWithFormulaFromNoteWithSameName() {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);

        cache.addOrChangeWaypoint(new Waypoint("", null, "Test 1", "", "", WaypointType.OWN), false);
        saveFreshCacheToDB(cache);

        final String note = "@Test 1 (O) " + WaypointParser.LEGACY_PARSING_COORD_FORMULA + " N 45° A.B(C+D)  E 9° (A-B).(2*D)EF | A = a + b |B=|a=2|b=| this is the description\n\"this shall NOT be part of the note\"";
        cache.setPersonalNote(note);
        cache.setPreventWaypointsFromNote(false);

        cache.addWaypointsFromNote();
        final List<Waypoint> waypoints = cache.getWaypoints();
        assertThat(waypoints.size()).isEqualTo(1);
        final Waypoint wp = waypoints.iterator().next();
        final String calcStateJson = wp.getCalcStateConfig();
        assertThat(calcStateJson).isNotNull();
        assertThat(calcStateJson).contains("N 45° A.B(C+D)");

        removeCacheCompletely(geocode);
    }

    /**
     * Waypoint with empty coordinates exist. Update waypointlist from note.
     * The  waypoint has same name like the existing waypoint, new coordinates and different type.
     * So expected size is 2:
     * - existing waypoint with empty coordinates.
     * - new waypoint with new coordinates.
     */
    public final void testUpdateExistingWaypointWithEmptyCoordsFromNoteWithSameNameDifferentType() {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);
        cache.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint userWaypoint = new Waypoint("Test", WaypointType.OWN, true);
        userWaypoint.setCoords(null);
        cache.addOrChangeWaypoint(userWaypoint, false);
        saveFreshCacheToDB(cache);

        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(new Waypoint("", null, "Test", "", "", WaypointType.OWN));
        wpList.add(new Waypoint("", null, "Test", "", "", WaypointType.FINAL));
        assertWaypointsParsed(cache, "@Test (F) (NO-COORD)", wpList);

        removeCacheCompletely(geocode);
    }

    /**
     * Waypoint with coordinates exist. Update waypointlist from note.
     * The waypoint has same prefix, but different name and coordinates.
     * So expected size is 1:
     * - existing waypoint with original name and coordinates.
     */
    public final void testUpdateExistingWaypointFromNoteWithSamePrefix() {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);
        cache.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint userWaypoint = new Waypoint("Test", WaypointType.OWN, true);
        final Geopoint userWpGeopoint = new Geopoint(42.0, 10.0);
        userWaypoint.setCoords(userWpGeopoint);
        userWaypoint.setPrefix("S1");
        cache.addOrChangeWaypoint(userWaypoint, false);
        saveFreshCacheToDB(cache);

        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(new Waypoint("", userWpGeopoint, "Test", "S1", "", WaypointType.OWN));
        assertWaypointsParsed(cache, "@[S1]ModTest N51 13.888 E007 03.444", wpList);

        removeCacheCompletely(geocode);
    }

    /**
     * Waypoint with empty coordinates exist. Update waypointlist from note.
     * The waypoint has same prefix, but different name and coordinates.
     * So expected size is 1:
     * - existing waypoint with original name, but new coordinates.
     */
    public final void testUpdateExistingWaypointWithEmptyCoordsFromNoteWithSamePrefix() {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);
        cache.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint userWaypoint = new Waypoint("Test", WaypointType.OWN, true);
        userWaypoint.setCoords(null);
        userWaypoint.setPrefix("S1");
        cache.addOrChangeWaypoint(userWaypoint, false);
        saveFreshCacheToDB(cache);

        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(new Waypoint("", new Geopoint("N51 13.888 E007 03.444"), "Test", "S1", "", WaypointType.OWN));
        assertWaypointsParsed(cache, "@[S1]ModTest N51 13.888 E007 03.444", wpList);

        removeCacheCompletely(geocode);
    }

    /**
     * Waypoint with empty coordinates exist. Update waypointlist from note.
     * The waypoint has prefix which does not exist, but name of existing waypoint.
     * So expected size is 2:
     * - existing waypoint unchanged
     * - new waypoint with prefix is added.
     */
    public final void testUpdateExistingWaypointWithEmptyCoordsFromNoteWithNewPrefix() {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);
        cache.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint userWaypoint = new Waypoint("Test", WaypointType.OWN, true);
        userWaypoint.setCoords(null);
        cache.addOrChangeWaypoint(userWaypoint, false);
        saveFreshCacheToDB(cache);

        final List<Waypoint> wpList = new ArrayList<>();
        wpList.add(new Waypoint("", null, "Test", "", "", WaypointType.OWN));
        wpList.add(new Waypoint("", new Geopoint("N51 13.888 E007 03.444"), "Test", "S1", "", WaypointType.OWN));
        assertWaypointsParsed(cache, "@[S1]Test N51 13.888 E007 03.444", wpList);

        removeCacheCompletely(geocode);
    }

    private static Waypoint createWaypointWithUserNote(final Geopoint point, final String name, final String prefix, final String userNote, final WaypointType type) {
        final Waypoint newWaypoint = new Waypoint("", point, name, prefix, "", type);
        newWaypoint.setUserNote(userNote);
        if (prefix.isEmpty()) {
            newWaypoint.setUserDefined();
        }
        return newWaypoint;
    }

    private void assertWaypointsParsed(final String note, final List<Waypoint> expectedWaypoints) {
        final Geocache cache = new Geocache();
        final String geocode = "Test" + System.nanoTime();
        cache.setGeocode(geocode);
        cache.setWaypoints(new ArrayList<>(), false);
        assertWaypointsParsed(cache, note, expectedWaypoints);
        cache.store(Collections.singleton(StoredList.TEMPORARY_LIST.id), null);
        removeCacheCompletely(geocode);
    }


    private void assertWaypointsParsed(final Geocache cache, final String note, final List<Waypoint> expectedWaypoints) {
        cache.setPersonalNote(note);
        cache.addWaypointsFromNote();
        final List<Waypoint> waypoints = cache.getWaypoints();
        assertThat(waypoints).isNotNull();
        assertThat(waypoints).hasSize(expectedWaypoints.size());

        final ListIterator<Waypoint> expectedWpIterator = expectedWaypoints.listIterator();
        for (int i = 0; i < expectedWaypoints.size(); i++) {
            final Waypoint waypoint = waypoints.get(i);
            final Waypoint expectedWaypoint = expectedWpIterator.next();
            assertThat(waypoint.getCoords()).isEqualTo(expectedWaypoint.getCoords());
            final String wpNote = expectedWaypoint.getUserNote();
            String wpName = expectedWaypoint.getName();
            if (wpName == null || wpName.isEmpty()) {
                wpName = CgeoApplication.getInstance().getString(R.string.cache_personal_note) + " " + (i + 1);
            }
            assertThat(waypoint.getName()).isEqualTo(wpName);
            assertThat(waypoint.getUserNote()).isEqualTo(wpNote);
        }
    }

    public static void testDisabledArchivedCombinations() {
        final Geocache cache = new Geocache();
        assertThat(cache.isDisabled()).isFalse();
        assertThat(cache.isArchived()).isFalse();
        cache.setDisabled(true);
        assertThat(cache.isDisabled()).isTrue();
        assertThat(cache.isArchived()).isFalse();
        cache.setArchived(true);
        assertThat(cache.isDisabled()).isFalse();
        assertThat(cache.isArchived()).isTrue();
        cache.setArchived(false);
        assertThat(cache.isDisabled()).isTrue();
        assertThat(cache.isArchived()).isFalse();
    }

    public static void testMergeDownloaded() {
        final Geocache previous = new Geocache();
        previous.setGeocode("GC12345");
        previous.setDetailed(true);
        previous.setDisabled(true);
        previous.setType(CacheType.TRADITIONAL);
        previous.setCoords(new Geopoint(40.0, 8.0));
        previous.setDescription("Test1");
        previous.setAttributes(Collections.singletonList("TestAttribute"));
        previous.setShortDescription("Short");
        previous.setHint("Hint");
        removeCacheCompletely(previous.getGeocode());

        final Geocache download = new Geocache();
        download.setGeocode("GC12345");
        download.setDetailed(true);
        download.setDisabled(false);
        download.setType(CacheType.MULTI);
        download.setCoords(new Geopoint(41.0, 9.0));
        download.setDescription("Test2");

        download.gatherMissingFrom(previous);

        assertThat(download.isDetailed()).as("merged detailed").isTrue();
        assertThat(download.isDisabled()).as("merged disabled").isFalse();
        assertThat(download.getType()).as("merged download").isEqualTo(CacheType.MULTI);
        assertThat(download.getCoords()).as("merged coordinates").isEqualTo(new Geopoint(41.0, 9.0));
        assertThat(download.getDescription()).as("merged description").isEqualTo("Test2");
        assertThat(download.getShortDescription()).as("merged short description").isEmpty();
        assertThat(download.getAttributes()).as("merged attributes").isEmpty();
        assertThat(download.getHint()).as("merged hint").isEmpty();
    }

    public static void testMergeDownloadedStored() {
        final Geocache stored = new Geocache();
        stored.setGeocode("GC12345");
        stored.setDetailed(true);
        stored.setDisabled(true);
        stored.setType(CacheType.TRADITIONAL);
        stored.setCoords(new Geopoint(40.0, 8.0));
        stored.setDescription("Test1");
        stored.setAttributes(Collections.singletonList("TestAttribute"));
        stored.setShortDescription("Short");
        stored.setHint("Hint");
        saveFreshCacheToDB(stored);

        final Geocache download = new Geocache();
        download.setGeocode("GC12345");
        download.setDetailed(true);
        download.setDisabled(false);
        download.setType(CacheType.MULTI);
        download.setCoords(new Geopoint(41.0, 9.0));
        download.setDescription("Test2");
        download.setAttributes(Collections.emptyList());

        download.gatherMissingFrom(stored);

        assertThat(download.isDetailed()).as("merged detailed").isTrue();
        assertThat(download.isDisabled()).as("merged disabled").isFalse();
        assertThat(download.getType()).as("merged download").isEqualTo(CacheType.MULTI);
        assertThat(download.getCoords()).as("merged coordinates").isEqualTo(new Geopoint(41.0, 9.0));
        assertThat(download.getDescription()).as("merged description").isEqualTo("Test2");
        assertThat(download.getShortDescription()).as("merged short description").isEmpty();
        assertThat(download.getAttributes()).as("merged attributes").isEmpty();
        assertThat(download.getHint()).as("merged hint").isEmpty();
    }

    public static void testMergeLocalUserModifiedCoordsNotServerSideModified() {
        final Geocache stored = new Geocache();
        stored.setGeocode("GC12345");
        stored.setUserModifiedCoords(true);
        stored.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint original = new Waypoint("ORIGINAL", WaypointType.ORIGINAL, false);
        original.setCoords(new Geopoint(42.0, 10.0));
        stored.addOrChangeWaypoint(original, false);
        saveFreshCacheToDB(stored);

        final Geocache download = new Geocache();
        download.setGeocode("GC12345");
        download.setCoords(new Geopoint(41.0, 9.0));
        download.setUserModifiedCoords(false);

        download.gatherMissingFrom(stored);

        assertThat(download.hasUserModifiedCoords()).as("merged user modified").isEqualTo(true);
        assertThat(download.getCoords()).as("merged coordinates").isEqualTo(new Geopoint(40.0, 8.0));
        assertThat(download.getOriginalWaypoint().getCoords()).as("merged original wp").isEqualTo(new Geopoint(41.0, 9.0));
    }

    public static void testMergeLocalUserModifiedCoordsAndServerSideModified() {
        final Geocache stored = new Geocache();
        stored.setGeocode("GC12345");
        stored.setUserModifiedCoords(true);
        stored.setCoords(new Geopoint(40.0, 8.0));
        final Waypoint originalStored = new Waypoint("ORIGINAL", WaypointType.ORIGINAL, false);
        originalStored.setCoords(new Geopoint(42.0, 10.0));
        stored.addOrChangeWaypoint(originalStored, false);
        saveFreshCacheToDB(stored);

        final Geocache download = new Geocache();
        download.setGeocode("GC12345");
        download.setCoords(new Geopoint(41.0, 9.0));
        final Waypoint originalDownloaded = new Waypoint("ORIGINAL", WaypointType.ORIGINAL, false);
        originalDownloaded.setCoords(new Geopoint(43.0, 11.0));
        download.addOrChangeWaypoint(originalDownloaded, false);
        download.setUserModifiedCoords(true);

        download.gatherMissingFrom(stored);

        assertThat(download.hasUserModifiedCoords()).as("merged user modified").isEqualTo(true);
        assertThat(download.getCoords()).as("merged coordinates").isEqualTo(new Geopoint(41.0, 9.0));
        assertThat(download.getWaypoints()).as("merged waypoints").hasSize(1);
        assertThat(download.getOriginalWaypoint().getCoords()).as("merged original wp").isEqualTo(new Geopoint(43.0, 11.0));
    }

    public static void testMergeLivemap() {
        final Geocache previous = new Geocache();
        previous.setGeocode("GC12345");
        previous.setDetailed(true);
        previous.setDisabled(true);
        previous.setType(CacheType.TRADITIONAL);
        previous.setCoords(new Geopoint(40.0, 8.0));
        removeCacheCompletely(previous.getGeocode());

        final Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");

        livemap.gatherMissingFrom(previous);

        assertThat(livemap.isDetailed()).as("merged detailed").isTrue();
        assertThat(livemap.isDisabled()).as("merged disabled").isTrue();
        assertThat(livemap.getType()).as("merged type").isEqualTo(CacheType.TRADITIONAL);
        assertThat(livemap.getCoords()).as("merged coordinates").isEqualToComparingFieldByField(new Geopoint(40.0, 8.0));
    }

    public static void testMergeLivemapStored() {
        final Geocache stored = new Geocache();
        stored.setGeocode("GC12345");
        stored.setDetailed(true);
        stored.setDisabled(true);
        stored.setType(CacheType.TRADITIONAL);
        stored.setCoords(new Geopoint(40.0, 8.0));
        saveFreshCacheToDB(stored);

        final Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");

        livemap.gatherMissingFrom(stored);

        assertThat(livemap.isDetailed()).as("merged detailed").isTrue();
        assertThat(livemap.isDisabled()).as("merged disabled").isTrue();
        assertThat(livemap.getType()).as("merged type").isEqualTo(CacheType.TRADITIONAL);
        assertThat(livemap.getCoords()).as("merged coordinates").isEqualToComparingFieldByField(new Geopoint(40.0, 8.0));
    }

    public static void testMergeLivemapBMSearched() {
        final Geocache bmsearched = new Geocache();
        bmsearched.setGeocode("GC12345");

        final Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");
        livemap.setCoords(new Geopoint(40.0, 8.0));

        livemap.gatherMissingFrom(bmsearched);

        assertThat(livemap.getCoords()).as("merged coordinates").isEqualTo(new Geopoint(40.0, 8.0));
    }

    /**
     * distance circle should be shown for some cache-types only for GC- and internal connector.
     */
    public final void testShowDistanceCircleForCaches() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC12345");

        cache.setType(CacheType.TRADITIONAL);
        assertThat(cache.applyDistanceRule()).as("show for traditional").isTrue();

        cache.setType(CacheType.USER_DEFINED);
        assertThat(cache.applyDistanceRule()).as("show for user-defined").isTrue();

        cache.setType(CacheType.MULTI);
        assertThat(cache.applyDistanceRule()).as("don't show for multi").isFalse();

        cache.setType(CacheType.MYSTERY);
        assertThat(cache.applyDistanceRule()).as("don't show for mystery").isFalse();

        cache.setGeocode("ZZ1234");
        cache.setType(CacheType.TRADITIONAL);
        assertThat(cache.applyDistanceRule()).as("show for Internal").isTrue();

        cache.setGeocode("OC12345");
        cache.setType(CacheType.TRADITIONAL);
        assertThat(cache.applyDistanceRule()).as("don't show for OC").isFalse();
    }

    /**
     * distance circle should be shown for some cache-types with user-modified-coordinates.
     */
    public final void testShowDistanceCircleForCacheWithUsermodifiedCoordinates() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC12345");
        cache.setUserModifiedCoords(true);

        cache.setType(CacheType.TRADITIONAL);
        assertThat(cache.applyDistanceRule()).as("show for traditional").isTrue();

        cache.setType(CacheType.USER_DEFINED);
        assertThat(cache.applyDistanceRule()).as("show for user-defined").isTrue();

        cache.setType(CacheType.MULTI);
        assertThat(cache.applyDistanceRule()).as("show for multi with user-modified coords").isTrue();

        cache.setType(CacheType.MYSTERY);
        assertThat(cache.applyDistanceRule()).as("show for mystery with user-modified coords").isTrue();
    }

    /**
     * distance circle must not be shown for archived caches.
     */
    public final void testShowDistanceCircleNotForArchivedCaches() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC12345");
        cache.setArchived(true);

        cache.setType(CacheType.TRADITIONAL);
        assertThat(cache.applyDistanceRule()).as("don't show for archived traditional").isFalse();

        cache.setType(CacheType.USER_DEFINED);
        assertThat(cache.applyDistanceRule()).as("don't show for archived user-defined").isFalse();

        cache.setType(CacheType.MULTI);
        assertThat(cache.applyDistanceRule()).as("don't show for archived multi").isFalse();

        cache.setType(CacheType.MYSTERY);
        assertThat(cache.applyDistanceRule()).as("don't show for archived mystery").isFalse();


        cache.setUserModifiedCoords(true);

        cache.setType(CacheType.MULTI);
        assertThat(cache.applyDistanceRule()).as("don't show for archived multi with user-modified coords").isFalse();

        cache.setType(CacheType.MYSTERY);
        assertThat(cache.applyDistanceRule()).as("don't show for archived mystery with user-modified coords").isFalse();
    }

    /**
     * distance circle should be shown for some waypoint-types.
     */
    public final void testShowDistanceCircleForWaypoint() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC12345");
        saveFreshCacheToDB(cache);

        final Waypoint finalWaypoint = new Waypoint("Final", WaypointType.FINAL, true);
        final Waypoint stageWaypoint = new Waypoint("Stage", WaypointType.STAGE, true);
        final Waypoint parkingWaypoint = new Waypoint("Stage", WaypointType.PARKING, true);
        final Waypoint ownWaypoint = new Waypoint("Stage", WaypointType.OWN, true);
        cache.addOrChangeWaypoint(finalWaypoint, false);
        cache.addOrChangeWaypoint(stageWaypoint, false);
        cache.addOrChangeWaypoint(parkingWaypoint, false);
        cache.addOrChangeWaypoint(ownWaypoint, false);

        assertThat(finalWaypoint.applyDistanceRule()).as("show for final waypoint").isTrue();
        assertThat(stageWaypoint.applyDistanceRule()).as("show for stage waypoint").isTrue();
        assertThat(parkingWaypoint.applyDistanceRule()).as("don't show for parking waypoint").isFalse();
        assertThat(ownWaypoint.applyDistanceRule()).as("don't show for own waypoint").isFalse();


        final Geocache ocCache = new Geocache();
        ocCache.setGeocode("OC12345");
        saveFreshCacheToDB(ocCache);

        final Waypoint ocFinalWaypoint = new Waypoint("Final", WaypointType.FINAL, true);
        ocCache.addOrChangeWaypoint(ocFinalWaypoint, false);

        assertThat(ocFinalWaypoint.applyDistanceRule()).as("don't show for final waypoint of OC-cache").isFalse();
    }

    /**
     * distance circle must not be shown for waypoints of archived caches.
     */
    public final void testShowDistanceCircleForArchivedWaypoint() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC12345");
        cache.setArchived(true);
        saveFreshCacheToDB(cache);

        final Waypoint finalWaypoint = new Waypoint("Final", WaypointType.FINAL, true);
        final Waypoint stageWaypoint = new Waypoint("Stage", WaypointType.STAGE, true);
        final Waypoint parkingWaypoint = new Waypoint("Stage", WaypointType.PARKING, true);
        final Waypoint ownWaypoint = new Waypoint("Stage", WaypointType.OWN, true);
        cache.addOrChangeWaypoint(finalWaypoint, false);
        cache.addOrChangeWaypoint(stageWaypoint, false);
        cache.addOrChangeWaypoint(parkingWaypoint, false);
        cache.addOrChangeWaypoint(ownWaypoint, false);

        assertThat(finalWaypoint.applyDistanceRule()).as("don't show for final waypoint").isFalse();
        assertThat(stageWaypoint.applyDistanceRule()).as("don't show for stage waypoint").isFalse();
        assertThat(parkingWaypoint.applyDistanceRule()).as("don't show for parking waypoint").isFalse();
        assertThat(ownWaypoint.applyDistanceRule()).as("don't show for own waypoint").isFalse();
    }

    public static void testNameForSorting() {
        final Geocache cache = new Geocache();
        cache.setName("GR8 01-01");
        assertThat(cache.getNameForSorting()).isEqualTo("GR000008 000001-000001");
    }

    public static void testGetPossibleLogTypes() throws Exception {
        final Geocache gcCache = new Geocache();
        gcCache.setGeocode("GC123");
        gcCache.setType(CacheType.WEBCAM);
        assertThat(gcCache.getPossibleLogTypes()).as("possible GC cache log types").contains(LogType.WEBCAM_PHOTO_TAKEN);

        final Geocache ocCache = new Geocache();
        ocCache.setGeocode("OC1234");
        ocCache.setType(CacheType.TRADITIONAL);
        assertThat(ocCache.getPossibleLogTypes()).as("traditional cache possible log types").doesNotContain(LogType.WEBCAM_PHOTO_TAKEN);
        assertThat(ocCache.getPossibleLogTypes()).as("OC cache possible log types").doesNotContain(LogType.NEEDS_MAINTENANCE);
    }

    public static void testLogTypeEventPast() throws Exception {
        final Calendar today = Calendar.getInstance();
        today.add(Calendar.DAY_OF_MONTH, -1);
        assertThat(createEventCache(today).getDefaultLogType()).isEqualTo(LogType.ATTENDED);
    }

    public static void testLogTypeEventToday() throws Exception {
        final Calendar today = Calendar.getInstance();
        assertThat(createEventCache(today).getDefaultLogType()).isEqualTo(LogType.ATTENDED);
    }

    public static void testLogTypeEventFuture() throws Exception {
        final Calendar today = Calendar.getInstance();
        today.add(Calendar.DAY_OF_MONTH, 1);
        assertThat(createEventCache(today).getDefaultLogType()).isEqualTo(LogType.WILL_ATTEND);
    }

    private static Geocache createEventCache(final Calendar calendar) {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.EVENT);
        cache.setHidden(calendar.getTime());
        return cache;
    }

    public static void testInventoryItems() {
        final Geocache cache = new Geocache();
        cache.setInventoryItems(5);
        assertThat(cache.getInventoryItems()).isEqualTo(5);
    }

    public static void testInventory() {
        final Geocache cache = new Geocache();
        final Trackable trackable = new Trackable();
        final List<Trackable> inventory = new ArrayList<>(Collections.singletonList(trackable));
        cache.setInventory(inventory);
        assertThat(cache.getInventory()).isEqualTo(inventory);
        assertThat(cache.getInventoryItems()).isEqualTo(inventory.size());
    }

    public static void testMergeInventory() {
        final Geocache cache = new Geocache();

        final List<Trackable> inventory1 = new ArrayList<>(4);

        // TB to be cleared
        final Trackable trackable1 = new Trackable();
        trackable1.setGeocode("TB1234");
        trackable1.setName("TB 1234");
        trackable1.setTrackingcode("TRACK 1");
        trackable1.forceSetBrand(TrackableBrand.TRAVELBUG);
        inventory1.add(trackable1);

        // TB to be updated
        final Trackable trackable2 = new Trackable();
        trackable2.setGeocode("GK1234"); // must be kept intact
        trackable2.setName("GK 1234 OLD NAME"); // must be overridden
        trackable2.setDistance(100F); // must be overridden
        trackable2.setTrackingcode("TRACK 2"); // must be kept intact
        trackable2.forceSetBrand(TrackableBrand.GEOKRETY);
        inventory1.add(trackable2);

        // TB to be removed
        final Trackable trackable3 = new Trackable();
        trackable3.setGeocode("GK6666");
        trackable3.forceSetBrand(TrackableBrand.GEOKRETY);
        inventory1.add(trackable3);

        // TB to be untouched
        final Trackable trackable4 = new Trackable();
        trackable4.setGeocode("UN0000");
        trackable4.forceSetBrand(TrackableBrand.UNKNOWN);
        inventory1.add(trackable4);

        cache.setInventory(inventory1);
        assertThat(cache.getInventory()).hasSize(4);
        assertThat(cache.getInventoryItems()).isEqualTo(4);

        final EnumSet<TrackableBrand> processedBrands = EnumSet.noneOf(TrackableBrand.class);
        processedBrands.add(TrackableBrand.GEOKRETY);
        // This brand must be cleared from result as it don't appear in "inventory2"
        processedBrands.add(TrackableBrand.TRAVELBUG);
        //deliberatly not declare TrackableBrand.UNKNOWN. They must not be removed from merge result

        final List<Trackable> inventory2 = new ArrayList<>(3);

        // new TB
        final Trackable trackable5 = new Trackable();
        trackable5.setGeocode("SW1234");
        trackable5.setName("SW 1234");
        trackable5.setDistance(100F);
        trackable5.forceSetBrand(TrackableBrand.UNKNOWN);
        inventory2.add(trackable5);

        // TB updater
        final Trackable trackable6 = new Trackable();
        trackable6.setGeocode("GK1234");
        trackable2.setName("GK 1234 _NEW_ NAME");
        trackable6.setDistance(200F);
        trackable6.forceSetBrand(TrackableBrand.GEOKRETY);
        inventory2.add(trackable6);

        // new TB
        final Trackable trackable7 = new Trackable();
        trackable7.setGeocode("GK4321");
        trackable7.setName("GK 4321");
        trackable7.setDistance(300F);
        trackable7.forceSetBrand(TrackableBrand.GEOKRETY);
        inventory2.add(trackable7);

        cache.mergeInventory(inventory2, processedBrands);

        assertThat(cache.getInventory()).hasSize(4);
        assertThat(cache.getInventoryItems()).isEqualTo(4);

        assertThat(cache.getInventory().get(0)).isEqualTo(trackable5);
        assertThat(cache.getInventory().get(0).getGeocode()).isEqualTo("SW1234");
        assertThat(cache.getInventory().get(0).getName()).isEqualTo("SW 1234");
        assertThat(cache.getInventory().get(0).getDistance()).isEqualTo(100F);
        assertThat(cache.getInventory().get(0).getOwner()).isNull();
        assertThat(cache.getInventory().get(0).getBrand()).isEqualTo(TrackableBrand.UNKNOWN);

        assertThat(cache.getInventory().get(1)).isEqualTo(trackable2);
        assertThat(cache.getInventory().get(1).getGeocode()).isEqualTo("GK1234");
        assertThat(cache.getInventory().get(1).getName()).isEqualTo("GK 1234 _NEW_ NAME");
        assertThat(cache.getInventory().get(1).getDistance()).isEqualTo(200F);
        assertThat(cache.getInventory().get(1).getTrackingcode()).isEqualTo("TRACK 2");
        assertThat(cache.getInventory().get(1).getOwner()).isNull();
        assertThat(cache.getInventory().get(1).getBrand()).isEqualTo(TrackableBrand.GEOKRETY);

        assertThat(cache.getInventory().get(2)).isEqualTo(trackable7);
        assertThat(cache.getInventory().get(2).getGeocode()).isEqualTo("GK4321");
        assertThat(cache.getInventory().get(2).getName()).isEqualTo("GK 4321");
        assertThat(cache.getInventory().get(2).getDistance()).isEqualTo(300F);
        assertThat(cache.getInventory().get(2).getOwner()).isNull();
        assertThat(cache.getInventory().get(2).getBrand()).isEqualTo(TrackableBrand.GEOKRETY);

        assertThat(cache.getInventory().get(3)).isEqualTo(trackable4);
        assertThat(cache.getInventory().get(3).getGeocode()).isEqualTo("UN0000");
        assertThat(cache.getInventory().get(3).getName()).isEqualTo("");
        assertThat(cache.getInventory().get(3).getDistance()).isEqualTo(-1F);
        assertThat(cache.getInventory().get(3).getOwner()).isNull();
        assertThat(cache.getInventory().get(3).getBrand()).isEqualTo(TrackableBrand.UNKNOWN);

        // test null inventory
        final Geocache cache1 = new Geocache();
        final List<Trackable> inventory3 = Collections.singletonList(trackable1);
        assertThat(cache1.getInventory()).isEmpty();

        cache1.mergeInventory(inventory3, EnumSet.of(TrackableBrand.TRAVELBUG));

        assertThat(cache1.getInventory()).hasSize(1);
        assertThat(cache1.getInventoryItems()).isEqualTo(1);

        assertThat(cache1.getInventory().get(0)).isEqualTo(trackable1);
        assertThat(cache1.getInventory().get(0).getGeocode()).isEqualTo("TB1234");
        assertThat(cache1.getInventory().get(0).getName()).isEqualTo("TB 1234");
        assertThat(cache1.getInventory().get(0).getDistance()).isEqualTo(-1F);
        assertThat(cache1.getInventory().get(0).getOwner()).isNull();
        assertThat(cache1.getInventory().get(0).getBrand()).isEqualTo(TrackableBrand.TRAVELBUG);
    }

    public static void testAddInventoryItem() {
        final Geocache cache = new Geocache();
        assertThat(cache.getInventory()).isEmpty();
        assertThat(cache.getInventoryItems()).isEqualTo(0);

        // 1st TB
        final Trackable trackable1 = new Trackable();
        trackable1.setGeocode("TB1234");
        trackable1.setName("FOO");
        trackable1.forceSetBrand(TrackableBrand.TRAVELBUG);

        cache.addInventoryItem(trackable1);
        assertThat(cache.getInventory()).hasSize(1);
        assertThat(cache.getInventoryItems()).isEqualTo(1);
        assertThat(cache.getInventory().get(0).getGeocode()).isEqualTo("TB1234");
        assertThat(cache.getInventory().get(0).getName()).isEqualTo("FOO");
        assertThat(cache.getInventory().get(0).getDistance()).isEqualTo(-1F);
        assertThat(cache.getInventory().get(0).getOwner()).isNull();
        assertThat(cache.getInventory().get(0).getBrand()).isEqualTo(TrackableBrand.TRAVELBUG);

        // TB to be updated
        final Trackable trackable2 = new Trackable();
        trackable2.setGeocode("TB1234");
        trackable2.setName("BAR");
        trackable2.setDistance(100);
        trackable2.forceSetBrand(TrackableBrand.TRAVELBUG);
        cache.addInventoryItem(trackable2);

        assertThat(cache.getInventory()).hasSize(1);
        assertThat(cache.getInventoryItems()).isEqualTo(1);
        assertThat(cache.getInventory().get(0).getGeocode()).isEqualTo("TB1234");
        assertThat(cache.getInventory().get(0).getName()).isEqualTo("BAR");
        assertThat(cache.getInventory().get(0).getDistance()).isEqualTo(100F);
        assertThat(cache.getInventory().get(0).getOwner()).isNull();
        assertThat(cache.getInventory().get(0).getBrand()).isEqualTo(TrackableBrand.TRAVELBUG);

        // TB to be added
        final Trackable trackable3 = new Trackable();
        trackable3.setGeocode("GK6666");
        trackable3.forceSetBrand(TrackableBrand.GEOKRETY);
        cache.addInventoryItem(trackable3);

        assertThat(cache.getInventory()).hasSize(2);
        assertThat(cache.getInventoryItems()).isEqualTo(2);
        assertThat(cache.getInventory().get(0).getGeocode()).isEqualTo("TB1234");
        assertThat(cache.getInventory().get(0).getName()).isEqualTo("BAR");
        assertThat(cache.getInventory().get(0).getDistance()).isEqualTo(100F);
        assertThat(cache.getInventory().get(0).getOwner()).isNull();
        assertThat(cache.getInventory().get(0).getBrand()).isEqualTo(TrackableBrand.TRAVELBUG);

        assertThat(cache.getInventory().get(1).getGeocode()).isEqualTo("GK6666");
        assertThat(cache.getInventory().get(1).getName()).isEqualTo("");
        assertThat(cache.getInventory().get(1).getDistance()).isEqualTo(-1F);
        assertThat(cache.getInventory().get(1).getOwner()).isNull();
        assertThat(cache.getInventory().get(1).getBrand()).isEqualTo(TrackableBrand.GEOKRETY);

    }

    public static void testIsOfflineNoList() {
        final Geocache cache = new Geocache();
        assertThat(cache.isOffline()).isFalse();
    }

    public static void testIsOfflineStandardList() {
        final Geocache cache = new Geocache();
        cache.setLists(Collections.singleton(StoredList.STANDARD_LIST_ID));
        assertThat(cache.isOffline()).isTrue();
    }

    public static void testIsOfflineTemporaryList() {
        final Geocache cache = new Geocache();
        cache.setLists(Collections.singleton(StoredList.TEMPORARY_LIST.id));
        assertThat(cache.isOffline()).isFalse();
    }

    public static void testIsOfflineMultipleLists() {
        final Geocache cache = new Geocache();
        cache.setLists(new HashSet<>(Arrays.asList(StoredList.TEMPORARY_LIST.id, 42)));
        assertThat(cache.isOffline()).isTrue();
    }

}
