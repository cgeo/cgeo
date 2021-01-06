package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.files.GPXTrackOrRouteImporter;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.PersistableUri;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.widget.Toast;

public class TrackUtils {

    private final Activity activity;

    private final ContentStorageActivityHelper fileSelector;

    public TrackUtils(final Activity activity) {
        this.activity = activity;
        this.fileSelector = new ContentStorageActivityHelper(activity);
    }

    /**
     * Enable/disable track related menu entries
     * @param menu menu to be configured
     */
    public void onPrepareOptionsMenu(final Menu menu) {
        final boolean trackfileSet = PersistableUri.TRACK.hasValue();
        menu.findItem(R.id.menu_center_on_track).setVisible(trackfileSet);
        menu.findItem(R.id.menu_unload_track).setVisible(trackfileSet);
    }

    /**
     * Check if selected menu entry is for "load track" or hide/unhide track
     * @param activity calling activity
     * @param id menu entry id
     * @return true, if selected menu entry is track related and consumed / false else
     */
    public boolean onOptionsItemSelected(final int id, final Route tracks, final Route.UpdateRoute updateTracks, final Route.CenterOnPosition centerOnPosition) {
        if (id == R.id.menu_load_track) {
            if (null == tracks || tracks.getNumSegments() == 0) {
                startIndividualTrackFileSelector(updateTracks);
            } else {
                Dialogs.confirm(activity, R.string.map_load_track, R.string.map_load_track_confirm, (dialog, which) ->
                    startIndividualTrackFileSelector(updateTracks));
            }
        } else if (id == R.id.menu_unload_track) {
            ContentStorage.get().setPersistedDocumentUri(PersistableUri.TRACK, null);
            updateTracks.updateRoute(null);
        } else if (id == R.id.menu_center_on_track) {
            if (null != tracks) {
                tracks.setCenter(centerOnPosition);
            }
        } else {
            return false;
        }
        return true;
    }

    private void startIndividualTrackFileSelector(final Route.UpdateRoute updateTracks) {

        fileSelector.selectPersistableUri(PersistableUri.TRACK, uri -> {
            if (uri != null && updateTracks != null) {
                loadTracks(updateTracks);
            }
        });
    }

    /**
     * Check if current activity result is a selected track file
     * @param requestCode current requestCode to check
     * @param resultCode current resultCode to check
     * @param data additional intent data delivered
     * @return true, if successfully selected track file / false else
     */
    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        return fileSelector.onActivityResult(requestCode, resultCode, data);
    }

    public void loadTracks(final Route.UpdateRoute updateRoute) {
        final Uri uri = PersistableUri.TRACK.getUri();
        if (null != uri) {
            GPXTrackOrRouteImporter.doImport(activity, PersistableUri.TRACK.getUri(), updateRoute);
        }
        ActivityMixin.invalidateOptionsMenu(activity);
    }

    public void showTrackInfo(final Route route) {
        if (null != route) {
            final int numPoints = route.getNumPoints();
            Toast.makeText(activity, activity.getResources().getQuantityString(R.plurals.load_track_success, numPoints, numPoints), Toast.LENGTH_SHORT).show();
        }
    }
}
