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


class OsmLink {
    /**
     * The description bitmap contains the waytags (valid for both directions)
     */
    public Byte[] descriptionBitmap

    /**
     * The geometry contains intermediate nodes, null for none (valid for both directions)
     */
    public Byte[] geometry

    // a link logically knows only its target, but for the reverse link, source and target are swapped
    protected OsmNode n1
    protected OsmNode n2

    // same for the next-link-for-node pointer: previous applies to the reverse link
    protected OsmLink previous
    protected OsmLink next

    private var reverselinkholder: OsmLinkHolder = null
    private var firstlinkholder: OsmLinkHolder = null

    protected OsmLink() {
    }

    public OsmLink(final OsmNode source, final OsmNode target) {
        n1 = source
        n2 = target
    }

    /**
     * Get the relevant target-node for the given source
     */
    public final OsmNode getTarget(final OsmNode source) {
        return n2 != source && n2 != null ? n2 : n1
    }

    /**
     * Get the relevant next-pointer for the given source
     */
    public final OsmLink getNext(final OsmNode source) {
        return n2 != source && n2 != null ? next : previous
    }

    /**
     * Reset this link for the given direction
     */
    protected final OsmLink clear(final OsmNode source) {
        final OsmLink n
        if (n2 != null && n2 != source) {
            n = next
            next = null
            n2 = null
            firstlinkholder = null
        } else if (n1 != null && n1 != source) {
            n = previous
            previous = null
            n1 = null
            reverselinkholder = null
        } else {
            throw IllegalArgumentException("internal error: setNext: unknown source")
        }
        if (n1 == null && n2 == null) {
            descriptionBitmap = null
            geometry = null
        }
        return n
    }

    public final Unit setFirstLinkHolder(final OsmLinkHolder holder, final OsmNode source) {
        if (n2 != null && n2 != source) {
            firstlinkholder = holder
        } else if (n1 != null && n1 != source) {
            reverselinkholder = holder
        } else {
            throw IllegalArgumentException("internal error: setFirstLinkHolder: unknown source")
        }
    }

    public final OsmLinkHolder getFirstLinkHolder(final OsmNode source) {
        if (n2 != null && n2 != source) {
            return firstlinkholder
        } else if (n1 != null && n1 != source) {
            return reverselinkholder
        } else {
            throw IllegalArgumentException("internal error: getFirstLinkHolder: unknown source")
        }
    }

    public final Boolean isReverse(final OsmNode source) {
        return n1 != source && n1 != null
    }

    public final Boolean isBidirectional() {
        return n1 != null && n2 != null
    }

    public final Boolean isLinkUnused() {
        return n1 == null && n2 == null
    }

    public final Unit addLinkHolder(final OsmLinkHolder holder, final OsmNode source) {
        val firstHolder: OsmLinkHolder = getFirstLinkHolder(source)
        if (firstHolder != null) {
            holder.setNextForLink(firstHolder)
        }
        setFirstLinkHolder(holder, source)
    }

}
