/**
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.util.FastMath;

final class StdPath extends OsmPath {
    // Gravitational constant, g
    private static final double GRAVITY = 9.81;  // in meters per second^(-2)
    /**
     * The elevation-hysteresis-buffer (0-10 m)
     */
    private int ehbd; // in micrometer
    private int ehbu; // in micrometer
    private float totalTime;  // travel time (seconds)
    private float totalEnergy; // total route energy (Joule)
    private float elevation_buffer; // just another elevation buffer (for travel time)

    private static double solveCubic(double a, double c, double d) {
        // Solves a * v^3 + c * v = d with a Newton method
        // to get the speed v for the section.

        double v = 8.;
        boolean findingStartvalue = true;
        for (int i = 0; i < 10; i++) {
            double y = (a * v * v + c) * v - d;
            if (y < .1) {
                if (findingStartvalue) {
                    v *= 2.;
                    continue;
                }
                break;
            }
            findingStartvalue = false;
            double y_prime = 3 * a * v * v + c;
            v -= y / y_prime;
        }
        return v;
    }

    @Override
    public void init(OsmPath orig) {
        StdPath origin = (StdPath) orig;
        this.ehbd = origin.ehbd;
        this.ehbu = origin.ehbu;
        this.totalTime = origin.totalTime;
        this.totalEnergy = origin.totalEnergy;
        this.elevation_buffer = origin.elevation_buffer;
    }

    @Override
    protected void resetState() {
        ehbd = 0;
        ehbu = 0;
        totalTime = 0.f;
        totalEnergy = 0.f;
        elevation_buffer = 0.f;
    }

    @Override
    protected double processWaySection(RoutingContext rc, double distance, double delta_h, double elevation, double angle, double cosangle, boolean isStartpoint, int nsection, int lastpriorityclassifier) {
        // calculate the costfactor inputs
        float turncostbase = rc.expctxWay.getTurncost();
        float cfup = rc.expctxWay.getUphillCostfactor();
        float cfdown = rc.expctxWay.getDownhillCostfactor();
        float cf = rc.expctxWay.getCostfactor();
        cfup = cfup == 0.f ? cf : cfup;
        cfdown = cfdown == 0.f ? cf : cfdown;

        int dist = (int) distance; // legacy arithmetics needs int

        // penalty for turning angle
        int turncost = (int) ((1. - cosangle) * turncostbase + 0.2); // e.g. turncost=90 -> 90 degree = 90m penalty
        if (message != null) {
            message.linkturncost += turncost;
            message.turnangle = (float) angle;
        }

        double sectionCost = turncost;

        // *** penalty for elevation
        // only the part of the descend that does not fit into the elevation-hysteresis-buffers
        // leads to an immediate penalty

        int delta_h_micros = (int) (1000000. * delta_h);
        ehbd += -delta_h_micros - dist * rc.downhillcutoff;
        ehbu += delta_h_micros - dist * rc.uphillcutoff;

        float downweight = 0.f;
        if (ehbd > rc.elevationpenaltybuffer) {
            downweight = 1.f;

            int excess = ehbd - rc.elevationpenaltybuffer;
            int reduce = dist * rc.elevationbufferreduce;
            if (reduce > excess) {
                downweight = ((float) excess) / reduce;
                reduce = excess;
            }
            excess = ehbd - rc.elevationmaxbuffer;
            if (reduce < excess) {
                reduce = excess;
            }
            ehbd -= reduce;
            if (rc.downhillcostdiv > 0) {
                int elevationCost = reduce / rc.downhillcostdiv;
                sectionCost += elevationCost;
                if (message != null) {
                    message.linkelevationcost += elevationCost;
                }
            }
        } else if (ehbd < 0) {
            ehbd = 0;
        }

        float upweight = 0.f;
        if (ehbu > rc.elevationpenaltybuffer) {
            upweight = 1.f;

            int excess = ehbu - rc.elevationpenaltybuffer;
            int reduce = dist * rc.elevationbufferreduce;
            if (reduce > excess) {
                upweight = ((float) excess) / reduce;
                reduce = excess;
            }
            excess = ehbu - rc.elevationmaxbuffer;
            if (reduce < excess) {
                reduce = excess;
            }
            ehbu -= reduce;
            if (rc.uphillcostdiv > 0) {
                int elevationCost = reduce / rc.uphillcostdiv;
                sectionCost += elevationCost;
                if (message != null) {
                    message.linkelevationcost += elevationCost;
                }
            }
        } else if (ehbu < 0) {
            ehbu = 0;
        }

        // get the effective costfactor (slope dependent)
        float costfactor = cfup * upweight + cf * (1.f - upweight - downweight) + cfdown * downweight;

        if (message != null) {
            message.costfactor = costfactor;
        }

        sectionCost += dist * costfactor + 0.5f;

        return sectionCost;
    }

    @Override
    protected double processTargetNode(RoutingContext rc) {
        // finally add node-costs for target node
        if (targetNode.nodeDescription != null) {
            boolean nodeAccessGranted = rc.expctxWay.getNodeAccessGranted() != 0.;
            rc.expctxNode.evaluate(nodeAccessGranted, targetNode.nodeDescription);
            float initialcost = rc.expctxNode.getInitialcost();
            if (initialcost >= 1000000.) {
                return -1.;
            }
            if (message != null) {
                message.linknodecost += (int) initialcost;
                message.nodeKeyValues = rc.expctxNode.getKeyValueDescription(nodeAccessGranted, targetNode.nodeDescription);
            }
            return initialcost;
        }
        return 0.;
    }

    @Override
    public int elevationCorrection(RoutingContext rc) {
        return (rc.downhillcostdiv > 0 ? ehbd / rc.downhillcostdiv : 0)
            + (rc.uphillcostdiv > 0 ? ehbu / rc.uphillcostdiv : 0);
    }

    @Override
    public boolean definitlyWorseThan(OsmPath path, RoutingContext rc) {
        StdPath p = (StdPath) path;

        int c = p.cost;
        if (rc.downhillcostdiv > 0) {
            int delta = p.ehbd - ehbd;
            if (delta > 0)
                c += delta / rc.downhillcostdiv;
        }
        if (rc.uphillcostdiv > 0) {
            int delta = p.ehbu - ehbu;
            if (delta > 0)
                c += delta / rc.uphillcostdiv;
        }

        return cost > c;
    }

    private double calcIncline(double dist) {
        double min_delta = 3.;
        double shift;
        if (elevation_buffer > min_delta) {
            shift = -min_delta;
        } else if (elevation_buffer < min_delta) {
            shift = -min_delta;
        } else {
            return 0.;
        }
        double decayFactor = FastMath.exp(-dist / 100.);
        float new_elevation_buffer = (float) ((elevation_buffer + shift) * decayFactor - shift);
        double incline = (elevation_buffer - new_elevation_buffer) / dist;
        elevation_buffer = new_elevation_buffer;
        return incline;
    }

    @Override
    protected void computeKinematic(RoutingContext rc, double dist, double delta_h, boolean detailMode) {
        if (!detailMode) {
            return;
        }

        // compute incline
        elevation_buffer += delta_h;
        double incline = calcIncline(dist);

        double wayMaxspeed;

        wayMaxspeed = rc.expctxWay.getMaxspeed() / 3.6f;
        if (wayMaxspeed == 0) {
            wayMaxspeed = rc.maxSpeed;
        }
        wayMaxspeed = Math.min(wayMaxspeed, rc.maxSpeed);

        double speed; // Travel speed
        double f_roll = rc.totalMass * GRAVITY * (rc.defaultC_r + incline);
        if (rc.footMode || rc.expctxWay.getCostfactor() > 4.9) {
            // Use Tobler's hiking function for walking sections
            speed = 6 * FastMath.exp(-3.5 * Math.abs(incline + 0.05)) / 3.6;
        } else if (rc.bikeMode) {
            speed = solveCubic(rc.S_C_x, f_roll, rc.bikerPower);
            speed = Math.min(speed, wayMaxspeed);
        } else {
            return;
        }
        float dt = (float) (dist / speed);
        totalTime += dt;
        // Calc energy assuming biking (no good model yet for hiking)
        // (Count only positive, negative would mean breaking to enforce maxspeed)
        double energy = dist * (rc.S_C_x * speed * speed + f_roll);
        if (energy > 0.) {
            totalEnergy += energy;
        }
    }

    @Override
    public double getTotalTime() {
        return totalTime;
    }

    @Override
    public double getTotalEnergy() {
        return totalEnergy;
    }
}
