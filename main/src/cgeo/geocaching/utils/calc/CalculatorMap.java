package cgeo.geocaching.utils.calc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A CalculatorMap instance manages a set of {@link Calculator}s which are assigned to a variable each.
 *
 * It allows adding, changing and removing such variable-calculatorformula assignments and recalculates all variable values
 * on each change. It also detects errors such as cyclic dependencies and provides appropriate error messages using {@link CalculatorException}s
 * internationalized error message capacities.
 *
 * CalculatorMap automatically adds/removes and maintains empty state entries for all variables where existing formulas have a dependency to but are
 * not themself added to the map by the user.
 *
 * The method names and semantics of this class are loosely related to the Java {@link Map} interface. However, this class does NOT implement the exact semantics
 * of this interface.
 */
public class CalculatorMap {

    private final Map<String, CalculatorState> calculatorStateMap = new HashMap<>();

    /** State of a variable */
    public enum State {
        /** Variable has a valid formula and a valid, calculated value */
        OK,
        /** Variable has either an invalid formula or its value was not calculateable (e.g. because dependencies are not available or also in an error state) */
        ERROR,
        /** Variable is part of a cyclic dependency, e.g. if formula for A depends on B and formula for B depends on A */
        CYCLE }

    /** Represents one variable-forumula assignment including its current state (e.g. current variable value or error state) */
    public static class CalculatorState {
        private final String var;
        private String formula;
        private Calculator calculator;

        private String error;

        private State state = State.OK;
        private Object result;

        private final Set<String> needs = new HashSet<>();
        private final Set<String> isNeededBy = new HashSet<>();

        private CalculatorState(final String var) {
            this.var = var;
        }

        /** The variable this state is for */
        @NonNull
        public String getVar() {
            return var;
        }

        /** Returns currently assigned Formula. May be null if this is an empty state */
        @Nullable
        public String getFormula() {
            return formula;
        }

        /** returns state of this state */
        @NonNull
        public State getState() {
            return state;
        }

        /** The Formula calculator. May be null if Formula is invalid / not parseable */
        @Nullable
        public Calculator getCalculator() {
            return calculator;
        }

        /** If State is {@link ERROR} or {@link CYCLE}, returns a user-displayable reason for this state. null otherwise */
        @Nullable
        public String getError() {
            return error;
        }

        /** If State is {@link OK} returns the calculated value for the formula. null otherwise */
        @Nullable
        public Object getResult() {
            return result;
        }
    }


    /**
     * Adds or changes a variable - formula assignment to this map.
     *
     * Note that on adding/changing a variable's formula, the map will ensure that for all
     * variables where the formula depends on there will also be an entry in this map (state might be empty though)
     */
    public void put(@NonNull final String var, @Nullable final String formula) {
        Objects.requireNonNull(var);
        CalculatorState state = calculatorStateMap.get(var);
        if (state == null) {
            state = new CalculatorState(var);
            calculatorStateMap.put(var, state);
        } else if (Objects.equals(formula, state.formula)) {
            //nothing to do
            return;
        }
        setFormula(var, formula);
        recalculateDependencies(var);
        recalculate(var);
    }

    /**
     * Removes a variable from this map.
     *
     * Note that the variable may still be represented in this Map after removal with an empty state.
     * This will be the case if another variable's formula depends on the removed variable.
     */
    public void remove(@NonNull final String var) {
        final CalculatorState state = calculatorStateMap.get(var);
        if (state == null) {
            return;
        }
        setFormula(var, null);
        recalculateDependencies(var);
        recalculate(var);

        if (state.isNeededBy.isEmpty()) {
            calculatorStateMap.remove(var);
        }
    }

    public void clear() {
        calculatorStateMap.clear();

    }

    /** Mimics method of same name in {@link Map} interface. Same as {@link #getVars} */
    @NonNull
    public Set<String> keySet() {
        return getVars();
    }

    /** returns a set of all vars currently present in this map. This includes vars with empty state (created due to existing dependencies) */
    @NonNull
    public Set<String> getVars() {
        return calculatorStateMap.keySet();
    }


    public boolean containsKey(final String var) {
        return calculatorStateMap.containsKey(var);
    }

    /** returns a state of a var existing in this instance */
    @Nullable
    public CalculatorState get(final String var) {
        return calculatorStateMap.get(var);
    }

    /** returns number of vars in this instance */
    public int size() {
        return calculatorStateMap.size();
    }

    public String createNonContainedKey(final String prefix) {
        int idx = 1;
        while (containsKey(prefix + idx)) {
            idx++;
        }
        return prefix + idx;
    }


    private void setFormula(final String var, final String formula) {
        final CalculatorState state = Objects.requireNonNull(get(var));
        state.formula = formula;
        state.error = null;
        state.state = State.OK;
        state.calculator = null;
        state.result = null;
        try {
            state.calculator = Calculator.compile(formula);
        } catch (CalculatorException ce) {
            state.state = State.ERROR;
            state.error = ce.getUserDisplayableString();
        }
    }

    private void recalculateDependencies(final String var) {
        final CalculatorState state = Objects.requireNonNull(get(var));
        final Set<String> newNeeds;
        if (state.calculator == null) {
            newNeeds = Collections.emptySet();
        } else {
            newNeeds = state.calculator.getNeededVariables();
        }
        for (String v : state.needs) {
            if (!newNeeds.contains(v)) {
                final CalculatorState neededState = Objects.requireNonNull(get(v));
                neededState.isNeededBy.remove(var);
                if (neededState.isNeededBy.isEmpty() && neededState.formula == null) {
                    calculatorStateMap.remove(v);
                }
            }
        }
        for (String v : newNeeds) {
            if (!state.needs.contains(v)) {
                CalculatorState neededState = get(v);
                if (neededState == null) {
                    put(v, null);
                    neededState = Objects.requireNonNull(get(v));
                }
                neededState.isNeededBy.add(var);
            }
        }
        state.needs.clear();
        state.needs.addAll(newNeeds);
    }

    private void recalculate(final String var) {
        final CalculatorState state = Objects.requireNonNull(get(var));
        state.state = State.OK;
        final List<List<String>> cyclesFound = new ArrayList<>();
        recalculate(state, var, true, new LinkedList<>(), cyclesFound);

        if (!cyclesFound.isEmpty()) {
            final Set<String> markError = new HashSet<>();
            for (List<String> cycle : cyclesFound) {
                int idx = 0;
                for (String cv : cycle) {
                    final CalculatorState s = Objects.requireNonNull(get(cv));
                    s.state = State.CYCLE;
                    s.result = null;
                    s.error = CalculatorException.getUserDisplayableMessage(CalculatorException.ErrorType.CYCLIC_DEPENDENCY, getCyclicString(cycle, idx++));
                    markError.addAll(s.isNeededBy);
                }
            }
            for (String me : markError) {
                markError(me);
            }
        }
    }

    private static String getCyclicString(final List<String> cycle, final int idx) {
        final StringBuilder sb = new StringBuilder(cycle.get(idx));
        for (int i = 0; i < cycle.size(); i++) {
            sb.append("->").append(cycle.get((i + idx + 1) % cycle.size()));
        }
        return sb.toString();
    }

    private void recalculate(final CalculatorState state, final String var, final boolean start, final LinkedList<String> treePath, final List<List<String>> cyclesFound) {
        if (!start && var.equals(state.var)) {
            //new CYCLE found!
            cyclesFound.add(new ArrayList<>(treePath));
            return;
        }

        if (!recalculateSingle(state, false)) {
            for (String n : state.isNeededBy) {
                treePath.add(0, state.var);
                recalculate(get(n), var, false, treePath, cyclesFound);
                treePath.remove(0);
            }
        }
    }

    private boolean recalculateSingle(final CalculatorState state, final boolean forceError) {

        if (state.calculator == null) {
            state.state = State.ERROR;
            if (state.error == null) {
                state.error = CalculatorException.getUserDisplayableMessage(CalculatorException.ErrorType.OTHER, "-");
            }
            state.result = null;
        } else {

            boolean hasCycleDep = false;
            for (String n : state.needs) {
                final CalculatorState cs = Objects.requireNonNull(get(n));
                if (cs.state == State.CYCLE) {
                    hasCycleDep = true;
                    break;
                }
            }
            if (hasCycleDep && state.state == State.CYCLE) {
                return true;
            }
            try {
                state.result = state.calculator.evaluate(
                    v -> state.needs.contains(v) ? Objects.requireNonNull(get(v)).getResult() : null); //may throw CalculatorException
                if (forceError) {
                    state.state = State.ERROR;
                    state.error = CalculatorException.getUserDisplayableMessage(CalculatorException.ErrorType.OTHER, "-");
                } else {
                    state.state = State.OK;
                    state.error = null;
                }
            } catch (CalculatorException ce) {
                state.result = null;
                state.state = State.ERROR;
                state.error = ce.getUserDisplayableString();
            }
        }
        return false;
    }

    private void markError(final String var) {
        final CalculatorState state = Objects.requireNonNull(get(var));
        if (state.state == State.CYCLE) {
            return;
        }
        state.result = null;
        recalculateSingle(state, true);
        for (String n : state.isNeededBy) {
            markError(n);
        }
    }

}
