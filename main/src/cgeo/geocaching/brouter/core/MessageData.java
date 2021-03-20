/**
 * Information on matched way point
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;


final class MessageData implements Cloneable {
    int linkdist = 0;
    int linkelevationcost = 0;
    int linkturncost = 0;
    int linknodecost = 0;
    int linkinitcost = 0;

    float costfactor;
    int priorityclassifier;
    int classifiermask;
    float turnangle;
    String wayKeyValues;
    String nodeKeyValues;

    int lon;
    int lat;
    short ele;

    float time;
    float energy;

    // speed profile
    int vmaxExplicit = -1;
    int vmax = -1;
    int vmin = -1;
    int vnode0 = 999;
    int vnode1 = 999;
    int extraTime = 0;

    String toMessage() {
        if (wayKeyValues == null) {
            return null;
        }

        int iCost = (int) (costfactor * 1000 + 0.5f);
        return (lon - 180000000) + "\t"
            + (lat - 90000000) + "\t"
            + ele / 4 + "\t"
            + linkdist + "\t"
            + iCost + "\t"
            + linkelevationcost
            + "\t" + linkturncost
            + "\t" + linknodecost
            + "\t" + linkinitcost
            + "\t" + wayKeyValues
            + "\t" + (nodeKeyValues == null ? "" : nodeKeyValues);
    }

    void add(MessageData d) {
        linkdist += d.linkdist;
        linkelevationcost += d.linkelevationcost;
        linkturncost += d.linkturncost;
        linknodecost += d.linknodecost;
        linkinitcost += d.linkinitcost;
    }

    MessageData copy() {
        try {
            return (MessageData) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "dist=" + linkdist + " prio=" + priorityclassifier + " turn=" + turnangle;
    }

    public int getPrio() {
        return priorityclassifier;
    }

    public boolean isBadOneway() {
        return (classifiermask & 1) != 0;
    }

    public boolean isGoodOneway() {
        return (classifiermask & 2) != 0;
    }

    public boolean isRoundabout() {
        return (classifiermask & 4) != 0;
    }

    public boolean isLinktType() {
        return (classifiermask & 8) != 0;
    }

    public boolean isGoodForCars() {
        return (classifiermask & 16) != 0;
    }

}
