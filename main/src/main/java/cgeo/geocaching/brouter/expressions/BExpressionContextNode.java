// context for simple expression
// context means:
// - the local variables
// - the local variable names
// - the lookup-input variables

package cgeo.geocaching.brouter.expressions;


public final class BExpressionContextNode extends BExpressionContext {
    private static final String[] buildInVariables =
            {"initialcost"};

    public BExpressionContextNode(final BExpressionMetaData meta) {
        super("node", meta);
    }

    /**
     * Create an Expression-Context for way context
     *
     * @param hashSize size of hashmap for result caching
     */
    public BExpressionContextNode(final int hashSize, final BExpressionMetaData meta) {
        super("node", hashSize, meta);
    }

    protected String[] getBuildInVariableNames() {
        return buildInVariables;
    }

    public float getInitialcost() {
        return getBuildInVariable(0);
    }
}
