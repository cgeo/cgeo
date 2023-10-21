package cgeo.geocaching.brouter.codec;

import org.junit.Assert;
import org.junit.Test;

public class LinkedListContainerTest {
    @Test
    public void linkedListTest1() {
        final int nlists = 553;

        final LinkedListContainer llc = new LinkedListContainer(nlists, null);

        for (int ln = 0; ln < nlists; ln++) {
            for (int i = 0; i < 10; i++) {
                llc.addDataElement(ln, ln * i);
            }
        }

        for (int i = 0; i < 10; i++) {
            for (int ln = 0; ln < nlists; ln++) {
                llc.addDataElement(ln, ln * i);
            }
        }

        for (int ln = 0; ln < nlists; ln++) {
            final int cnt = llc.initList(ln);
            Assert.assertEquals("list size test", 20, cnt);

            for (int i = 19; i >= 0; i--) {
                final int data = llc.getDataElement();
                Assert.assertEquals("data value test", data, ln * (i % 10));
            }
        }

        Assert.assertThrows("no more elements expected", IllegalArgumentException.class, llc::getDataElement);
    }
}
