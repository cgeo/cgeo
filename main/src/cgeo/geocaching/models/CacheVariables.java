package cgeo.geocaching.models;

import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.calc.CalculatorMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Stores cache variables including view state (e.g. ordering). Also handles persistence (load-from/store-to DB) */
public class CacheVariables {

    private final String geocode;
    private final CalculatorMap calculatorMap = new CalculatorMap();
    private final List<String> variables = new ArrayList<>();
    private final Map<String, Long> variablesSet = new HashMap<>();

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
        this.geocode = geocode;
        loadState();
    }

    public String getGeocode() {
        return geocode;
    }

    public Object getValue(final String var) {
        final CalculatorMap.CalculatorState state = getMap().get(var);
        return state == null ? null : state.getResult();
    }

    public CalculatorMap getMap() {
        return calculatorMap;
    }

    public List<String> getVariables() {
        return variables;
    }

    public Set<String> getVariableSet() {
        return variablesSet.keySet();
    }

    public void clear() {
        getMap().clear();
        variables.clear();
        variablesSet.clear();
    }

    public void setOrder(final List<String> order) {
        this.variables.clear();
        this.variables.addAll(order);
        this.variablesSet.keySet().retainAll(order);
        for (String v : order) {
            if (!this.variablesSet.containsKey(v)) {
                this.variablesSet.put(v, null);
            }
        }
    }

    private void loadState() {
        clear();
        final List<CacheVariableDbRow> rows = DataStore.loadVariables(this.geocode);
        Collections.sort(rows, (r1, r2) -> Integer.compare(r1.varorder, r2.varorder));
        for (CacheVariableDbRow row : rows) {
            if (this.variablesSet.containsKey(row.varname)) {
                //must be a database error, ignore duplicate variable
                continue;
            }
            getMap().put(row.varname, row.formula);
            this.variables.add(row.varname);
            this.variablesSet.put(row.varname, row.id);
        }
    }

    public void persistState() {
        final List<CacheVariableDbRow> rows = new ArrayList<>();
        int idx = 0;
        for (String v : this.variables) {
            rows.add(new CacheVariableDbRow(
                this.variablesSet.get(v) == null ? -1 : Objects.requireNonNull(this.variablesSet.get(v)),
                v, idx++, Objects.requireNonNull(this.getMap().get(v)).getFormula()));
        }
        DataStore.upsertVariables(this.geocode, rows);

    }
}
