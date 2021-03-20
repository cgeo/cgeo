/**
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import java.util.Map;

import cgeo.geocaching.brouter.expressions.BExpressionContextNode;
import cgeo.geocaching.brouter.expressions.BExpressionContextWay;


final class KinematicModel extends OsmPathModel {
    public double turnAngleDecayTime;
    public double f_roll;
    public double f_air;
    public double f_recup;
    public double p_standby;
    public double outside_temp;
    public double recup_efficiency;
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
    public void init(BExpressionContextWay expctxWay, BExpressionContextNode expctxNode, Map<String, String> extraParams) {
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
        f_roll = getParam("f_roll", 232.f);
        f_air = getParam("f_air", 0.4f);
        f_recup = getParam("f_recup", 400.f);
        p_standby = getParam("p_standby", 250.f);
        outside_temp = getParam("outside_temp", 20.f);
        recup_efficiency = getParam("recup_efficiency", 0.7f);
        totalweight = getParam("totalweight", 1640.f);
        vmax = getParam("vmax", 80.f) / 3.6;
        leftWaySpeed = getParam("leftWaySpeed", 12.f) / 3.6;
        rightWaySpeed = getParam("rightWaySpeed", 12.f) / 3.6;

        pw = 2. * f_air * vmax * vmax * vmax - p_standby;
        cost0 = (pw + p_standby) / vmax + f_roll + f_air * vmax * vmax;
    }

    protected float getParam(String name, float defaultValue) {
        String sval = params == null ? null : params.get(name);
        if (sval != null) {
            return Float.parseFloat(sval);
        }
        float v = ctxWay.getVariableValue(name, defaultValue);
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
        double minspeed = getWayMinspeed();
        double espeed = minspeed > vmax ? minspeed : vmax;
        double maxspeed = getWayMaxspeed();
        return maxspeed < espeed ? maxspeed : espeed;
    }

    /**
     * get the breaking speed for current balance-power (pw) and effective speed limit (vl)
     */
    public double getBreakingSpeed(double vl) {
        if (vl == lastEffectiveLimit) {
            return lastBreakingSpeed;
        }

        double v = vl * 0.8;
        double pw2 = pw + p_standby;
        double e = recup_efficiency;
        double x0 = pw2 / vl + f_air * e * vl * vl + (1. - e) * f_roll;
        for (int i = 0; i < 5; i++) {
            double v2 = v * v;
            double x = pw2 / v + f_air * e * v2 - x0;
            double dx = 2. * e * f_air * v - pw2 / v2;
            v -= x / dx;
        }
        lastEffectiveLimit = vl;
        lastBreakingSpeed = v;

        return v;
    }

}
