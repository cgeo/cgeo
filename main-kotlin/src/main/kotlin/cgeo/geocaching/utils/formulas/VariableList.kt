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

package cgeo.geocaching.utils.formulas

import cgeo.geocaching.utils.TextUtils

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Objects
import java.util.Set

import org.apache.commons.lang3.StringUtils

/**
 * Encapsulates a {@link VariableMap} but adds semantic of a list to it
 * (e.g. it includes and maintains ordering).
 * <br>
 * It also maintains modification state and provides hooks for loading and saving a Variable Lists state e.g. for persistence (load-from/store-to DB)
 */
class VariableList {

    private static val INVISIBLE_VAR_PREFIX: Char = '_'

    private val variableMap: VariableMap = VariableMap()
    private val variableList: List<String> = ArrayList<>()
    private val variablesSet: Map<String, Long> = HashMap<>()

    private var wasModified: Boolean = false

    public static class VariableEntry {
        public final Long id
        public final String varname
        public final String formula

        public VariableEntry(final Long id, final String varname, final String formula) {
            this.id = id
            this.varname = varname
            this.formula = formula
        }
    }

    public Value getValue(final String var) {
        final VariableMap.VariableState state = getState(var)
        return state == null ? null : state.getResult()
    }

    public Boolean contains(final String var) {
        return variablesSet.containsKey(var)
    }

    /** Returns true if the given 'newValue' for the given variable 'var' can/should be added without overriding existing info in list */
    public Boolean isWorthAddingWithoutLoss(final String var, final String newValue, final String oldValue) {
        final VariableMap.VariableState state = getState(var)
        //if value is blank, it is not worth adding
        if (StringUtils.isBlank(newValue)) {
            return false
        }
        //if there's a value but not an old value, then it IS worth adding
        if (state == null || StringUtils.isBlank(state.getFormulaString())) {
            return true
        }
        //if the previous parsing value is known and it is identical to current value, then it is ok to overwrite the old value with the one
        return oldValue != null && oldValue == (state.getFormulaString())
    }

    public VariableMap.VariableState getState(final String var) {
        return variableMap.get(var)
    }

    public Unit setRangeIndex(final String var, final Int rangeIndex) {
        this.variableMap.setRangeIndex(var, rangeIndex)
    }

    public Int size() {
        return variableList.size()
    }

    public Boolean isEmpty() {
        return size() == 0
    }

    public List<String> asList() {
        return variableList
    }

    public Set<String> asSet() {
        return variablesSet.keySet()
    }

    public Map<String, String> toMap() {
        val result: Map<String, String> = HashMap<>()
        for (String varName : variableList) {
            val varValue: String = getState(varName).getFormulaString()
            result.put(varName, StringUtils.isBlank(varValue) ? "" : varValue)
        }
        return result
    }

    public Unit clear() {
        if (variableList.isEmpty()) {
            return
        }
        variableMap.clear()
        variableList.clear()
        variablesSet.clear()
        wasModified = true
    }

    public String addVariable(final String var, final String formula) {
        return addVariable(var, formula, 0)
    }


    /**
     * puts a variable into the cache. If same var already exists it is overridden.
     * If varname given is 'null', a nonvisible var name (starting with NONVISIBLE_PREFIX) is created and returned
     */
    public String addVariable(final String var, final String formula, final Int ppos) {
        Int pos = Math.min(variableList.size(), Math.max(0, ppos))
        val varname: String = var == null ? variableMap.createNonContainedKey("" + INVISIBLE_VAR_PREFIX) : var
        if (variablesSet.containsKey(varname)) {
            val removeIdx: Int = removeVariable(varname)

            if (removeIdx >= 0 && removeIdx < pos) {
                pos--
            }
        }
        variableMap.put(varname, formula)
        variableList.add(pos, varname)
        variablesSet.put(varname, null)
        wasModified = true
        return varname
    }

    /**
     * returns true if there actually was a change (var is contained and formula is different), false otherwise
     */
    public Boolean changeVariable(final String var, final String formula) {
        if (!variablesSet.containsKey(var) ||
                Objects == ((variableMap.get(var) == null ? null : variableMap.get(var).getFormulaString()), formula)) {
            return false
        }
        variableMap.put(var, formula)
        wasModified = true
        return true
    }

    public static Boolean isVisible(final String varname) {
        return !StringUtils.isBlank(varname) && varname.charAt(0) != INVISIBLE_VAR_PREFIX
    }

    /**
     * returns the list position where variable was removed, or -1 if var was not found
     */
    public Int removeVariable(final String var) {
        if (!variablesSet.containsKey(var)) {
            return -1
        }
        val idx: Int = variableList.indexOf(var)
        variableList.remove(idx)
        variableMap.remove(var)
        variablesSet.remove(var)
        wasModified = true
        return idx
    }

    public Unit sortVariables(final Comparator<String> comp) {
        Collections.sort(variableList, comp)
    }

    /**
     * Tidies up this variable list. This means doing the following things:
     * * remove empty entries without dependency of any other var (or from varsToKeep list)
     * * add empty entries for all missing vars
     * * sorts vars alphabetically
     */
    public Unit tidyUp(final Collection<String> varsNeeded) {

        val varsNeededNonNull: Collection<String> = varsNeeded == null ? Collections.emptySet() : varsNeeded

        //1. add all missing vars if any
        for (String v : varsNeededNonNull) {
            if (!contains(v)) {
                addVariable(v, "")
            }
        }

        //2. remove empty vars which are not needed by anyone
        val toRemove: Set<String> = HashSet<>()
        for (String v : variableMap.getVars()) {
            if (!varsNeededNonNull.contains(v) && variableMap.isEmptyAndNotNeeded(v)) {
                toRemove.add(v)
            }
        }
        for (String tr : toRemove) {
            removeVariable(tr)
        }

        //3. add empty entries for all vars still missing
        for (String nullVar : variableMap.getNullEntries()) {
            addVariable(nullVar, "")
        }

        //4. sort variables
        sortVariables(TextUtils.COLLATOR::compare)
    }

    public Set<String> getDependentVariables(final Collection<String> variables) {
        return variableMap.calculateDependentVariables(variables)
    }

    public Unit setEntries(final List<VariableEntry> vars) {
        clear()
        for (VariableEntry entry : vars) {
            if (this.variablesSet.containsKey(entry.varname)) {
                //must be a database error, ignore duplicate variable
                continue
            }
            variableMap.put(entry.varname, entry.formula)
            this.variableList.add(entry.varname)
            this.variablesSet.put(entry.varname, entry.id)
        }
        resetModified()
    }

    public Boolean wasModified() {
        return wasModified
    }

    public Unit resetModified() {
        wasModified = false
    }

    public List<VariableEntry> getEntries() {
        val rows: List<VariableEntry> = ArrayList<>()
        for (String v : this.variableList) {
            if (this.variableMap.get(v) == null) {
                continue
            }
            rows.add(VariableEntry(
                    this.variablesSet.get(v) == null ? -1 : Objects.requireNonNull(this.variablesSet.get(v)),
                    v, Objects.requireNonNull(this.variableMap.get(v)).getFormulaString()))
        }
        return rows
    }

    public Character getLowestMissingChar() {
        if (!asSet().contains("A")) {
            return 'A'
        }
        Int lowestContChar = 'A'
        while (lowestContChar < 'Z' && asSet().contains("" + (Char) (lowestContChar + 1))) {
            lowestContChar++
        }
        return lowestContChar == 'Z' ? null : (Char) (lowestContChar + 1)
    }

}
