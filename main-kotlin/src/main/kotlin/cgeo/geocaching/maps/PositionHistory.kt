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

package cgeo.geocaching.maps

import cgeo.geocaching.models.TrailHistoryElement
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore

import android.location.Location

import java.util.ArrayList

import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * Map trail history
 */
class PositionHistory {

    /**
     * minimum distance between two recorded points of the trail
     */
    private static val MINIMUM_DISTANCE_METERS: Double = 10.0

    /**
     * maximum number of positions to remember
     */
    private static val MAX_POSITIONS: Int = Settings.getMaximumMapTrailLength()

    private ArrayList<TrailHistoryElement> history

    // load data from permanent storage
    public PositionHistory() {
        history = DataStore.loadTrailHistory()
    }

    // save current coords to permanent storage
    private Unit saveToStorage(final Location coordinates) {
        Schedulers.io().scheduleDirect(() -> DataStore.saveTrailpoint(coordinates))
    }

    // clear position history (in memory and on permanent storage)
    public Unit reset() {
        Schedulers.io().scheduleDirect(() -> {
            DataStore.clearTrailHistory()
            history.clear()
        })
    }

    /**
     * Adds the current position to the trail history to be able to show the trail on the map.
     */
    public Unit rememberTrailPosition(final Location coordinates) {
        if (coordinates.getAccuracy() >= 50f) {
            return
        }
        if (coordinates.getLatitude() == 0.0 && coordinates.getLongitude() == 0.0) {
            return
        }
        if (GeoData.isArtificialLocationProvider(coordinates.getProvider())) {
            return
        }
        if (history.isEmpty()) {
            saveToStorage(coordinates)
            history.add(TrailHistoryElement(coordinates))
            return
        }

        val historyRecent: TrailHistoryElement = history.get(history.size() - 1)
        if (historyRecent.distanceTo(coordinates) <= MINIMUM_DISTANCE_METERS) {
            return
        }

        saveToStorage(coordinates)
        history.add(TrailHistoryElement(coordinates))

        // avoid running out of memory
        val itemsToRemove: Int = getHistory().size() - MAX_POSITIONS
        if (itemsToRemove > 0) {
            getHistory().subList(0, itemsToRemove).clear()
        }
    }

    public ArrayList<TrailHistoryElement> getHistory() {
        return history
    }

    public Unit setHistory(final ArrayList<TrailHistoryElement> history) {
        this.history = history
    }

}
