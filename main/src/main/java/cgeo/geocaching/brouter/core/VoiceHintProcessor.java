/**
 * Processor for Voice Hints
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import java.util.ArrayList;
import java.util.List;

public final class VoiceHintProcessor {

    double significantAngle = 22.5;
    double internalCatchingRange = 2.;

    // private double catchingRange; // range to catch angles and merge turns
    private final boolean explicitRoundabouts;

    public VoiceHintProcessor(final boolean explicitRoundabouts) {
        // this.catchingRange = catchingRange;
        this.explicitRoundabouts = explicitRoundabouts;
    }

    private float sumNonConsumedWithinCatchingRange(final List<VoiceHint> inputs, int offset) {
        double distance = 0.;
        float angle = 0.f;
        while (offset >= 0 && distance < internalCatchingRange) {
            final VoiceHint input = inputs.get(offset--);
            if (input.turnAngleConsumed) {
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
            distance += input.goodWay.linkdist;
            final int currentPrio = input.goodWay.getPrio();
            final int oldPrio = input.oldWay.getPrio();
            final int minPrio = Math.min(oldPrio, currentPrio);

            final boolean isLink2Highway = input.oldWay.isLinktType() && !input.goodWay.isLinktType();
            final boolean isHighway2Link = !input.oldWay.isLinktType() && input.goodWay.isLinktType();

            if (explicitRoundabouts && input.oldWay.isRoundabout()) {
                if (roundaboudStartIdx == -1) {
                    roundaboudStartIdx = hintIdx;
                }
                roundAboutTurnAngle += sumNonConsumedWithinCatchingRange(inputs, hintIdx);
                boolean isExit = roundaboutExit == 0; // exit point is always exit
                if (input.badWays != null) {
                    for (MessageData badWay : input.badWays) {
                        if (!badWay.isBadOneway() && badWay.isGoodForCars() && Math.abs(badWay.turnangle) < 120.) {
                            isExit = true;
                        }
                    }
                }
                if (isExit) {
                    roundaboutExit++;
                }
                continue;
            }
            if (roundaboutExit > 0) {
                roundAboutTurnAngle += sumNonConsumedWithinCatchingRange(inputs, hintIdx);
                final double startTurn = (roundaboudStartIdx != -1 ? inputs.get(roundaboudStartIdx).goodWay.turnangle : turnAngle);
                input.angle = roundAboutTurnAngle;
                input.distanceToNext = distance;
                input.roundaboutExit = startTurn < 0 ? -roundaboutExit : roundaboutExit;
                distance = 0.;
                results.add(input);
                roundAboutTurnAngle = 0.f;
                roundaboutExit = 0;
                roundaboudStartIdx = -1;
                continue;
            }
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

                    if (badPrio > maxPrioAll && !isBadHighway2Link) {
                        maxPrioAll = badPrio;
                    }

                    if (badWay.costfactor < 20.f && Math.abs(badTurn) < minAbsAngeRaw) {
                        minAbsAngeRaw = Math.abs(badTurn);
                    }

                    if (badPrio < minPrio) {
                        continue; // ignore low prio ways
                    }

                    if (badWay.isBadOneway()) {
                        continue; // ignore wrong oneways
                    }

                    if (Math.abs(badTurn) - Math.abs(turnAngle) > 80.f) {
                        continue; // ways from the back should not trigger a slight turn
                    }

                    if (badPrio > maxPrioCandidates) {
                        maxPrioCandidates = badPrio;
                    }
                    if (badTurn > maxAngle) {
                        maxAngle = badTurn;
                    }
                    if (badTurn < minAngle) {
                        minAngle = badTurn;
                    }
                }
            }

            final boolean hasSomethingMoreStraight = (Math.abs(turnAngle) - minAbsAngeRaw) > 20.;

            // unconditional triggers are all junctions with
            // - higher detour prios than the minimum route prio (except link->highway junctions)
            // - or candidate detours with higher prio then the route exit leg
            final boolean unconditionalTrigger = hasSomethingMoreStraight ||
                    (maxPrioAll > minPrio && !isLink2Highway) ||
                    (maxPrioCandidates > currentPrio) ||
                    VoiceHint.is180DegAngle(turnAngle) ||
                    (!isHighway2Link && isBadwayLink && Math.abs(turnAngle) > 5.f) ||
                    (isHighway2Link && !isBadwayLink && Math.abs(turnAngle) < 5.f);

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

                input.angle = sumNonConsumedWithinCatchingRange(inputs, hintIdx);
                input.distanceToNext = distance;
                distance = 0.;
                results.add(input);
            }
            if (results.size() > 0 && distance < internalCatchingRange) { //catchingRange
                results.get(results.size() - 1).angle += sumNonConsumedWithinCatchingRange(inputs, hintIdx);
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
            if (!(hint.needsRealTurn && (hint.cmd == VoiceHint.C || hint.cmd == VoiceHint.BL))) {
                double dist = hint.distanceToNext;
                // sum up other hints within the catching range (e.g. 40m)
                while (dist < internalCatchingRange && i > 0) {
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
                if (results2.size() > 0) {
                    results2.get(results2.size() - 1).distanceToNext += hint.distanceToNext;
                }
            }
        }
        return results2;
    }

    @SuppressWarnings("checkstyle:ModifiedControlVariable")
    public List<VoiceHint> postProcess(List<VoiceHint> inputs, double catchingRange, double minRange) {
        final List<VoiceHint> results = new ArrayList<VoiceHint>();
        VoiceHint inputLast = null;
        final ArrayList<VoiceHint> tmpList = new ArrayList<>();
        for (int hintIdx = 0; hintIdx < inputs.size(); hintIdx++) {
            final VoiceHint input = inputs.get(hintIdx);

            if (input.cmd == VoiceHint.C && !input.goodWay.isLinktType()) {
                int badWayPrio = 0;
                if (input.badWays != null) {
                    for (MessageData md : input.badWays) {
                        badWayPrio = Math.max(badWayPrio, md.getPrio());
                    }
                }
                if (input.goodWay.getPrio() < badWayPrio) {
                    results.add(input);
                } else {
                    if (inputLast != null) { // when drop add distance to last
                        inputLast.distanceToNext += input.distanceToNext;
                    }
                    continue;
                }
            } else {
                if (input.distanceToNext < catchingRange) {
                    double dist = input.distanceToNext;
                    float angles = input.angle;
                    int i = 1;
                    boolean save = true;
                    tmpList.clear();
                    while (dist < catchingRange && hintIdx + i < inputs.size()) {
                        final VoiceHint h2 = inputs.get(hintIdx + i);
                        dist += h2.distanceToNext;
                        angles += h2.angle;
                        if (VoiceHint.is180DegAngle(input.angle) || VoiceHint.is180DegAngle(h2.angle)) {  // u-turn, 180 degree
                            save = true;
                            break;
                        } else if (Math.abs(angles) > 180 - significantAngle) { // u-turn, collects e.g. two left turns in range
                            input.angle = angles;
                            input.calcCommand();
                            input.distanceToNext += h2.distanceToNext;
                            save = true;
                            hintIdx++;
                            break;
                        } else if (Math.abs(angles) < significantAngle && input.distanceToNext < minRange) {
                            input.angle = angles;
                            input.calcCommand();
                            input.distanceToNext += h2.distanceToNext;
                            save = true;
                            hintIdx++;
                            break;
                        } else if (Math.abs(input.angle) > significantAngle) {
                            tmpList.add(h2);
                            hintIdx++;
                        } else if (dist > catchingRange) { // distance reached
                            break;
                        } else {
                            if (inputLast != null) { // when drop add distance to last
                                inputLast.distanceToNext += input.distanceToNext;
                            }
                            save = false;
                        }
                        i++;
                    }
                    if (save) {
                        results.add(input); // add when last
                        if (tmpList.size() > 0) { // add when something in stock
                            results.addAll(tmpList);
                            hintIdx += tmpList.size() - 1;
                        }
                    }
                } else {
                    results.add(input);
                }
                inputLast = input;
            }
        }
        return results;
    }

}
