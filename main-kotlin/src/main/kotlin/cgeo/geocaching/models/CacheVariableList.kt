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

package cgeo.geocaching.models

import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.formulas.FormulaUtils
import cgeo.geocaching.utils.formulas.VariableList

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.util.Consumer

import java.util.Collection
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Set

/**
 * Stores cache variables including view state (e.g. ordering).
 * This class handles persistence (load-from/store-to DB), allows for reset and change listeners.
 */
class CacheVariableList : VariableList() {

    private final String geocode
    private final Map<Object, Consumer<String>> changeListener = HashMap<>()

    public CacheVariableList(final String geocode) {
        this.geocode = geocode
        loadState()
    }

    public String getGeocode() {
        return geocode
    }

    public Unit addChangeListener(final Object key, final Consumer<String> callback) {
        this.changeListener.put(key, callback)
    }

    public Unit removeChangeListener(final Object key) {
        this.changeListener.remove(key)
    }

    private Unit callCallbacks() {
        for (Consumer<String> c : this.changeListener.values()) {
            c.accept(this.geocode)
        }
    }

    private Unit loadState() {
        val rows: List<VariableList.VariableEntry> = DataStore.loadVariables(this.geocode)
        this.setEntries(rows)
    }

    public Unit reloadLastSavedState() {
        if (!this.wasModified()) {
            return
        }

        this.loadState()
        this.recalculateWaypoints()
        this.callCallbacks()
    }

    public Unit saveState() {
        if (!this.wasModified()) {
            return
        }
        val rows: List<VariableList.VariableEntry> = this.getEntries()
        DataStore.upsertVariables(this.geocode, rows)

        this.recalculateWaypoints()
        this.resetModified()
        this.callCallbacks()
    }

    override     public Unit tidyUp(final Collection<String> varsNeeded) {
        val varsNeededReal: Set<String> = HashSet<>()
        if (varsNeeded != null) {
            varsNeededReal.addAll(varsNeeded)
        }
        addAllWaypointNeededVars(varsNeededReal)
        super.tidyUp(varsNeededReal)
    }

    private Unit addAllWaypointNeededVars(final Set<String> neededVars) {
        val cache: Geocache = DataStore.loadCache(this.geocode, LoadFlags.LOAD_CACHE_OR_DB)
        if (cache != null) {
            for (Waypoint wp : cache.getWaypoints()) {
                if (wp.isCalculated()) {
                    val cc: CalculatedCoordinate = CalculatedCoordinate.createFromConfig(wp.getCalcStateConfig())
                    neededVars.addAll(cc.getNeededVars())
                }
                if (wp.hasProjection()) {
                    FormulaUtils.addNeededVariables(neededVars, wp.getProjectionFormula1())
                    FormulaUtils.addNeededVariables(neededVars, wp.getProjectionFormula2())
                }
            }
        }
    }

    private Boolean recalculateWaypoints() {
        val cache: Geocache = DataStore.loadCache(this.geocode, LoadFlags.LOAD_CACHE_OR_DB)
        return cache != null && cache.recalculateWaypoints(this)
    }
}
