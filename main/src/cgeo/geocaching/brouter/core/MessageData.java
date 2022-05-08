/**
 * Information on matched way point
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;


final class MessageData implements Cloneable {
    public int linkdist = 0;
    public int linkelevationcost = 0;
    public int linkturncost = 0;
    public int linknodecost = 0;
    public int linkinitcost = 0;

    public float costfactor;
    public int priorityclassifier;
    public int classifiermask;
    public float turnangle;
    public String wayKeyValues;
    public String nodeKeyValues;

    public int lon;
    public int lat;
    public short ele;

    public float time;
    public float energy;

    // speed profile
    public int vmaxExplicit = -1;
    public int vmax = -1;
    public int vmin = -1;
    public int vnode0 = 999;
    public int vnode1 = 999;
    public int extraTime = 0;

    public String toMessage() {
        if (wayKeyValues == null) {
            return null;
        }

        final int iCost = (int) (costfactor * 1000 + 0.5f);
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
                + "\t" + (nodeKeyValues == null ? "" : nodeKeyValues)
                + "\t" + ((int) time)
                + "\t" + ((int) energy);
    }

    public void add(final MessageData d) {
        linkdist += d.linkdist;
        linkelevationcost += d.linkelevationcost;
        linkturncost += d.linkturncost;
        linknodecost += d.linknodecost;
        linkinitcost += d.linkinitcost;
    }

    public MessageData copy() {
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
