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
 * Container for a turn restriction
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess

class TurnRestriction {
    public Boolean isPositive
    public Short exceptions

    public Int fromLon
    public Int fromLat

    public Int toLon
    public Int toLat

    public TurnRestriction next

    public static Boolean isTurnForbidden(final TurnRestriction first, final Int fromLon, final Int fromLat, final Int toLon, final Int toLat, final Boolean bikeMode, final Boolean carMode) {
        Boolean hasAnyPositive = false
        Boolean hasPositive = false
        Boolean hasNegative = false
        TurnRestriction tr = first
        while (tr != null) {
            if ((tr.exceptBikes() && bikeMode) || (tr.exceptMotorcars() && carMode)) {
                tr = tr.next
                continue
            }
            if (tr.fromLon == fromLon && tr.fromLat == fromLat) {
                if (tr.isPositive) {
                    hasAnyPositive = true
                }
                if (tr.toLon == toLon && tr.toLat == toLat) {
                    if (tr.isPositive) {
                        hasPositive = true
                    } else {
                        hasNegative = true
                    }
                }
            }
            tr = tr.next
        }
        return !hasPositive && (hasAnyPositive || hasNegative)
    }

    public Boolean exceptBikes() {
        return (exceptions & 1) != 0
    }

    public Boolean exceptMotorcars() {
        return (exceptions & 2) != 0
    }

    override     public String toString() {
        return "pos=" + isPositive + " fromLon=" + fromLon + " fromLat=" + fromLat + " toLon=" + toLon + " toLat=" + toLat
    }

}
