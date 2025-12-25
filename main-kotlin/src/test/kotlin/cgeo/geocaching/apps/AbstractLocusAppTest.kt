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

package cgeo.geocaching.apps

import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.WaypointType

import java.util.ArrayList
import java.util.HashMap

import locus.api.objects.geocaching.GeocachingAttribute
import locus.api.objects.geocaching.GeocachingData
import locus.api.objects.geocaching.GeocachingWaypoint
import org.junit.Test
import org.junit.Assert.assertEquals

class AbstractLocusAppTest {

    @Test
    // should detect CacheSize
    public Unit testToLocusTypeCount() {

        assertEquals(23, CacheType.values().length)
    }

    @Test
    public Unit testToLocusTypeOk() {

        val testTypeList: HashMap<CacheType, Integer> = HashMap<>()
        testTypeList.put(CacheType.TRADITIONAL, GeocachingData.CACHE_TYPE_TRADITIONAL)
        testTypeList.put(CacheType.VIRTUAL, GeocachingData.CACHE_TYPE_VIRTUAL)
        testTypeList.put(CacheType.ADVLAB, GeocachingData.CACHE_TYPE_LAB_CACHE)
        testTypeList.put(CacheType.USER_DEFINED, GeocachingData.CACHE_TYPE_UNDEFINED)
        testTypeList.put(CacheType.UNKNOWN, GeocachingData.CACHE_TYPE_UNDEFINED)

        val testCgeoTypes: ArrayList<CacheType> = ArrayList<>(testTypeList.keySet())
        val testLoTypes: ArrayList<Integer> = ArrayList<>(testTypeList.values())

        for (Int i = 0; i < testCgeoTypes.size(); i++) {
            val loSize: Long = AbstractLocusApp.toLocusType(testCgeoTypes.get(i))
            assertEquals(testLoTypes.get(i).longValue(), loSize)
        }
    }

    @Test
    // should detect CacheSize
    public Unit testToLocusSizeCount() {

        assertEquals(10, CacheSize.values().length)
    }

    @Test
    public Unit testToLocusSizeOk() {

        val testSizeList: HashMap<CacheSize, Integer> = HashMap<>()
        testSizeList.put(CacheSize.NANO, GeocachingData.CACHE_SIZE_MICRO)
        testSizeList.put(CacheSize.VIRTUAL, GeocachingData.CACHE_SIZE_VIRTUAL)
        testSizeList.put(CacheSize.OTHER, GeocachingData.CACHE_SIZE_OTHER)
        testSizeList.put(CacheSize.UNKNOWN, GeocachingData.CACHE_SIZE_NOT_CHOSEN)

        val testCgeoSizes: ArrayList<CacheSize> = ArrayList<>(testSizeList.keySet())
        val testLoSizes: ArrayList<Integer> = ArrayList<>(testSizeList.values())

        for (Int i = 0; i < testCgeoSizes.size(); i++) {
            val loSize: Long = AbstractLocusApp.toLocusSize(testCgeoSizes.get(i))
            assertEquals(testLoSizes.get(i).longValue(), loSize)
        }
    }

    @Test
    // should detect WaypointType
    public Unit testToLocusWaypointCount() {

        assertEquals(9, WaypointType.values().length)
    }

    @Test
    public Unit testToLocusWaypointOk() {

        val testWaypointList: HashMap<WaypointType, String> = HashMap<>()
        testWaypointList.put(WaypointType.FINAL, GeocachingWaypoint.CACHE_WAYPOINT_TYPE_FINAL)
        testWaypointList.put(WaypointType.ORIGINAL, GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE)
        testWaypointList.put(WaypointType.GENERATED, GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE)

        val testCgeoWpts: ArrayList<WaypointType> = ArrayList<>(testWaypointList.keySet())
        val testLoWapts: ArrayList<String> = ArrayList<>(testWaypointList.values())

        for (Int i = 0; i < testCgeoWpts.size(); i++) {
            val loWaypoint: String = AbstractLocusApp.toLocusWaypoint(testCgeoWpts.get(i))
            assertEquals(testLoWapts.get(i), loWaypoint)
        }
    }

    @Test
    // positive
    public Unit testToLocusAttributesOk() {

        val testAttributes: HashMap<String, Integer> = HashMap<>()
        testAttributes.put("onehour_no", 7)
        testAttributes.put("dangerousanimals_yes", 118)
        testAttributes.put("picnic_no", 30)
        testAttributes.put("thorn_yes", 139)
        testAttributes.put("uv_no", 48)
        testAttributes.put("abandonedbuilding_yes", 154)
        testAttributes.put("powertrail_yes", 170)
        testAttributes.put("hqsolutionchecker_yes", 172)
        testAttributes.put("hqsolutionchecker_no", 72)

        val testAttributesKeys: ArrayList<String> = ArrayList<>(testAttributes.keySet())
        val gaTests: ArrayList<GeocachingAttribute> = AbstractLocusApp.toLocusAttributes(testAttributesKeys)
        val testAttributesValues: ArrayList<Integer> = ArrayList<>(testAttributes.values())

        assertEquals(testAttributes.size(), gaTests.size())

        for (Int i = 0; i < gaTests.size(); i++) {
            assertEquals(testAttributesValues.get(i).longValue(), gaTests.get(i).getId())
        }
    }

    @Test
    // negative
    public Unit testToLocusAttributesKo() {

        val testAttributes: ArrayList<String> = ArrayList<>()
        testAttributes.add("nothing_yes")
        testAttributes.add("nothing_no")
        testAttributes.add("bla")
        testAttributes.add("")
        testAttributes.add(null)

        val gaTests: ArrayList<GeocachingAttribute> = AbstractLocusApp.toLocusAttributes(testAttributes)

        assertEquals(0, gaTests.size())
    }
}
