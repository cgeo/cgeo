package cgeo.geocaching.utils.calc;

import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/** Stores cache variables including view state (e.g. ordering). Also handles persistence (load-from/store-to DB) */
public class VariableList {

    private static final char INVISIBLE_VAR_PREFIX = '_';

    private final CalculatorMap calculatorMap = new CalculatorMap();
    private final List<String> variableList = new ArrayList<>();
    private final Map<String, Long> variablesSet = new HashMap<>();

    private boolean wasModified = false;

    public static class VariableEntry {
        public final long id;
        public final String varname;
        public final String formula;

        public VariableEntry(final long id, final String varname, final String formula) {
            this.id = id;
            this.varname = varname;
            this.formula = formula;
        }
    }

    @Nullable
    public Value getValue(final String var) {
        final CalculatorMap.CalculatorState state = getState(var);
        return state == null ? null : state.getResult();
    }

    @Nullable
    public CalculatorMap.CalculatorState getState(final String var) {
        return calculatorMap.get(var);
    }

    public int size() {
        return variableList.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public List<String> getVariableList() {
        return variableList;
    }

    public Set<String> getVariableSet() {
        return variablesSet.keySet();
    }

    public void clear() {
        if (variableList.isEmpty()) {
            return;
        }
        calculatorMap.clear();
        variableList.clear();
        variablesSet.clear();
        wasModified = true;
    }

    /**
     * puts a new variable into the cache. If same var already exists it is overridden.
     * If varname given is 'null', a nonvisible var name (starting with NONVISIBLE_PREFIX) is created and returned
     */
    @NonNull
    public String addVariable(@Nullable  final String var, @Nullable final String formula, final int ppos) {
        int pos = Math.min(variableList.size(), Math.max(0, ppos));
        final String varname = var == null ? calculatorMap.createNonContainedKey("" + INVISIBLE_VAR_PREFIX) : var;
        if (variablesSet.containsKey(varname)) {
            final int removeIdx = removeVariable(varname);

            if (removeIdx >= 0 && removeIdx < pos) {
                pos--;
            }
        }
        calculatorMap.put(varname, formula);
        variableList.add(pos, varname);
        variablesSet.put(varname, null);
        wasModified = true;
        return varname;
    }

    /** returns true if there actually was a change (var is contained and formula is different), false otherwise */
    public boolean changeVariable(final String var, final String formula) {
        if (!variablesSet.containsKey(var) ||
            Objects.equals(Objects.requireNonNull(calculatorMap.get(var)).getFormula(), formula)) {
            return false;
        }
        calculatorMap.put(var, formula);
        wasModified = true;
        return true;
    }

    public static boolean isVisible(final String varname) {
        return !StringUtils.isBlank(varname) && varname.charAt(0) != INVISIBLE_VAR_PREFIX;
    }

    /** returns the list position where variable was removed, or -1 if var was not found */
    public int removeVariable(@NonNull final String var) {
        if (!variablesSet.containsKey(var)) {
            return -1;
        }
        final int idx = variableList.indexOf(var);
        variableList.remove(idx);
        calculatorMap.remove(var);
        variablesSet.remove(var);
        wasModified = true;
        return idx;
    }

    public void sortVariables(final Comparator<String> comp) {
        Collections.sort(variableList, comp);
        wasModified = true;
    }


    /** gets an alphabetically sorted list of all vars missing in any var for calculation */
    @NonNull
    public List<String> getAllMissingVars() {
        final List<String> varsMissing = new ArrayList<>(calculatorMap.getVars());
        varsMissing.removeAll(variableList);
        TextUtils.sortListLocaleAware(varsMissing);
        return varsMissing;
    }

    public void setEntries(final List<VariableEntry> vars) {
        clear();
        for (VariableEntry entry : vars) {
            if (this.variablesSet.containsKey(entry.varname)) {
                //must be a database error, ignore duplicate variable
                continue;
            }
            calculatorMap.put(entry.varname, entry.formula);
            this.variableList.add(entry.varname);
            this.variablesSet.put(entry.varname, entry.id);
        }
        wasModified = false;
    }

    public boolean wasModified() {
        return wasModified;
    }

    public void resetModified() {
        wasModified = false;
    }

    public List<VariableEntry> getEntries() {
        final List<VariableEntry> rows = new ArrayList<>();
        for (String v : this.variableList) {
            rows.add(new VariableEntry(
                this.variablesSet.get(v) == null ? -1 : Objects.requireNonNull(this.variablesSet.get(v)),
                v, Objects.requireNonNull(this.calculatorMap.get(v)).getFormula()));
        }
        return rows;
    }

    @Nullable
    public Character getLowestMissingChar() {
        if (!getVariableSet().contains("A")) {
            return 'A';
        }
        int lowestContChar = 'A';
        while (lowestContChar < 'Z' && getVariableSet().contains("" + (char) (lowestContChar + 1))) {
            lowestContChar++;
        }
        return lowestContChar == 'Z' ? null : (char) (lowestContChar + 1);
    }
}
