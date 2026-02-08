package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Func3;
import cgeo.geocaching.utils.functions.Func4;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a node in the abstract syntax tree (AST) of a parsed formula.
 * <p>
 * FormulaNode encapsulates a single operation, value, or subexpression within a formula. Each node
 * contains references to its child nodes, the operation or function it represents, and the logic for
 * evaluating itself and its children. Nodes can represent constants, variables, operators, or functions.
 * <p>
 * The class supports error propagation and formatted output for error visualization. It is used internally
 * by the formula parser and evaluator to build and traverse the formula tree.
 * <p>
 * FormulaNode instances are package-private and are not intended to be used directly outside the formula package.
 */
final class FormulaNode {
    public static final String RANGE_NODE_ID = "range-node";

    private final String id;
    private Func3<ValueList, Function<String, Value>, Integer, Value> function;
    private Func4<ValueList, Function<String, Value>, Integer, Set<Integer>, CharSequence> functionToErrorString;
    private FormulaNode[] children;

    private final Set<String> neededVars;
    private final boolean hasRanges;

    private static final FormulaNode[] FORMULA_NODE_EMPTY_ARRAY = new FormulaNode[0];

    FormulaNode(final String id, final FormulaNode[] children,
                final Func3<ValueList, Function<String, Value>, Integer, Value> function) {
        this(id, children, function, null, null);
    }

    FormulaNode(final String id, final FormulaNode[] children,
                final Func3<ValueList, Function<String, Value>, Integer, Value> function,
                final Func4<ValueList, Function<String, Value>, Integer, Set<Integer>, CharSequence> functionToErrorString) {
        this(id, children, function, functionToErrorString, null);
    }

    FormulaNode(final String id, final FormulaNode[] children,
                final Func3<ValueList, Function<String, Value>, Integer, Value> function,
                final Func4<ValueList, Function<String, Value>, Integer, Set<Integer>, CharSequence> functionToErrorString,
                final Consumer<Set<String>> explicitlyNeeded) {

        this.id = id;
        this.children = children == null ? FORMULA_NODE_EMPTY_ARRAY : children;
        this.function = function;
        this.functionToErrorString = functionToErrorString;
        this.neededVars = Collections.unmodifiableSet(calculateNeededVars(explicitlyNeeded, children));
        this.hasRanges = hasRanges(this);

        if (isConstant()) {
            //this means that function is constant!
            this.function = createConstantFunction();
            final CharSequence csResult = evalToCharSequenceInternal(y -> null, -1).getAsCharSequence();
            this.functionToErrorString = (objs, vars, rangeIdx, b) -> csResult;
            this.children = FORMULA_NODE_EMPTY_ARRAY;
        }
    }

    @NonNull
    private String getId() {
        return this.id;
    }

    public boolean isConstant() {
        return this.neededVars.isEmpty() && !this.hasRanges;
    }

    @Nullable
    public Value getConstantValue() {
        if (!isConstant()) {
            return null;
        }
        //if this is a constant function, then the following call will work
        return function.call(null, null, -1);
    }

    @NonNull
    public Set<String> getNeededVars() {
        return neededVars;
    }

    @NonNull
    private static Set<String> calculateNeededVars(final Consumer<Set<String>> explicitlyNeeded, final FormulaNode[] children) {
        final Set<String> neededVars = new HashSet<>();
        if (explicitlyNeeded != null) {
            explicitlyNeeded.accept(neededVars);
        }

        if (children != null) {
            for (FormulaNode child : children) {
                neededVars.addAll(child.neededVars);
            }
        }
        return neededVars;
    }

    private static boolean hasRanges(@NonNull final FormulaNode node) {
        if (RANGE_NODE_ID.equals(node.id)) {
            return true;
        }
        for (FormulaNode c : node.children) {
            if (hasRanges(c)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private Func3<ValueList, Function<String, Value>, Integer, Value> createConstantFunction() {
        Value result = null;
        FormulaException resultException = null;
        try {
            result = eval(y -> null, -1);
        } catch (FormulaException fe) {
            resultException = fe;
        }
        final Value finalResult = result;
        final FormulaException finalResultException = resultException;
        return (objs, vars, idx) -> {
            if (finalResultException != null) {
                throw finalResultException;
            }
            return finalResult;
        };
    }

    // Sichtbarkeit für Formula
    @NonNull
    Value eval(final Function<String, Value> variables, final int rangeIdx) throws FormulaException {
        return evalInternal(variables == null ? x -> null : variables, rangeIdx);
    }

    // Sichtbarkeit für Formula
    @NonNull
    CharSequence evalToCharSequence(final Function<String, Value> vars, final int rangeIdx) {
        final Value result = evalToCharSequenceInternal(vars == null ? x -> null : vars, rangeIdx);
        if (FormulaError.ErrorValue.isError(result)) {
            return TextUtils.setSpan(((FormulaError.ErrorValue) result).getAsCharSequence(), FormulaError.createWarningSpan(), -1, -1, 5);
        }
        return result.toString();
    }

    @NonNull
    private Value evalInternal(final Function<String, Value> variables, final int rangeIdx) throws FormulaException {
        final ValueList childValues = new ValueList();
        for (FormulaNode child : children) {
            childValues.add(child.eval(variables, rangeIdx));
        }
        return this.function.call(childValues, variables, rangeIdx);
    }

    @NonNull
    private Value evalToCharSequenceInternal(final Function<String, Value> variables, final int rangeIdx) {
        final ValueList childValues = new ValueList();
        boolean hasError = false;
        for (FormulaNode child : children) {
            final Value v = child.evalToCharSequenceInternal(variables, rangeIdx);
            if (FormulaError.ErrorValue.isError(v)) {
                hasError = true;
            }
            childValues.add(v);
        }
        Set<Integer> childsInError = null;
        if (!hasError) {
            try {
                return this.function.call(childValues, variables, rangeIdx);
            } catch (FormulaException fe) {
                //error is right here...
                childsInError = fe.getChildrenInError();
            }
        }

        //we had an error in the chain -> produce a String using the error function
        CharSequence cs = null;
        if (this.functionToErrorString != null) {
            cs = this.functionToErrorString.call(childValues, variables, rangeIdx, childsInError);
        }
        if (cs == null) {
            cs = FormulaError.optionalError(FormulaError.valueListToCharSequence(childValues), childsInError);
        }
        return FormulaError.ErrorValue.create(cs);
    }

    /**
     * visibility for test/debug purposes only!
     */
    @NonNull
    String toDebugString(final Function<String, Value> variables, final int rangeIdx, final boolean includeId, final boolean recursive) {
        final StringBuilder sb = new StringBuilder();
        if (includeId) {
            sb.append("[").append(getId()).append("]");
        }
        sb.append(evalToCharSequence(variables, rangeIdx));
        if (recursive) {
            sb.append("{");
            boolean first = true;
            for (FormulaNode child : children) {
                if (!first) {
                    sb.append(";");
                }
                first = false;
                sb.append(child.toDebugString(variables, rangeIdx, includeId, recursive));
            }
            sb.append("}");
        }
        return sb.toString();
    }
}
