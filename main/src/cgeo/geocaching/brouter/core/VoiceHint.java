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
    public static final int TU = 10; // U-turn
    public static final int TRU = 11; // Right U-turn
    public static final int OFFR = 12; // Off route
    public static final int RNDB = 13; // Roundabout
    public static final int RNLB = 14; // Roundabout left

    public int ilon;
    public int ilat;
    public short selev;
    public int cmd;
    public MessageData oldWay;
    public MessageData goodWay;
    public List<MessageData> badWays;
    public double distanceToNext;
    public int indexInTrack;
    public float angle;
    public boolean turnAngleConsumed;
    public boolean needsRealTurn;
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

    public int getExitNumber() {
        return roundaboutExit;
    }

    public String getCommandString() {
        switch (cmd) {
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
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

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
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

    public String getMessageString() {
        switch (cmd) {
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
                return "u-turn";
            case RNDB:
                return "Take exit " + roundaboutExit;
            case RNLB:
                return "Take exit " + (-roundaboutExit);
            default:
                throw new IllegalArgumentException("unknown command: " + cmd);
        }
    }

    public int getLocusAction() {
        switch (cmd) {
            case TU:
                return 13;
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

    public int getOruxAction() {
        switch (cmd) {
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
        if (lowerBadWayAngle > angle || higherBadWayAngle < angle) {
            cmdAngle = goodWay.turnangle;
        }

        if (roundaboutExit > 0) {
            cmd = RNDB;
        } else if (roundaboutExit < 0) {
            cmd = RNLB;
        } else if (cmdAngle < -159.) {
            cmd = TU;
        } else if (cmdAngle < -135.) {
            cmd = TSHL;
        } else if (cmdAngle < -45.) {
            // a TL can be pushed in either direction by a close-by alternative
            if (higherBadWayAngle > -90. && higherBadWayAngle < -15. && lowerBadWayAngle < -180.) {
                cmd = TSHL;
            } else if (lowerBadWayAngle > -180. && lowerBadWayAngle < -90. && higherBadWayAngle > 0.) {
                cmd = TSLL;
            } else {
                cmd = TL;
            }
        } else if (cmdAngle < -21.) {
            if (cmd != KR) { // don't overwrite KR with TSLL
                cmd = TSLL;
            }
        } else if (cmdAngle < 21.) {
            if (cmd != KR && cmd != KL) { // don't overwrite KL/KR hints!
                cmd = C;
            }
        } else if (cmdAngle < 45.) {
            if (cmd != KL) { // don't overwrite KL with TSLR
                cmd = TSLR;
            }
        } else if (cmdAngle < 135.) {
            // a TR can be pushed in either direction by a close-by alternative
            if (higherBadWayAngle > 90. && higherBadWayAngle < 180. && lowerBadWayAngle < 0.) {
                cmd = TSLR;
            } else if (lowerBadWayAngle > 15. && lowerBadWayAngle < 90. && higherBadWayAngle > 180.) {
                cmd = TSHR;
            } else {
                cmd = TR;
            }
        } else if (cmdAngle < 159.) {
            cmd = TSHR;
        } else {
            cmd = TRU;
        }
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
