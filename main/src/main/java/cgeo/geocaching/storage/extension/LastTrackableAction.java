package cgeo.geocaching.storage.extension;

import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

public class LastTrackableAction extends DataStore.DBExtension {

    /**
     * key = (brandId)trackCode
     * brandId = TrackableBrand.id
     * long1 = actionId = LogTypeTrackable.id
     * long2 = date/time of last log (to allow for age-driven cleanup)
     */

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_LAST_TRACKABLE_ACTION;

    public static LogTypeTrackable getNextAction(final String tGeocode) {
        final boolean autoVisit = Settings.isTrackableAutoVisit();
        if (autoVisit) {
            return LogTypeTrackable.VISITED;
        }
        if (tGeocode == null) {
            return LogTypeTrackable.DO_NOTHING;
        }

        final DataStore.DBExtension temp = load(type, tGeocode);
        if (temp != null) {
            final LogTypeTrackable lastAction = LogTypeTrackable.getById((int) temp.getLong1());
            Log.d("get tb: key=" + tGeocode + ", action=" + lastAction);

            // what is the next possible action?
            if (lastAction == LogTypeTrackable.VISITED || lastAction == LogTypeTrackable.NOTE || lastAction == LogTypeTrackable.DISCOVERED_IT) {
                return lastAction;
            }
            // default DO_NOTHING for
            // RETRIEVED_IT, GRABBED_IT (autoVisit is handled above)
            // DO_NOTHING, DROPPED_OFF, ARCHIVED, MOVE_COLLECTION, MOVE_INVENTORY
        } else {
            Log.d("get tb: key=" + tGeocode + " (not found)");
        }
        return LogTypeTrackable.DO_NOTHING;
    }

    public static void setAction(final String tGeocode, final LogTypeTrackable action) {
        if (tGeocode == null) {
            return;
        }
        removeAll(type, tGeocode);
        add(type, tGeocode, action.id, System.currentTimeMillis(), 0, 0, "", "", "", "");
        Log.d("add tb: key=" + tGeocode + ", action=" + action);
    }

}
