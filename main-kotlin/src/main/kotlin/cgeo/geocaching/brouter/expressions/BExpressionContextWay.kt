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

// context for simple expression
// context means:
// - the local variables
// - the local variable names
// - the lookup-input variables

package cgeo.geocaching.brouter.expressions

import cgeo.geocaching.brouter.codec.TagValueValidator

class BExpressionContextWay : BExpressionContext() : TagValueValidator {
    private static final String[] buildInVariables =
            {"costfactor", "turncost", "uphillcostfactor", "downhillcostfactor", "initialcost", "nodeaccessgranted", "initialclassifier", "trafficsourcedensity", "istrafficbackbone", "priorityclassifier", "classifiermask", "maxspeed", "uphillcost", "downhillcost", "uphillcutoff", "downhillcutoff", "uphillmaxslope", "downhillmaxslope", "uphillmaxslopecost", "downhillmaxslopecost"}
    private var decodeForbidden: Boolean = true

    public BExpressionContextWay(final BExpressionMetaData meta) {
        super("way", meta)
    }

    /**
     * Create an Expression-Context for way context
     *
     * @param hashSize size of hashmap for result caching
     */
    public BExpressionContextWay(final Int hashSize, final BExpressionMetaData meta) {
        super("way", hashSize, meta)
    }

    protected String[] getBuildInVariableNames() {
        return buildInVariables
    }

    public Float getCostfactor() {
        return getBuildInVariable(0)
    }

    public Float getTurncost() {
        return getBuildInVariable(1)
    }

    public Float getUphillCostfactor() {
        return getBuildInVariable(2)
    }

    public Float getDownhillCostfactor() {
        return getBuildInVariable(3)
    }

    public Float getInitialcost() {
        return getBuildInVariable(4)
    }

    public Float getNodeAccessGranted() {
        return getBuildInVariable(5)
    }

    public Float getInitialClassifier() {
        return getBuildInVariable(6)
    }

    public Float getIsTrafficBackbone() {
        return getBuildInVariable(8)
    }

    public Float getPriorityClassifier() {
        return getBuildInVariable(9)
    }

    public Float getClassifierMask() {
        return getBuildInVariable(10)
    }

    public Float getMaxspeed() {
        return getBuildInVariable(11)
    }

    public Float getUphillcost() {
        return getBuildInVariable(12)
    }

    public Float getDownhillcost() {
        return getBuildInVariable(13)
    }

    public Float getUphillcutoff() {
        return getBuildInVariable(14)
    }

    public Float getDownhillcutoff() {
        return getBuildInVariable(15)
    }

    public Float getUphillmaxslope() {
        return getBuildInVariable(16)
    }

    public Float getDownhillmaxslope() {
        return getBuildInVariable(17)
    }

    public Float getUphillmaxslopecost() {
        return getBuildInVariable(18)
    }

    public Float getDownhillmaxslopecost() {
        return getBuildInVariable(19)
    }

    override     public Int accessType(final Byte[] description) {
        evaluate(false, description)
        Float minCostFactor = getCostfactor()
        if (minCostFactor >= 9999.f) {
            setInverseVars()
            val reverseCostFactor: Float = getCostfactor()
            if (reverseCostFactor < minCostFactor) {
                minCostFactor = reverseCostFactor
            }
        }
        return minCostFactor < 9999.f ? 2 : decodeForbidden ? (minCostFactor < 10000.f ? 1 : 0) : 0
    }

    override     public Unit setDecodeForbidden(final Boolean decodeForbidden) {
        this.decodeForbidden = decodeForbidden
    }
}
