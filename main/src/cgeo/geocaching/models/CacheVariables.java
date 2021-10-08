package cgeo.geocaching.models;

import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.calc.CalculatorMap;
import cgeo.geocaching.utils.calc.Value;

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
public class CacheVariables {

    private static final char INVISIBLE_VAR_PREFIX = '_';

    private final String geocode;
    private final boolean doPersist;


    private final CalculatorMap calculatorMap = new CalculatorMap();
    private final List<String> variableList = new ArrayList<>();
    private final Map<String, Long> variablesSet = new HashMap<>();

    private boolean hasUnsavedChanges = false;

    public static class CacheVariableDbRow {
        public final long id;
        public final String varname;
        public final int varorder;
        public final String formula;

        public CacheVariableDbRow(final long id, final String varname, final int varorder, final String formula) {
            this.id = id;
            this.varname = varname;
            this.varorder = varorder;
            this.formula = formula;
        }
    }

    public CacheVariables(final String geocode) {
        this(geocode, true);
    }

    /** Constructor for usage in JUNit-tests (to prevent actual persisting to DB) */
    protected CacheVariables(@NonNull final String geocode, final boolean doPersist) {
        this.geocode = geocode;
        this.doPersist = doPersist;
        loadState();
    }

    public String getGeocode() {
        return geocode;
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
        hasUnsavedChanges = true;
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
        hasUnsavedChanges = true;
        return varname;
    }

    /** returns true if there actually was a change (var is contained and formula is different), false otherwise */
    public boolean changeVariable(final String var, final String formula) {
        if (!variablesSet.containsKey(var) || Objects.equals(calculatorMap.get(var).getFormula(), formula)) {
            return false;
        }
        calculatorMap.put(var, formula);
        hasUnsavedChanges = true;
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
        hasUnsavedChanges = true;
        return idx;
    }

    public void sortVariables(final Comparator<String> comp) {
        Collections.sort(variableList, comp);
        hasUnsavedChanges = true;
    }


    /** gets an alphabetically sorted list of all vars missing in any var for calculation */
    @NonNull
    public List<String> getAllMissingVars() {
        final List<String> varsMissing = new ArrayList<>(calculatorMap.getVars());
        varsMissing.removeAll(variableList);
        TextUtils.sortListLocaleAware(varsMissing);
        return varsMissing;
    }

    private void loadState() {
        clear();
        if (doPersist) {
            final List<CacheVariableDbRow> rows = DataStore.loadVariables(this.geocode);
            Collections.sort(rows, (r1, r2) -> Integer.compare(r1.varorder, r2.varorder));
            for (CacheVariableDbRow row : rows) {
                if (this.variablesSet.containsKey(row.varname)) {
                    //must be a database error, ignore duplicate variable
                    continue;
                }
                calculatorMap.put(row.varname, row.formula);
                this.variableList.add(row.varname);
                this.variablesSet.put(row.varname, row.id);
            }
        }
        hasUnsavedChanges = false;
    }

    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    public void saveState() {
        final List<CacheVariableDbRow> rows = new ArrayList<>();
        int idx = 0;
        for (String v : this.variableList) {
            rows.add(new CacheVariableDbRow(
                this.variablesSet.get(v) == null ? -1 : Objects.requireNonNull(this.variablesSet.get(v)),
                v, idx++, Objects.requireNonNull(this.calculatorMap.get(v)).getFormula()));
        }
        if (doPersist) {
            DataStore.upsertVariables(this.geocode, rows);
        }
        hasUnsavedChanges = false;
    }
}
