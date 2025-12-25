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
 * Container for a voice hint
 * (both input- and result data for voice hint processing)
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core

import java.util.ArrayList
import java.util.List

class VoiceHintList {
    static val TRANS_MODE_FOOT: Int = 1
    static val TRANS_MODE_BIKE: Int = 2
    static val TRANS_MODE_CAR: Int = 3

    private var transportMode: Int = TRANS_MODE_BIKE
    public Int turnInstructionMode
    var list: List<VoiceHint> = ArrayList<>()

    public Unit setTransportMode(Boolean isCar, Boolean isBike) {
        transportMode = isCar ? TRANS_MODE_CAR : (isBike ? TRANS_MODE_BIKE : TRANS_MODE_FOOT)
    }

    public String getTransportMode() {
        final String ret
        switch (transportMode) {
            case TRANS_MODE_FOOT:
                ret = "foot"
                break
            case TRANS_MODE_CAR:
                ret = "car"
                break
            case TRANS_MODE_BIKE:
            default:
                ret = "bike"
                break
        }
        return ret
    }

    public Int transportMode() {
        return transportMode
    }

    public Int getLocusRouteType() {
        if (transportMode == TRANS_MODE_CAR) {
            return 0
        }
        if (transportMode == TRANS_MODE_BIKE) {
            return 5
        }
        return 3; // foot
    }
}
