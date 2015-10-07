package cgeo.geocaching.maps;

import android.location.Location;

import java.util.ArrayList;

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
    private static final int MAX_POSITIONS = 700;

    private ArrayList<Location> history = new ArrayList<>();

    /**
     * Adds the current position to the trail history to be able to show the trail on the map.
     */
    void rememberTrailPosition(final Location coordinates) {
        if (coordinates.getAccuracy() >= 50f) {
            return;
        }
        if (coordinates.getLatitude() == 0.0 && coordinates.getLongitude() == 0.0) {
            return;
        }
        if (history.isEmpty()) {
            history.add(coordinates);
            return;
        }

        final Location historyRecent = history.get(history.size() - 1);
        if (historyRecent.distanceTo(coordinates) <= MINIMUM_DISTANCE_METERS) {
            return;
        }

        history.add(coordinates);

        // avoid running out of memory
        final int itemsToRemove = getHistory().size() - MAX_POSITIONS;
        if (itemsToRemove > 0) {
            for (int i = 0; i < itemsToRemove; i++) {
                getHistory().remove(0);
            }
        }
    }

    public ArrayList<Location> getHistory() {
        return history;
    }

    public void setHistory(final ArrayList<Location> history) {
        this.history = history;
    }

}