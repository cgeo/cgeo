package cgeo.geocaching.utils;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SelectTrackFileActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.files.GPXTrackOrRouteImporter;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.Toast;
import static android.app.Activity.RESULT_OK;

import androidx.annotation.Nullable;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

public class TrackUtils {

    private static final int REQUEST_CODE_GET_TRACKFILE = 47121;

    protected TrackUtils() {
    }

    /**
     * Enable/disable track related menu entries
     * @param menu menu to be configured
     */
    public static void onPrepareOptionsMenu(final Menu menu) {
        final boolean trackfileSet = StringUtils.isNotBlank(Settings.getTrackFile());
        menu.findItem(R.id.menu_center_on_track).setVisible(trackfileSet);
        menu.findItem(R.id.menu_unload_track).setVisible(trackfileSet);
    }

    /**
     * Check if selected menu entry is for "load track" or hide/unhide track
     * @param activity calling activity
     * @param id menu entry id
     * @return true, if selected menu entry is track related and consumed / false else
     */
    public static boolean onOptionsItemSelected(final Activity activity, final int id, final Route tracks, final Runnable hideOptionsChanged, final Route.UpdateRoute updateTracks, final Route.CenterOnPosition centerOnPosition) {
        if (id == R.id.menu_load_track) {
            if (null == tracks || tracks.getNumSegments() == 0) {
                startIndividualTrackFileSelector(activity);
            } else {
                Dialogs.confirm(activity, R.string.map_load_track, R.string.map_load_track_confirm, (dialog, which) -> startIndividualTrackFileSelector(activity));
            }
        } else if (id == R.id.menu_unload_track) {
            Settings.setTrackFile(null);
            updateTracks.updateRoute(null);
        } else if (id == R.id.menu_hide_track) {
            Settings.setHideTrack(!Settings.isHideTrack());
            hideOptionsChanged.run();
        } else if (id == R.id.menu_center_on_track) {
            if (null != tracks) {
                tracks.setCenter(centerOnPosition);
            }
        } else {
            return false;
        }
        return true;
    }

    private static void startIndividualTrackFileSelector(final Activity activity) {
        final Intent intent = new Intent(activity, SelectTrackFileActivity.class);
        activity.startActivityForResult(intent, REQUEST_CODE_GET_TRACKFILE);
    }

    /**
     * Check if current activity result is a selected track file
     * @param requestCode current requestCode to check
     * @param resultCode current resultCode to check
     * @param data additional intent data delivered
     * @return true, if successfully selected track file / false else
     */
    public static boolean onActivityResult(final Activity activity, final int requestCode, final int resultCode, final Intent data, @Nullable final Route.UpdateRoute updateRoute) {
        if (requestCode == REQUEST_CODE_GET_TRACKFILE && resultCode == RESULT_OK && data.hasExtra(Intents.EXTRA_GPX_FILE)) {
            final String filename = data.getStringExtra(Intents.EXTRA_GPX_FILE);
            if (null != filename) {
                final File file = new File(filename);
                if (!file.isDirectory()) {
                    Settings.setTrackFile(filename);
                    if (null != updateRoute) {
                        loadTracks(activity, updateRoute);
                    }
                }
            }
            return true;
        }
        return false;
    }

    public static void loadTracks(final Activity activity, final Route.UpdateRoute updateRoute) {
        final String trackfile = Settings.getTrackFile();
        if (null != trackfile) {
            GPXTrackOrRouteImporter.doImport(activity, new File(trackfile), updateRoute);
        }
        ActivityMixin.invalidateOptionsMenu(activity);
    }

    public static void showTrackInfo(final Activity activity, final Route route) {
        if (null != route) {
            final int numPoints = route.getNumPoints();
            Toast.makeText(activity, activity.getResources().getQuantityString(R.plurals.load_track_success, numPoints, numPoints), Toast.LENGTH_SHORT).show();
        }
    }
}
