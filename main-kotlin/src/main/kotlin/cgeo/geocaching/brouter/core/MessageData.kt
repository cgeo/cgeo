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
 * Information on matched way point
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core


class MessageData : Cloneable {
    var linkdist: Int = 0
    var linkelevationcost: Int = 0
    var linkturncost: Int = 0
    var linknodecost: Int = 0
    var linkinitcost: Int = 0

    public Float costfactor
    public Int priorityclassifier
    public Int classifiermask
    public Float turnangle
    public String wayKeyValues
    public String nodeKeyValues

    public Int lon
    public Int lat
    public Short ele

    public Float time
    public Float energy

    // speed profile
    var vmaxExplicit: Int = -1
    var vmax: Int = -1
    var vmin: Int = -1
    var vnode0: Int = 999
    var vnode1: Int = 999
    var extraTime: Int = 0

    public String toMessage() {
        if (wayKeyValues == null) {
            return null
        }

        val iCost: Int = (Int) (costfactor * 1000 + 0.5f)
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
                + "\t" + ((Int) time)
                + "\t" + ((Int) energy)
    }

    public Unit add(final MessageData d) {
        linkdist += d.linkdist
        linkelevationcost += d.linkelevationcost
        linkturncost += d.linkturncost
        linknodecost += d.linknodecost
        linkinitcost += d.linkinitcost
    }

    public MessageData copy() {
        try {
            return (MessageData) clone()
        } catch (CloneNotSupportedException e) {
            throw RuntimeException(e)
        }
    }

    override     public String toString() {
        return "dist=" + linkdist + " prio=" + priorityclassifier + " turn=" + turnangle
    }

    public Int getPrio() {
        return priorityclassifier
    }

    public Boolean isBadOneway() {
        return (classifiermask & 1) != 0
    }

    public Boolean isRoundabout() {
        return (classifiermask & 4) != 0
    }

    public Boolean isLinktType() {
        return (classifiermask & 8) != 0
    }

    public Boolean isGoodForCars() {
        return (classifiermask & 16) != 0
    }

}
