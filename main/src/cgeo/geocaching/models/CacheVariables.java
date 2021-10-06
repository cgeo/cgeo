package cgeo.geocaching.models;

import cgeo.geocaching.utils.calc.CalculatorMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Stores cache variables including view state (e.g. ordering). Also handles persistence (load-from/store-to DB) */
public class CacheVariables {

    private final String geocode;
    private final CalculatorMap calculatorMap = new CalculatorMap();
    private final List<String> variables = new ArrayList<>();
    private final Set<String> variablesSet = new HashSet<>();

    public CacheVariables(final String geocode) {
        this.geocode = geocode;
        //TODO: load from DB if cache is stored
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
        return variablesSet;
    }

    public void clear() {
        getMap().clear();
        variables.clear();
        variablesSet.clear();
    }

    public void setOrder(final List<String> order) {
        this.variables.clear();
        this.variablesSet.clear();
        this.variables.addAll(order);
        this.variablesSet.addAll(order);
    }

    public void persistState() {
        //TODO: persist state to DB if cache is stored
    }
}
