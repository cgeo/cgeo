package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.files.GPXTrackOrRouteImporter;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.PersistableUri;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

public class TrackUtils {

    private static final String STATE_CSAH = "csam";

    private final Activity activity;

    private final ContentStorageActivityHelper fileSelector;
    private final Route.UpdateRoute updateTracks;
    private final Route.CenterOnPosition centerOnPosition;

    public TrackUtils(final Activity activity, final Bundle savedState, final Route.UpdateRoute updateTracks, final Route.CenterOnPosition centerOnPosition) {
        this.activity = activity;
        this.updateTracks = updateTracks;
        this.centerOnPosition = centerOnPosition;
        this.fileSelector = new ContentStorageActivityHelper(activity, savedState == null ? null : savedState.getBundle(STATE_CSAH))
        .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE_PERSISTED, PersistableUri.class, uri -> {
            if (uri != null && this.updateTracks != null) {
                loadTracks(this.updateTracks);
            }
        });
    }

    public Bundle getState() {
        final Bundle state = new Bundle();
        state.putBundle(STATE_CSAH, fileSelector.getState());
        return state;
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
    public boolean onOptionsItemSelected(final int id, final Route tracks) {
        if (id == R.id.menu_load_track) {
            if (null == tracks || tracks.getNumSegments() == 0) {
                startIndividualTrackFileSelector();
            } else {
                SimpleDialog.of(activity).setTitle(R.string.map_load_track).setMessage(R.string.map_load_track_confirm).confirm((dialog, which) ->
                    startIndividualTrackFileSelector());
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

    private void startIndividualTrackFileSelector() {

        fileSelector.selectPersistableUri(PersistableUri.TRACK);
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
