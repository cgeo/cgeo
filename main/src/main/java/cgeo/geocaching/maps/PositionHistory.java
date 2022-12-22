package cgeo.geocaching.maps;

import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;

import android.location.Location;

import java.util.ArrayList;

import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Map trail history
 */
public class PositionHistory {

    /**
     * minimum distance between two recorded points of the trail
     */
    private static final double MINIMUM_DISTANCE_METERS = 10.0;

    /**
     * maximum number of positions to remember
     */
    private static final int MAX_POSITIONS = Settings.getMaximumMapTrailLength();

    private ArrayList<TrailHistoryElement> history = new ArrayList<>();

    // load data from permanent storage
    public PositionHistory() {
        history = DataStore.loadTrailHistory();
    }

    // save current coords to permanent storage
    private void saveToStorage(final Location coordinates) {
        Schedulers.io().scheduleDirect(() -> DataStore.saveTrailpoint(coordinates));
    }

    // clear position history (in memory and on permanent storage)
    public void reset() {
        Schedulers.io().scheduleDirect(() -> {
            DataStore.clearTrailHistory();
            history.clear();
        });
    }

    /**
     * Adds the current position to the trail history to be able to show the trail on the map.
     */
    public void rememberTrailPosition(final Location coordinates) {
        if (coordinates.getAccuracy() >= 50f) {
            return;
        }
        if (coordinates.getLatitude() == 0.0 && coordinates.getLongitude() == 0.0) {
            return;
        }
        if (GeoData.isArtificialLocationProvider(coordinates.getProvider())) {
            return;
        }
        if (history.isEmpty()) {
            saveToStorage(coordinates);
            history.add(new TrailHistoryElement(coordinates));
            return;
        }

        final TrailHistoryElement historyRecent = history.get(history.size() - 1);
        if (historyRecent.distanceTo(coordinates) <= MINIMUM_DISTANCE_METERS) {
            return;
        }

        saveToStorage(coordinates);
        history.add(new TrailHistoryElement(coordinates));

        // avoid running out of memory
        final int itemsToRemove = getHistory().size() - MAX_POSITIONS;
        if (itemsToRemove > 0) {
            for (int i = 0; i < itemsToRemove; i++) {
                getHistory().remove(0);
            }
        }
    }

    public ArrayList<TrailHistoryElement> getHistory() {
        return history;
    }

    public void setHistory(final ArrayList<TrailHistoryElement> history) {
        this.history = history;
    }

}
