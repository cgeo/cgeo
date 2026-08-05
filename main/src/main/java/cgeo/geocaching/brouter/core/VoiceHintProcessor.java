/**
 * Processor for Voice Hints
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import java.util.ArrayList;
import java.util.List;

public final class VoiceHintProcessor {

    static final double SIGNIFICANT_ANGLE = 22.5;
    static final double INTERNAL_CATCHING_RANGE_NEAR = 2.;
    static final double INTERNAL_CATCHING_RANGE_WIDE = 10.;

    // private double catchingRange; // range to catch angles and merge turns
    private final boolean explicitRoundabouts;
    private final int transportMode;

    public VoiceHintProcessor(final double catchingRange, final boolean explicitRoundabouts, final int transportMode) {
        // this.catchingRange = catchingRange;
        this.explicitRoundabouts = explicitRoundabouts;
        this.transportMode = transportMode;
    }

    private float sumNonConsumedWithinCatchingRange(final List<VoiceHint> inputs, final int offset, final double range) {
        int offsetLocal = offset;
        double distance = 0.;
        float angle = 0.f;
        while (offset >= 0 && distance < range) {
            final VoiceHint input = inputs.get(offsetLocal--);
            if (input.turnAngleConsumed || input.cmd == VoiceHint.BL || input.cmd == VoiceHint.END) {
                break;
            }
            angle += input.goodWay.turnangle;
            distance += input.goodWay.linkdist;
            input.turnAngleConsumed = true;
        }
        return angle;
    }


    /**
     * process voice hints. Uses VoiceHint objects
     * for both input and output. Input is in reverse
     * order (from target to start), but output is
     * returned in travel-direction and only for
     * those nodes that trigger a voice hint.
     * <p>
     * Input objects are expected for every segment
     * of the track, also for those without a junction
     * <p>
     * VoiceHint objects in the output list are enriched
     * by the voice-command, the total angle and the distance
     * to the next hint
     *
     * @param inputs tracknodes, un reverse order
     * @return voice hints, in forward order
     */
    public List<VoiceHint> process(final List<VoiceHint> inputs) {
        final List<VoiceHint> results = new ArrayList<>();
        double distance = 0.;
        float roundAboutTurnAngle = 0.f; // sums up angles in roundabout

        int roundaboutExit = 0;
        int roundaboudStartIdx = -1;

        for (int hintIdx = 0; hintIdx < inputs.size(); hintIdx++) {
            final VoiceHint input = inputs.get(hintIdx);

            if (input.cmd == VoiceHint.BL) {
                results.add(input);
                continue;
            }

            final float turnAngle = input.goodWay.turnangle;
            if (hintIdx != 0) {
                distance += input.goodWay.linkdist;
            }
            final int currentPrio = input.goodWay.getPrio();
            final int oldPrio = input.oldWay.getPrio();
            final int minPrio = Math.min(oldPrio, currentPrio);

            final boolean isLink2Highway = input.oldWay.isLinktType() && !input.goodWay.isLinktType();
            final boolean isHighway2Link = !input.oldWay.isLinktType() && input.goodWay.isLinktType();

            if (explicitRoundabouts && input.oldWay.isRoundabout()) {
                if (roundaboudStartIdx == -1) {
                    roundaboudStartIdx = hintIdx;
                }
                roundAboutTurnAngle += sumNonConsumedWithinCatchingRange(inputs, hintIdx, INTERNAL_CATCHING_RANGE_NEAR);
                if (roundaboudStartIdx == hintIdx) {
                    if (input.badWays != null) {
                        // remove goodWay
                        roundAboutTurnAngle -= input.goodWay.turnangle;
                        // add a badWay
                        for (MessageData badWay : input.badWays) {
                            if (!badWay.isBadOneway()) {
                                roundAboutTurnAngle += badWay.turnangle;
                            }
                        }
                    }
                }
                boolean isExit = roundaboutExit == 0; // exit point is always exit
                if (input.badWays != null) {
                    for (MessageData badWay : input.badWays) {
                        if (!badWay.isBadOneway() && badWay.isGoodForCars()) {
                            isExit = true;
                            break;
                        }
                    }
                }
                if (isExit) {
                    roundaboutExit++;
                }
                continue;
            }
            if (roundaboutExit > 0) {
                input.angle = roundAboutTurnAngle;
                input.goodWay.turnangle = roundAboutTurnAngle;
                input.distanceToNext = distance;
                input.turnAngleConsumed = true;
                //input.roundaboutExit = startTurn < 0 ? roundaboutExit : -roundaboutExit;
                input.roundaboutExit = roundAboutTurnAngle < 0 ? roundaboutExit : -roundaboutExit;
                float tmpangle = 0;
                final VoiceHint tmpRndAbt = new VoiceHint();
                tmpRndAbt.badWays = new ArrayList<>();
                for (int i = hintIdx - 1; i > roundaboudStartIdx; i--) {
                    final VoiceHint vh = inputs.get(i);
                    tmpangle += inputs.get(i).goodWay.turnangle;
                    if (vh.badWays != null) {
                        for (MessageData badWay : vh.badWays) {
                            if (!badWay.isBadOneway()) {
                                final MessageData md = new MessageData();
                                md.linkdist = vh.goodWay.linkdist;
                                md.priorityclassifier = vh.goodWay.priorityclassifier;
                                md.turnangle = tmpangle;
                                tmpRndAbt.badWays.add(md);
                            }
                        }
                    }
                }
                distance = 0.;

                input.badWays = tmpRndAbt.badWays;
                results.add(input);
                roundAboutTurnAngle = 0.f;
                roundaboutExit = 0;
                roundaboudStartIdx = -1;
                continue;
            }

            final VoiceHint inputNext = hintIdx + 1 < inputs.size() ? inputs.get(hintIdx + 1) : null;

            int maxPrioAll = -1; // max prio of all detours
            int maxPrioCandidates = -1; // max prio of real candidates

            float maxAngle = -180.f;
            float minAngle = 180.f;
            float minAbsAngeRaw = 180.f;

            boolean isBadwayLink = false;

            if (input.badWays != null) {
                for (MessageData badWay : input.badWays) {
                    final int badPrio = badWay.getPrio();
                    final float badTurn = badWay.turnangle;
                    if (badWay.isLinktType()) {
                        isBadwayLink = true;
                    }
                    final boolean isBadHighway2Link = !input.oldWay.isLinktType() && badWay.isLinktType();

                    if (badPrio > maxPrioAll) {
                        maxPrioAll = badPrio;
                        input.maxBadPrio = Math.max(input.maxBadPrio, badPrio);
                    }

                    if (badWay.costfactor < 20.f && Math.abs(badTurn) < minAbsAngeRaw) {
                        minAbsAngeRaw = Math.abs(badTurn);
                    }

                    if (badWay.isBadOneway()) {
                        if (minAbsAngeRaw == 180f) {
                            minAbsAngeRaw = Math.abs(turnAngle); // disable hasSomethingMoreStraight
                        }
                        continue; // ignore wrong oneways
                    }

                    if (Math.abs(badTurn) - Math.abs(turnAngle) > 80.f) {
                        if (minAbsAngeRaw == 180f) {
                            minAbsAngeRaw = Math.abs(turnAngle); // disable hasSomethingMoreStraight
                        }
                        continue; // ways from the back should not trigger a slight turn
                    }

                    if (badWay.costfactor < 20.f && Math.abs(badTurn) < minAbsAngeRaw) {
                        minAbsAngeRaw = Math.abs(badTurn);
                    }

                    if (badPrio > maxPrioCandidates) {
                        maxPrioCandidates = badPrio;
                        input.maxBadPrio = Math.max(input.maxBadPrio, badPrio);
                    }
                    if (badTurn > maxAngle) {
                        maxAngle = badTurn;
                    }
                    if (badTurn < minAngle) {
                        minAngle = badTurn;
                    }
                }
            }

            // has a significant angle and one or more bad ways around
            // https://brouter.de/brouter-test/#map=17/53.07509/-0.95780/standard&lonlats=-0.95757,53.073428;-0.95727,53.076064&profile=car-eco
            final boolean hasSomethingMoreStraight = (Math.abs(turnAngle) > 35f) && input.badWays != null;

            // bad way has more prio, but is not a link
            //
            final boolean noLinkButBadWayPrio = (maxPrioAll > minPrio && !isLink2Highway);

            // bad way has more prio
            //
            final boolean badWayHasPrio = (maxPrioCandidates > currentPrio);

            // is a u-turn - same way back
            // https://brouter.de/brouter-test/#map=16/51.0608/13.7707/standard&lonlats=13.7658,51.060989;13.767893,51.061628;13.765273,51.062953&pois=13.76739,51.061609,Biergarten2956
            final boolean isUTurn = VoiceHint.is180DegAngle(turnAngle);

            // way has prio, but also has an angle
            // https://brouter.de/brouter-test/#map=15/47.7925/16.2582/standard&lonlats=16.24952,47.785458;16.269679,47.794653&profile=car-eco
            final boolean isBadWayLinkButNoLink = (!isHighway2Link && isBadwayLink && Math.abs(turnAngle) > 5.f);

            //
            // https://brouter.de/brouter-test/#map=14/47.7927/16.2848/standard&lonlats=16.267617,47.795275;16.286438,47.787354&profile=car-eco
            final boolean isLinkButNoBadWayLink = (isHighway2Link && !isBadwayLink && Math.abs(turnAngle) < 5.f);

            // way has same prio, but bad way has smaller angle and is not a bad link and prio is near
            // small: https://brouter.de/brouter-test/#map=17/49.40750/8.69257/standard&lonlats=8.692461,49.407997;8.694028,49.408478&profile=car-eco
            // high:  https://brouter.de/brouter-test/#map=14/52.9951/-0.5786/standard&lonlats=-0.59261,52.991576;-0.583606,52.998947&profile=car-eco
            final boolean samePrioSmallBadAngle = (currentPrio == oldPrio) && (minPrio - maxPrioAll <= 2) && !isBadwayLink && minAbsAngeRaw != 180f && minAbsAngeRaw < 35f;

            // way has prio, but has to give way
            // https://brouter.de/brouter-test/#map=15/54.1344/-4.6015/standard&lonlats=-4.605432,54.136747;-4.609336,54.130058&profile=car-eco
            final boolean mustGiveWay = transportMode != VoiceHintList.TRANS_MODE_FOOT  &&
                    input.badWays != null &&
                    !badWayHasPrio &&
                    (input.hasGiveWay() || (inputNext != null && inputNext.hasGiveWay()));

            // unconditional triggers are all junctions with
            // - higher detour prios than the minimum route prio (except link->highway junctions)
            // - or candidate detours with higher prio then the route exit leg
            final boolean unconditionalTrigger = hasSomethingMoreStraight ||
                    noLinkButBadWayPrio ||
                    badWayHasPrio ||
                    isUTurn ||
                    isBadWayLinkButNoLink ||
                    isLinkButNoBadWayLink ||
                    samePrioSmallBadAngle ||
                    mustGiveWay;

            // conditional triggers (=real turning angle required) are junctions
            // with candidate detours equal in priority than the route exit leg
            final boolean conditionalTrigger = maxPrioCandidates >= minPrio;

            if (unconditionalTrigger || conditionalTrigger) {
                input.angle = turnAngle;
                input.calcCommand();
                final boolean isStraight = input.cmd == VoiceHint.C;
                input.needsRealTurn = (!unconditionalTrigger) && isStraight;

                // check for KR/KL
                if (Math.abs(turnAngle) > 5.) { // don't use to small angles
                    if (maxAngle < turnAngle && maxAngle > turnAngle - 45.f - (Math.max(turnAngle, 0.f))) {
                        input.cmd = VoiceHint.KR;
                    }
                    if (minAngle > turnAngle && minAngle < turnAngle + 45.f - (Math.min(turnAngle, 0.f))) {
                        input.cmd = VoiceHint.KL;
                    }
                }

                if (explicitRoundabouts) {
                    input.angle = sumNonConsumedWithinCatchingRange(inputs, hintIdx, INTERNAL_CATCHING_RANGE_WIDE);
                } else {
                    input.turnAngleConsumed = true;
                }
                input.distanceToNext = distance;
                distance = 0.;
                results.add(input);
            }
            if (!results.isEmpty() && distance < INTERNAL_CATCHING_RANGE_NEAR) { //catchingRange
                results.get(results.size() - 1).angle += sumNonConsumedWithinCatchingRange(inputs, hintIdx, INTERNAL_CATCHING_RANGE_NEAR);
            }
        }

        // go through the hint list again in reverse order (=travel direction)
        // and filter out non-signficant hints and hints too close to it's predecessor

        final List<VoiceHint> results2 = new ArrayList<>();
        int i = results.size();
        while (i > 0) {
            VoiceHint hint = results.get(--i);
            if (hint.cmd == 0) {
                hint.calcCommand();
            }
            if (hint.cmd == VoiceHint.END) {
                results2.add(hint);
                continue;
            }
            if (!(hint.needsRealTurn && (hint.cmd == VoiceHint.C || hint.cmd == VoiceHint.BL))) {
                double dist = hint.distanceToNext;
                // sum up other hints within the catching range (e.g. 40m)
                while (dist < INTERNAL_CATCHING_RANGE_NEAR && i > 0) {
                    final VoiceHint h2 = results.get(i - 1);
                    dist = h2.distanceToNext;
                    hint.distanceToNext += dist;
                    hint.angle += h2.angle;
                    i--;
                    if (h2.isRoundabout()) { // if we hit a roundabout, use that as the trigger
                        h2.angle = hint.angle;
                        hint = h2;
                        break;
                    }
                }

                if (!explicitRoundabouts) {
                    hint.roundaboutExit = 0; // use an angular hint instead
                }
                hint.calcCommand();
                results2.add(hint);
            } else if (hint.cmd == VoiceHint.BL) {
                results2.add(hint);
            } else {
                if (!results2.isEmpty()) {
                    results2.get(results2.size() - 1).distanceToNext += hint.distanceToNext;
                }
            }
        }
        return results2;
    }

    @SuppressWarnings("checkstyle:ModifiedControlVariable")
    public List<VoiceHint> postProcess(final List<VoiceHint> inputs, final double catchingRange, final double minRange) {
        final List<VoiceHint> results = new ArrayList<>();
        VoiceHint inputLast = null;
        VoiceHint inputLastSaved = null;
        for (int hintIdx = 0; hintIdx < inputs.size(); hintIdx++) {
            final VoiceHint input = inputs.get(hintIdx);
            VoiceHint nextInput = null;
            if (hintIdx + 1 < inputs.size()) {
                nextInput = inputs.get(hintIdx + 1);
            }
            if (input.cmd == VoiceHint.BL) {
                results.add(input);
                continue;
            }

            if (nextInput == null) {
                if (input.cmd == VoiceHint.END) {
                    continue;
                } else if ((input.cmd == VoiceHint.C ||
                        input.cmd == VoiceHint.KR ||
                        input.cmd == VoiceHint.KL)
                        && !input.goodWay.isLinktType()) {
                    if (checkStraightHold(input, inputLastSaved, minRange)) {
                        results.add(input);
                    } else {
                        if (inputLast != null) { // when drop add distance to last
                            inputLast.distanceToNext += input.distanceToNext;
                        }
                        continue;
                    }
                } else {
                    results.add(input);
                }
            } else {
                if ((inputLastSaved != null && inputLastSaved.distanceToNext > catchingRange) || input.distanceToNext > catchingRange) {
                    if ((input.cmd == VoiceHint.C ||
                            input.cmd == VoiceHint.KR ||
                            input.cmd == VoiceHint.KL)) {
                        if (checkStraightHold(input, inputLastSaved, minRange)) {
                            // add only on prio
                            results.add(input);
                            inputLastSaved = input;
                        } else {
                            if (inputLastSaved != null) { // when drop add distance to last
                                inputLastSaved.distanceToNext += input.distanceToNext;
                            }
                        }
                    } else if ((input.goodWay.getPrio() == 29 && input.maxBadPrio == 30) &&
                            checkForNextNoneMotorway(inputs, hintIdx, 3)
                    ) {
                        // leave motorway
                        if (input.cmd == VoiceHint.KR || input.cmd == VoiceHint.TSLR) {
                            input.cmd = VoiceHint.ER;
                        } else if (input.cmd == VoiceHint.KL || input.cmd == VoiceHint.TSLL) {
                            input.cmd = VoiceHint.EL;
                        }
                        results.add(input);
                        inputLastSaved = input;
                    } else {
                        // add all others
                        // ignore motorway / primary continue
                        if (((input.goodWay.getPrio() != 28) &&
                                (input.goodWay.getPrio() != 30) &&
                                (input.goodWay.getPrio() != 26))
                                || input.isRoundabout()
                                || Math.abs(input.angle) > 21.f
                                || (Math.abs(input.angle) - input.lowerBadWayAngle) < 21f)  {
                            results.add(input);
                            inputLastSaved = input;
                        } else {
                            if (inputLastSaved != null) { // when drop add distance to last
                                inputLastSaved.distanceToNext += input.distanceToNext;
                            }
                        }
                    }
                } else if (input.distanceToNext < catchingRange) {
                    double dist = input.distanceToNext;
                    float angles = input.angle;
                    boolean save = false;

                    dist += nextInput.distanceToNext;
                    angles += nextInput.angle;

                    if ((input.cmd == VoiceHint.C ||
                            input.cmd == VoiceHint.KR ||
                            input.cmd == VoiceHint.KL)
                        && !input.goodWay.isLinktType()) {
                        if (input.goodWay.getPrio() < input.maxBadPrio) {
                            if (inputLastSaved != null && inputLastSaved.cmd != VoiceHint.C
                                    && (inputLastSaved.distanceToNext > minRange)
                                    && transportMode != VoiceHintList.TRANS_MODE_CAR) {
                                // add when straight and not linktype
                                // and last vh not straight
                                save = true;
                                // remove when next straight and not linktype
                                if (nextInput != null &&
                                        nextInput.cmd == VoiceHint.C &&
                                        !nextInput.goodWay.isLinktType()) {
                                    input.distanceToNext += nextInput.distanceToNext;
                                    hintIdx++;
                                }
                            }

                        } else {
                            if (inputLastSaved != null) { // when drop add distance to last
                                inputLastSaved.distanceToNext += input.distanceToNext;
                            }
                        }
                    } else if ((input.goodWay.getPrio() == 29 && input.maxBadPrio == 30)) {
                        // leave motorway
                        if (input.cmd == VoiceHint.KR || input.cmd == VoiceHint.TSLR) {
                            input.cmd = VoiceHint.ER;
                        } else if (input.cmd == VoiceHint.KL || input.cmd == VoiceHint.TSLL) {
                            input.cmd = VoiceHint.EL;
                        }
                        save = true;
                    } else if (VoiceHint.is180DegAngle(input.angle)) {
                        // add u-turn, 180 degree
                        save = true;
                    } else if (transportMode == VoiceHintList.TRANS_MODE_CAR && Math.abs(angles) > 180 - SIGNIFICANT_ANGLE) {
                        // add when inc car mode and u-turn, collects e.g. two left turns in range
                        input.angle = angles;
                        input.calcCommand();
                        input.distanceToNext += nextInput.distanceToNext;
                        save = true;
                        hintIdx++;
                    } else if (Math.abs(angles) < SIGNIFICANT_ANGLE && input.distanceToNext < minRange) {
                        input.angle = angles;
                        input.calcCommand();
                        input.distanceToNext += nextInput.distanceToNext;
                        save = true;
                        hintIdx++;
                    } else if (Math.abs(input.angle) > SIGNIFICANT_ANGLE) {
                        // add when angle above 22.5 deg
                        save = true;
                    } else if (Math.abs(input.angle) < SIGNIFICANT_ANGLE) {
                        // add when angle below 22.5 deg ???
                        // save = true;
                    } else {
                        // otherwise ignore but add distance to next
                        // when drop add distance to last
                        nextInput.distanceToNext += input.distanceToNext;
                        save = false;
                    }

                    if (save) {
                        results.add(input); // add when last
                        inputLastSaved = input;
                    }
                } else {
                    results.add(input);
                    inputLastSaved = input;
                }
            }
            inputLast = input;
        }
        if (!results.isEmpty()) {
            // don't use END tag
            if (results.get(results.size() - 1).cmd == VoiceHint.END) {
                results.remove(results.size() - 1);
            }
        }

        return results;
    }

    boolean checkForNextNoneMotorway(List<VoiceHint> inputs, int offset, int testsize) {
        for (int i = 1; i < testsize + 1 && offset + i < inputs.size(); i++) {
            final int prio = inputs.get(offset + i).goodWay.getPrio();
            if (prio < 29) {
                return true;
            }
            if (prio == 30) {
                return false;
            }
        }
        return false;
    }

    boolean checkStraightHold(VoiceHint input, VoiceHint inputLastSaved, double minRange) {
        if (input.indexInTrack == 0) {
            return false;
        }

        boolean badOneWay = false;
        if (input.badWays != null) {
            for (MessageData md: input.badWays) {
                if (md.isBadOneway()) {
                    badOneWay = true;
                    break;
                }
            }
        }
        if (badOneWay && input.lowerBadWayAngle == -181.f && input.higherBadWayAngle == 181.f) {
            return false;
        }
        if ((input.lowerBadWayAngle != -181.f && Math.abs(input.lowerBadWayAngle) > 135.f && Math.abs(input.higherBadWayAngle) > 35.f) ||
                (input.higherBadWayAngle != 181.f && input.higherBadWayAngle > 135.f && Math.abs(input.lowerBadWayAngle) > 35.f)) {
            return false;
        }

        return
                ((Math.abs(input.lowerBadWayAngle) < 35.f || input.higherBadWayAngle < 35.f)
                        || input.goodWay.getPrio() < input.maxBadPrio
                        || input.goodWay.getPrio() > input.oldWay.getPrio())
                        && (inputLastSaved == null || inputLastSaved.distanceToNext > minRange)
                        && (input.distanceToNext > minRange)
                ;
    }

}
