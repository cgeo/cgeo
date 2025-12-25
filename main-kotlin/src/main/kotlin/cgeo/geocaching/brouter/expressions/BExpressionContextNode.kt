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


class BExpressionContextNode : BExpressionContext() {
    private static final String[] buildInVariables =
            {"initialcost"}

    public BExpressionContextNode(final BExpressionMetaData meta) {
        super("node", meta)
    }

    /**
     * Create an Expression-Context for way context
     *
     * @param hashSize size of hashmap for result caching
     */
    public BExpressionContextNode(final Int hashSize, final BExpressionMetaData meta) {
        super("node", hashSize, meta)
    }

    protected String[] getBuildInVariableNames() {
        return buildInVariables
    }

    public Float getInitialcost() {
        return getBuildInVariable(0)
    }
}
