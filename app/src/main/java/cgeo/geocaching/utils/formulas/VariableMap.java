package cgeo.geocaching.utils.formulas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * A VariableMap instance manages a set of {@link Formula}s which are assigned to a variable each.
 *
 * It allows adding, changing and removing such variable-formula assignments and recalculates all variable values
 * on each change. It also detects errors such as cyclic dependencies and provides appropriate error messages using {@link FormulaException}s
 * internationalized error message capacities.
 *
 * VariableMap automatically adds/removes and maintains empty state entries for all variables where existing formulas have a dependency to but are
 * not themself added to the map by the user. Those state can be detected by having null as FormulaString.
 *
 * The method names and semantics of this class are loosely related to the Java {@link Map} interface. However, this class does NOT implement the exact semantics
 * of this interface.
 */
public class VariableMap {

    private final Map<String, VariableState> variableStateMap = new HashMap<>();

    /**
     * State of a variable
     */
    public enum State {
        /**
         * Variable has a valid formula and a valid, calculated value
         */
        OK,
        /**
         * Variable has either an invalid formula or its value was not calculateable (e.g. because dependencies are not available or also in an error state)
         */
        ERROR,
        /**
         * Variable is part of a cyclic dependency, e.g. if formula for A depends on B and formula for B depends on A
         */
        CYCLE
    }

    /**
     * Represents one variable-forumula assignment including its current state (e.g. current variable value or error state)
     */
    public static class VariableState {
        private final String var;
        private String formulaString;
        private Formula formula;

        private String error;

        private State state = State.OK;
        private CharSequence resultAsCharSequence;
        private Value result;
        private int rangeIndex = 0;

        private final Set<String> needs = new HashSet<>();
        private final Set<String> isNeededBy = new HashSet<>();

        private VariableState(final String var) {
            this.var = var;
        }

        /**
         * The variable this state is for
         */
        @NonNull
        public String getVar() {
            return var;
        }

        /**
         * Returns currently assigned Formula string. May be null if this is an empty state
         */
        @Nullable
        public String getFormulaString() {
            return formulaString;
        }

        /**
         * returns state of this state
         */
        @NonNull
        public State getState() {
            return state;
        }

        /**
         * The Formula assigned to this var. May be null if Formula is invalid / not parseable
         */
        @Nullable
        public Formula getFormula() {
            return formula;
        }

        /**
         * If State is {@link State#ERROR} or {@link State#CYCLE}, returns a user-displayable reason for this state. null otherwise
         */
        @Nullable
        public String getError() {
            return error;
        }

        /**
         * If State is {@link State#OK} returns the calculated value for the formula. null otherwise
         */
        @Nullable
        public Value getResult() {
            return result;
        }

        /**
         * If State is {@link State#OK} or {@link State#ERROR} with compiled Formula, returns the calculated value as a (formatted) CharSequence. null otherwise
         */
        @Nullable
        public CharSequence getResultAsCharSequence() {
            return resultAsCharSequence;
        }

        /**
         * Range index currently used to calculate results of the Formula
         */
        public int getRangeIndex() {
            return this.rangeIndex;
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
        VariableState state = variableStateMap.get(var);
        if (state == null) {
            state = new VariableState(var);
            variableStateMap.put(var, state);
        } else if (Objects.equals(formula, state.formulaString)) {
            //nothing to do
            return;
        }
        setFormula(var, formula);
        recalculateDependencies(var);
        recalculate(var);
    }

    public void setRangeIndex(@NonNull final String var, final int rangeIndex) {
        final VariableState state = variableStateMap.get(var);
        if (state == null || state.rangeIndex == rangeIndex) {
            return;
        }
        state.rangeIndex = rangeIndex;
        recalculate(var);

    }

    /**
     * Removes a variable from this map.
     *
     * Note that the variable may still be represented in this Map after removal with an empty state.
     * This will be the case if another variable's formula depends on the removed variable.
     */
    public void remove(@NonNull final String var) {
        final VariableState state = variableStateMap.get(var);
        if (state == null) {
            return;
        }
        setFormula(var, null);
        recalculateDependencies(var);
        recalculate(var);

        if (state.isNeededBy.isEmpty()) {
            variableStateMap.remove(var);
        }
    }

    public void clear() {
        variableStateMap.clear();

    }

    /**
     * Mimics method of same name in {@link Map} interface. Same as {@link #getVars}
     */
    @NonNull
    public Set<String> keySet() {
        return getVars();
    }

    /**
     * returns a set of all vars currently present in this map. This includes vars with empty state (created due to existing dependencies)
     */
    @NonNull
    public Set<String> getVars() {
        return variableStateMap.keySet();
    }


    public boolean containsKey(final String var) {
        return variableStateMap.containsKey(var);
    }

    /**
     * Returns true if (and only if) given var is either NOT present in the map OR
     * has an empty formula and is not needed by any other var
     */
    public boolean isEmptyAndNotNeeded(final String var) {
        final VariableState state = get(var);
        return state == null || (StringUtils.isBlank(state.getFormulaString()) && state.isNeededBy.isEmpty());
    }

    /**
     * Returns all variables in this map which have a null state (e.g. are not explicitely created but only as dependencies of other vars
     */
    public Set<String> getNullEntries() {
        final Set<String> result = new HashSet<>();
        for (Map.Entry<String, VariableState> entry : variableStateMap.entrySet()) {
            if (entry.getValue().formulaString == null) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * returns a state of a var existing in this instance
     */
    @Nullable
    public VariableState get(final String var) {
        return variableStateMap.get(var);
    }

    /**
     * returns number of vars in this instance
     */
    public int size() {
        return variableStateMap.size();
    }

    public String createNonContainedKey(final String prefix) {
        int idx = 1;
        while (containsKey(prefix + idx)) {
            idx++;
        }
        return prefix + idx;
    }

    /**
     * Calculates a set of all variables which are needed (directly or indirectly) to calculate the given vars
     */
    public Set<String> calculateDependentVariables(final Collection<String> variables) {
        final Set<String> result = new HashSet<>();
        if (variables != null) {
            for (String v : variables) {
                calculateDependentVariablesInternal(result, v);
            }
        }
        return result;
    }

    private void calculateDependentVariablesInternal(final Set<String> result, final String var) {
        if (result.contains(var)) {
            return;
        }
        result.add(var);
        final VariableState state = get(var);
        if (state != null) {
            for (String dep : state.needs) {
                calculateDependentVariablesInternal(result, dep);
            }
        }
    }


    private void setFormula(final String var, final String formulaString) {
        final VariableState state = Objects.requireNonNull(get(var));
        state.formulaString = formulaString;
        state.error = null;
        state.state = State.OK;
        state.formula = null;
        state.result = null;
        state.resultAsCharSequence = null;
        try {
            state.formula = Formula.compile(formulaString);
        } catch (FormulaException ce) {
            state.state = State.ERROR;
            state.error = ce.getUserDisplayableString();
            state.resultAsCharSequence = ce.getExpressionFormatted();
        }
    }

    private void recalculateDependencies(final String var) {
        final VariableState state = Objects.requireNonNull(get(var));
        final Set<String> newNeeds;
        if (state.formula == null) {
            newNeeds = Collections.emptySet();
        } else {
            newNeeds = state.formula.getNeededVariables();
        }
        for (String v : state.needs) {
            if (!newNeeds.contains(v)) {
                final VariableState neededState = Objects.requireNonNull(get(v));
                neededState.isNeededBy.remove(var);
                if (neededState.isNeededBy.isEmpty() && neededState.formulaString == null) {
                    variableStateMap.remove(v);
                }
            }
        }
        for (String v : newNeeds) {
            if (!state.needs.contains(v)) {
                VariableState neededState = get(v);
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
        final VariableState state = get(var);
        if (state == null) {
            return;
        }
        state.state = State.OK;
        final List<List<String>> cyclesFound = new ArrayList<>();
        recalculate(state, var, true, new LinkedList<>(), cyclesFound);

        if (!cyclesFound.isEmpty()) {
            final Set<String> markError = new HashSet<>();
            for (List<String> cycle : cyclesFound) {
                int idx = 0;
                for (String cv : cycle) {
                    final VariableState s = Objects.requireNonNull(get(cv));
                    s.state = State.CYCLE;
                    s.result = null;
                    s.error = FormulaException.getUserDisplayableMessage(FormulaException.ErrorType.CYCLIC_DEPENDENCY, getCyclicString(cycle, idx++));
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

    private void recalculate(final VariableState state, final String var, final boolean start, final LinkedList<String> treePath, final List<List<String>> cyclesFound) {
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

    private boolean recalculateSingle(final VariableState state, final boolean forceError) {

        state.result = null;

        if (state.formula == null) {
            state.state = State.ERROR;
            if (state.error == null) {
                state.error = FormulaException.getUserDisplayableMessage(FormulaException.ErrorType.OTHER, "-");
            }
        } else {
            state.resultAsCharSequence = null;
            boolean hasCycleDep = false;
            for (String n : state.needs) {
                final VariableState cs = Objects.requireNonNull(get(n));
                if (cs.state == State.CYCLE) {
                    hasCycleDep = true;
                    break;
                }
            }
            if (hasCycleDep && state.state == State.CYCLE) {
                return true;
            }
            try {
                state.result = state.formula.evaluate(
                        v -> state.needs.contains(v) ? Objects.requireNonNull(get(v)).getResult() : null, state.rangeIndex); //may throw FormulaException
                state.resultAsCharSequence = state.result.toString();
                if (forceError) {
                    state.state = State.ERROR;
                    state.error = FormulaException.getUserDisplayableMessage(FormulaException.ErrorType.OTHER, "-");
                } else {
                    state.state = State.OK;
                    state.error = null;
                }
            } catch (FormulaException ce) {
                state.result = null;
                state.resultAsCharSequence = ce.getExpressionFormatted();
                state.state = State.ERROR;
                state.error = ce.getUserDisplayableString();
            }
        }
        return false;
    }

    private void markError(final String var) {
        final VariableState state = Objects.requireNonNull(get(var));
        if (state.state == State.CYCLE) {
            return;
        }
        state.result = null;
        state.resultAsCharSequence = null;
        recalculateSingle(state, true);
        for (String n : state.isNeededBy) {
            markError(n);
        }
    }

}
