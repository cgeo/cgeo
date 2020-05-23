package cgeo.geocaching.utils;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SelectTrackFileActivity;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import static android.app.Activity.RESULT_OK;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

public class TrackUtils {

    public static final int REQUEST_CODE_GET_TRACKFILE = 47121;

    public static class Track {
        public String trackName;
        public ArrayList<Geopoint> track;

        public Track() {
            trackName = "";
            track = new ArrayList<>();
        }
    }

    protected TrackUtils() {
    }

    /**
     * Enable/disable track related menu entries
     * @param menu menu to be configured
     */
    public static void onPrepareOptionsMenu(final Menu menu) {
        final boolean trackfileSet = StringUtils.isNotBlank(Settings.getTrackFile());
        menu.findItem(R.id.menu_hide_track).setVisible(trackfileSet);
        menu.findItem(R.id.menu_hide_track).setChecked(Settings.isHideTrack());
    }

    /**
     * Check if selected menu entry is for "load track" or hide/unhide track
     * @param activity calling activity
     * @param id menu entry id
     * @return true, if selected menu entry is track related and consumed / false else
     */
    public static boolean onOptionsItemSelected(final Activity activity, final int id) {
        if (id == R.id.menu_load_track) {
            final Intent intent = new Intent(activity, SelectTrackFileActivity.class);
            activity.startActivityForResult(intent, REQUEST_CODE_GET_TRACKFILE);
            return true;
        } else if (id == R.id.menu_hide_track) {
            Settings.setHideTrack(!Settings.isHideTrack());
            // @todo: feedback to caller?
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if current activity result is a selected track file
     * @param requestCode current requestCode to check
     * @param resultCode current resultCode to check
     * @param data additional intent data delivered
     * @return true, if successfully selected track file / false else
     */
    public static boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_CODE_GET_TRACKFILE && resultCode == RESULT_OK) {
            if (data.hasExtra(Intents.EXTRA_GPX_FILE)) {
                final String filename = data.getStringExtra(Intents.EXTRA_GPX_FILE);
                final File file = new File(filename);
                if (!file.isDirectory()) {
                    Settings.setTrackFile(filename);
                }
                return true;
            }
        }
        return false;
    }

}
