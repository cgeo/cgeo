package cgeo.geocaching.storage.extension;

import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

import androidx.annotation.Nullable;

public class LastTrackableAction extends DataStore.DBExtension {

    /**
     * key = (brandId)trackCode
     * brandId = TrackableBrand.id
     * long1 = actionId = LogTypeTrackable.id
     * long2 = date/time of last log (to allow for age-driven cleanup)
     */

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_LAST_TRACKABLE_ACTION;

    @Nullable
    public static LogTypeTrackable getLastAction(final String tGeocode) {

        final DataStore.DBExtension temp = load(type, tGeocode);
        if (temp != null) {
            final LogTypeTrackable lastAction = LogTypeTrackable.getById((int) temp.getLong1());
            Log.d("get tb: key=" + tGeocode + ", action=" + lastAction);

            return lastAction;
        } else {
            Log.d("get tb: key=" + tGeocode + " (not found)");
        }
        return null;
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
