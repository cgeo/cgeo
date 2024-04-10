/**
 * Container for a voice hint
 * (both input- and result data for voice hint processing)
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import java.util.ArrayList;
import java.util.List;

public class VoiceHint {
    public static final int C = 1; // continue (go straight)
    public static final int TL = 2; // turn left
    public static final int TSLL = 3; // turn slightly left
    public static final int TSHL = 4; // turn sharply left
    public static final int TR = 5; // turn right
    public static final int TSLR = 6; // turn slightly right
    public static final int TSHR = 7; // turn sharply right
    public static final int KL = 8; // keep left
    public static final int KR = 9; // keep right
    public static final int TLU = 10; // U-turn
    public static final int TRU = 11; // Right U-turn
    public static final int OFFR = 12; // Off route
    public static final int RNDB = 13; // Roundabout
    public static final int RNLB = 14; // Roundabout left
    public static final int TU = 15; // 180 degree u-turn
    public static final int BL = 16; // Beeline routing

    public int ilon;
    public int ilat;
    public short selev;
    public int cmd;
    public MessageData oldWay;
    public MessageData goodWay;
    public List<MessageData> badWays;
    public double distanceToNext;
    public int indexInTrack;
    float angle = Float.MAX_VALUE;
    public boolean turnAngleConsumed;
    public boolean needsRealTurn;
    int maxBadPrio = -1;
    public int roundaboutExit;

    public float getTime() {
        return oldWay == null ? 0.f : oldWay.time;
    }

    public boolean isRoundabout() {
        return roundaboutExit != 0;
    }

    public void addBadWay(final MessageData badWay) {
        if (badWay == null) {
            return;
        }
        if (badWays == null) {
            badWays = new ArrayList<>();
        }
        badWays.add(badWay);
    }

    public int getCommand() {
        return cmd;
    }

    public int getJsonCommandIndex() {
        switch (cmd) {
            case TLU:
                return 10;
            case TU:
                return 15;
            case TSHL:
                return 4;
            case TL:
                return 2;
            case TSLL:
                return 3;
            case KL:
                return 8;
            case C:
                return 1;
            case KR:
                return 9;
            case TSLR:
                return 6;
            case TR:
                return 5;
            case TSHR:
                return 7;
            case TRU:
                return 11;
            case RNDB:
                return 13;
            case RNLB:
                return 14;
            case BL:
                return 16;
            case OFFR:
                return 12;
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

    public int getExitNumber() {
        return roundaboutExit;
    }

    /*
     * used by comment style, osmand style
     */
    public String getCommandString() {
        switch (cmd) {
            case TLU:
                return "TU";  // should be changed to TLU when osmand uses new voice hint constants
            case TU:
                return "TU";
            case TSHL:
                return "TSHL";
            case TL:
                return "TL";
            case TSLL:
                return "TSLL";
            case KL:
                return "KL";
            case C:
                return "C";
            case KR:
                return "KR";
            case TSLR:
                return "TSLR";
            case TR:
                return "TR";
            case TSHR:
                return "TSHR";
            case TRU:
                return "TRU";
            case RNDB:
                return "RNDB" + roundaboutExit;
            case RNLB:
                return "RNLB" + (-roundaboutExit);
            case BL:
                return "BL";
            case OFFR:
                return "OFFR";
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

    /*
     * used by trkpt/sym style
     */
    public String getCommandString(int c) {
        switch (c) {
            case TLU:
                return "TLU";
            case TU:
                return "TU";
            case TSHL:
                return "TSHL";
            case TL:
                return "TL";
            case TSLL:
                return "TSLL";
            case KL:
                return "KL";
            case C:
                return "C";
            case KR:
                return "KR";
            case TSLR:
                return "TSLR";
            case TR:
                return "TR";
            case TSHR:
                return "TSHR";
            case TRU:
                return "TRU";
            case RNDB:
                return "RNDB" + roundaboutExit;
            case RNLB:
                return "RNLB" + (-roundaboutExit);
            case BL:
                return "BL";
            case OFFR:
                return "OFFR";
            default:
                return "unknown command: " + c;
        }
    }

    /*
     * used by gpsies style
     */
    public String getSymbolString() {
        switch (cmd) {
            case TU:
                return "TU";
            case TSHL:
                return "TSHL";
            case TL:
                return "Left";
            case TSLL:
                return "TSLL";
            case KL:
                return "TSLL"; // ?
            case C:
                return "Straight";
            case KR:
                return "TSLR"; // ?
            case TSLR:
                return "TSLR";
            case TR:
                return "Right";
            case TSHR:
                return "TSHR";
            case TRU:
                return "TU";
            case RNDB:
                return "RNDB" + roundaboutExit;
            case RNLB:
                return "RNLB" + (-roundaboutExit);
            case BL:
                return "BL";
            case OFFR:
                return "OFFR";
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

    /*
     * used by new locus trkpt style
     */
    public String getLocusSymbolString() {
        switch (cmd) {
            case TLU:
                return "u-turn_left";
            case TU:
                return "u-turn";
            case TSHL:
                return "left_sharp";
            case TL:
                return "left";
            case TSLL:
                return "left_slight";
            case KL:
                return "stay_left"; // ?
            case C:
                return "straight";
            case KR:
                return "stay_right"; // ?
            case TSLR:
                return "right_slight";
            case TR:
                return "right";
            case TSHR:
                return "right_sharp";
            case TRU:
                return "u-turn_right";
            case RNDB:
                return "roundabout_e" + roundaboutExit;
            case RNLB:
                return "roundabout_e" + (-roundaboutExit);
            case BL:
                return "beeline";
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

    /*
     * used by osmand style
     */
    public String getMessageString() {
        switch (cmd) {
            case TLU:
                return "u-turn"; // should be changed to u-turn-left when osmand uses new voice hint constants
            case TU:
                return "u-turn";
            case TSHL:
                return "sharp left";
            case TL:
                return "left";
            case TSLL:
                return "slight left";
            case KL:
                return "keep left";
            case C:
                return "straight";
            case KR:
                return "keep right";
            case TSLR:
                return "slight right";
            case TR:
                return "right";
            case TSHR:
                return "sharp right";
            case TRU:
                return "u-turn";  // should be changed to u-turn-right when osmand uses new voice hint constants
            case RNDB:
                return "Take exit " + roundaboutExit;
            case RNLB:
                return "Take exit " + (-roundaboutExit);
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

    /*
     * used by old locus style
     */
    public int getLocusAction() {
        switch (cmd) {
            case TLU:
                return 13;
            case TU:
                return 12;
            case TSHL:
                return 5;
            case TL:
                return 4;
            case TSLL:
                return 3;
            case KL:
                return 9; // ?
            case C:
                return 1;
            case KR:
                return 10; // ?
            case TSLR:
                return 6;
            case TR:
                return 7;
            case TSHR:
                return 8;
            case TRU:
                return 14;
            case RNDB:
                return 26 + roundaboutExit;
            case RNLB:
                return 26 - roundaboutExit;
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

    /*
     * used by orux style
     */
    public int getOruxAction() {
        switch (cmd) {
            case TLU:
                return 1003;
            case TU:
                return 1003;
            case TSHL:
                return 1019;
            case TL:
                return 1000;
            case TSLL:
                return 1017;
            case KL:
                return 1015; // ?
            case C:
                return 1002;
            case KR:
                return 1014; // ?
            case TSLR:
                return 1016;
            case TR:
                return 1001;
            case TSHR:
                return 1018;
            case TRU:
                return 1003;
            case RNDB:
                return 1008 + roundaboutExit;
            case RNLB:
                return 1008 + roundaboutExit;
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

    /*
     * used by cruiser, equivalent to getCommandString() - osmand style - when osmand changes the voice hint  constants
     */
    public String getCruiserCommandString() {
        switch (cmd) {
            case TLU:
                return "TLU";
            case TU:
                return "TU";
            case TSHL:
                return "TSHL";
            case TL:
                return "TL";
            case TSLL:
                return "TSLL";
            case KL:
                return "KL";
            case C:
                return "C";
            case KR:
                return "KR";
            case TSLR:
                return "TSLR";
            case TR:
                return "TR";
            case TSHR:
                return "TSHR";
            case TRU:
                return "TRU";
            case RNDB:
                return "RNDB" + roundaboutExit;
            case RNLB:
                return "RNLB" + (-roundaboutExit);
            case BL:
                return "BL";
            case OFFR:
                return "OFFR";
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

    /*
     * used by cruiser, equivalent to getMessageString() - osmand style - when osmand changes the voice hint  constants
     */
    public String getCruiserMessageString() {
        switch (cmd) {
            case TLU:
                return "u-turn left";
            case TU:
                return "u-turn";
            case TSHL:
                return "sharp left";
            case TL:
                return "left";
            case TSLL:
                return "slight left";
            case KL:
                return "keep left";
            case C:
                return "straight";
            case KR:
                return "keep right";
            case TSLR:
                return "slight right";
            case TR:
                return "right";
            case TSHR:
                return "sharp right";
            case TRU:
                return "u-turn right";
            case RNDB:
                return "take exit " + roundaboutExit;
            case RNLB:
                return "take exit " + (-roundaboutExit);
            case BL:
                return "beeline";
            case OFFR:
                return "offroad";
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

    public void calcCommand() {
        float lowerBadWayAngle = -181;
        float higherBadWayAngle = 181;
        if (badWays != null) {
            for (MessageData badWay : badWays) {
                if (badWay.isBadOneway()) {
                    continue;
                }
                if (lowerBadWayAngle < badWay.turnangle && badWay.turnangle < goodWay.turnangle) {
                    lowerBadWayAngle = badWay.turnangle;
                }
                if (higherBadWayAngle > badWay.turnangle && badWay.turnangle > goodWay.turnangle) {
                    higherBadWayAngle = badWay.turnangle;
                }
            }
        }

        float cmdAngle = angle;

        // fall back to local angle if otherwise inconsistent
        //if ( lowerBadWayAngle > angle || higherBadWayAngle < angle )
        //{
        //cmdAngle = goodWay.turnangle;
        //}
        if (angle == Float.MAX_VALUE) {
            cmdAngle = goodWay.turnangle;
        }
        if (cmd == BL) {
            return;
        }

        if (roundaboutExit > 0) {
            cmd = RNDB;
        } else if (roundaboutExit < 0) {
            cmd = RNLB;
        } else if (is180DegAngle(cmdAngle) && cmdAngle <= -179.f && higherBadWayAngle == 181.f && lowerBadWayAngle == -181.f) {
            cmd = TU;
        } else if (cmdAngle < -159.f) {
            cmd = TLU;
        } else if (cmdAngle < -135.f) {
            cmd = TSHL;
        } else if (cmdAngle < -45.f) {
            // a TL can be pushed in either direction by a close-by alternative
            if (cmdAngle < -95.f && higherBadWayAngle < -30.f && lowerBadWayAngle < -180.f) {
                cmd = TSHL;
            } else if (cmdAngle > -85.f && lowerBadWayAngle > -180.f && higherBadWayAngle > -10.f) {
                cmd = TSLL;
            } else {
                if (cmdAngle < -110.f) {
                    cmd = TSHL;
                } else if (cmdAngle > -60.f) {
                    cmd = TSLL;
                } else {
                    cmd = TL;
                }
            }
        } else if (cmdAngle < -21.f) {
            if (cmd != KR) { // don't overwrite KR with TSLL
                cmd = TSLL;
            }
        } else if (cmdAngle < -5.f) {
            if (lowerBadWayAngle < -100.f && higherBadWayAngle < 45.f) {
                cmd = TSLL;
            } else if (lowerBadWayAngle >= -100.f && higherBadWayAngle < 45.f) {
                cmd = KL;
            } else {
                cmd = C;
            }
        } else if (cmdAngle < 5.f) {
            if (lowerBadWayAngle > -30.f) {
                cmd = KR;
            } else if (higherBadWayAngle < 30.f) {
                cmd = KL;
            } else {
                cmd = C;
            }
        } else if (cmdAngle < 21.f) {
            // a TR can be pushed in either direction by a close-by alternative
            if (lowerBadWayAngle > -45.f && higherBadWayAngle > 100.f) {
                cmd = TSLR;
            } else if (lowerBadWayAngle > -45.f && higherBadWayAngle <= 100.f) {
                cmd = KR;
            } else {
                cmd = C;
            }
        } else if (cmdAngle < 45.f) {
            cmd = TSLR;
        } else if (cmdAngle < 135.f) {
            if (cmdAngle < 85.f && higherBadWayAngle < 180.f && lowerBadWayAngle < 10.f) {
                cmd = TSLR;
            } else if (cmdAngle > 95.f && lowerBadWayAngle > 30.f && higherBadWayAngle > 180.f) {
                cmd = TSHR;
            } else {
                if (cmdAngle > 110.) {
                    cmd = TSHR;
                } else if (cmdAngle < 60.) {
                    cmd = TSLR;
                } else {
                    cmd = TR;
                }
            }
        } else if (cmdAngle < 159.f) {
            cmd = TSHR;
        } else if (is180DegAngle(cmdAngle) && cmdAngle >= 179.f && higherBadWayAngle == 181.f && lowerBadWayAngle == -181.f) {
            cmd = TU;
        } else {
            cmd = TRU;
        }
    }

    static boolean is180DegAngle(float angle) {
        return (Math.abs(angle) <= 180.f && Math.abs(angle) >= 179.f);
    }

    public String formatGeometry() {
        final float oldPrio = oldWay == null ? 0.f : oldWay.priorityclassifier;
        final StringBuilder sb = new StringBuilder(30);
        sb.append(' ').append((int) oldPrio);
        appendTurnGeometry(sb, goodWay);
        if (badWays != null) {
            for (MessageData badWay : badWays) {
                sb.append(" ");
                appendTurnGeometry(sb, badWay);
            }
        }
        return sb.toString();
    }

    private void appendTurnGeometry(final StringBuilder sb, final MessageData msg) {
        sb.append("(").append((int) (msg.turnangle + 0.5)).append(")").append(msg.priorityclassifier);
    }

}
