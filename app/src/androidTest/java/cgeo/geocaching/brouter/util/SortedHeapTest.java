package cgeo.geocaching.brouter.util;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public class SortedHeapTest {
    @Test
    public void sortedHeapTest1() {
        final SortedHeap<String> sh = new SortedHeap<>();
        final Random rnd = new Random();
        for (int i = 0; i < 100000; i++) {
            int val = rnd.nextInt(1000000);
            sh.add(val, "" + val);
            val = rnd.nextInt(1000000);
            sh.add(val, "" + val);
            sh.popLowestKeyValue();
        }

        int cnt = 0;
        int lastval = 0;
        for (; ; ) {
            final String s = sh.popLowestKeyValue();
            if (s == null) {
                break;
            }
            cnt++;
            final int val = Integer.parseInt(s);
            Assert.assertTrue("sorting test", val >= lastval);
            lastval = val;
        }
        Assert.assertTrue("total count test", cnt == 100000);

    }

    @Test
    public void sortedHeapTest2() {
        final SortedHeap<String> sh = new SortedHeap<>();
        for (int i = 0; i < 100000; i++) {
            sh.add(i, "" + i);
        }

        int cnt = 0;
        int expected = 0;
        for (; ; ) {
            final String s = sh.popLowestKeyValue();
            if (s == null) {
                break;
            }
            cnt++;
            final int val = Integer.parseInt(s);
            Assert.assertTrue("sequence test", val == expected);
            expected++;
        }
        Assert.assertTrue("total count test", cnt == 100000);

    }
}
