// context for simple expression
// context means:
// - the local variables
// - the local variable names
// - the lookup-input variables

package cgeo.geocaching.brouter.expressions;

import cgeo.geocaching.brouter.codec.TagValueValidator;

public final class BExpressionContextWay extends BExpressionContext implements TagValueValidator {
    private static final String[] buildInVariables =
            {"costfactor", "turncost", "uphillcostfactor", "downhillcostfactor", "initialcost", "nodeaccessgranted", "initialclassifier", "trafficsourcedensity", "istrafficbackbone", "priorityclassifier", "classifiermask", "maxspeed"};
    private boolean decodeForbidden = true;

    public BExpressionContextWay(final BExpressionMetaData meta) {
        super("way", meta);
    }

    /**
     * Create an Expression-Context for way context
     *
     * @param hashSize size of hashmap for result caching
     */
    public BExpressionContextWay(final int hashSize, final BExpressionMetaData meta) {
        super("way", hashSize, meta);
    }

    protected String[] getBuildInVariableNames() {
        return buildInVariables;
    }

    public float getCostfactor() {
        return getBuildInVariable(0);
    }

    public float getTurncost() {
        return getBuildInVariable(1);
    }

    public float getUphillCostfactor() {
        return getBuildInVariable(2);
    }

    public float getDownhillCostfactor() {
        return getBuildInVariable(3);
    }

    public float getInitialcost() {
        return getBuildInVariable(4);
    }

    public float getNodeAccessGranted() {
        return getBuildInVariable(5);
    }

    public float getInitialClassifier() {
        return getBuildInVariable(6);
    }

    public float getTrafficSourceDensity() {
        return getBuildInVariable(7);
    }

    public float getIsTrafficBackbone() {
        return getBuildInVariable(8);
    }

    public float getPriorityClassifier() {
        return getBuildInVariable(9);
    }

    public float getClassifierMask() {
        return getBuildInVariable(10);
    }

    public float getMaxspeed() {
        return getBuildInVariable(11);
    }

    @Override
    public int accessType(final byte[] description) {
        evaluate(false, description);
        float minCostFactor = getCostfactor();
        if (minCostFactor >= 9999.f) {
            setInverseVars();
            final float reverseCostFactor = getCostfactor();
            if (reverseCostFactor < minCostFactor) {
                minCostFactor = reverseCostFactor;
            }
        }
        return minCostFactor < 9999.f ? 2 : decodeForbidden ? (minCostFactor < 10000.f ? 1 : 0) : 0;
    }

    @Override
    public void setDecodeForbidden(final boolean decodeForbidden) {
        this.decodeForbidden = decodeForbidden;
    }
}
