package cgeo.geocaching.models;

import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.formulas.VariableList;

import androidx.annotation.NonNull;

import java.util.List;

/** Stores cache variables including view state (e.g. ordering). Also handles persistence (load-from/store-to DB) */
public class CacheVariableList extends VariableList {

    private final String geocode;

    public CacheVariableList(@NonNull final String geocode) {
        this.geocode = geocode;
        loadState();
    }

    public String getGeocode() {
        return geocode;
    }


    private void loadState() {
        this.clear();
        final List<VariableList.VariableEntry> rows = DataStore.loadVariables(this.geocode);
        this.setEntries(rows);
    }

    public void saveState() {
        final List<VariableList.VariableEntry> rows = this.getEntries();
        DataStore.upsertVariables(this.geocode, rows);
        this.resetModified();
    }
}
