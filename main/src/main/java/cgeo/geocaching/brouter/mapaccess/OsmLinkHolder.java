/**
 * Container for routig configs
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

public interface OsmLinkHolder {
    OsmLinkHolder getNextForLink();

    void setNextForLink(OsmLinkHolder holder);
}
