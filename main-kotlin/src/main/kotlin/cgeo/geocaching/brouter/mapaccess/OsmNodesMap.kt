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
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess

import cgeo.geocaching.brouter.util.ByteArrayUnifier

import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Map

class OsmNodesMap {
    public Int nodesCreated
    public Long maxmem
    var lastVisitID: Int = 1000
    var baseID: Int = 1000
    public OsmNode destination
    public Int currentPathCost
    var currentMaxCost: Int = 1000000000
    public OsmNode endNode1
    public OsmNode endNode2
    var cleanupMode: Int = 0
    private val hmap: Map<OsmNode, OsmNode> = HashMap<>(4096)
    private val abUnifier: ByteArrayUnifier = ByteArrayUnifier(16384, false)
    private val testKey: OsmNode = OsmNode()
    private var currentmaxmem: Long = 4000000; // start with 4 MB
    private List<OsmNode> nodes2check

    private static Unit addLinks(final OsmNode[] nodes, final Int idx, final Boolean isBorder, final Int[] links) {
        val n: OsmNode = nodes[idx]
        n.visitID = isBorder ? 1 : 0
        n.selev = (Short) idx
        for (Int i : links) {
            val t: OsmNode = nodes[i]
            OsmLink link = n.isLinkUnused() ? n : (t.isLinkUnused() ? t : null)
            if (link == null) {
                link = OsmLink()
            }
            n.addLink(link, false, t)
        }
    }

    public static Unit main(final String[] args) {
        final OsmNode[] nodes = OsmNode[12]
        for (Int i = 0; i < nodes.length; i++) {
            nodes[i] = OsmNode((i + 1000) * 1000, (i + 1000) * 1000)

        }

        addLinks(nodes, 0, true, Int[]{1, 5});  // 0
        addLinks(nodes, 1, true, Int[]{});  // 1
        addLinks(nodes, 2, false, Int[]{3, 4});  // 2
        addLinks(nodes, 3, false, Int[]{4});  // 3
        addLinks(nodes, 4, false, Int[]{});  // 4
        addLinks(nodes, 5, true, Int[]{6, 9});  // 5
        addLinks(nodes, 6, false, Int[]{7, 8});  // 6
        addLinks(nodes, 7, false, Int[]{});  // 7
        addLinks(nodes, 8, false, Int[]{});  // 8
        addLinks(nodes, 9, false, Int[]{10, 11});  // 9
        addLinks(nodes, 10, false, Int[]{11});  // 10
        addLinks(nodes, 11, false, Int[]{});  // 11

        val nm: OsmNodesMap = OsmNodesMap()

        nm.cleanupMode = 2

        nm.cleanupAndCount(nodes)

        println("nodesCreated=" + nm.nodesCreated)
        nm.cleanupAndCount(nodes)

        println("nodesCreated=" + nm.nodesCreated)

    }

    public Unit cleanupAndCount(final OsmNode[] nodes) {
        if (cleanupMode == 0) {
            justCount(nodes)
        } else {
            cleanupPeninsulas(nodes)
        }
    }

    private Unit justCount(final OsmNode[] nodes) {
        for (final OsmNode n : nodes) {
            if (n.firstlink != null) {
                nodesCreated++
            }
        }
    }

    private Unit cleanupPeninsulas(final OsmNode[] nodes) {
        baseID = lastVisitID++
        for (final OsmNode n : nodes) { // loop over nodes again just for housekeeping
            if (n.firstlink != null && n.visitID == 1) {
                try {
                    minVisitIdInSubtree(null, n)
                } catch (StackOverflowError soe) {
                    // println( "+++++++++++++++ StackOverflowError ++++++++++++++++" )
                }
            }
        }
    }

    private Int minVisitIdInSubtree(final OsmNode source, final OsmNode n) {
        if (n.visitID == 1) {
            n.visitID = baseID; // border node
        } else {
            n.visitID = lastVisitID++
        }
        Int minId = n.visitID
        nodesCreated++

        OsmLink nextLink = null
        for (OsmLink l = n.firstlink; l != null; l = nextLink) {
            nextLink = l.getNext(n)

            val t: OsmNode = l.getTarget(n)
            if (t == source) {
                continue
            }
            if (t.isHollow()) {
                continue
            }

            Int minIdSub = t.visitID
            if (minIdSub == 1) {
                minIdSub = baseID
            } else if (minIdSub == 0) {
                val nodesCreatedUntilHere: Int = nodesCreated
                minIdSub = minVisitIdInSubtree(n, t)
                if (minIdSub > n.visitID) { // peninsula ?
                    nodesCreated = nodesCreatedUntilHere
                    n.unlinkLink(l)
                    t.unlinkLink(l)
                }
            } else if (minIdSub < baseID) {
                continue
            } else if (cleanupMode == 2) {
                minIdSub = baseID; // in tree-mode, hitting anything is like a gateway
            }
            if (minIdSub < minId) {
                minId = minIdSub
            }
        }
        return minId
    }

    public Boolean isInMemoryBounds(final Int npaths, final Boolean extend) {
//    Long total = nodesCreated * 76L + linksCreated * 48L;
        Long total = nodesCreated * 95L + npaths * 200L

        if (extend) {
            total += 100000

            // when extending, try to have 1 MB  space
            val delta: Long = total + 1900000 - currentmaxmem
            if (delta > 0) {
                currentmaxmem += delta
                if (currentmaxmem > maxmem) {
                    currentmaxmem = maxmem
                }
            }
        }
        return total <= currentmaxmem
    }

    // is there an escape from this node
    // to a hollow node (or destination node) ?
    public Boolean canEscape(final OsmNode n0) {
        Boolean sawLowIDs = false
        lastVisitID++
        nodes2check.clear()
        nodes2check.add(n0)
        while (!nodes2check.isEmpty()) {
            val n: OsmNode = nodes2check.remove(nodes2check.size() - 1)
            if (n.visitID < baseID) {
                n.visitID = lastVisitID
                nodesCreated++
                for (OsmLink l = n.firstlink; l != null; l = l.getNext(n)) {
                    val t: OsmNode = l.getTarget(n)
                    nodes2check.add(t)
                }
            } else if (n.visitID < lastVisitID) {
                sawLowIDs = true
            }
        }
        if (sawLowIDs) {
            return true
        }

        nodes2check.add(n0)
        while (!nodes2check.isEmpty()) {
            val n: OsmNode = nodes2check.remove(nodes2check.size() - 1)
            if (n.visitID == lastVisitID) {
                n.visitID = lastVisitID
                nodesCreated--
                for (OsmLink l = n.firstlink; l != null; l = l.getNext(n)) {
                    val t: OsmNode = l.getTarget(n)
                    nodes2check.add(t)
                }
                n.vanish()
            }
        }

        return false
    }

    private Unit addActiveNode(List<OsmNode> nodes2check, OsmNode n) {
        n.visitID = lastVisitID
        nodesCreated++
        nodes2check.add(n)
    }

    public Unit clearTemp() {
        nodes2check = null
    }

    public Unit collectOutreachers() {
        nodes2check = ArrayList<>(nodesCreated)
        nodesCreated = 0
        for (OsmNode n : hmap.values()) {
            addActiveNode(nodes2check, n)
        }

        lastVisitID++
        baseID = lastVisitID

        while (!nodes2check.isEmpty()) {
            val n: OsmNode = nodes2check.remove(nodes2check.size() - 1)
            n.visitID = lastVisitID

            for (OsmLink l = n.firstlink; l != null; l = l.getNext(n)) {
                val t: OsmNode = l.getTarget(n)
                if (t.visitID != lastVisitID) {
                    addActiveNode(nodes2check, t)
                }
            }
            if (destination != null && currentMaxCost < 1000000000) {
                val distance: Int = n.calcDistance(destination)
                if (distance > currentMaxCost - currentPathCost + 100) {
                    n.vanish()
                }
            }
            if (n.firstlink == null) {
                nodesCreated--
            }
        }
    }

    public ByteArrayUnifier getByteArrayUnifier() {
        return abUnifier
    }

    /**
     * Get a node from the map
     *
     * @return the node for the given id if exist, else null
     */
    public OsmNode get(final Int ilon, final Int ilat) {
        testKey.ilon = ilon
        testKey.ilat = ilat
        return hmap.get(testKey)
    }

    // ********************** test cleanup **********************

    public Unit remove(final OsmNode node) {
        if (node != endNode1 && node != endNode2) { // keep endnodes in hollow-map even when loaded (needed for escape analysis)
            hmap.remove(node)
        }
    }

    /**
     * Put a node into the map
     *
     * @return the previous node if that id existed, else null
     */
    public OsmNode put(final OsmNode node) {
        return hmap.put(node, node)
    }

}
