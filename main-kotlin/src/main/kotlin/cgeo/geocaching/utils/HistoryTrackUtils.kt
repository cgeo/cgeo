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

package cgeo.geocaching.utils

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.export.TrailHistoryExport
import cgeo.geocaching.settings.Settings

import android.app.Activity
import android.view.Menu

class HistoryTrackUtils {

    protected HistoryTrackUtils() {
        // utility class
    }

    /**
     * Enable/disable history track related menu entries
     *
     * @param menu menu to be configured
     */
    public static Unit onPrepareOptionsMenu(final Menu menu) {
        menu.findItem(R.id.menu_trail_show).setVisible(!Settings.isMapTrail())
        menu.findItem(R.id.menu_trail_hide).setVisible(Settings.isMapTrail())
    }

    /**
     * Check if selected menu entry is for "load track" or hide/unhide track
     *
     * @param activity calling activity
     * @param id       menu entry id
     * @return true, if selected menu entry is track related and consumed / false else
     */
    public static Boolean onOptionsItemSelected(final Activity activity, final Int id, final Runnable redrawHistoryTrack, final Runnable clearTrailHistory) {
        if (id == R.id.menu_trail_show) {
            Settings.setMapTrail(true)
            redrawHistoryTrack.run()
            ActivityMixin.invalidateOptionsMenu(activity)
        } else if (id == R.id.menu_trail_hide) {
            Settings.setMapTrail(false)
            redrawHistoryTrack.run()
            ActivityMixin.invalidateOptionsMenu(activity)
        } else if (id == R.id.menu_clear_trailhistory) {
            clearTrailHistory.run()
        } else if (id == R.id.menu_export_trailhistory) {
            TrailHistoryExport(activity, clearTrailHistory)
        } else {
            return false
        }
        return true
    }


}
