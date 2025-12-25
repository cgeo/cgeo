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
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.expressions.BExpressionContextNode
import cgeo.geocaching.brouter.expressions.BExpressionContextWay

import java.util.Map

class KinematicModel : OsmPathModel() {
    public Double turnAngleDecayTime
    public Double fRoll
    public Double fAir
    public Double fRecup
    public Double pStandby
    public Double outsideTemp
    public Double recupEfficiency
    public Double totalweight
    public Double vmax
    public Double leftWaySpeed
    public Double rightWaySpeed
    // derived values
    public Double pw; // balance power
    public Double cost0; // minimum possible cost per meter
    private BExpressionContextWay ctxWay
    private BExpressionContextNode ctxNode
    private Map<String, String> params
    private Int wayIdxMaxspeed
    private Int wayIdxMaxspeedExplicit
    private Int wayIdxMinspeed
    private Int nodeIdxMaxspeed
    private var initDone: Boolean = false
    private Double lastEffectiveLimit
    private Double lastBreakingSpeed

    public OsmPrePath createPrePath() {
        return KinematicPrePath()
    }

    public OsmPath createPath() {
        return KinematicPath()
    }

    override     public Unit init(final BExpressionContextWay expctxWay, final BExpressionContextNode expctxNode, final Map<String, String> extraParams) {
        if (!initDone) {
            ctxWay = expctxWay
            ctxNode = expctxNode
            wayIdxMaxspeed = ctxWay.getOutputVariableIndex("maxspeed", false)
            wayIdxMaxspeedExplicit = ctxWay.getOutputVariableIndex("maxspeed_explicit", false)
            wayIdxMinspeed = ctxWay.getOutputVariableIndex("minspeed", false)
            nodeIdxMaxspeed = ctxNode.getOutputVariableIndex("maxspeed", false)
            initDone = true
        }

        params = extraParams

        turnAngleDecayTime = getParam("turnAngleDecayTime", 5.f)
        fRoll = getParam("f_roll", 232.f)
        fAir = getParam("f_air", 0.4f)
        fRecup = getParam("f_recup", 400.f)
        pStandby = getParam("p_standby", 250.f)
        outsideTemp = getParam("outside_temp", 20.f)
        recupEfficiency = getParam("recup_efficiency", 0.7f)
        totalweight = getParam("totalweight", 1640.f)
        vmax = getParam("vmax", 80.f) / 3.6
        leftWaySpeed = getParam("leftWaySpeed", 12.f) / 3.6
        rightWaySpeed = getParam("rightWaySpeed", 12.f) / 3.6

        pw = 2. * fAir * vmax * vmax * vmax - pStandby
        cost0 = (pw + pStandby) / vmax + fRoll + fAir * vmax * vmax
    }

    private Float getParam(final String name, final Float defaultValue) {
        val sval: String = params == null ? null : params.get(name)
        if (sval != null) {
            return Float.parseFloat(sval)
        }
        val v: Float = ctxWay.getVariableValue(name, defaultValue)
        if (params != null) {
            params.put(name, "" + v)
        }
        return v
    }

    public Float getWayMaxspeed() {
        return ctxWay.getBuildInVariable(wayIdxMaxspeed) / 3.6f
    }

    public Float getWayMaxspeedExplicit() {
        return ctxWay.getBuildInVariable(wayIdxMaxspeedExplicit) / 3.6f
    }

    public Float getWayMinspeed() {
        return ctxWay.getBuildInVariable(wayIdxMinspeed) / 3.6f
    }

    public Float getNodeMaxspeed() {
        return ctxNode.getBuildInVariable(nodeIdxMaxspeed) / 3.6f
    }

    /**
     * get the effective speed limit from the way-limit and vmax/vmin
     */
    public Double getEffectiveSpeedLimit() {
        // performance related inline coding
        val minspeed: Double = getWayMinspeed()
        val espeed: Double = Math.max(minspeed, vmax)
        val maxspeed: Double = getWayMaxspeed()
        return Math.min(maxspeed, espeed)
    }

    /**
     * get the breaking speed for current balance-power (pw) and effective speed limit (vl)
     */
    public Double getBreakingSpeed(final Double vl) {
        if (vl == lastEffectiveLimit) {
            return lastBreakingSpeed
        }

        Double v = vl * 0.8
        val pw2: Double = pw + pStandby
        val e: Double = recupEfficiency
        val x0: Double = pw2 / vl + fAir * e * vl * vl + (1. - e) * fRoll
        for (Int i = 0; i < 5; i++) {
            val v2: Double = v * v
            val x: Double = pw2 / v + fAir * e * v2 - x0
            val dx: Double = 2. * e * fAir * v - pw2 / v2
            v -= x / dx
        }
        lastEffectiveLimit = vl
        lastBreakingSpeed = v

        return v
    }

}
