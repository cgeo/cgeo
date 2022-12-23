/**
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;


public class OsmLink {
    /**
     * The description bitmap contains the waytags (valid for both directions)
     */
    public byte[] descriptionBitmap;

    /**
     * The geometry contains intermediate nodes, null for none (valid for both directions)
     */
    public byte[] geometry;

    // a link logically knows only its target, but for the reverse link, source and target are swapped
    protected OsmNode n1;
    protected OsmNode n2;

    // same for the next-link-for-node pointer: previous applies to the reverse link
    protected OsmLink previous;
    protected OsmLink next;

    private OsmLinkHolder reverselinkholder = null;
    private OsmLinkHolder firstlinkholder = null;

    protected OsmLink() {
    }

    public OsmLink(final OsmNode source, final OsmNode target) {
        n1 = source;
        n2 = target;
    }

    /**
     * Get the relevant target-node for the given source
     */
    public final OsmNode getTarget(final OsmNode source) {
        return n2 != source && n2 != null ? n2 : n1;
    /* if ( n2 != null && n2 != source )
    {
      return n2;
    }
    else if ( n1 != null && n1 != source )
    {
      return n1;
    }
    else
    {
      new Throwable( "ups" ).printStackTrace();
      throw new IllegalArgumentException( "internal error: getTarget: unknown source; " + source + " n1=" + n1 + " n2=" + n2 );
    } */
    }

    /**
     * Get the relevant next-pointer for the given source
     */
    public final OsmLink getNext(final OsmNode source) {
        return n2 != source && n2 != null ? next : previous;
    /* if ( n2 != null && n2 != source )
    {
      return next;
    }
    else if ( n1 != null && n1 != source )
    {
      return previous;
    }
    else
    {
      throw new IllegalArgumentException( "internal error: gextNext: unknown source" );
    } */
    }

    /**
     * Reset this link for the given direction
     */
    protected final OsmLink clear(final OsmNode source) {
        final OsmLink n;
        if (n2 != null && n2 != source) {
            n = next;
            next = null;
            n2 = null;
            firstlinkholder = null;
        } else if (n1 != null && n1 != source) {
            n = previous;
            previous = null;
            n1 = null;
            reverselinkholder = null;
        } else {
            throw new IllegalArgumentException("internal error: setNext: unknown source");
        }
        if (n1 == null && n2 == null) {
            descriptionBitmap = null;
            geometry = null;
        }
        return n;
    }

    public final void setFirstLinkHolder(final OsmLinkHolder holder, final OsmNode source) {
        if (n2 != null && n2 != source) {
            firstlinkholder = holder;
        } else if (n1 != null && n1 != source) {
            reverselinkholder = holder;
        } else {
            throw new IllegalArgumentException("internal error: setFirstLinkHolder: unknown source");
        }
    }

    public final OsmLinkHolder getFirstLinkHolder(final OsmNode source) {
        if (n2 != null && n2 != source) {
            return firstlinkholder;
        } else if (n1 != null && n1 != source) {
            return reverselinkholder;
        } else {
            throw new IllegalArgumentException("internal error: getFirstLinkHolder: unknown source");
        }
    }

    public final boolean isReverse(final OsmNode source) {
        return n1 != source && n1 != null;
    /* if ( n2 != null && n2 != source )
    {
      return false;
    }
    else if ( n1 != null && n1 != source )
   {
      return true;
    }
    else
    {
      throw new IllegalArgumentException( "internal error: isReverse: unknown source" );
    } */
    }

    public final boolean isBidirectional() {
        return n1 != null && n2 != null;
    }

    public final boolean isLinkUnused() {
        return n1 == null && n2 == null;
    }

    public final void addLinkHolder(final OsmLinkHolder holder, final OsmNode source) {
        final OsmLinkHolder firstHolder = getFirstLinkHolder(source);
        if (firstHolder != null) {
            holder.setNextForLink(firstHolder);
        }
        setFirstLinkHolder(holder, source);
    }

}
