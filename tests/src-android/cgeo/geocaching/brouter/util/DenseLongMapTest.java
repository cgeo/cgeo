package cgeo.geocaching.brouter.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public class DenseLongMapTest {
    @Test
    public void hashMapComparisonTest() {
        hashMapComparison(100000, 100000, 100000);
        hashMapComparison(100000, 100000, 13000000);
        Assert.assertTrue(true);
    }

    private void hashMapComparison(final int mapsize, final int trycount, final long keyrange) {
        final Random rand = new Random(12345);
        final HashMap<Long, Integer> hmap = new HashMap<>();
        final DenseLongMap dmap = new DenseLongMap(512);

        for (int i = 0; i < mapsize; i++) {
            final int value = i % 255;
            final long k = (long) (rand.nextDouble() * keyrange);
            final Long kk = new Long(k);

            hmap.put(kk, new Integer(value));
            dmap.put(k, value); // duplicate puts allowed!
        }

        for (int i = 0; i < trycount; i++) {
            final long k = (long) (rand.nextDouble() * keyrange);
            final Long kk = new Long(k);
            final Integer vv = hmap.get(kk);
            final int hvalue = vv == null ? -1 : vv.intValue();
            final int dvalue = dmap.getInt(k);

            if (hvalue != dvalue) {
                Assert.fail("value missmatch for key " + k + " hashmap=" + hvalue + " densemap=" + dvalue);
            }
        }
    }

    @Test
    public void oneBitTest() {
        final int keyrange = 300000;
        final int mapputs = 100000;
        final int trycount = 100000;

        final Random rand = new Random(12345);
        final HashSet<Long> hset = new HashSet<>();

        final DenseLongMap dmap = new DenseLongMap(512);
        for (int i = 0; i < mapputs; i++) {
            final long k = (long) (rand.nextDouble() * keyrange);
            hset.add(new Long(k));
            dmap.put(k, 0);
        }
        for (int i = 0; i < trycount; i++) {
            final long k = (long) (rand.nextDouble() * keyrange);
            final boolean hcontains = hset.contains(new Long(k));
            final boolean dcontains = dmap.getInt(k) == 0;

            if (hcontains != dcontains) {
                Assert.fail("value missmatch for key " + k + " hashset=" + hcontains + " densemap=" + dcontains);
            }
        }
    }

    // @Test - memory test disabled for load reasons
    public void memoryUsageTest() {
        final int keyrange = 32000000;
        final int mapputs = keyrange * 2;

        final Random rand = new Random(12345);
        final DenseLongMap dmap = new DenseLongMap(6);

        System.gc();
        final long mem1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        for (int i = 0; i < mapputs; i++) {
            final int value = i % 63;
            final long k = (long) (rand.nextDouble() * keyrange);
            dmap.put(k, value);
        }

        System.gc();
        final long mem2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        final long memusage = mem2 - mem1;

        if (memusage > (keyrange / 8) * 7) {
            Assert.fail("memory usage too high: " + memusage + " for keyrange " + keyrange);
        }

        // need to use the map again for valid memory measure
        Assert.assertTrue("out of range test", dmap.getInt(-1) == -1);
    }

}
