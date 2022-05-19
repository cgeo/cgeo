package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.formulas.VariableList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores cache variables including view state (e.g. ordering).
 * This class handles persistence (load-from/store-to DB), allows for reset and change listeners.
 */
public class CacheVariableList extends VariableList {

    private final String geocode;
    private final Map<Object, Consumer<String>> changeListener = new HashMap<>();

    public CacheVariableList(@NonNull final String geocode) {
        this.geocode = geocode;
        loadState();
    }

    public String getGeocode() {
        return geocode;
    }

    public void addChangeListener(final Object key, final Consumer<String> callback) {
        this.changeListener.put(key, callback);
    }

    public void removeChangeListener(final Object key) {
        this.changeListener.remove(key);
    }

    private void callCallbacks() {
        for (Consumer<String> c : this.changeListener.values()) {
            c.accept(this.geocode);
        }
    }

    private void loadState() {
        final List<VariableList.VariableEntry> rows = DataStore.loadVariables(this.geocode);
        this.setEntries(rows);
    }

    public void reloadLastSavedState() {
        if (!this.wasModified()) {
            return;
        }

        this.loadState();
        this.recalculateWaypoints();
        this.callCallbacks();
    }

    public void saveState() {
        if (!this.wasModified()) {
            return;
        }
        final List<VariableList.VariableEntry> rows = this.getEntries();
        DataStore.upsertVariables(this.geocode, rows);

        this.recalculateWaypoints();
        this.resetModified();
        this.callCallbacks();
    }

    @Override
    public void tidyUp(@Nullable final Collection<String> varsNeeded) {
        final Set<String> varsNeededReal = new HashSet<>();
        if (varsNeeded != null) {
            varsNeededReal.addAll(varsNeeded);
        }
        addAllWaypointNeededVars(varsNeededReal);
        super.tidyUp(varsNeededReal);
    }

    private void addAllWaypointNeededVars(final Set<String> neededVars) {
        final Geocache cache = DataStore.loadCache(this.geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (cache != null) {
            for (Waypoint wp : cache.getWaypoints()) {
                if (wp.isCalculated()) {
                    final CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig(wp.getCalcStateConfig());
                    neededVars.addAll(cc.getNeededVars());
                }
            }
        }
    }

    private boolean recalculateWaypoints() {
        final Geocache cache = DataStore.loadCache(this.geocode, LoadFlags.LOAD_CACHE_OR_DB);
        return recalculateWaypoints(cache);
    }

    public boolean recalculateWaypoints(final Geocache cache) {

        boolean hasCalculatedWp = false;
        if (cache != null) {
            for (Waypoint wp : cache.getWaypoints()) {
                if (wp.isCalculated()) {
                    hasCalculatedWp = true;
                    final CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig(wp.getCalcStateConfig());
                    final Geopoint gp = cc.calculateGeopoint(this::getValue);
                    wp.setCoords(gp);
                }
            }
            if (hasCalculatedWp) {
                DataStore.saveWaypoints(cache);
            }
        }
        return hasCalculatedWp;
    }
}
