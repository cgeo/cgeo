/**
 * Container for a voice hint
 * (both input- and result data for voice hint processing)
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import java.util.ArrayList;

public class VoiceHintList {
    int turnInstructionMode;
    ArrayList<VoiceHint> list = new ArrayList<VoiceHint>();
    private String transportMode;

    public void setTransportMode(boolean isCar, boolean isBike) {
        transportMode = isCar ? "car" : (isBike ? "bike" : "foot");
    }

    public String getTransportMode() {
        return transportMode;
    }

    public int getLocusRouteType() {
        if ("car".equals(transportMode)) {
            return 0;
        }
        if ("bike".equals(transportMode)) {
            return 5;
        }
        return 3; // foot
    }
}
