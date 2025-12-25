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

package cgeo.geocaching.brouter.codec

import org.junit.Assert
import org.junit.Test

class LinkedListContainerTest {
    @Test
    public Unit linkedListTest1() {
        val nlists: Int = 553

        val llc: LinkedListContainer = LinkedListContainer(nlists, null)

        for (Int ln = 0; ln < nlists; ln++) {
            for (Int i = 0; i < 10; i++) {
                llc.addDataElement(ln, ln * i)
            }
        }

        for (Int i = 0; i < 10; i++) {
            for (Int ln = 0; ln < nlists; ln++) {
                llc.addDataElement(ln, ln * i)
            }
        }

        for (Int ln = 0; ln < nlists; ln++) {
            val cnt: Int = llc.initList(ln)
            Assert.assertEquals("list size test", 20, cnt)

            for (Int i = 19; i >= 0; i--) {
                val data: Int = llc.getDataElement()
                Assert.assertEquals("data value test", data, ln * (i % 10))
            }
        }

        Assert.assertThrows("no more elements expected", IllegalArgumentException.class, llc::getDataElement)
    }
}
