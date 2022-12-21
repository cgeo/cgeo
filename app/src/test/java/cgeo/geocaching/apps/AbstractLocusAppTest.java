package cgeo.geocaching.apps;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;

import java.util.ArrayList;
import java.util.HashMap;

import locus.api.objects.geocaching.GeocachingAttribute;
import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingWaypoint;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AbstractLocusAppTest {

    @Test
    // should detect new CacheSize
    public void testToLocusTypeCount() {

        assertEquals(23, CacheType.values().length);
    }

    @Test
    public void testToLocusTypeOk() {

        final HashMap<CacheType, Integer> testTypeList = new HashMap<>();
        testTypeList.put(CacheType.TRADITIONAL, GeocachingData.CACHE_TYPE_TRADITIONAL);
        testTypeList.put(CacheType.VIRTUAL, GeocachingData.CACHE_TYPE_VIRTUAL);
        testTypeList.put(CacheType.USER_DEFINED, GeocachingData.CACHE_TYPE_UNDEFINED);
        testTypeList.put(CacheType.UNKNOWN, GeocachingData.CACHE_TYPE_UNDEFINED);

        final ArrayList<CacheType> testCgeoTypes = new ArrayList<>(testTypeList.keySet());
        final ArrayList<Integer> testLoTypes = new ArrayList<>(testTypeList.values());

        for (int i = 0; i < testCgeoTypes.size(); i++) {
            final long loSize = AbstractLocusApp.toLocusType(testCgeoTypes.get(i));
            assertEquals(testLoTypes.get(i).longValue(), loSize);
        }
    }

    @Test
    // should detect new CacheSize
    public void testToLocusSizeCount() {

        assertEquals(10, CacheSize.values().length);
    }

    @Test
    public void testToLocusSizeOk() {

        final HashMap<CacheSize, Integer> testSizeList = new HashMap<>();
        testSizeList.put(CacheSize.NANO, GeocachingData.CACHE_SIZE_MICRO);
        testSizeList.put(CacheSize.VIRTUAL, GeocachingData.CACHE_SIZE_OTHER);
        testSizeList.put(CacheSize.UNKNOWN, GeocachingData.CACHE_SIZE_NOT_CHOSEN);

        final ArrayList<CacheSize> testCgeoSizes = new ArrayList<>(testSizeList.keySet());
        final ArrayList<Integer> testLoSizes = new ArrayList<>(testSizeList.values());

        for (int i = 0; i < testCgeoSizes.size(); i++) {
            final long loSize = AbstractLocusApp.toLocusSize(testCgeoSizes.get(i));
            assertEquals(testLoSizes.get(i).longValue(), loSize);
        }
    }

    @Test
    // should detect new WaypointType
    public void testToLocusWaypointCount() {

        assertEquals(9, WaypointType.values().length);
    }

    @Test
    public void testToLocusWaypointOk() {

        final HashMap<WaypointType, String> testWaypointList = new HashMap<>();
        testWaypointList.put(WaypointType.FINAL, GeocachingWaypoint.CACHE_WAYPOINT_TYPE_FINAL);
        testWaypointList.put(WaypointType.ORIGINAL, GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE);

        final ArrayList<WaypointType> testCgeoWpts = new ArrayList<>(testWaypointList.keySet());
        final ArrayList<String> testLoWapts = new ArrayList<>(testWaypointList.values());

        for (int i = 0; i < testCgeoWpts.size(); i++) {
            final String loWaypoint = AbstractLocusApp.toLocusWaypoint(testCgeoWpts.get(i));
            assertEquals(testLoWapts.get(i), loWaypoint);
        }
    }

    @Test
    // positive
    public void testToLocusAttributesOk() {

        final HashMap<String, Integer> testAttributes = new HashMap<>();
        testAttributes.put("onehour_no", 7);
        testAttributes.put("dangerousanimals_yes", 118);
        testAttributes.put("picnic_no", 30);
        testAttributes.put("thorn_yes", 139);
        testAttributes.put("uv_no", 48);
        testAttributes.put("abandonedbuilding_yes", 154);
        testAttributes.put("powertrail_yes", 170);
        testAttributes.put("hqsolutionchecker_yes", 172);
        testAttributes.put("hqsolutionchecker_no", 72);

        final ArrayList<String> testAttributesKeys = new ArrayList<>(testAttributes.keySet());
        final ArrayList<GeocachingAttribute> gaTests = AbstractLocusApp.toLocusAttributes(testAttributesKeys);
        final ArrayList<Integer> testAttributesValues = new ArrayList<>(testAttributes.values());

        assertEquals(testAttributes.size(), gaTests.size());

        for (int i = 0; i < gaTests.size(); i++) {
            assertEquals(testAttributesValues.get(i).longValue(), gaTests.get(i).getId());
        }
    }

    @Test
    // negative
    public void testToLocusAttributesKo() {

        final ArrayList<String> testAttributes = new ArrayList<>();
        testAttributes.add("nothing_yes");
        testAttributes.add("nothing_no");
        testAttributes.add("bla");
        testAttributes.add("");
        testAttributes.add(null);

        final ArrayList<GeocachingAttribute> gaTests = AbstractLocusApp.toLocusAttributes(testAttributes);

        assertEquals(0, gaTests.size());
    }
}
