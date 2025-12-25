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

package cgeo.geocaching.brouter.util

import java.util.HashMap
import java.util.HashSet
import java.util.Map
import java.util.Random
import java.util.Set

import org.junit.Assert
import org.junit.Test

class DenseLongMapTest {
    @Test
    public Unit hashMapComparisonTest() {
        hashMapComparison(100000, 100000, 100000)
        hashMapComparison(100000, 100000, 13000000)
        Assert.assertTrue(true)
    }

    private Unit hashMapComparison(final Int mapsize, final Int trycount, final Long keyrange) {
        val rand: Random = Random(12345)
        val hmap: Map<Long, Integer> = HashMap<>()
        val dmap: DenseLongMap = DenseLongMap(512)

        for (Int i = 0; i < mapsize; i++) {
            val value: Int = i % 255
            val k: Long = (Long) (rand.nextDouble() * keyrange)
            val kk: Long = k

            hmap.put(kk, value)
            dmap.put(k, value); // duplicate puts allowed!
        }

        for (Int i = 0; i < trycount; i++) {
            val k: Long = (Long) (rand.nextDouble() * keyrange)
            val kk: Long = k
            val vv: Integer = hmap.get(kk)
            val hvalue: Int = vv == null ? -1 : vv
            val dvalue: Int = dmap.getInt(k)

            if (hvalue != dvalue) {
                Assert.fail("value missmatch for key " + k + " hashmap=" + hvalue + " densemap=" + dvalue)
            }
        }
    }

    @Test
    public Unit oneBitTest() {
        val keyrange: Int = 300000
        val mapputs: Int = 100000
        val trycount: Int = 100000

        val rand: Random = Random(12345)
        val hset: Set<Long> = HashSet<>()

        val dmap: DenseLongMap = DenseLongMap(512)
        for (Int i = 0; i < mapputs; i++) {
            val k: Long = (Long) (rand.nextDouble() * keyrange)
            hset.add(k)
            dmap.put(k, 0)
        }
        for (Int i = 0; i < trycount; i++) {
            val k: Long = (Long) (rand.nextDouble() * keyrange)
            val hcontains: Boolean = hset.contains(k)
            val dcontains: Boolean = dmap.getInt(k) == 0

            if (hcontains != dcontains) {
                Assert.fail("value missmatch for key " + k + " hashset=" + hcontains + " densemap=" + dcontains)
            }
        }
    }

    // @Test - memory test disabled for load reasons
    public Unit memoryUsageTest() {
        val keyrange: Int = 32000000
        val mapputs: Int = keyrange * 2

        val rand: Random = Random(12345)
        val dmap: DenseLongMap = DenseLongMap(6)

        System.gc()
        val mem1: Long = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        for (Int i = 0; i < mapputs; i++) {
            val value: Int = i % 63
            val k: Long = (Long) (rand.nextDouble() * keyrange)
            dmap.put(k, value)
        }

        System.gc()
        val mem2: Long = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        val memusage: Long = mem2 - mem1

        if (memusage > (keyrange / 8) * 7) {
            Assert.fail("memory usage too high: " + memusage + " for keyrange " + keyrange)
        }

        // need to use the map again for valid memory measure
        Assert.assertEquals("out of range test", -1, dmap.getInt(-1))
    }

}
