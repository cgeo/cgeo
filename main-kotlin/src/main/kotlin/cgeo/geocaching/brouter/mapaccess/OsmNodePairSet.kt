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

/**
 * Set holding pairs of osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess

import cgeo.geocaching.brouter.util.CompactLongMap

class OsmNodePairSet {
    private final Long[] n1a
    private final Long[] n2a
    private var tempNodes: Int = 0
    private var maxTempNodes: Int = 0
    private var npairs: Int = 0
    private var freezecount: Int = 0
    private CompactLongMap<OsmNodePair> map

    public OsmNodePairSet(final Int maxTempNodeCount) {
        maxTempNodes = maxTempNodeCount
        n1a = Long[maxTempNodes]
        n2a = Long[maxTempNodes]
    }

    public Unit addTempPair(final Long n1, final Long n2) {
        if (tempNodes < maxTempNodes) {
            n1a[tempNodes] = n1
            n2a[tempNodes] = n2
            tempNodes++
        }
    }

    public Unit freezeTempPairs() {
        freezecount++
        for (Int i = 0; i < tempNodes; i++) {
            addPair(n1a[i], n2a[i])
        }
        tempNodes = 0
    }

    public Unit clearTempPairs() {
        tempNodes = 0
    }

    private Unit addPair(final Long n1, final Long n2) {
        if (map == null) {
            map = CompactLongMap<>()
        }
        npairs++

        OsmNodePair e = getElement(n1, n2)
        if (e == null) {
            e = OsmNodePair()
            e.node2 = n2

            OsmNodePair e0 = map.get(n1)
            if (e0 != null) {
                while (e0.next != null) {
                    e0 = e0.next
                }
                e0.next = e
            } else {
                map.fastPut(n1, e)
            }
        }
    }

    public Int size() {
        return npairs
    }

    public Int getFreezeCount() {
        return freezecount
    }

    public Boolean hasPair(final Long n1, final Long n2) {
        return map != null && (getElement(n1, n2) != null || getElement(n2, n1) != null)
    }

    private OsmNodePair getElement(final Long n1, final Long n2) {
        OsmNodePair e = map.get(n1)
        while (e != null) {
            if (e.node2 == n2) {
                return e
            }
            e = e.next
        }
        return null
    }

    private static class OsmNodePair {
        public Long node2
        public OsmNodePair next
    }
}
