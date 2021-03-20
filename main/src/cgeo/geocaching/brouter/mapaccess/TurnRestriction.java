/**
 * Container for a turn restriction
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

public final class TurnRestriction {
    public boolean isPositive;
    public short exceptions;

    public int fromLon;
    public int fromLat;

    public int toLon;
    public int toLat;

    public TurnRestriction next;

    public static boolean isTurnForbidden(TurnRestriction first, int fromLon, int fromLat, int toLon, int toLat, boolean bikeMode, boolean carMode) {
        boolean hasAnyPositive = false;
        boolean hasPositive = false;
        boolean hasNegative = false;
        TurnRestriction tr = first;
        while (tr != null) {
            if ((tr.exceptBikes() && bikeMode) || (tr.exceptMotorcars() && carMode)) {
                tr = tr.next;
                continue;
            }
            if (tr.fromLon == fromLon && tr.fromLat == fromLat) {
                if (tr.isPositive) {
                    hasAnyPositive = true;
                }
                if (tr.toLon == toLon && tr.toLat == toLat) {
                    if (tr.isPositive) {
                        hasPositive = true;
                    } else {
                        hasNegative = true;
                    }
                }
            }
            tr = tr.next;
        }
        return !hasPositive && (hasAnyPositive || hasNegative);
    }

    public boolean exceptBikes() {
        return (exceptions & 1) != 0;
    }

    public boolean exceptMotorcars() {
        return (exceptions & 2) != 0;
    }

    @Override
    public String toString() {
        return "pos=" + isPositive + " fromLon=" + fromLon + " fromLat=" + fromLat + " toLon=" + toLon + " toLat=" + toLat;
    }

}
