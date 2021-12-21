package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.formulas.VariableList;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores cache variables including view state (e.g. ordering).
 * This class handles persistence (load-from/store-to DB), allows for reset and change listeners.
 */
public class CacheVariableList extends VariableList {

    private final String geocode;

    private Map<Object, Consumer<String>> changeListener = new HashMap<>();

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

    private boolean recalculateWaypoints() {

        boolean hasCalculatedWp = false;
        final Geocache cache = DataStore.loadCache(this.geocode, LoadFlags.LOAD_CACHE_OR_DB);
        if (cache != null) {
            for (Waypoint wp : cache.getWaypoints()) {
                if (wp.isCalculated()) {
                    hasCalculatedWp = true;
                    final CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig(wp.getCalcStateJson());
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
