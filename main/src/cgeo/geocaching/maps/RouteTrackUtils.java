package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.export.IndividualRouteExport;
import cgeo.geocaching.files.GPXIndividualRouteImporter;
import cgeo.geocaching.files.GPXTrackOrRouteImporter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.routing.RouteSortActivity;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.ContentStorageActivityHelper;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.PersistableUri;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.functions.Action2;
import cgeo.geocaching.utils.functions.Func0;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class RouteTrackUtils {

    private static final int REQUEST_SORT_INDIVIDUAL_ROUTE = 4712;
    private static final String STATE_CSAH_ROUTE = "csah_route";
    private static final String STATE_CSAH_TRACK = "csah_track";

    private final Activity activity;
    private final ContentStorageActivityHelper fileSelectorRoute;
    private final ContentStorageActivityHelper fileSelectorTrack;
    private View popup = null;

    private final Runnable reloadIndividualRoute;
    private final Runnable clearIndividualRoute;

    private final Route.UpdateRoute updateTracks;
    private final Route.CenterOnPosition centerOnPosition;

    private final Func0<Boolean> isTargetSet;

    public RouteTrackUtils(final Activity activity, final Bundle savedState, final Route.CenterOnPosition centerOnPosition, final Runnable clearIndividualRoute, final Runnable reloadIndividualRoute, final Route.UpdateRoute updateTracks, final Func0<Boolean> isTargetSet) {
        this.activity = activity;
        this.centerOnPosition = centerOnPosition;
        this.clearIndividualRoute = clearIndividualRoute;
        this.reloadIndividualRoute = reloadIndividualRoute;
        this.updateTracks = updateTracks;
        this.isTargetSet = isTargetSet;

        this.fileSelectorRoute = new ContentStorageActivityHelper(activity, savedState == null ? null : savedState.getBundle(STATE_CSAH_ROUTE))
            .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE, Uri.class, uri -> {
                if (uri != null) {
                    GPXIndividualRouteImporter.doImport(activity, uri);
                    reloadIndividualRoute.run();
                }
            });
        this.fileSelectorTrack = new ContentStorageActivityHelper(activity, savedState == null ? null : savedState.getBundle(STATE_CSAH_TRACK))
            .addSelectActionCallback(ContentStorageActivityHelper.SelectAction.SELECT_FILE_PERSISTED, PersistableUri.class, uri -> {
                if (uri != null && this.updateTracks != null) {
                    loadTracks(this.updateTracks, true);
                }
            });
    }

    /**
     * Shows a popup menu for individual route related items
     *
     */
    public void showPopup(final IndividualRoute individualRoute, final Route tracks, final Action2<Geopoint, String> setTarget) {
        this.popup = activity.getLayoutInflater().inflate(R.layout.routes_tracks_dialog, null);
        updateDialog(this.popup, individualRoute, tracks, setTarget);
        final BottomSheetDialog dialog = Dialogs.bottomSheetDialogWithActionbar(activity, this.popup, R.string.routes_tracks_dialog_title);
        dialog.setOnDismissListener(dialog1 -> popup = null);
        dialog.show();
    }

    private void updateDialog(final View dialog, final IndividualRoute individualRoute, final Route tracks, final Action2<Geopoint, String> setTarget) {
        updateDialogIndividualRoute(dialog, individualRoute, setTarget);
        updateDialogTracks(dialog, tracks);
        updateDialogClearTargets(dialog, individualRoute, setTarget);
    }

    private void updateDialogIndividualRoute(final View dialog, final IndividualRoute individualRoute, final Action2<Geopoint, String> setTarget) {
        if (dialog == null) {
            return;
        }
        dialog.findViewById(R.id.indivroute_load).setOnClickListener(v1 -> {
            if (null == individualRoute || individualRoute.getNumSegments() == 0) {
                startIndividualRouteFileSelector();
            } else {
                SimpleDialog.of(activity).setTitle(R.string.map_load_individual_route).setMessage(R.string.map_load_individual_route_confirm).confirm(
                    (d, w) -> startIndividualRouteFileSelector());
            }
        });

        if (isIndividualRouteVisible(individualRoute)) {
            dialog.findViewById(R.id.indivroute).setVisibility(View.VISIBLE);

            final View vSort = dialog.findViewById(R.id.item_sort);
            vSort.setVisibility(View.VISIBLE);
            vSort.setOnClickListener(v1 -> activity.startActivityForResult(new Intent(activity, RouteSortActivity.class), REQUEST_SORT_INDIVIDUAL_ROUTE));

            dialog.findViewById(R.id.item_center).setOnClickListener(v1 -> individualRoute.setCenter(centerOnPosition));
            dialog.findViewById(R.id.item_delete).setOnClickListener(v1 -> SimpleDialog.of(activity).setTitle(R.string.map_clear_individual_route).setMessage(R.string.map_clear_individual_route_confirm).confirm((d, w) -> {
                clearIndividualRoute.run();
                updateDialogIndividualRoute(dialog, individualRoute, setTarget);
            }));

            dialog.findViewById(R.id.indivroute_export_route).setOnClickListener(v1 -> new IndividualRouteExport(activity, individualRoute, false));
            dialog.findViewById(R.id.indivroute_export_track).setOnClickListener(v1 -> new IndividualRouteExport(activity, individualRoute, true));

            final CheckBox vAutoTarget = dialog.findViewById(R.id.auto_target);
            vAutoTarget.setChecked(Settings.isAutotargetIndividualRoute());
            vAutoTarget.setOnClickListener(v1 -> {
                setAutotargetIndividualRoute(activity, individualRoute, !Settings.isAutotargetIndividualRoute());
                updateDialogClearTargets(popup, individualRoute, setTarget);
            });
        } else {
            dialog.findViewById(R.id.indivroute).setVisibility(View.GONE);
        }
    }

    private void updateDialogTracks(final View dialog, final Route tracks) {
        if (dialog == null) {
            return;
        }
        dialog.findViewById(R.id.trackroute_load).setOnClickListener(v1 -> {
            if (null == tracks || tracks.getNumSegments() == 0) {
                startIndividualTrackFileSelector();
            } else {
                SimpleDialog.of(activity).setTitle(R.string.map_load_track).setMessage(R.string.map_load_track_confirm).confirm((d, w) ->
                    startIndividualTrackFileSelector());
            }
        });
        final LinearLayout tracklist = dialog.findViewById(R.id.tracklist);
        tracklist.removeAllViews();
        if (PersistableUri.TRACK.hasValue()) {
            final View vt = activity.getLayoutInflater().inflate(R.layout.routes_tracks_item, null);
            ((TextView) vt.findViewById(R.id.item_title)).setText(PersistableUri.TRACK.getUri().getLastPathSegment());
            vt.findViewById(R.id.item_center).setOnClickListener(v1 -> {
                if (null != tracks) {
                    tracks.setCenter(centerOnPosition);
                }
            });
            vt.findViewById(R.id.item_delete).setOnClickListener(v1 -> {
                ContentStorage.get().setPersistedDocumentUri(PersistableUri.TRACK, null);
                updateDialogTracks(dialog, null);
                updateTracks.updateRoute(null);
            });
            tracklist.addView(vt);
        }
    }

    private void updateDialogClearTargets(final View dialog, final IndividualRoute individualRoute, final Action2<Geopoint, String> setTarget) {
        final View vClearTargets = dialog.findViewById(R.id.clear_targets);
        vClearTargets.setEnabled(isTargetSet.call() || Settings.isAutotargetIndividualRoute());
        vClearTargets.setOnClickListener(v1 -> {
            if (setTarget != null) {
                setTarget.call(null, null);
            }
            Settings.setAutotargetIndividualRoute(false);
            individualRoute.triggerTargetUpdate(true);
            ActivityMixin.showToast(activity, R.string.map_manual_targets_cleared);
            updateDialogIndividualRoute(dialog, individualRoute, setTarget);
        });
    }

    private boolean isIndividualRouteVisible(final IndividualRoute route) {
        return route != null && route.getNumSegments() > 0;
    }

    private void startIndividualRouteFileSelector() {
        fileSelectorRoute.selectFile(null, PersistableFolder.GPX.getUri());
    }

    private void startIndividualTrackFileSelector() {
        fileSelectorTrack.selectPersistableUri(PersistableUri.TRACK);
    }

    public void loadTracks(final Route.UpdateRoute updateRoute, final boolean resetVisibilitySetting) {
        final Uri uri = PersistableUri.TRACK.getUri();
        if (null != uri) {
            GPXTrackOrRouteImporter.doImport(activity, PersistableUri.TRACK.getUri(), (route) -> {
                updateDialogTracks(popup, route);
                updateRoute.updateRoute(route);
            }, resetVisibilitySetting);
        }
    }

    public void showTrackInfo(final Route route) {
        if (null != route) {
            final int numPoints = route.getNumPoints();
            Toast.makeText(activity, activity.getResources().getQuantityString(R.plurals.load_track_success, numPoints, numPoints), Toast.LENGTH_SHORT).show();
        }
    }

    public void onPrepareOptionsMenu(final Menu menu, final View anchor, final IndividualRoute route) {
        anchor.setVisibility(isIndividualRouteVisible(route) ? View.VISIBLE : View.GONE);
    }

    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (fileSelectorRoute.onActivityResult(requestCode, resultCode, data)) {
            return true;
        }
        if (requestCode == REQUEST_SORT_INDIVIDUAL_ROUTE) {
            reloadIndividualRoute.run();
            return true;
        }
        return (fileSelectorTrack.onActivityResult(requestCode, resultCode, data));
    }

    public Bundle getState() {
        final Bundle bundle = new Bundle();
        bundle.putBundle(STATE_CSAH_ROUTE, this.fileSelectorRoute.getState());
        bundle.putBundle(STATE_CSAH_TRACK, this.fileSelectorTrack.getState());
        return bundle;
    }

    public static void setAutotargetIndividualRoute(final Activity activity, final IndividualRoute route, final boolean newValue) {
        Settings.setAutotargetIndividualRoute(newValue);
        route.triggerTargetUpdate(!Settings.isAutotargetIndividualRoute());
        ActivityMixin.invalidateOptionsMenu(activity);
    }
}
