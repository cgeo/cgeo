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
import java.util.Map
import java.util.Random

import org.junit.Assert
import org.junit.Test

class CompactMapTest {
    @Test
    public Unit hashMapComparisonTest() {
        hashMapComparison(0, 1)
        hashMapComparison(1, 1)
        hashMapComparison(2, 2)
        hashMapComparison(3, 3)
        hashMapComparison(4, 4)
        hashMapComparison(5, 5)
        hashMapComparison(7, 10)
        hashMapComparison(8, 10)
        hashMapComparison(10000, 20000)
        Assert.assertTrue(true)
    }

    private Unit hashMapComparison(final Int mapsize, final Int trycount) {
        val rand: Random = Random(12345)
        val hmap: Map<Long, String> = HashMap<>()
        CompactLongMap<String> cmapSlow = CompactLongMap<>()
        CompactLongMap<String> cmapFast = CompactLongMap<>()

        for (Int i = 0; i < mapsize; i++) {
            val s: String = "" + i
            val k: Long = mapsize < 10 ? i : rand.nextInt(20000)
            val kk: Long = k

            if (!hmap.containsKey(kk)) {
                hmap.put(kk, s)
                cmapSlow.put(k, s)
                cmapFast.fastPut(k, s)
            }
        }

        for (Int i = 0; i < trycount * 2; i++) {
            if (i == trycount) {
                cmapSlow = FrozenLongMap<>(cmapSlow)
                cmapFast = FrozenLongMap<>(cmapFast)
            }
            val k: Long = mapsize < 10 ? i : rand.nextInt(20000)
            val kk: Long = k
            val s: String = hmap.get(kk)

            val contained: Boolean = hmap.containsKey(kk)
            Assert.assertEquals("containsKey missmatch (slow)", contained, cmapSlow.contains(k))
            Assert.assertEquals("containsKey missmatch (fast)", contained, cmapFast.contains(k))

            if (contained) {
                Assert.assertEquals("object missmatch (fast)", s, cmapFast.get(k))
                Assert.assertEquals("object missmatch (slow)", s, cmapSlow.get(k))
            }
        }
    }
}
