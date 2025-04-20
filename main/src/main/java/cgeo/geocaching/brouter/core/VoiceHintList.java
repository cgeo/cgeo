/**
 * Container for a voice hint
 * (both input- and result data for voice hint processing)
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import java.util.ArrayList;
import java.util.List;

public class VoiceHintList {
    static final int TRANS_MODE_FOOT = 1;
    static final int TRANS_MODE_BIKE = 2;
    static final int TRANS_MODE_CAR  = 3;

    private int transportMode = TRANS_MODE_BIKE;
    public int turnInstructionMode;
    public List<VoiceHint> list = new ArrayList<>();

    public void setTransportMode(boolean isCar, boolean isBike) {
        transportMode = isCar ? TRANS_MODE_CAR : (isBike ? TRANS_MODE_BIKE : TRANS_MODE_FOOT);
    }

    public String getTransportMode() {
        final String ret;
        switch (transportMode) {
            case TRANS_MODE_FOOT:
                ret = "foot";
                break;
            case TRANS_MODE_CAR:
                ret = "car";
                break;
            case TRANS_MODE_BIKE:
            default:
                ret = "bike";
                break;
        }
        return ret;
    }

    public int transportMode() {
        return transportMode;
    }

    public int getLocusRouteType() {
        if (transportMode == TRANS_MODE_CAR) {
            return 0;
        }
        if (transportMode == TRANS_MODE_BIKE) {
            return 5;
        }
        return 3; // foot
    }
}
