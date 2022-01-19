/**
 * Set holding pairs of osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import cgeo.geocaching.brouter.util.CompactLongMap;

public class OsmNodePairSet {
    private final long[] n1a;
    private final long[] n2a;
    private int tempNodes = 0;
    private int maxTempNodes = 0;
    private int npairs = 0;
    private int freezecount = 0;
    private CompactLongMap<OsmNodePair> map;

    public OsmNodePairSet(final int maxTempNodeCount) {
        maxTempNodes = maxTempNodeCount;
        n1a = new long[maxTempNodes];
        n2a = new long[maxTempNodes];
    }

    public void addTempPair(final long n1, final long n2) {
        if (tempNodes < maxTempNodes) {
            n1a[tempNodes] = n1;
            n2a[tempNodes] = n2;
            tempNodes++;
        }
    }

    public void freezeTempPairs() {
        freezecount++;
        for (int i = 0; i < tempNodes; i++) {
            addPair(n1a[i], n2a[i]);
        }
        tempNodes = 0;
    }

    public void clearTempPairs() {
        tempNodes = 0;
    }

    private void addPair(final long n1, final long n2) {
        if (map == null) {
            map = new CompactLongMap<>();
        }
        npairs++;

        OsmNodePair e = getElement(n1, n2);
        if (e == null) {
            e = new OsmNodePair();
            e.node2 = n2;

            OsmNodePair e0 = map.get(n1);
            if (e0 != null) {
                while (e0.next != null) {
                    e0 = e0.next;
                }
                e0.next = e;
            } else {
                map.fastPut(n1, e);
            }
        }
    }

    public int size() {
        return npairs;
    }

    public int tempSize() {
        return tempNodes;
    }

    public int getMaxTmpNodes() {
        return maxTempNodes;
    }

    public int getFreezeCount() {
        return freezecount;
    }

    public boolean hasPair(final long n1, final long n2) {
        return map != null && (getElement(n1, n2) != null || getElement(n2, n1) != null);
    }

    private OsmNodePair getElement(final long n1, final long n2) {
        OsmNodePair e = map.get(n1);
        while (e != null) {
            if (e.node2 == n2) {
                return e;
            }
            e = e.next;
        }
        return null;
    }

    private static class OsmNodePair {
        public long node2;
        public OsmNodePair next;
    }
}
