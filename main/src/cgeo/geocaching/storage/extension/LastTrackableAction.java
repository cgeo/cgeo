package cgeo.geocaching.storage.extension;

import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.TrackableLog;
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

    public static LogTypeTrackable getNextAction(final TrackableBrand brand, final String trackCode) {
        final boolean autoVisit = Settings.isTrackableAutoVisit();
        if (autoVisit) {
            return LogTypeTrackable.VISITED;
        }

        final String key = buildKey(brand, trackCode);
        final DataStore.DBExtension temp = load(type, key);
        if (temp != null) {
            final LogTypeTrackable lastAction = LogTypeTrackable.getById((int) temp.getLong1());
            Log.d("get tb: key=" + key + ", action=" + lastAction);

            // what is the next possible action?
            if (lastAction == LogTypeTrackable.VISITED || lastAction == LogTypeTrackable.NOTE || lastAction == LogTypeTrackable.DISCOVERED_IT) {
                return lastAction;
            }
            if (lastAction == LogTypeTrackable.RETRIEVED_IT || lastAction == LogTypeTrackable.GRABBED_IT) {
                return LogTypeTrackable.VISITED;
            }
            // default DO_NOTHING for
            // DO_NOTHING, DROPPED_OFF, ARCHIVED, MOVE_COLLECTION, MOVE_INVENTORY
        } else {
            Log.d("get tb: key=" + key + " (not found)");
        }
        return LogTypeTrackable.DO_NOTHING;
    }

    public static void setAction(final TrackableBrand brand, final String trackCode, final LogTypeTrackable action) {
        final String key = buildKey(brand, trackCode);
        removeAll(type, key);
        add(type, key, action.id, System.currentTimeMillis(), 0, 0, "", "", "", "");
        Log.d("add tb: key=" + key + ", action=" + action);
    }

    public static void setAction(final TrackableLog trackableLog) {
        setAction(trackableLog.brand, trackableLog.trackCode, trackableLog.action);
    }

    private static String buildKey(final TrackableBrand brand, final String trackCode) {
        return "(" + brand.getId() + ")" + trackCode;
    }
}
