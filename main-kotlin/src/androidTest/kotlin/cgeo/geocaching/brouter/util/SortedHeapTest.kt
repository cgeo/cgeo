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

import java.util.Random

import org.junit.Assert
import org.junit.Test

class SortedHeapTest {
    @Test
    public Unit sortedHeapTest1() {
        val sh: SortedHeap<String> = SortedHeap<>()
        val rnd: Random = Random()
        for (Int i = 0; i < 100000; i++) {
            Int val = rnd.nextInt(1000000)
            sh.add(val, "" + val)
            val = rnd.nextInt(1000000)
            sh.add(val, "" + val)
            sh.popLowestKeyValue()
        }

        Int cnt = 0
        Int lastval = 0
        for (; ; ) {
            val s: String = sh.popLowestKeyValue()
            if (s == null) {
                break
            }
            cnt++
            val val: Int = Integer.parseInt(s)
            Assert.assertTrue("sorting test", val >= lastval)
            lastval = val
        }
        Assert.assertEquals("total count test", 100000, cnt)

    }

    @Test
    public Unit sortedHeapTest2() {
        val sh: SortedHeap<String> = SortedHeap<>()
        for (Int i = 0; i < 100000; i++) {
            sh.add(i, "" + i)
        }

        Int cnt = 0
        Int expected = 0
        for (; ; ) {
            val s: String = sh.popLowestKeyValue()
            if (s == null) {
                break
            }
            cnt++
            val val: Int = Integer.parseInt(s)
            Assert.assertEquals("sequence test", val, expected)
            expected++
        }
        Assert.assertEquals("total count test", 100000, cnt)

    }
}
