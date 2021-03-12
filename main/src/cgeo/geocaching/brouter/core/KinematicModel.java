/**
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.expressions.BExpressionContextNode;
import cgeo.geocaching.brouter.expressions.BExpressionContextWay;

import java.util.Map;

final class KinematicModel extends OsmPathModel {
    public double turnAngleDecayTime;
    public double fRoll;
    public double fAir;
    public double fRecup;
    public double pStandby;
    public double outsideTemp;
    public double recupEfficiency;
    public double totalweight;
    public double vmax;
    public double leftWaySpeed;
    public double rightWaySpeed;
    // derived values
    public double pw; // balance power
    public double cost0; // minimum possible cost per meter
    protected BExpressionContextWay ctxWay;
    protected BExpressionContextNode ctxNode;
    protected Map<String, String> params;
    private int wayIdxMaxspeed;
    private int wayIdxMaxspeedExplicit;
    private int wayIdxMinspeed;
    private int nodeIdxMaxspeed;
    private boolean initDone = false;
    private double lastEffectiveLimit;
    private double lastBreakingSpeed;

    public OsmPrePath createPrePath() {
        return new KinematicPrePath();
    }

    public OsmPath createPath() {
        return new KinematicPath();
    }

    @Override
    public void init(final BExpressionContextWay expctxWay, final BExpressionContextNode expctxNode, final Map<String, String> extraParams) {
        if (!initDone) {
            ctxWay = expctxWay;
            ctxNode = expctxNode;
            wayIdxMaxspeed = ctxWay.getOutputVariableIndex("maxspeed", false);
            wayIdxMaxspeedExplicit = ctxWay.getOutputVariableIndex("maxspeed_explicit", false);
            wayIdxMinspeed = ctxWay.getOutputVariableIndex("minspeed", false);
            nodeIdxMaxspeed = ctxNode.getOutputVariableIndex("maxspeed", false);
            initDone = true;
        }

        params = extraParams;

        turnAngleDecayTime = getParam("turnAngleDecayTime", 5.f);
        fRoll = getParam("f_roll", 232.f);
        fAir = getParam("f_air", 0.4f);
        fRecup = getParam("f_recup", 400.f);
        pStandby = getParam("p_standby", 250.f);
        outsideTemp = getParam("outside_temp", 20.f);
        recupEfficiency = getParam("recup_efficiency", 0.7f);
        totalweight = getParam("totalweight", 1640.f);
        vmax = getParam("vmax", 80.f) / 3.6;
        leftWaySpeed = getParam("leftWaySpeed", 12.f) / 3.6;
        rightWaySpeed = getParam("rightWaySpeed", 12.f) / 3.6;

        pw = 2. * fAir * vmax * vmax * vmax - pStandby;
        cost0 = (pw + pStandby) / vmax + fRoll + fAir * vmax * vmax;
    }

    protected float getParam(final String name, final float defaultValue) {
        final String sval = params == null ? null : params.get(name);
        if (sval != null) {
            return Float.parseFloat(sval);
        }
        final float v = ctxWay.getVariableValue(name, defaultValue);
        if (params != null) {
            params.put(name, "" + v);
        }
        return v;
    }

    public float getWayMaxspeed() {
        return ctxWay.getBuildInVariable(wayIdxMaxspeed) / 3.6f;
    }

    public float getWayMaxspeedExplicit() {
        return ctxWay.getBuildInVariable(wayIdxMaxspeedExplicit) / 3.6f;
    }

    public float getWayMinspeed() {
        return ctxWay.getBuildInVariable(wayIdxMinspeed) / 3.6f;
    }

    public float getNodeMaxspeed() {
        return ctxNode.getBuildInVariable(nodeIdxMaxspeed) / 3.6f;
    }

    /**
     * get the effective speed limit from the way-limit and vmax/vmin
     */
    public double getEffectiveSpeedLimit() {
        // performance related inline coding
        final double minspeed = getWayMinspeed();
        final double espeed = Math.max(minspeed, vmax);
        final double maxspeed = getWayMaxspeed();
        return Math.min(maxspeed, espeed);
    }

    /**
     * get the breaking speed for current balance-power (pw) and effective speed limit (vl)
     */
    public double getBreakingSpeed(final double vl) {
        if (vl == lastEffectiveLimit) {
            return lastBreakingSpeed;
        }

        double v = vl * 0.8;
        final double pw2 = pw + pStandby;
        final double e = recupEfficiency;
        final double x0 = pw2 / vl + fAir * e * vl * vl + (1. - e) * fRoll;
        for (int i = 0; i < 5; i++) {
            final double v2 = v * v;
            final double x = pw2 / v + fAir * e * v2 - x0;
            final double dx = 2. * e * fAir * v - pw2 / v2;
            v -= x / dx;
        }
        lastEffectiveLimit = vl;
        lastBreakingSpeed = v;

        return v;
    }

}
