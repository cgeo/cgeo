// context for simple expression
// context means:
// - the local variables
// - the local variable names
// - the lookup-input variables

package cgeo.geocaching.brouter.expressions;

import cgeo.geocaching.brouter.codec.TagValueValidator;

public final class BExpressionContextWay extends BExpressionContext implements TagValueValidator {
    private static final String[] buildInVariables =
            {"costfactor", "turncost", "uphillcostfactor", "downhillcostfactor", "initialcost", "nodeaccessgranted", "initialclassifier", "trafficsourcedensity", "istrafficbackbone", "priorityclassifier", "classifiermask", "maxspeed", "uphillcost", "downhillcost", "uphillcutoff", "downhillcutoff", "uphillmaxslope", "downhillmaxslope", "uphillmaxslopecost", "downhillmaxslopecost"};
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

    public float getUphillcost() {
        return getBuildInVariable(12);
    }

    public float getDownhillcost() {
        return getBuildInVariable(13);
    }

    public float getUphillcutoff() {
        return getBuildInVariable(14);
    }

    public float getDownhillcutoff() {
        return getBuildInVariable(15);
    }

    public float getUphillmaxslope() {
        return getBuildInVariable(16);
    }

    public float getDownhillmaxslope() {
        return getBuildInVariable(17);
    }

    public float getUphillmaxslopecost() {
        return getBuildInVariable(18);
    }

    public float getDownhillmaxslopecost() {
        return getBuildInVariable(19);
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
