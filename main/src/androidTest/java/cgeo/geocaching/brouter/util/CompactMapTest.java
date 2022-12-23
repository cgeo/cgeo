package cgeo.geocaching.brouter.util;

import java.util.HashMap;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public class CompactMapTest {
    @Test
    public void hashMapComparisonTest() {
        hashMapComparison(0, 1);
        hashMapComparison(1, 1);
        hashMapComparison(2, 2);
        hashMapComparison(3, 3);
        hashMapComparison(4, 4);
        hashMapComparison(5, 5);
        hashMapComparison(7, 10);
        hashMapComparison(8, 10);
        hashMapComparison(10000, 20000);
        Assert.assertTrue(true);
    }

    private void hashMapComparison(final int mapsize, final int trycount) {
        final Random rand = new Random(12345);
        final HashMap<Long, String> hmap = new HashMap<>();
        CompactLongMap<String> cmapSlow = new CompactLongMap<>();
        CompactLongMap<String> cmapFast = new CompactLongMap<>();

        for (int i = 0; i < mapsize; i++) {
            final String s = "" + i;
            final long k = mapsize < 10 ? i : rand.nextInt(20000);
            final Long kk = new Long(k);

            if (!hmap.containsKey(kk)) {
                hmap.put(kk, s);
                cmapSlow.put(k, s);
                cmapFast.fastPut(k, s);
            }
        }

        for (int i = 0; i < trycount * 2; i++) {
            if (i == trycount) {
                cmapSlow = new FrozenLongMap<>(cmapSlow);
                cmapFast = new FrozenLongMap<>(cmapFast);
            }
            final long k = mapsize < 10 ? i : rand.nextInt(20000);
            final Long kk = new Long(k);
            final String s = hmap.get(kk);

            final boolean contained = hmap.containsKey(kk);
            Assert.assertTrue("containsKey missmatch (slow)", contained == cmapSlow.contains(k));
            Assert.assertTrue("containsKey missmatch (fast)", contained == cmapFast.contains(k));

            if (contained) {
                Assert.assertEquals("object missmatch (fast)", s, cmapFast.get(k));
                Assert.assertEquals("object missmatch (slow)", s, cmapSlow.get(k));
            }
        }
    }
}
