package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.export.TrailHistoryExport;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.view.Menu;

public class HistoryTrackUtils {

    protected HistoryTrackUtils() {
        // utility class
    }

    /**
     * Enable/disable history track related menu entries
     *
     * @param menu menu to be configured
     */
    public static void onPrepareOptionsMenu(final Menu menu) {
        menu.findItem(R.id.menu_trail_show).setVisible(!Settings.isMapTrail());
        menu.findItem(R.id.menu_trail_hide).setVisible(Settings.isMapTrail());
    }

    /**
     * Check if selected menu entry is for "load track" or hide/unhide track
     *
     * @param activity calling activity
     * @param id       menu entry id
     * @return true, if selected menu entry is track related and consumed / false else
     */
    public static boolean onOptionsItemSelected(final Activity activity, final int id, final Runnable redrawHistoryTrack, final Runnable clearTrailHistory) {
        if (id == R.id.menu_trail_show) {
            Settings.setMapTrail(true);
            redrawHistoryTrack.run();
            ActivityMixin.invalidateOptionsMenu(activity);
        } else if (id == R.id.menu_trail_hide) {
            Settings.setMapTrail(false);
            redrawHistoryTrack.run();
            ActivityMixin.invalidateOptionsMenu(activity);
        } else if (id == R.id.menu_clear_trailhistory) {
            clearTrailHistory.run();
        } else if (id == R.id.menu_export_trailhistory) {
            new TrailHistoryExport(activity, clearTrailHistory);
        } else {
            return false;
        }
        return true;
    }


}
